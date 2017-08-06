#!/usr/bin/python
# -:- coding: utf8 -:-

#
# (C) Strainu 2016-2017
#
# Distributed under the terms of the MIT license.
#
import pywikibot
from pywikibot import pagegenerators
from pywikibot.data import sparql
from pywikibot import config as user

import sys
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
        u'codp': ('P281', True, 'string'),
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
    def __init__(self, config, always=False, item=None):
        self.config = config
        self.always = always
        self.setItem(item)

    def extractLabel(self):
        if 'ro' in self.item.labels:
            return self.item.labels['ro']
        if 'en' in self.item.labels:
            return self.item.labels['en']
        if 'fr' in self.item.labels:
            return self.item.labels['fr']

    def setItem(self, item):
        self.item = item
        if not item:
            return
        self.item.get()
        self.label = self.extractLabel()

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
            answer = False
            oldValue = set([])
            prop, pref, datatype = self.config["properties"][key]
            if prop in self.item.claims:
                # don't bother about those yet
                oldValue = set(v.getTarget() for v in self.item.claims[prop])
                if not force:
                    pywikibot.output(u"Wikidata already has %s: %s" % (key, str(oldValue)))
                    return
            if type(data[key]).__name__ != 'list':
                data[key] = [data[key]]
            claims = []
            descs = []
            for elem in data[key]:
                if datatype == 'wikibase-item':
                    val = pywikibot.ItemPage(pywikibot.Site(), elem)
                    desc = val.title()
                elif datatype == 'globe-coordinate':
                    val = pywikibot.Coordinate(lat=float(elem[0]),
                                               lon=float(elem[1]),
                                               globe='earth',
                                               precision=0.001)
                    desc = elem
                elif datatype == 'commonsMedia':
                    val = pywikibot.FilePage(pywikibot.Site('commons', 'commons'),
                                             u"File:" + sf.stripNamespace(elem))
                    while val.isRedirectPage():
                        val = val.getRedirectTarget()
                    desc = val.title()
                    if not val.exists():
                        raise ValueError("Local image given")
                else:
                    val = desc = elem
                claim = pywikibot.Claim(self.item.repo, prop, datatype=datatype)
                claim.setTarget(val)
                if pref:
                    claim.setRank('preferred')
                claims.append(claim)
                descs.append(desc)

            cv = set(v.getTarget() for v in claims)
            rmlist = list(oldValue.difference(cv))
            addlist = list(cv.difference(oldValue))
            if not len(addlist) and not len(rmlist):
                return
            answer = answer or self.userConfirm("Update element %s with %s '%s' (old value '%s')?" % (self.label, key, str(cv), str(oldValue)))
            if answer:
                if len(rmlist):
                    print("Removing", rmlist)
                    rmclaims = [v for v in self.item.claims[prop] if v.getTarget() in rmlist]
                    self.item.removeClaims(rmclaims)
                if len(addlist):
                    print("Adding", addlist)
                    addclaims = [v for v in claims if v.getTarget() in addlist]
                    for claim in addclaims:
                        self.item.addClaim(claim)
        except pywikibot.bot.QuitKeyboardInterrupt:
            raise
        except Exception as e:
            print("key", key)
            print("data", data)
            print("config", self.config)
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

    def getUniqueClaim(self, name, canBeNull=False):
        sProp, _, _ = self.config["properties"][name]
        if sProp not in self.item.claims:
            if not canBeNull:
                pywikibot.error(u"%s does not have a %s claim" % (self.label, name))
            return None
        elif len(self.item.claims[sProp]) > 1:
            pywikibot.error(u"%s has several '%s' claims" % (self.label, name))
            return None
        return self.item.claims[sProp][0].getTarget()

    def getClaim(self, name):
        sProp, _, _ = self.config["properties"][name]
        if sProp not in self.item.claims:
            return None
        return [self.item.claims[sProp][i].getTarget() for i in range(len(self.item.claims[sProp]))]

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

    def getWikiArticle(self, item):
        try:
            rp = item.getSitelink("rowiki")
            rp = pywikibot.Page(pywikibot.Site('ro', 'wikipedia'), rp)
            if rp.isRedirectPage():
                rp = rp.getRedirectTarget()
            return rp
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
                text += u"[[%s|%s]]" % (newcat, sortFromName(self.label))
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


class ImageProcessing(ItemProcessing, CityData):
    def __init__(self, config, lmi=None, always=False):
        super(ImageProcessing, self).__init__(config, always)
        self._dataType = u"image"
        self._lmi = lmi

    def addImage(self, _img=None, _type=u"imagine"):
        if not _img:
            return
        self.updateProperty(_type, {_type: _img})

    def doWork(self, page, item):
        self.setItem(item)
        self.addImage(self.getInfoboxElement(item, element=u"imagine"), _type=u"imagine")
        self.addImage(self.getInfoboxElement(item, element=u"hartă"), _type=u"hartă")

        if self.getUniqueClaim(u"imagine", canBeNull=True):
            return

        label = self.getWikiArticle(self.item)
        if not label:
            return
        label = label.title()
        for monument in self._lmi or []:
            if len(monument["Imagine"]) and monument["Localitate"].find(u"[[" + label + u"|") > -1:
                self.addImage(monument["Imagine"])
                break


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
                pywikibot.error(unicode(self.item.labels.get('ro')) + u" is not in the right country: " + str(latf) + ", " + str(longf))
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

if __name__ == "__main__":
    pywikibot.handle_args()
    user.mylang = 'wikidata'
    user.family = 'wikidata'
    sirutaDb = sirutalib.SirutaDatabase()
    #postCodes = postal_codes.PostalCodes("codp_B.csv", "codp_50k.csv", "codp_1k.csv")
    lmiDb = readJson("ro_lmi_db.json", "monument database")
    page = pywikibot.Page(pywikibot.Site(), "P843", ns=120)
    #page = pywikibot.Page(pywikibot.Site(), "Q16898095", ns=0)
    generator = pagegenerators.ReferringPageGenerator(page)
    bot = robot.WikidataBot(site=True, generator=generator)

    # bot.workers.append(CountyProcessing(config))
    #bot.workers.append(PostCodeProcessing(config, siruta=sirutaDb, postCode=postCodes))
    # bot.workers.append(URLProcessing(config))
    bot.workers.append(ImageProcessing(config, lmi=lmiDb))
    bot.workers.append(CommonsProcessing(config))
    # bot.workers.append(SIRUTAProcessing(config, siruta=sirutaDb))
    # bot.workers.append(RelationsProcessing(config, siruta=sirutaDb))
    # bot.workers.append(TimezoneProcessing(config))
    # bot.workers.append(CoordProcessing(config))

    bot.run()
