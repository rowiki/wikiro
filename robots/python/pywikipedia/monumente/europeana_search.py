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
#'oYfxfaNeP'
#'UTTpcWM7L'
class EuropeanaInterface:
    def __init__(self, newkey='oYfxfaNeP'):
        self._key = newkey
        self._rows = 100
        self._cursor = '*'
        
    def _getSlice(self, text, start, _type):
        url = "http://europeana.eu/api/v2/search.json?wskey={0}&query={1}&qf=TYPE:{2}&cursor={3}&rows={4}&profile=minimal&reusability=open"
        actual_url = url.format(self._key, urllib.quote_plus(text), _type, urllib.quote_plus(self._cursor), self._rows)
        print actual_url
        response = urllib2.urlopen(actual_url)
        txt = response.read()
        return json.loads(txt)
        
    def searchEuropeana(self, text, filterCallback=None, filterField=None):
        next = 1
        total = 2
        items = []
        while next < total:
            #TODO: allow non-text search
            _slice = self._getSlice(text, next, "TEXT")
            #print _slice
            if _slice == None:
                return []
            if "success" not in _slice or _slice["success"] != True:
                return []
            if "nextCursor" in _slice:
                self._cursor = _slice["nextCursor"]
                print self._cursor
            else:
                break

            #items.extend(_slice["items"])
            for elem in _slice["items"]:
                if filterField not in elem or filterCallback == None or filterCallback(elem[filterField]):
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
    #print _id
    return True
    lmiregex = re.compile("(([a-z]{1,2})_(i|ii|iii|iv)_([a-z])_([a-z])_([0-9]{5}(\.[0-9]{2,3})?))", re.I)
    if re.search(lmiregex, _id):
        return True
    else:
        return False

if __name__ == "__main__":
    lmiregex = re.compile("(([a-z]{1,2})([_-])(i|ii|iii|iv)([_-])([a-z])([_-])([a-z])([_-])([0-9]{5}([\._-][0-9]{2,3})?))", re.I)
    robot = EuropeanaInterface()
    result = {}
    no = 0
    tot = 0
    f = open("europeana_monuments.json", "w+")
    try:
        f2 = open("europeana_search.json", "r")
        search_results = json.load(f)
    except Exception:
        search_results = robot.searchEuropeana("România", filterLMI, "id")
        f2 = open("europeana_search.json", "w+")
        json.dump(search_results, f2, indent=2)
    #for elem in robot.searchEuropeana("Institutul Național al Patrimoniului", filterLMI, "id"):
    for elem in search_results:
        tot += 1
        print "Total: " + str(tot)
        if "link" not in elem:
            continue

        text = robot.getItem(elem["link"])
        proxies = text["object"]["proxies"]

        for proxy in proxies:
            if proxy["about"].find("provider") != -1:
                break
        else:
            continue
        url = text["object"]["europeanaAggregation"]["edmLandingPage"]

        #print elem["id"]
        lmi = re.search(lmiregex, elem["id"])
        if lmi:
            lmi = lmi.group(0).replace("_", "-")#TODO: the replace does not work for submonuments
        else:
            relations = []
            if "edmIsRelatedTo" in proxy:
                relations = proxy["edmIsRelatedTo"]["def"]
            print relations
            for relation in relations:
                lmi = re.search(lmiregex, relation)
                if lmi:
                    lmi = lmi.group(0).replace("_", "-")
                    break
                    
        if not lmi:
            continue
        else:
            lmi = re.sub(r'-([0-9]{2,3})$', '.\g<1>', lmi)

        descr = ""
        if "dcDescription" in proxy:
            descr = proxy["dcDescription"]["ro"][0]

        if descr == None or \
            descr == "" or \
            descr.find(u"de adăugat") > -1 or \
            descr.find(u"imagine a monumentului") > -1:
            continue
            
        if lmi not in result:
            result[lmi] = {
                'cod': lmi,
                'url': url,
                'descr': descr
            }
        else:
            result[lmi + "_duplicate"] = {
                'cod': lmi,
                'url': url,
                'descr': descr
            }
        no += 1
        print result[lmi]
        print "Useful: " + str(no)
        f.seek(0)
        json.dump(result, f, indent=2)
        

    f.close()

