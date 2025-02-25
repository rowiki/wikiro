#!/usr/bin/python
# -:- coding: utf8 -:-

#
# (C) Strainu 2016-2017
#
# Distributed under the terms of the MIT license.
#
import pywikibot

from pywikibot import config as user
from pywikibot import pagegenerators
from pywikibot.data import sparql

import json
import sys

import sirutalib

import wikiro.robots.python.pwb.localitati.postal_codes as postal_codes
import wikiro.robots.python.pwb.strainu_functions as sf
import wikiro.robots.python.pwb.csvUtils as csvUtils

from wikiro.robots.python.pwb.localitati import config
from wikiro.robots.python.pwb.wikidata import robot_romania as robot


class ItemProcessing(robot.ItemUtils):
    def isCounty(self):
        return self.isOfType(u"Q1776764")

    def isMunicipality(self):
        return self.isOfType(u"Q640364")

    def isCity(self):
        return self.isOfType(u"Q16858213")

    def searchSirutaInWD(self, siruta):
        query = "SELECT ?item WHERE { ?item wdt:P843 \"%d\" .     SERVICE wikibase:label { bd:serviceParam wikibase:language \"ro\" }}" % siruta
        query_object = sparql.SparqlQuery()
        data = query_object.get_items(query, result_type=list)
        if len(data) != 1:
            pywikibot.error("There are %d items with siruta %d" % (len(data), siruta))
            return
        return data[0]




class CityData(robot.WorkItem):
    _dataType = u"city"

    def getWikiArticle(self, item, lang="ro"):
        try:
            rp = item.getSitelink(lang + "wiki")
            rp = pywikibot.Page(pywikibot.Site(lang, 'wikipedia'), rp)
            if rp.isRedirectPage():
                rp = rp.getRedirectTarget()
            return rp
        except pywikibot.NoPage:
            # most entries will not have a page, so ignore
            return None
        except Exception as e:
            pywikibot.error(e)
            return None

    def getInfoboxContent(self, item,
                          name=u'(Infocaseta Așezare|Cutie Sate|Casetă comune România|Infocaseta Județe)'):
        text = self.getWikiArticle(item)
        if text:
            text = text.get()
        else:
            return None
        tl = sf.extractTemplate(text, name)
        if tl:
            tl = sf.tl2Dict(tl)[0]
        tl2 = sf.extractTemplate(text, u"commonscat")
        if tl and tl2:
            tl2 = sf.tl2Dict(tl2)[0]
            tl[u"commonscat"] = tl2.get(1)
        return tl

    def getInfoboxElement(self, item, element=None,
                          name=u'(Infocaseta Așezare|Cutie Sate|Casetă comune România|Infocaseta Județe)'):
        elem = None
        tl = self.getInfoboxContent(item, name)
        if tl:
            elem = tl.get(element)
            if elem:
                elem = elem.strip()
        return elem

    def doWork(self, page, item):
        raise NotImplementedError()

    def invalidArea(self, item):
        pywikibot.output(u"Country not set, setting...")
        i = ItemProcessing(config, item=item)
        i.updateProperty(u"Țară", {u"Țară": u"România"})
        self.always = i.always   # remember the choice

    def description(self):
        return u"Updating %s data on Wikidata" % self._dataType

class CityDataCached(CityData):
    _cache = {}

    def clearCache(self):
        self._cache = {}

    def getWikiArticle(self, item, lang="ro"):
        if (item, lang) not in self._cache:
            self._cache[(item, lang)] = super(CityDataCached, self).getWikiArticle(item, lang)

        return  self._cache[(item, lang)]

