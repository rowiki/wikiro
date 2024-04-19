#!/usr/bin/python
# -*- coding: utf-8  -*-

import sys, json
sys.path.append("..")
import pywikibot
from pywikibot import pagegenerators
from pywikibot import config as user
'''
This script creates RAN links in the Cod namespace on ro.wp basted on 
the monument database.

See [[:ro:Wikipedia:Coduri]] (ro) for details on the Cod namespace.
'''

def main():
    f = open("ro_ran_db.json", "r+")
    pywikibot.output("Reading database file...")
    db = json.load(f)
    pywikibot.output("...done")
    f.close();
    
    #f = open("ro_pages.json", "r+")
    #pywikibot.output("Reading articles file...")
    #pages_ro = json.load(f)
    #pywikibot.output("...done")
    #f.close();
    
    site = pywikibot.Site()
    
    #for code in pages_ro:
    #    page = pywikibot.Page(site, u"Cod:RAN:" + code + "/articol")
    #    #pywikibot.output(page.title())
    #    if page.exists() and not page.isRedirectPage():
    #            pywikibot.output(u"Page %s is not a redirect" % page.title())
    #    else:
    #    #if not page.exists():
    #        page.put(u"#redirecteaza[[%s]]" % pages_ro[code][0]["name"], "Redirecting code to the Wikipedia article")
            
    for monument in db:
        #if not monument["Cod"] in pages_ro:
        #    continue
        page = pywikibot.Page(site, u"Cod:RAN:RO:" + monument["Cod"])
        pywikibot.output(u"Cod:RAN:RO:" + monument["Cod"])
        if page.exists():
            continue

        source_page = monument["source"][monument["source"].find(u'=')+1:monument["source"].find(u'&')]
        pywikibot.output(source_page)
        source_page = pywikibot.Page(site, source_page)
        page_text = u"#redirect [[{0}#{1}]]".format(source_page.title(), monument["Cod"])
        #print page_text
        if not page.exists() or page.get(False, True) != page_text:
            page.put(page_text, "Redirecting code to the Wikipedia list")

if __name__ == "__main__":
    try:
        main()
    finally:
        pywikibot.stopme()

