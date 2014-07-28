#!/usr/bin/python
# -*- coding: utf-8  -*-
'''
Search for a text using the Europeana API, then check return all the entries as JSON

Additionally, expose some functions that can be used to process the results
'''
import urllib
import urllib2
import json
import re

class EuropeanaInterface:
    def __init__(self, newkey='oYfxfaNeP'):
        self._key = newkey
        self._rows = 100
        
    def _getSlice(self, text, start, _type):
        url = "http://europeana.eu/api/v2/search.json?wskey={0}&query={1}&qf=TYPE:{2}&start={3}&rows={4}&profile=minimal&reusability=open"
        actual_url = url.format(self._key, urllib.quote_plus(text), _type, start, self._rows)
        #print actual_url
        response = urllib2.urlopen(actual_url)
        txt = response.read()
        return json.loads(txt)
        
    def searchEuropeana(self, text, filterCallback, filterField):
        next = 1
        total = 2
        items = []
        while next < total:
            #TODO: allow non-text search
            _slice = self._getSlice(text, next, "TEXT")
            if _slice == None:
                return []
            if "success" not in _slice or _slice["success"] != True:
                return []

            #items.extend(_slice["items"])
            for elem in _slice["items"]:
                if filterField not in elem or filterCallback(elem[filterField]):
                    items.append(elem)
            #print len(items)
            total = _slice["totalResults"]
            #print total
            count = _slice["itemsCount"]
            #print count
            #print json.dumps(items, indent=2)
            next += count

        return items
    
    def getItem(self, url):
        response = urllib2.urlopen(url)
        txt = response.read()
        return json.loads(txt)

def filterLMI(_id):
    lmiregex = re.compile("(([a-z]{1,2})_(i|ii|iii|iv)_([a-z])_([a-z])_([0-9]{5}(\.[0-9]{2,3})?))", re.I)
    if re.search(lmiregex, _id):
        return True
    else:
        return False

if __name__ == "__main__":
    lmiregex = re.compile("(([a-z]{1,2})_(i|ii|iii|iv)_([a-z])_([a-z])_([0-9]{5}(\.[0-9]{2,3})?))", re.I)
    robot = EuropeanaInterface()
    result = {}
    for elem in robot.searchEuropeana("Institutul Național al Patrimoniului", filterLMI, "id"):
        if "link" not in elem:
            continue
            
        text = robot.getItem(elem["link"])
        proxies = text["object"]["proxies"]
        url = text["object"]["europeanaAggregation"]["edmLandingPage"]
        lmi = re.search(lmiregex, elem["id"]).group(0).replace("_", "-")
        descr = ""
        for proxy in proxies:
            if proxy["about"].find("provider") == -1:
                continue
            descr = proxy["dcDescription"]["ro"][0]
            break
        
        if descr == "" or \
            descr.find(u"de adăugat") > -1:
            continue
            
        result[lmi] = {
            'cod': lmi,
            'url': url,
            'descr': descr
	}
        print lmi.encode("utf8") + ",\"" + descr.encode("utf8") + "\",\"" + urllib.quote_plus(url) + "\""
        

    f = open("europeana_monuments.json", "w+")
    json.dump(result, f)
    f.close()