class CommonsProcessing(ItemProcessing, CityData):
    def __init__(self, config, always=False):
        super(CommonsProcessing, self).__init__(config, always)
        self._dataType = u"Commons"

    def createCommonsProperty(self, siruta):
        if self.isCounty() and self.label != u"București":
            self.updateProperty(u"Commons",
                                {u"Commons": self.item.labels.get('ro') + u" County"})
        else:
            cc = self.getInfoboxElement(self.item, u"commonscat")
            if cc:
               print(cc)
               self.updateProperty(u"Commons", {u"Commons": cc})
        # TODO: search for it first
        pass

    def updateCommons(self):
        cProp, cPref, cDatatype = self.config["properties"][u"Commons"]
        sProp, sPref, sDatatype = self.config["properties"][u"SIRUTA"]
        if self.isOfType(u"Q15303838") or self.isOfType(u"Q659103"):
            print("Skipping municipality seat for now")
            return
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
        print(sirutaTl)
        if sirutaTl not in page.templates():
            text = page.get()
            newText = u"{{%s|%s}}\n%s" % (sirutaTl.title(withNamespace=False), siruta, text)
            pywikibot.showDiff(text, newText)
            answer = self.userConfirm("Add SIRUTA code '%s' to category '%s'?" % (siruta, page.title()))
            if answer:
                page.put(newText, u"Adding SIRUTA code")

    def doWork(self, page, item):
        self.setItem(item)
        self.updateCommons()


class DiacriticsProcessing(ItemProcessing, CityData):
    def __init__(self, config, always=False):
        super(DiacriticsProcessing, self).__init__(config, always)
        self._dataType = u"label"

    def doWork(self, pag, item):
        self.setItem(item)
        self.wikidataDiacritics()
        self.wikipediaDiacritics()

    def wikipediaDiacritics(self):
        page = self.getWikiArticle(self.item)
        if not page:
            return
        newTitle = page.title().replace(u'â', 'î')
        newPage = pywikibot.Page(page.site, newTitle)
        if newPage.exists():
            return
        newText = u"#redirect [[" + page.title() + u"]]"
        answer = self.userConfirm("Redirect '%s' to '%s'" % (newTitle, page.title()))
        if answer:
            newPage.put(newText, u"Redirecting to [[%s]]" % page.title())

    def wikidataDiacritics(self):
        content = self.item.get()
        labels = content.get('labels')
        newlabels = {}
        for label in labels:
            text = labels[label]
            new_text = text.replace(u'ş', u'ș')
            new_text = new_text.replace(u'ţ', u'ț')
            new_text = new_text.replace(u'Ş', u'Ș')
            new_text = new_text.replace(u'Ţ', u'Ț')
            new_text = new_text.replace(u'ã', u'ă')
            if new_text != text:
                newlabels[label] = new_text
        if len(newlabels):
            print(newlabels)
            try:
                item.editLabels(newlabels, summary="Correcting Romanian diacritics in labels of Romanian cities")
            except:
                pass
            #exit(0)


class CountyProcessing(ItemProcessing, CityData):
    def __init__(self, config, always=False):
        super(CountyProcessing, self).__init__(config, always)
        self._dataType = u"county"

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
                text += u"[[%s|%s]]" % (newcat, self.label)
            print(text)
            answer = self.userConfirm("Create category %s?" % page.title())
            if answer:
                page.put(text, u"Creating new subcategory for a Romanian County")

    def checkISOCodes(self):
        prop, _, _ = self.config['properties'].get(u"ISO3166-2")
        if prop not in self.item.claims:
            pywikibot.error("County %s does not have an ISO 3166-2 code" % self.label)

    def doWork(self, page, item):
        self.setItem(item)
        if not self.isCounty():
            return
        # self.createCountySubcategories()
        self.checkISOCodes()

