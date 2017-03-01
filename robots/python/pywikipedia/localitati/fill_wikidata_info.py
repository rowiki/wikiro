#!/usr/bin/python
# -:- coding: utf8 -:-

#
# (C) Strainu 2016
#
# Distributed under the terms of the MIT license.
#
import pywikibot
from pywikibot import i18n, config, pagegenerators, textlib, weblib
from pywikibot.data import sparql
from pywikibot import config as user

import sys
import csv
import json

import sirutalib

sys.path.append("wikiro/robots/python/pywikipedia")
import strainu_functions as sf
from wikidata import robot_romania as robot
import postal_codes

config = {
    'properties': {
            u'Denumire': ('', None, 'label'),
            u'Coord': ('P625', False, 'globe-coordinate'),
            u'imagine': ('P18', True, 'commonsMedia'),
            u'hartă': ('P242', False, 'commonsMedia'),
            u'Țară': ('P17', False, 'wikibase-item'),
            u'Commons': ('P373', False, 'string'),
            u'SIRUTA': ('P843', False, 'string'),
            u'este un/o': ('P31', False, 'wikibase-item'),
            u'subdiviziuni': ('P150', False, 'wikibase-item'),
            u'localități componente': ('P1383', False, 'wikibase-item'),
            u'fus orar': ('P421', False, 'wikibase-item'),
            u'primar': ('P6', False, 'wikibase-item'),
            u'codp': ('P281', False, 'string'),
            u'SIRUTASUP': ('P131', False, 'wikibase-item'),
            u'ISO3166-2': ('P300', False, 'string'),
            u'populație': ('P1082', False, 'quantity'),
            u'reședință pentru': ('P1376', False, 'wikibase-item'),
            u'site': ('P856', False, 'url'),
            }
}

def sortFromName(self, name):
    return name.replace(u"ș", u"sș").\
                replace(u"ț", u"tț").\
                replace(u"Ș", u"SȘ").\
                replace(u"Ț", u"TȚ").\
                replace(u"ă", u"aă").\
                replace(u"Ă", u"AĂ").\
                replace(u"â", u"aâ").\
                replace(u"Â", u"AÂ").\
                replace(u"î", u"iî").\
                replace(u"Î", u"IÎ")

