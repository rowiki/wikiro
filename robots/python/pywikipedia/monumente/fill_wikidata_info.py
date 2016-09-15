#!/usr/bin/python
# -:- coding: utf8 -:-

#
# (C) Strainu 2016
#
# Distributed under the terms of the MIT license.
#
import pywikibot
from pywikibot import i18n, config, pagegenerators, textlib, weblib

import sys
import csv
import json
sys.path.append("wikiro/robots/python/pywikipedia")
import strainu_functions as sf
from wikidata import robot_romania as robot

    
class MonumentsData(robot.WorkItem):
    def __init__(self, db):
        self.db = {}
	for monument in db:
	    if monument["Denumire"].find("[[") < 0:
	        continue
            name = sf.extractLink(monument["Denumire"])
            self.db[name] = monument	
        self.always = False

    def userConfirm(self, question):
        """Obtain user response."""
        if self.always:
            return True

        choice = pywikibot.input_choice(question,
                                        [('Yes', 'y'),
                                         ('No', 'N'),
                                         ('Always', 'a')],
                                        default='N')

        if choice == 'n':
            return False
            
        if choice == 'a':
            self.always = True
            
        return True

    def updateWikidata(self, item, data):
        try:
            if "P1770" in item.claims:
                if str(item.claims["P1770"][0].getTarget()) != str(data["Cod"]):
                    print item.claims["P1770"][0].getTarget()
                    print data["Cod"]
            else:
                lmi = pywikibot.Claim(item.repo, u'P1770', datatype='external-id')#LMI
                lmi.setTarget(data["Cod"])
                answer = self.userConfirm("Update element %s with LMI code %s?" % (item.labels['ro'], data["Cod"]))
                if answer:
                    item.addClaim(lmi)
        except Exception as e:
            print e
            print u"Could not update " + item.labels['ro']

    def doWork(self, page, item):
        try:
            if page.title() not in self.db:
                print u"Could not find article" + page.title()
            self.updateWikidata(item, self.db[page.title()])
        except Exception as e:
            print e
            print u"Failed to update monument data to Wikidata, skipping..."
        
    def description(self):
        return u"Updating monument data to wikidata"

def readJson(filename, what):
	try:
		f = open(filename, "r+")
		pywikibot.output("Reading " + what + " file...")
		db = json.load(f)
		pywikibot.output("...done")
		f.close()
		return db
	except IOError:
		pywikibot.output("Failed to read " + filename + ". Trying to do without it.")
		return {}

if __name__ == "__main__":
    _lang = "ro"
    _db = "lmi"
    db_json = readJson("_".join(filter(None, [_lang, _db, "db.json"])), "database")
    page = pywikibot.Page(pywikibot.Site(), "Format:codLMI", ns=10)
    generator = pagegenerators.ReferringPageGenerator(page, onlyTemplateInclusion=True)
    bot = robot.WikidataBot(site=True, generator = generator)

    bot.workers.append(MonumentsData(db_json))
    bot.run()