class PopulationProcessing(ItemProcessing, CityData):
    def __init__(self, config, siruta=None, census=None, year="2021", always=False):
        super(PopulationProcessing, self).__init__(config, always)
        self._dataType = u"population"
        self.sirutaDb = siruta
        self.censusDb = census
        self.year = year

        if not self.validateCensus(year):
            raise Exception(f"Invalid census year {year}")
       
    def validateCensus(self, year):
        year = int(year)
        if year in self.config["censuses"]:
            return True
        print(f"{year} was not a census year.")
        return False

    def addPopulation(self):
        try:
            prop, _, _ = self.config["properties"][u"populație"]
            census_date = pywikibot.WbTime(year=self.year,
                    month=self.config["censuses"][self.year]["month"],
                    day=self.config["censuses"][self.year]["day"])
            if prop in self.item.claims:
                claims = self.item.claims[prop]
                for claim in claims:
                    #print claim.qualifiers
                    if claim.has_qualifier('P585', census_date):
                        print(f"{self.item.labels['ro']} already has population for {self.year}")
                        return
                    if claim.rank == 'preferred':
                        claim.changeRank('normal')
            siruta_prop, _, _ = self.config["properties"]["SIRUTA"]
            if siruta_prop in self.item.claims:
                claims = self.item.claims[siruta_prop]
                siruta = str(int(claims[0].getTarget()))
                if siruta not in self.censusDb:
                    #print(f"SIRUTA {siruta} does not exist in the population database")
                    return
                print(siruta, self.censusDb[siruta])
                pop = self.censusDb[siruta]["Locuitori"]
                print(f"Population: {pop}", flush=True)
            else:
                print(f"Item does not have SIRUTA")
                return

            yearclaim = pywikibot.Claim(self.item.repo, prop) #Population
            target = pywikibot.WbQuantity(amount=pop)
            yearclaim.setTarget(target)
            yearclaim.rank = 'preferred'

            date = pywikibot.Claim(self.item.repo, u'P585')#Date of publication
            date.setTarget(census_date)

            method = pywikibot.Claim(self.item.repo, u'P459')#Method
            target = pywikibot.ItemPage(self.item.repo, u"Q39825")
            method.setTarget(target)
            method2 = pywikibot.Claim(self.item.repo, u'P459')#Method
            target = pywikibot.ItemPage(self.item.repo, u"Q747810")
            method2.setTarget(target)
            method3 = pywikibot.Claim(self.item.repo, u'P459')#Method
            target = pywikibot.ItemPage(self.item.repo, self.config["censuses"][self.year]["q"])
            method3.setTarget(target)

            #criteria = pywikibot.Claim(item.repo, u'P1013')#Criteria
            #target = pywikibot.ItemPage(item.repo, u"Q15917105")
            #criteria.setTarget(target)

            answer = self.userConfirm(f"Update element {self.item.labels['ro']} with population for year {self.year}?")
            if answer:
                self.item.addClaim(yearclaim)
                yearclaim.addQualifier(date)
                yearclaim.addQualifier(method)
                yearclaim.addQualifier(method2)
                yearclaim.addQualifier(method3)
                #yearclaim.addQualifier(criteria)
                print(f"Updated element {self.item.labels['ro']} for year {self.year}")
            else:
                pass
        except pywikibot.bot_choice.QuitKeyboardInterrupt as e:
            raise e
        except Exception as e:
            print(e)
            import pdb
            pdb.set_trace()
            print(f"Could not update {self.item.labels['ro']}")


        pass

    def doWork(self, page, item):
        self.setItem(item)
        self.addPopulation()

class PostCodeProcessing(ItemProcessing, CityData):
    def __init__(self, config, siruta=None, postCode=None, always=False):
        super(PostCodeProcessing, self).__init__(config, always)
        self._dataType = u"post code"
        self.sirutaDb = siruta
        self.postCodesDb = postCode

    def addPostalCode(self):
        sirutaWD = self.getUniqueClaim(u"SIRUTA")
        codpWD = self.getUniqueClaim(u"codp", canBeNull=True)
        # codpWP = self.getInfoboxElement(self.item, element=u"codpoștal")
        # codp = self.sirutaDb.get_postal_code(int(sirutaWD))
        print(sirutaWD)
        codpPost = self.postCodesDb.getFromSIRUTA(int(sirutaWD))
        print ('wd', codpWD)
        print ('post', codpPost)
        if not codpPost:
            return
        if codpWD and codpPost != codpWD:
            self.updateProperty(u"codp", {u"codp": unicode(codpPost)}, force=True)
        if not codpWD:
            self.updateProperty(u"codp", {u"codp": unicode(codpPost)})

    def doWork(self, page, item):
        self.setItem(item)
        self.addPostalCode()


