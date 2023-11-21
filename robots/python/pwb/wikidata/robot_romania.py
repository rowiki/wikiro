#!/usr/bin/python
# -:- coding: utf8 -:-

#
# (C) Strainu 2016
#
# Distributed under the terms of the MIT license.
#
import pywikibot

from pywikibot import i18n, config, pagegenerators, textlib
from pywikibot.bot import SingleSiteBot


"""This class contains utility functons for processing Wikidata items.
"""
class ItemUtils:
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
        print(question)

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
                    pywikibot.output(u"Wikidata has %s: %s" % (key, str(oldValue)))
                    return False
            if not isinstance(data[key], list):
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
                    commons = pywikibot.Site('commons', 'commons')
                    if isinstance(elem, pywikibot.FilePage):
                            val = elem
                            val._link._site = commons
                    else:
                        val = pywikibot.FilePage(commons, u"File:" + sf.stripNamespace(elem))

                    if not val.exists() or not val.fileIsShared():
                        raise ValueError("Local image given: %s", val.title())
                    while val.isRedirectPage():
                        val = pywikibot.FilePage(val.getRedirectTarget())
                    desc = val.title()
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
            answer = answer or self.userConfirm(f"Update element {self.label} with {key} \"{','.join(cv)}\" (old value \"{','.join(oldValue)}\")?")
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
                return True
        except pywikibot.bot.QuitKeyboardInterrupt:
            raise
        except ValueError as e:
            print(e)
        except Exception as e:
            print("key", key)
            print("data", data)
            print("config", self.config)
            pywikibot.error(u"Could not update " + self.label + " because of error " + str(e))
            import traceback
            traceback.print_exc()
            return False

    def isOfType(self, typeName):
        for claim in (self.item.claims.get(u"P31") or []):
            if claim.getTarget().title() == typeName:
                return True
        return False

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

    def hasClaim(self, name):
        sProp, _, _ = self.config["properties"][name]
        if sProp not in self.item.claims:
            return False
        return True



'''
This class describes a self-contained work item that can be executed independently.
It does not return anything, but can alter the state of the objects it receives
as parameters.
'''
class WorkItem(object):
    def __init__(self):
        pass

    def invalidArea(self, item):
        pywikibot.output(item.labels.get('ro') + u" is not in the right country.")

    def doWork(self, page, item):
        raise NotImplementedError
        
    def description(self):
        return u"Empty Work Item"
            
class DemoWorkItem(WorkItem):

    def doWork(self, page, item):
        print(u"Page: " + page.title())
        print(u"Item: " + item.id())

    def description(self):
        return u"Demo Work Item"

class WikidataBot(SingleSiteBot):
    def __init__(self, site=True, generator=None):
        super(WikidataBot, self).__init__(
            generator=generator, site=site)
        self.workers = []

    def validateCountry(self, item):
        #P17=Q218 or P131=Q218 or P843
        for p in ["P17", "P131"]:
            if p in item.claims:
                for claim in item.claims[p]:
                    if claim.getTarget().title() == "Q218":
                        return True
                else:
                    for work in self.workers:
                        work.invalidArea(item)
                    return False
        if "P843" in item.claims:
            return True
        return False

    def getItem(self, page):
        try:
            if self.site == pywikibot.Site("wikidata", "wikidata"):
                item = pywikibot.ItemPage(self.site,page.title())
                item.get(get_redirect=True)
                return item
            return page.data_item()
        except:
            print(u"Could not obtain wikidata item for " + page.title())
            return None

    def treat(self, page):
        """Process one page/village/city."""
        #Validations
        if page.namespace() != 0:
            return
        item = self.getItem(page)
        if not item:
            return
        if not self.validateCountry(item):
            return
        #if self._treat_counter == 25:
        #    self.quit()
        print(page.title())
        
        #TODO:thread this
        for work in self.workers:
            work.doWork(page, item)
            
if __name__ == "__main__":
    print(pywikibot.handle_args())
    generator = pagegenerators.AllpagesPageGenerator()
    bot = WikidataBot(site=True, generator = generator)

    bot.workers.append(DemoWorkItem())
    bot.run()    
