#!/usr/bin/python
# -:- coding: utf8 -:-

#
# (C) Strainu 2016
#
# Distributed under the terms of the MIT license.
#
import pywikibot
import re

from pywikibot import i18n, config, pagegenerators, textlib, weblib
from pywikibot.bot import SingleSiteBot

class WorkItem():
    def __init__(self):
        self._count = 0

    def treat(self, page):
        self._count += 1

    @property
    def count(self):
        return self._count

    @count.setter
    def count(self, value):
        return NotImplementedError

class RegexWorkItem(WorkItem):
    def __init__(self, reg):
        super().__init__()
        self.regex = re.compile(reg, re.I)

    def treat(self, page):
        if page.isRedirectPage():
            return
        if self.regex.search(page.get()):
            #print(page.title())
            self._count += 1

        

class CountBot(SingleSiteBot):
    def __init__(self, site=True, generator=None):
        super().__init__(
            generator=generator, site=site)
        self.workers = []

    def treat(self, page):
        for worker in self.workers:
            worker.treat(page)

    def result(self):
        for worker in self.workers:
            print(type(worker), "has count", worker.count)



if __name__ == "__main__":
    generator = pagegenerators.AllpagesPageGenerator()
    bot = CountBot(site=True, generator = generator)

    bot.workers.append(WorkItem())
    bot.workers.append(RegexWorkItem("(\<\s?ref[^e]|\{\{sfn)"))
    bot.workers.append(RegexWorkItem("(<\s?references|\{\{listănote|\{\{reflist|\{\{listă note)"))
    bot.run()
    bot.result()