class URLProcessing(ItemProcessing, CityData):
    def __init__(self, config, always=False):
        super(URLProcessing, self).__init__(config, always)
        self._dataType = u"URL"

    def addSite(self, _url=None, _type=u"site"):
        if not _url:
            return
        self.updateProperty(_type, {_type: _url})

    def doWork(self, page, item):
        self.setItem(item)
        self.addSite(self.getInfoboxElement(item, element=u"sit-adresă"))
        self.addSite(self.getInfoboxElement(item, element=u"website"))


class ImageProcessing(ItemProcessing, CityDataCached):
    def __init__(self, config, lmi=None, always=False):
        super(ImageProcessing, self).__init__(config, always)
        self._dataType = u"image"
        self._lmi = lmi
        self._blacklist = ["svg", "location", "josephin", "coa", " jud", "3d", "harta", "distrikto"]

    def addImage(self, _img=None, _type=u"imagine"):
        if not _img:
            return False
        return self.updateProperty(_type, {_type: _img})

    def doWork(self, page, item):
        self.setItem(item)
        self.clearCache()
        if self.hasClaim(u"imagine") or self.hasClaim(u"colaj"):
            return
#        self.addImage(self.getInfoboxElement(item, element=u"imagine"), _type=u"imagine")
#        #self.addImage(self.getInfoboxElement(item, element=u"hartă"), _type=u"hartă")
#        if "P31" in item.claims:
#            isa = set(v.getTarget().title() for v in item.claims["P31"])
#            #if "Q532" not in isa:
#            #    self.addImage(self.getInfoboxElement(item, element=u"stemă"), _type=u"stemă")
#            #    self.addImage(self.getInfoboxElement(item, element=u"drapel"), _type=u"drapel")
#            if self.hasClaim(u"imagine"):
#                return
#            if "P1376" in item.claims and ("Q34842263" in isa or "Q34842776" in isa):
#                parent = item.claims["P1376"][0].getTarget()
#                parent.get() #if we have the data, this is a no-op
#                if "P18" in parent.claims:
#                    self.addImage(parent.claims["P18"][0].getTarget(), _type=u"imagine")
#        if self.hasClaim(u"imagine"):
#            return
#
#        label = self.getWikiArticle(self.item)
#        label_hu = self.getWikiArticle(self.item, "hu")
#        label_de = self.getWikiArticle(self.item, "de")
#        if not label:
#            return
#        pi = (label and label.page_image()) or \
#             (label_hu and label_hu.page_image()) or \
#             (label_de and label_de.page_image())
#        if pi and not any(v in pi.title().lower() for v in self._blacklist):
#            print(pi)
#            self.addImage(pi)
#
#        label = label.title()
#        for monument in self._lmi or []:
#            if len(monument["Imagine"]) and monument["Localitate"].find(u"[[" + label + u"|") > -1:
#                self.addImage(monument["Imagine"])
#                break
#        if self.hasClaim(u"imagine"):
#            return

        #check items in this area
        if "P6" in item.claims:
            query = "SELECT ?item WHERE {  ?item wdt:P131 wd:%s.  SERVICE wikibase:label { bd:serviceParam wikibase:language \"ro\" . ?item rdfs:label ?label  }  ?item wdt:P18 ?_image.}" % item.title() 
            print(query)
            query_object = sparql.SparqlQuery()
            data = query_object.get_items(query, result_type=set)
            print(data)
            for image_item in data:
                image_item_page = pywikibot.ItemPage(pywikibot.Site(), image_item)
                image_item_page.get()
                if "P18" in image_item_page.claims:
                    self.addImage(image_item_page.claims["P18"][0].getTarget(), _type=u"imagine")



class SIRUTAProcessing(ItemProcessing, CityData):
    def __init__(self, config, siruta=None, always=False):
        super(SIRUTAProcessing, self).__init__(config, always)
        self._dataType = u"SIRUTA"
        self.sirutaDb = siruta

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

    def doWork(self, page, item):
        self.setItem(item)
        self.addSirutaSup()