class ItemProcessing:
    def __init__(self, config, item, siruta=None, always=False):
        self.always = always
        self.config = config
        self.item = item
        item.get()
        self.label = self.extractLabel()
        self.sirutaDb = siruta# or sirutalib.SirutaDatabase()

    def extractLabel(self):
        if 'ro' in self.item.labels:
            return self.item.labels['ro']
        if 'en' in self.item.labels:
            return self.item.labels['en']

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

    def updateProperty(self, key, data, force=False):
        try:
            prop, pref, datatype = self.config["properties"][key]
            if prop in self.item.claims and not force:
                #don't bother about those yet
                pywikibot.output(u"Wikidata already has %s: %s" % (key, self.item.claims[prop][0].getTarget()))
                pass
            else:
                if datatype == 'wikibase-item':
                    val = pywikibot.ItemPage(pywikibot.Site(), data[key])
                    desc = val.title()
                elif datatype == 'globe-coordinate':
                    val = pywikibot.Coordinate(lat=float(data[key][0]),
                                         lon=float(data[key][1]),
                                         globe='earth',
                                         precision=0.001
                                         )
                    desc = data[key]
                elif datatype == 'commonsMedia':
                    val = pywikibot.FilePage(pywikibot.Site('commons', 'commons'), u"File:" + sf.stripNamespace(data[key]))
                    while val.isRedirectPage():
                        val = val.getRedirectTarget()
                    desc = val.title()
                    if not val.exists():
                        raise ValueError("Local image given")
                else:
                    val = desc = data[key]
                claim = pywikibot.Claim(self.item.repo, prop, datatype=datatype)
                claim.setTarget(val)
                answer = self.userConfirm("Update element %s with %s \"%s\"?" % (self.label, key, desc))
                if answer:
                    self.item.addClaim(claim)
                    if pref:
                        claim.changeRank('preferred')
        except Exception as e:
            pywikibot.output(e)
            import traceback
            traceback.print_exc()
            pywikibot.output(u"Could not update " + self.label)

    def isOfType(self, typeName):
        for claim in (self.item.claims.get(u"P31") or []):
            if claim.getTarget().title() == typeName:
                return True
        return False

    def isCounty(self):
        return self.isOfType(u"Q1776764")

    def isMunicipality(self):
        return self.isOfType(u"Q640364")

    def isCity(self):
        return self.isOfType(u"Q16858213")

    def createCommonsProperty(self, siruta):
        if self.isCounty() and self.label != u"București":
             self.updateProperty(item, u"Commons", {u"Commons": self.item.labels.get('ro') + u" County"})
        #TODO: search for it first
        pass

    def getUniqueClaim(self, name, canBeNull=False):
        sProp,_,_ = self.config["properties"][name]
        if sProp not in self.item.claims:
            if not canBeNull:
                pywikibot.error(u"%s does not have a %s claim" % (self.label, name))
            return None
        elif len(self.item.claims[sProp]) > 1:
            pywikibot.error(u"%s has several %s claims" % (self.label, name))
            return None
        return self.item.claims[sProp][0].getTarget()

    def getClaim(self, name):
        sProp,_,_ = self.config["properties"][name]
        if sProp not in self.item.claims:
            return None
        return [self.item.claims[sProp][i].getTarget() for i in range(len(self.item.claims[sProp]))]

    def updateCommons(self):
        cProp, cPref, cDatatype = self.config["properties"][u"Commons"]
        sProp, sPref, sDatatype = self.config["properties"][u"SIRUTA"]
        if cProp not in self.item.claims:
            self.createCommonsProperty(self.item.claims[sProp][0].getTarget())
        elif len(self.item.claims[cProp]) > 1:
            pywikibot.error(u"%s has several Commons categories" % self.label)
        else:
            self.updateCommonsCat(cProp, sProp)
            pass

    def updateCommonsCat(self, cProp, sProp):
        cat = self.item.claims[cProp][0].getTarget()
        siruta = self.item.claims[sProp][0].getTarget()
        site = pywikibot.Site("commons", "commons")
        page = pywikibot.Page(site, cat, ns=14)
        sirutaTl = pywikibot.Page(site, "SIRUTA", ns=10)
        if not page.exists():
            pywikibot.error(u"[[:commons:Category:%s]] does not exist" % cat)
        return
        while page.isRedirectPage():
            page = page.getRedirectTarget()
        print sirutaTl
        if sirutaTl not in page.templates():
            text = page.get()
            newText = u"{{%s|%s}}\n%s" % (sirutaTl.title(withNamespace=False), siruta, text)
            pywikibot.showDiff(text, newText)
            answer = self.userConfirm("Add SIRUTA code \"%s\" to the category %s?" % (siruta, page.title()))
            if answer:
                page.put(newText, u"Adding SIRUTA code")

    def createCountySubcategories(self):
        if self.label == u"București":
            return
        cats = {
            u"Administrative divisions of %s County": [u"Category:Administrative divisions of Romania by county", ], 
            u"Cities and towns in %s County": [u"Category:Cities in Romania by county"], 
            u"Communes in %s County": [u"Category:Communes in Romania"], 
            u"Villages in %s County": [u"Category:Villages in Romania by county"],
        }
        for template in cats:
            cat = template % self.label
            site = pywikibot.Site("commons", "commons")
            page = pywikibot.Page(site, cat, ns=14)
            if page.exists():
                continue
            text = u"[[Category:%s County]]\n" % self.label
            for newcat in cats[template]:
                text += u"[[%s|%s]]" % (newcat, sortFromName(self.label))
            print text
            answer = self.userConfirm("Create category %s?" % page.title())
            if answer:
                page.put(text, u"Creating new subcategory for a Romanian County")

    def checkISOCodes(self):
        prop,_,_ = self.config['properties'].get(u"ISO3166-2")
        if prop not in self.item.claims:
            pywikibot.error("County %s does not have an ISO 3166-2 code" % self.item.labels.get('ro'))

    def addPostalCode(self, cp=None):
        sirutaWD = self.getUniqueClaim(u"SIRUTA")
        codpWD = self.getUniqueClaim(u"codp", canBeNull=True)
        codp = self.sirutaDb.get_postal_code(int(sirutaWD))
        if not codp:
            if not cp:
                return
            codp = cp
        if codpWD and unicode(codp) != codpWD:
            pywikibot.error("Mismatch for postal code: SIRUTA has %s, WD has %s" % (codp, codpWD))
        if not codpWD:
            self.updateProperty(u"codp", {u"codp": unicode(codp)})
    
    def addSite(self, _url=None, _type=u"site"):
        if not _url:
            return
        self.updateProperty(_type, {_type: _url})

    def addImage(self, _img=None, _type=u"imagine"):
        if not _img:
            return
        self.updateProperty(_type, {_type: _img})

    def searchSirutaInWD(self, siruta):
        query = "SELECT ?item WHERE { ?item wdt:P843 \"%d\" .     SERVICE wikibase:label { bd:serviceParam wikibase:language \"ro\" }}" % siruta
        query_object = sparql.SparqlQuery()
        data = query_object.get_items(query, result_type=list)
        if len(data) != 1:
            pywikibot.error("There are %d items with siruta %d" % (len(data), siruta))
            return
        return data[0]

    def addSirutaSup(self):
        sirutaWD = int(self.getUniqueClaim(u"SIRUTA"))
        sirutasup = self.sirutaDb.get_sup_code(sirutaWD)
        data = self.searchSirutaInWD(sirutasup)
        sirutasupWD = self.getUniqueClaim(u"SIRUTASUP", canBeNull=True)
        if sirutasupWD and data != sirutasupWD.title():
            pywikibot.error(u"Mismatch for SIRUTASUP: SIRUTA has %s, WD has %s" % (data, sirutasupWD.title()))
            return
        if not sirutasupWD:
            self.updateProperty(u"SIRUTASUP", {u"SIRUTASUP": data})

    def addType(self):
        village_type = {
            1:  u'Q640364',
            2:  u'Q834101',
            3:  u'Q659103',
            4:  u'Q640364',
            5:  u'Q834101',
            6:  u'Q15921300',
            9:  u'Q15921247',
            10: u'Q15921247',
            11: u'Q532',
            17: u'Q15921247',
            18: u'Q15921247',
            19: u'Q532',
            22: u'Q532',
            23: u'Q532',
            40: u'Q1776764',
        }
        sirutaWD = int(self.getUniqueClaim(u"SIRUTA"))
        type = self.sirutaDb.get_type(sirutaWD)
        typeWD = self.getClaim(u"este un/o")
        if not typeWD:
            self.updateProperty(u"este un/o", {u"este un/o": village_type[type]})

    def addSubdivisions(self, field):
        sirutaWD = int(self.getUniqueClaim(u"SIRUTA"))
        subdivisions = self.sirutaDb.get_inf_codes(sirutaWD)
        subdivisionsWD = self.getClaim(field)
        for s in (subdivisionsWD or []):
            i = ItemProcessing(self.config, s)
            subsiruta = int(i.getUniqueClaim(u"SIRUTA"))
            subdivisions.remove(subsiruta)
        for s in subdivisions:
            q = self.searchSirutaInWD(s)
            self.updateProperty(field, {field: q}, force=True)

    def addCountySubdivisions(self):
        self.addSubdivisions(u"subdiviziuni")

    def addCitySubdivisions(self):
        self.addSubdivisions(u"localități componente")

    def addTimezone(self):
        prop, pref, datatype = self.config["properties"][u"fus orar"]
        if prop in self.item.claims:
             return
        for timezone, time in [("Q6723", "Q1777301"), ("Q6760", "Q36669")]:
            tz = pywikibot.ItemPage(pywikibot.Site(), timezone)
            tzdesc = tz.title()
            claim = pywikibot.Claim(self.item.repo, prop, datatype=datatype)
            claim.setTarget(tz)
            answer = self.userConfirm("Update element %s with %s \"%s\"?" % (self.label, u"fus orar", tzdesc))
            if answer:
                self.item.addClaim(claim)
                t = pywikibot.ItemPage(pywikibot.Site(), time)
                date = pywikibot.Claim(self.item.repo, u'P1264')
                date.setTarget(t)
                claim.addQualifier(date)

    def addCapital(self):
        sirutaWD = int(self.getUniqueClaim(u"SIRUTA"))
        type = self.sirutaDb.get_type(sirutaWD)
        if type not in [1, 5, 9, 17, 22]:
            return

        sirutasup = self.sirutaDb.get_sup_code(sirutaWD)
        data = self.searchSirutaInWD(sirutasup)
        sirutasupWD = self.getUniqueClaim(u"reședință pentru")
        if sirutasupWD and data != sirutasupWD.title():
            pywikibot.error(u"Mismatch for \"reședință pentru\": SIRUTA has %s, WD has %s" % (data, sirutasupWD.title()))
            return
        if not sirutasupWD:
            self.updateProperty(u"reședință pentru", {u"reședință pentru": data})


