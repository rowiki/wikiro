#!/usr/bin/python

import pywikibot
import csv

def checkPage(title):
    page = pywikibot.Page(pywikibot.Site(), title)
    if page.exists():
        return page
    return None

def processLine(county, oldName, oldCommune, newName, newCommune):
    redirect = checkPage(", ".join([oldName, county]))
    if redirect:
        pywikibot.output("%s already exists" % redirect.title())
        return
    title = checkPage("%s (%s), %s" % (newName, newCommune, county))
    if not title:
    	title = checkPage(", ".join([newName, county]))
    if not title:
    	title = checkPage(newName)
    if not title:
        pywikibot.output("%s does not exist" % newName)
        return
    redirect = pywikibot.Page(pywikibot.Site(), ", ".join([oldName, county]))
    text = u"#redirect [[%s]]" % title.title()
    desc = u"Redirectez satul %s spre satul cu care a fost unit prin legea 2/1968 ([[%s]]) #satedisparute1968" % (redirect.title(), title.title())
    pywikibot.output(desc)
    redirect.put(text, desc)

    if oldCommune != newCommune:
        redirect = checkPage("Comuna %s, %s" % (oldCommune, county))
        title = checkPage("Comuna %s, %s" % (newCommune, county))
        if not title or redirect:
            pywikibot.output("Cannot redirect coomune")
            return
        redirect = pywikibot.Page(pywikibot.Site(), "Comuna %s, %s" % (oldCommune, county))
        text = u"#redirect [[%s]]" % title.title()
        desc = u"Redirectez %s spre comuna cu care a fost unită prin legea 2/1968 ([[%s]]) #satedisparute1968" % (redirect.title(), title.title())
        pywikibot.output(desc)
        redirect.put(text, desc)

def main(csvfile):
    with open(csvfile, 'r') as csvFile:
        csv_reader = csv.reader(csvFile)
        for row in csv_reader:
            processLine(*row)

if __name__ == u"__main__":
    main("sate_desfiintate.csv")
    main("sate_redenumite.csv")
