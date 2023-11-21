#!/usr/bin/python
# -:- coding: utf8 -:-

#
# (C) Strainu 2023
#
# Distributed under the terms of the MIT license.
#
import pywikibot
from pywikibot import config as user
import re
from robot_romania import ItemUtils, WikidataBot, WorkItem

config = {
    'properties': {
        'cod râu': ('P11888', False, 'string')
    }
}

class Rivers(ItemUtils, WorkItem):
    def __init__(self, config=None, always=False):
        super(Rivers, self).__init__(config, always)

    def doWork(self, page, item):
        self.setItem(item)
        code = self.getUniqueClaim("cod râu", True)
        if code is None or re.search("([XVI]+)-([0-9]+[a-z]?\.?){1,6}", code) != None:
            return
        if code.lower().find("necodificat") > -1:
            self.updateProperty("cod râu", {"cod râu": []}, force=True)
        elif re.search("([XVI]+)\.([0-9]+[a-z]?\.?){1,6}", code) != None:
            code = code.replace(".", "-", 1)
            self.updateProperty("cod râu", {"cod râu": code}, force=True)
        
    def description(self):
        return "Bot that correct the information about Romanian rivers on Wikidata"


if __name__ == "__main__":
    pywikibot.handle_args()
    user.mylang = 'wikidata'
    user.family = 'wikidata'

    page = pywikibot.Page(pywikibot.Site(), "P11888", ns=120)
    generator = page.getReferences(follow_redirects=True, content=False)

    bot = WikidataBot(site=True, generator=generator)

    bot.workers.append(Rivers(config, False))

    bot.run()