class RelationsProcessing(ItemProcessing, CityData):
    def __init__(self, config, siruta=None, always=False):
        super(RelationsProcessing, self).__init__(config, always)
        self._dataType = u"relations"
        self.sirutaDb = siruta
        self.village_type = {
            1: [u'Q640364', u'Q34843301'],
            2: [u'Q16858213'],
            3: [u'Q659103'],
            4: [u'Q640364'],
            5: [u'Q16858213', u'Q34843301'],
            6: [u'Q15921300'],
            9: [u'Q15921247', u'Q34842776'],
            10: [u'Q15921247'],
            11: [u'Q532'],
            17: [u'Q15921247', u'Q34842263'],
            18: [u'Q15921247'],
            19: [u'Q532'],
            22: [u'Q532', u'Q34841063'],
            23: [u'Q532'],
            40: [u'Q1776764'],
        }

    def addType(self):
        sirutaWD = int(self.getUniqueClaim(u"SIRUTA"))
        type = self.sirutaDb.get_type(sirutaWD)
        self.updateProperty(u"este un/o", {u"este un/o": self.village_type[type]}, force=True)

    def addSubdivisions(self, field):
        sirutaWD = int(self.getUniqueClaim(u"SIRUTA"))
        subdivisions = self.sirutaDb.get_inf_codes(sirutaWD)
        subdivisionsWD = self.getClaim(field)
        for s in (subdivisionsWD or []):
            i = ItemProcessing(self.config, item=s)
            subsiruta = int(i.getUniqueClaim(u"SIRUTA"))
            subdivisions.remove(subsiruta)
        for s in subdivisions:
            q = self.searchSirutaInWD(s)
            self.updateProperty(field, {field: q}, force=True)

    def addCountySubdivisions(self):
        self.addSubdivisions(u"subdiviziuni")

    def addCitySubdivisions(self):
        self.addSubdivisions(u"localități componente")

    def addCapital(self):
        sirutaWD = int(self.getUniqueClaim(u"SIRUTA"))
        type = self.sirutaDb.get_type(sirutaWD)
        if type not in [1, 5, 9, 17, 22]:
            return

        sirutasup = self.sirutaDb.get_sup_code(sirutaWD)
        data = self.searchSirutaInWD(sirutasup)
        sirutasupWD = self.getUniqueClaim(u"reședință pentru")
        if sirutasupWD and data != sirutasupWD.title():
            pywikibot.error(u"Mismatch for 'reședință pentru': SIRUTA has %s, WD has %s" % (data, sirutasupWD.title()))
            return
        if not sirutasupWD:
            self.updateProperty(u"reședință pentru", {u"reședință pentru": data})

    def doWork(self, page, item):
        self.setItem(item)
        self.addCapital()
        self.addType()

        if self.isCounty():
            self.addCountySubdivisions()
        else:
            self.addCitySubdivisions()


class TimezoneProcessing(ItemProcessing, CityData):
    def __init__(self, config, always=False):
        super(TimezoneProcessing, self).__init__(config, always)
        self._dataType = u"timezone"

    def addTimezone(self):
        prop, pref, datatype = self.config["properties"][u"fus orar"]
        if prop in self.item.claims:
            return
        for timezone, time in [("Q6723", "Q1777301"), ("Q6760", "Q36669")]:
            tz = pywikibot.ItemPage(pywikibot.Site(), timezone)
            tzdesc = tz.title()
            claim = pywikibot.Claim(self.item.repo, prop, datatype=datatype)
            claim.setTarget(tz)
            answer = self.userConfirm("Update element %s with %s '%s'?" % (self.label, u"fus orar", tzdesc))
            if answer:
                self.item.addClaim(claim)
                t = pywikibot.ItemPage(pywikibot.Site(), time)
                date = pywikibot.Claim(self.item.repo, u'P1264')
                date.setTarget(t)
                claim.addQualifier(date)

    def doWork(self, page, item):
        self.setItem(item)
        self.addTimezone()