class CityData(robot.WorkItem):
    def __init__(self, config):
        self.db = {}
        self.always = False
        self.config = config
        self.sirutaDb = sirutalib.SirutaDatabase()

    def doWork(self, page, item):
        try:
            rp = item.getSitelink("rowiki")
            rp = pywikibot.Page(pywikibot.Site('ro', 'wikipedia'), rp)
            if rp.isRedirectPage():
                rp = rp.getRedirectTarget()
            text = rp.get()
            tl = sf.extractTemplate(text, u'(Infocaseta Așezare|Cutie Sate|Casetă comune România)')
            i = ItemProcessing(self.config, item, self.sirutaDb, always=self.always)
            if tl:
                tl = sf.tl2Dict(tl)[0]
                #print tl
                if u'imagine' in tl:
                    print u'imagine'
                    i.addImage(tl[u'imagine'].strip())
                if u'codpoștal' in tl:
                    print u'codp'
                    i.addPostalCode(tl[u'codpoștal'].strip())
                if u'hartă' in tl:
                    print u'hartă'
                    i.addImage(tl[u'hartă'].strip(), _type=u"hartă")
                if u'sit-adresă' in tl:
                    print u'sit'
                    i.addSite(tl[u'sit-adresă'].strip())
                if u'website' in tl:
                    print u'website'
                    i.addSite(tl[u'website'].strip())
            #if i.isCounty():
            #    i.createCountySubcategories()
            #    i.checkISOCodes()
            #    i.addCountySubdivisions()
            #else:
            #    i.addCitySubdivisions()
            #i.addPostalCode()
            #i.addSirutaSup()
            #i.updateCommons()
            #i.addType()
            #i.addTimezone()
            #i.addCapital()
            self.always = i.always #remember the choice
        except Exception as e:
            pywikibot.output(e)
            import traceback
            traceback.print_exc()
            pywikibot.output(u"Failed to update city data to Wikidata, skipping...")

    def invalidArea(self, item):
        pywikibot.output(u"Country not set, setting...")
        i = ItemProcessing(self.config, item, self.sirutaDb, always=self.always)
        i.updateProperty(u"Țară", {u"Țară": u"România"})
        self.always = i.always # remember the choice
        
    def description(self):
        return u"Updating city data on Wikidata"

if __name__ == "__main__":
    pywikibot.handle_args()
    user.mylang = 'wikidata'
    user.family = 'wikidata'
    sirutaDb = sirutalib.SirutaDatabase()
    postCodes = postal_codes.PostalCodes("codp_B.csv","codp_50k.csv","codp_1k.csv")
    page = pywikibot.Page(pywikibot.Site(), "P843", ns=120)
    #page = pywikibot.Page(pywikibot.Site(), "Q193055", ns=0)
    generator = pagegenerators.ReferringPageGenerator(page)
    bot = robot.WikidataBot(site=True, generator = generator)

    bot.workers.append(CityData(config))
    bot.run()
