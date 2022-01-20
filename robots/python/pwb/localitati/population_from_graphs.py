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
sys.path.append("wikiro/robots/python/pwb")
import strainu_functions as sf
from wikidata import robot_romania as robot

    
class PopulationDataCSV(robot.WorkItem):
    def __init__(self, template):
        self.censusItems = {
            1859: {"q": "Q22704065", "name": u"Recensământul populației din 1859-1860", "month": None, "day": None},
            1860: {"q": "Q22704065", "name": u"Recensământul populației din 1859-1860", "month": None, "day": None}, #same as above
            1899: {"q": "Q22704088", "name": u"Recensământul populației din 1899", "month": None, "day": None},
            1912: {"q": "Q22704095", "name": u"Recensământul General al Populației din 1912", "month": None, "day": None},
            1930: {"q": "Q12739150", "name": u"Recensământul populației din 1859-1860", "month": None, "day": None},
            1941: {"q": "Q22704099", "name": u"Recensământul General al României", "month": None, "day": None},
            1948: {"q": "Q22704103", "name": u"Recensământul populației din ianuarie 1948", "month": 1, "day": None},
            1956: {"q": "Q22704106", "name": u"Recensământul populației din februarie 1956", "month": 2, "day": None},
            1966: {"q": "Q22704111", "name": u"Recensământul populației și locuințelor din martie 1966", "month": 3, "day": None},
            1977: {"q": "Q22704114", "name": u"Recensământul populației și locuințelor din anul 1977", "month": 1, "day": None},
            1992: {"q": "Q22704118", "name": u"Recensământul populației și locuințelor din anul 1992", "month": 1, "day": None},
            2002: {"q": "Q4350762", "name": u"Recensământul populației și locuințelor din anul 2002", "month": 3, "day": None},
            2011: {"q": "Q12181933", "name": u"Recensământul populației și locuințelor din anul 2011", "month": 10, "day": 31},
        }
        self.template = template
        self.always = False

    def validateCensus(self, year):
        year = int(year)
        if year in self.censusItems:
            return True
        print str(year) + u" was not a census year."
        return False

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

    def updateWikidata(self, item, year, pop):
        try:
            census_date = pywikibot.WbTime(year=year, month=self.censusItems[year]["month"], day=self.censusItems[year]["day"])
            if "P1082" in item.claims:
                claims = item.claims["P1082"]
                for claim in claims:
                    #print claim.qualifiers
                    if claim.has_qualifier('P585', census_date):
                        print item.labels['ro'] + u" already has population for " + str(year)
                        return

            yearclaim = pywikibot.Claim(item.repo, u'P1082') #Population
            target = pywikibot.WbQuantity(amount=pop)
            yearclaim.setTarget(target)

            date = pywikibot.Claim(item.repo, u'P585')#Date of publication
            date.setTarget(census_date)

            method = pywikibot.Claim(item.repo, u'P459')#Method
            target = pywikibot.ItemPage(item.repo, u"Q39825")
            method.setTarget(target)
            method2 = pywikibot.Claim(item.repo, u'P459')#Method
            target = pywikibot.ItemPage(item.repo, u"Q747810")
            method2.setTarget(target)
            method3 = pywikibot.Claim(item.repo, u'P459')#Method
            target = pywikibot.ItemPage(item.repo, self.censusItems[year]["q"])
            method3.setTarget(target)

            #criteria = pywikibot.Claim(item.repo, u'P1013')#Criteria
            #target = pywikibot.ItemPage(item.repo, u"Q15917105")
            #criteria.setTarget(target)

            answer = self.userConfirm("Update element %s with population for year %d?" % (item.labels['ro'], year))
            if answer:
                item.addClaim(yearclaim)
                yearclaim.addQualifier(date)
                yearclaim.addQualifier(method)
                yearclaim.addQualifier(method2)
                yearclaim.addQualifier(method3)
                #yearclaim.addQualifier(criteria)
                print "Updated element %s for year %d" % (item.labels['ro'], year)
            else:
                pass
        except Exception as e:
            print e
            print u"Could not update " + item.labels['ro']

    def doWork(self, page, item):
        try:
            print self.description() + " for " + page.title()
            tpl = sf.tl2Dict(sf.extractTemplate(page.get(), self.template))[0]
            csvTitle = self.template + u"/" + tpl[1]
            csvPage = pywikibot.Page(pywikibot.Site(), csvTitle, ns=10)
            text = csvPage.get()
            reader = csv.reader(text.split('\n')[1:], delimiter=',')
            for row in reader:
                if self.validateCensus(row[0]):
                    self.updateWikidata(item, int(row[0]), row[1])
        except Exception as e:
            print e
            print u"Failed to update population data to Wikidata, skipping..."
        
    def description(self):
        return u"Getting population data from demography graphs (CSV)"


def processGraphs():
    page = pywikibot.Page(pywikibot.Site(), "Format:Grafic demografie", ns=10)
    generator = pagegenerators.ReferringPageGenerator(page, onlyTemplateInclusion=True)
    bot = robot.WikidataBot(site=True, generator = generator)

    bot.workers.append(PopulationDataCSV("Grafic demografie"))
    #bot.workers.append(InfoboxData(page, item))
    bot.run()

if __name__ == "__main__":
    processGraphs()