class CoordProcessing(ItemProcessing, CityData):
    def __init__(self, config, always=False):
        super(CoordProcessing, self).__init__(config, always)
        self._found = False

    def convertDecimalSep(self, inp):
        if not inp:
            return '0'
        inp = str(inp)
        return inp.replace(',', '.')

    def coordFromWiki(self):
        if self._found:
            return
        latd = self.convertDecimalSep(self.getInfoboxElement(self.item, element=u"latd"))
        latm = self.convertDecimalSep(self.getInfoboxElement(self.item, element=u"latm"))
        lats = self.convertDecimalSep(self.getInfoboxElement(self.item, element=u"lats"))
        latNS = self.getInfoboxElement(self.item, element=u"latNS")
        longd = self.convertDecimalSep(self.getInfoboxElement(self.item, element=u"longd"))
        longm = self.convertDecimalSep(self.getInfoboxElement(self.item, element=u"longm"))
        longs = self.convertDecimalSep(self.getInfoboxElement(self.item, element=u"longs"))
        longEV = self.getInfoboxElement(self.item, element=u"longEV")

        if latNS and latNS.strip() != 'N':
            pywikibot.error(u"latNS has invalid value %s", latNS)
            return
        if longEV and longEV.strip() != 'E':
            pywikibot.error(u"longEV has invalid value %s", longEV)
            return
        if latd and longd:
            print(latd, latm, lats, longd, longm, longs)
            latf = float(latd) + float(latm) / 60 + float(lats) / 3600
            longf = float(longd) + float(longm) / 60 + float(longs) / 3600
            if latf < 43 or latf > 48.25 or longf < 20 or longf > 29.67:
                pywikibot.error(self.item.labels.get('ro') + u" is not in the right country: " + str(latf) + ", " + str(longf))
                return
            # TODO: check county/UAT data
            self._found = True
            self.updateProperty(u"Coord", {u"Coord": (latf, longf)})


    def coordFromOSM(self):
        if self._found:
            return

    def searchCoords(self):
        self.coordFromWiki()
        self.coordFromOSM()
        
    def doWork(self, page, item):
        self._found = False
        self.setItem(item)
        self.searchCoords()


def readJson(filename, what):
    try:
        f = open(filename, "r+")
        pywikibot.output("Reading " + what + " file...")
        db = json.load(f)
        pywikibot.output("...done")
        f.close()
        return db
    except IOError:
        pywikibot.error("Failed to read " + filename + ". Trying to do without it.")
        return {}

def readCsv(filename, key, what):
    try:
        f = open(filename, "r+")
        pywikibot.output("Reading " + what + " file...")
        db = csvUtils.csvToJson(filename, field=key)
        pywikibot.output("...done")
        f.close()
        return db
    except IOError as e:
        print(e, flush=True)
        pywikibot.error("Failed to read " + filename + ". Trying to do without it.")
        return {}

def main():
    pywikibot.handle_args()
    user.mylang = 'wikidata'
    user.family = 'wikidata'
    sirutaDb = sirutalib.SirutaDatabase()
    #postCodes = postal_codes.PostalCodes("codp_B.csv", "codp_50k.csv", "codp_1k.csv")
    lmiDb = readJson("ro_lmi_db.json", "monument database")
    populationDb = readCsv("populatie_2021_sate.csv", "SIRUTA", "population")

    page = pywikibot.Page(pywikibot.Site(), "P843", ns=120)
    #page = pywikibot.Page(pywikibot.Site(), "Q16898095", ns=0)
    generator = page.getReferences(follow_redirects=True, content=False)
    bot = robot.WikidataBot(site=True, generator=generator)

    # bot.workers.append(CountyProcessing(config))
    # bot.workers.append(PostCodeProcessing(config, siruta=sirutaDb, postCode=postCodes))
    # bot.workers.append(URLProcessing(config))
    # bot.workers.append(DiacriticsProcessing(config))
    # bot.workers.append(ImageProcessing(config, lmi=lmiDb))
    # bot.workers.append(CommonsProcessing(config))
    # bot.workers.append(SIRUTAProcessing(config, siruta=sirutaDb))
    # bot.workers.append(RelationsProcessing(config, siruta=sirutaDb))
    # bot.workers.append(TimezoneProcessing(config))
    # bot.workers.append(CoordProcessing(config))
    bot.workers.append(PopulationProcessing(config, sirutaDb, populationDb, 2021))
    bot.run()


if __name__ == "__main__":
    try:
        #import cProfile
        #cProfile.run('main()', 'profiling/fillwikidatainfo.txt')
        main()
    finally:
        pywikibot.stopme()
