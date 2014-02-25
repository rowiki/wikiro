#!/usr/bin/python
# -*- coding: utf-8  -*-

import sys, json
import sirutalib
import string
sys.path.append("..")
import strainu_functions as strainu
import wikipedia
'''
This script creates SIRUTA links in the Cod namespace on ro.wp basted on 
the SIRUTA database.

See [[Wikipedia:Coduri]] (ro) for details on the Cod namespace.

The script uses SIRUTAlib: https://github.com/strainu/SIRUTA
'''


def main():
    siruta = sirutalib.SirutaDatabase()
    
    site = wikipedia.getSite()
    
    list_ = siruta.get_siruta_list()
    low = [u" DE ", u" CEL ", u" LA ", u" SUB ", u" DIN ", u" LUI ", u" CU ", u" PE ", u" IN "]
    up = [u" II "]
    for code in list_:
        page = wikipedia.Page(site, u"Cod:SIRUTA:" + str(code))
        name = siruta.get_name(code,prefix=False)
        if name <> None:
            name = strainu.capitalizeWithSigns(name, keep=low + up)
            for word in low:
                name = name.replace(word, string.lower(word))
        commune = siruta.get_sup_name(code,prefix=False)
        if commune <> None:
            commune = strainu.capitalizeWithSigns(commune, keep=low + up)
            for word in low:
                commune = commune.replace(word, string.lower(word))
        county = siruta.get_county_string(code, prefix=False)
        if county <> None:
            county = strainu.capitalizeWithSigns(county)
        tip = siruta.get_type(code)
        if tip == 3:
            target = u"Comuna %s, %s" % (name, county)
            tpage = wikipedia.Page(site, target)
            if not tpage.exists():
                print u"Wrong commune " + target
            else:
                pass#wikipedia.output(page.title() + target)
        elif tip == 1 or tip == 2 or tip == 4 or tip == 5:
            #needs to be a city
            target = name
            tpage = wikipedia.Page(site, target)
            if not tpage.exists():
                target = u"%s, %s" % (name, county) 
                tpage = wikipedia.Page(site, target)
                if not tpage.exists():
                    target = u"%s (oraș)" % name 
                    tpage = wikipedia.Page(site, target)
                    if not tpage.exists():
                        print u"Wrong 0 city " + target
                    else:
                        if tpage.isRedirectPage() == True:
                            tpage = tpage.getRedirectTarget()
                        cats = tpage.categories()
                        found = 0
                        for cat in cats:
                            t = cat.title()
                            if t == u"Categorie:Orașe în România" or \
                                t == u"Categorie:Municipii în România" or \
                                t == u"Categorie:" + tpage.title() or \
                                t == u"Categorie:Orașe în județul " + county:
                                pass#wikipedia.output(page.title() + target)
                                found = 1
                                break
                        if found == 0:
                            print u"Wrong 1 city " + target
                else:
                    if tpage.isRedirectPage() == True:
                        tpage = tpage.getRedirectTarget()
                    cats = tpage.categories()
                    found = 0
                    for cat in cats:
                        t = cat.title()
                        if t == u"Categorie:Orașe în România" or \
                            t == u"Categorie:Municipii în România" or \
                            t == u"Categorie:" + tpage.title() or \
                            t == u"Categorie:Orașe în județul " + county:
                            pass#wikipedia.output(page.title() + target)
                            found = 1
                            break
                    if found == 0:
                        print u"Wrong 2 city " + target
            else:
                if tpage.isRedirectPage() == True:
                    #print tpage.title()
                    tpage = tpage.getRedirectTarget()
                    #print tpage.title()
                cats = tpage.categories()
                found = 0
                for cat in cats:
                    t = cat.title()
                    if t == u"Categorie:Orașe în România" or \
                            t == u"Categorie:Municipii în România" or \
                            t == u"Categorie:" + tpage.title() or \
                            t == u"Categorie:Orașe în județul " + county:
                        pass#wikipedia.output(page.title() + target)
                        found = 1
                        break
                if found == 0:
                    print u"Wrong 3 city " + target
        elif tip == 6 or tip == 9 or tip == 17:
            pass
        elif tip == 40:
            target = u"Județul " + name
            tpage = wikipedia.Page(site, target)
            if not tpage.exists():
                print u"Wrong county " + target
            else:
                if strainu.extractTemplate(tpage.get(), u"NUTS-Ro") == None:
                    print u"Wrong county article " + target
                else:
                    pass#wikipedia.output(page.title() + target)
        else:
            target = u"%s (%s), %s" % (name, commune, county) 
            tpage = wikipedia.Page(site, target)
            if not tpage.exists():
                target = u"%s, %s" % (name, county) 
                tpage = wikipedia.Page(site, target)
                if not tpage.exists():
                    print u"Wrong village %s (%s), commune %s" % (target, page.title(), commune)
                else:
                    pass#wikipedia.output(page.title() + target)
            else:
                pass#wikipedia.output(page.title() + target)
        #if page.exists() and not page.isRedirect():
        #       wikipedia.output(u"Page %s is not a redirect" % page.title())
        #else:
        #page.put(u"#redirect [[%s]]" % pages_ro[code][0]["name"], "Redirecting code to the Wikipedia article")
if __name__ == "__main__":
    try:
        main()
    finally:
        wikipedia.stopme()

