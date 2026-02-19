#!/usr/bin/python
# -:- coding: utf8 -:-

import json
import sys

#
# (C) Strainu 2016
#
# Distributed under the terms of the MIT license.
#
import pywikibot
from pywikibot import pagegenerators

sys.path.append("wikiro/robots/python/pwb")
import strainu_functions as sf
from wikidata import robot_romania as robot

config = {
'lmi': 	{
	'properties':	{
			u'Cod': ('P1770', True, 'external-id'),
			u'FostCod': ('P1770', False, 'external-id'),
			u'CodRan': ('P2845', False, 'external-id'),
			u'Denumire': ('', None, 'label'),
			u'UAT': ('P131', False, 'wikibase-item'),
			u'Localitate': ('P276', False, 'wikibase-item'),
			u'Creatori': ('P1770', True, 'wikibase-item'),
			u'Coord': ('P625', False, 'globe-coordinate'),
			u'OsmCoord': ('P625', True, 'globe-coordinate'),
			u'Imagine': ('P18', True, 'commonsMedia'),
			u'Plan': ('P3311', False, 'commonsMedia'),
			u'Commons': ('P373', False, 'string'),
                        u'Țară': ('P17', False, 'wikibase-item'),
			u'Monument': ('P1435', False, 'wikibase-item'),
			}
	}
}
    
class MonumentsData(robot.WorkItem):
    def __init__(self, db, config):
        self.db = {}
        for monument in db:
            if monument["Denumire"].find("[[") < 0:
                continue
            name = sf.extractLink(monument["Denumire"])
            monument["Commons"] = monument["Commons"].replace("commons:Category:", "")
            l = monument["Localitate"].split(';')
            if len(l) > 1:
                monument["Localitate"] = sf.extractLink(monument["Localitate"])
            else:
                monument["Localitate"] = ""
            monument["UAT"] = sf.extractLink(l[-1])
            if monument["Lat"] != '':
                monument["Coord"] = (monument["Lat"], monument["Lon"])
            if monument["OsmLat"] != '':
                monument["OsmCoord"] = (monument["OsmLat"], monument["OsmLon"])
            if name not in self.db:
                self.db[name] = monument
        self.always = False
        self.config = config

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

    def updateProperty(self, item, key, data):
        try:
            prop, pref, datatype = self.config["properties"][key]
            if prop in item.claims:
                #don't bother about those yet
                pywikibot.output(u"Wikidata already has %s: %s" % (key, item.claims[prop][0].getTarget()))
                pass
            else:
                if datatype == 'wikibase-item':
                    page = pywikibot.Page(pywikibot.Site(), data[key])
                    while page.isRedirectPage():
                        page = page.getRedirectTarget()
                    val = page.data_item()
                    desc = page.title()
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
                claim = pywikibot.Claim(item.repo, prop, datatype=datatype)
                claim.setTarget(val)
                answer = self.userConfirm("Update element %s with %s \"%s\"?" % (item.labels['ro'], key, desc))
                if answer:
                    item.addClaim(claim)
                    if pref:
                        claim.changeRank('preferred')
        except Exception as e:
            pywikibot.output(e)
            import traceback
            traceback.print_exc()
            pywikibot.output(u"Could not update " + item.labels.get('ro'))

    def updateWikidata(self, item, data):
        for key in [u"Cod", u"FostCod", u"Localitate", u"Commons", u"Imagine", u"OsmCoord", u"Plan"]:
        #for key in [u"Imagine", u"Plan"]:
            if key in data and data[key] != u"":
                self.updateProperty(item, key, data)
        self.updateProperty(item, u"Monument", {u"Monument": "Monument istoric"})

    def doWork(self, page, item):
        try:
            if page.title() not in self.db:
                pywikibot.output(u"Could not find article " + page.title())
                return
            self.updateWikidata(item, self.db[page.title()])
        except Exception as e:
            pywikibot.output(e)
            import traceback
            traceback.print_exc()
            pywikibot.output(u"Failed to update monument data to Wikidata, skipping...")

    def invalidArea(self, item):
        pywikibot.output(u"Country not set, setting...")
        self.updateProperty(item, u"Țară", {u"Țară": u"România"})
        
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
    generator = page.getReferences(only_template_inclusion=True)
    bot = robot.WikidataBot(site=True, generator = generator)

    bot.workers.append(MonumentsData(db_json, config[_db]))
    bot.run()
