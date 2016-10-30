#!/usr/bin/python
# -:- coding: utf8 -:-

#
# (C) Strainu 2016
#
# Distributed under the terms of the MIT license.
#
import pywikibot

from pywikibot import i18n, config, pagegenerators, textlib, weblib
from pywikibot.bot import SingleSiteBot


'''
This describesa self-contained work item that can be executed independently.
It does not return anything, but can alter the state of the objects it receives
as parameters.
'''
class WorkItem(object):
    def __init__(self):
        pass

    def invalidArea(self, item):
        pywikibot.output(unicode(item.labels.get('ro')) + u" is not in the right country.")

    def doWork(self, page, item):
        raise NotImplementedError
        
    def description(self):
        return u"Empty Work Item"
            
class DemoWorkItem(WorkItem):

    def doWork(self, page, item):
        print u"Page: " + page.title()
        print u"Item: " + item.id()

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
            print u"Could not obtain wikidata item for " + page.title()
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
        print page.title()
        
        #TODO:thread this
        for work in self.workers:
            work.doWork(page, item)
            
if __name__ == "__main__":
    print pywikibot.handle_args()
    generator = pagegenerators.AllpagesPageGenerator()
    bot = WikidataBot(site=True, generator = generator)

    bot.workers.append(DemoWorkItem())
    bot.run()    
