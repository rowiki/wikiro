#!/usr/bin/python
# -*- coding: utf-8  -*-
"""
This is a Bot written by Strainu to obtain all elements with a "wikipedia:" tag
from a given area in OSM, then add a {{coord}} template to that Wikipedia article
(if it doesn't already exist)

The following command line parameters are supported:

-bbox               The are in which to search for elementsin the format longmin,latmin,longmax,latmax

-summary            Define the summary to use

-lang               Replace the language you are working on with another language in searching for wiki links

--- Example ---
1.
# This is a script to add a {{coord}} template to the top of the 
# pages with a wikipedia= or wikipedia:ro= entry in OSM in Romania
# Warning! Put it in one line, otherwise it won't work correctly.

python osm2wiki.py -bbox:20.2148438,43.7710938,29.7729492,48.1147666 -summary:"Bot: Adding a coordinate" -lang:ro


"""

#
# (C) Strainu, 2010
#
# Distributed under the terms of the MIT license.
#
__version__ = '$Id: osm2wiki.py 8448 2010-08-24 08:25:57Z xqt $'
#

import re, pagegenerators, urllib2, urllib
import wikipedia as pywikibot
import codecs, config
import xml.dom.minidom
import math

msg = {
    'ar': u'بوت: إضافة %s',
    'cs': u'Robot přidal souřadnice %s',
    'de': u'Bot: "%s" hinzugefügt',
    'en': u'Bot: Adding coordinates: %s',
    'fr': u'Robot : Ajoute des coordonees: %s',
    'he': u'בוט: מוסיף %s',
    'fa': u'ربات: افزودن %s',
    'it': u'Bot: Aggiungo %s',
    'ja': u'ロボットによる: 追加 %s',
    'ksh': u'Bot: dobeijedonn: %s',
    'nds': u'Bot: tofoiegt: %s',
    'nn': u'Robot: La til %s',
    'pdc': u'Waddefresser: %s dezu geduh',
    'pl': u'Robot dodaje: %s',
    'pt': u'Bot: Adicionando %s',
    'ro': u'Robot: Adaug coordonate: %s',
    'ru': u'Бот: добавление %s',
    'sv': u'Bot: Lägger till %s',
    'szl': u'Bot dodowo: %s',
    'vo': u'Bot: Läükon vödemi: %s',
    'zh': u'機器人: 正在新增 %s',
    }
    

# Useful for the untagged function
def pageText(url):
    """ Function to load HTML text of a URL """
    try:
        request = urllib2.Request(url)
        request.add_header("User-Agent", pywikibot.useragent)
        response = urllib2.urlopen(request)
        text = response.read()
        response.close()
        # When you load to many users, urllib2 can give this error.
    except urllib2.HTTPError:
        pywikibot.output(u"Server error. Pausing for 10 seconds... " + time.strftime("%d %b %Y %H:%M:%S (UTC)", time.gmtime()) )
        response.close()
        time.sleep(10)
        return pageText(url)
    return text
    
def getTitleByInterwiki(url, srcLang, dstLang):
    srcSite = pywikibot.getSite(srcLang)
    srcPage = pywikibot.Page(srcSite, url)
    srcText = srcPage.get()
    dict = pywikibot.getLanguageLinks(srcText, srcSite)
    dstSite = pywikibot.getSite(dstLang)
    if dstSite in dict:
        return dict[dstSite].title()
    else:
        return None
    
def getPageListFromUrl(url, destLang, lang):
    pages = []
    xmlText = pageText(url)
    wikitag = "wikipedia"
    if lang != None:
        wikitag = wikitag + ":" + lang
    #pywikibot.output(unicode(xmlText, "utf8"))
    try:
        document = xml.dom.minidom.parseString(xmlText)
        nodes = document.getElementsByTagName("node")
        for node in nodes:
            lat = node.getAttribute("lat")
            lon = node.getAttribute("lon")
            for tag in node.childNodes:
                if tag.nodeType != tag.ELEMENT_NODE:
                    continue
                if tag.tagName != "tag" or tag.hasAttribute('k') == False or tag.hasAttribute('v') == False:
                    continue
                if tag.getAttribute('k') != wikitag:
                    continue
                wikiUrl = tag.getAttribute('v')
                idLang = lang # variable used for get interwiki links
                pywikibot.output("\n")
                
                if lang == None:
                    l = "[a-z]+"
                else:
                    l = lang
                
                regexp  = re.match(u"http://(%s).wikipedia.org/wiki/(.*)" % l, wikiUrl);#wikipedia=URL
                regexp2  = re.match(u"(%s):(.*)" % l, wikiUrl);#wikipedia=lang:title
                
                if regexp == None and regexp2:
                    regexp = regexp2
                if regexp:
                    if lang == None:
                        #pywikibot.output("We identified a new language: %s from url %s" % (regexp.group(1), regexp.group(2)))
                        idLang = regexp.group(1)
                    wikiUrl = regexp.group(2)
                else:#wikipedia=title
                    #this means unexpected language
                    if re.search(u"wikipedia.org", wikiUrl) or re.search(u":", wikiUrl):
                        pywikibot.output(u"Unexpected value as wikipedia article: %s" % wikiUrl)
                        continue
                    #Simple title
                    elif lang == None:
                            #we must not make any assumptions about the language
                            #in the wikipedia=title case
                            pywikibot.output(u"Unknown language for wikipedia article: %s" % wikiUrl)
                            continue
                if idLang == destLang:
                    wikiTitle = wikiUrl
                else:
                    wikiTitle = getTitleByInterwiki(wikiUrl, idLang, destLang)
                    if wikiTitle == None:
                        pywikibot.output(u"Could not find equivalent for wikipedia article: %s" % wikiUrl)
                        continue
                pywikibot.output(u"New article identified: " + wikiTitle)
                if pages:
                    pages = pages + [(wikiTitle, lat, lon)]
                else:
                    pages = [(wikiTitle, lat, lon)]
    except Exception as inst:
        pywikibot.output(u"Unknown error, exiting: %s" % inst);
    #print pages
    return pages
    
def getPageList(bbox, lang):
    """ Function to get the OSM elements with the searched parameters """
    pywikibot.output(u"Fetching nodes linking to %s.wikipedia.org in area: %s" % (lang,bbox));
    urlHead = "http://osmxapi.hypercube.telascience.org/api/0.6/node[wikipedia"
    pages = []
    url = urlHead+ "=*][bbox=" + bbox + "]"
    pywikibot.output(u"URL: %s" % url)
    pages = getPageListFromUrl(url, lang, None)
    for l in ["bg", "de", "en", "fr", "ro", "sr", "uk"]:
        pywikibot.output("\n")
        url = urlHead+ ":" + l + "=*][bbox=" + bbox + "]"
        pywikibot.output(u"URL: %s" % url)
        pages = pages + getPageListFromUrl(url, lang, l)
    return pages
    
def getDeg(decimal):
    if decimal < 0:
        decimal = -decimal
    return int(math.floor(decimal))
    
def getMin(decimal):
    if decimal < 0:
        decimal = -decimal
    return int(math.floor((decimal - math.floor(decimal)) * 60))
    
def getSec(decimal):
    if decimal < 0:
        decimal = -decimal
    return int(math.floor(((decimal - math.floor(decimal)) * 3600) % 60))

def putCoordOnWiki(lang, pages):
    site = pywikibot.getSite(lang)
    for page in pages:
        if page == None:
            continue
        pywikibot.output("\n")
        pywikibot.output("Parsing page: %s, %f, %f" % (page[0], float(page[1]), float(page[2])))
        generator = pywikibot.Page(site, page[0])
        if generator.isRedirectPage():
            generator = generator.getRedirectTarget()
        html = site.getUrl( "/wiki/" + generator.urlname(), True)
        #pywikibot.output(html)
        if re.search("geohack", html):
            pywikibot.output("Page %s already has some coordinates. Please check them manually!" % page[0])
            continue
        text = generator.get()
        lat = float(page[1])
        lon = float(page[2])
        template = "{{Coord|" + unicode(getDeg(lat)) + "|" + unicode(getMin(lat)) + "|" + unicode(getSec(lat)) + "|"
        if lat > 0:
            template = template + "N|"
        else:
            template = template + "S|"
            
        template = template + unicode(getDeg(lon)) + "|" +  unicode(getMin(lon)) + "|" +  unicode(getSec(lon)) + "|"
        if lon > 0:
            template = template + "E|display=title}}"
        else:
            template = template + "V|display=title}}"
        pywikibot.output(template)
        
        comment = pywikibot.translate(site, msg) % template
        generator.put(template + "\n" + text, comment)

def main():
    bbox="20.2148438,43.7710938,29.7729492,48.1147666" #default to Romania
    summary=None
    lang=pywikibot.getSite().language()
    # Loading the arguments
    for arg in pywikibot.handleArgs():
        if arg.startswith('-bbox'):
            if len(arg) == 5:
                bbox = pywikibot.input(
                    u'Please input the area to search for tagged nodes:')
            else:
                bbox = arg[6:]
        elif arg.startswith('-summary'):
            if len(arg) == 8:
                summary = pywikibot.input(u'What summary do you want to use?')
            else:
                summary = arg[9:]
        elif arg.startswith('-lang'):
            if len(arg) == 5:
                lang = pywikibot.input(u'What language do you want to use?')
            else:
                lang = arg[6:]
        
    pages = getPageList(bbox, lang)
    putCoordOnWiki(lang, pages)

if __name__ == "__main__":
    try:
        main()
    finally:
        pywikibot.stopme()
