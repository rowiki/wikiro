#!/usr/bin/python3

import re

import pywikibot
from pywikibot import pagegenerators as pg
from pywikibot.bot import SingleSiteBot
from pywikibot.date import intToRomanNum


class YearsBot(SingleSiteBot):
    def __init__(self, start=1, end=2020, step=1, ns=0, 
                 prefix=None, suffix=None, site=None):
        super(YearsBot, self).__init__(
            start=start, end=end, ns=ns, site=site)
        if type(ns) == int:
            ns = site.namespaces.resolve(ns)
        self.pages = []
        for i in range(start, end+1, step):
            page = str(i)
            if prefix:
                page = prefix + " " + page
            if suffix:
                page = page + " " + suffix
            page = ns[0].custom_prefix() + page
            self.pages.append(page)
        self.generator = pg.PagesFromTitlesGenerator(self.pages)

    def treat(self, page):
        print(page.title())
        pass

class YearsCategories(YearsBot):
    def treat(self, page):
        if page.namespace() != 14:
            return
        year = int(page.title(with_ns=False))
        decade = year - year % 10
        text = """{{Commonscat|%d}}

{{catmain|%d}} {{ancat}} {{Deceniu antet categorie|deceniu=%d}}

[[Categorie:Anii %d]] [[Categorie:Ani]]""" % (year, year, decade, decade)
        #print(text)
        page.put(text, "Actualizez categoria anilor %d" % year)
        #exit(0)


class DecadesCategories(YearsBot):
    def treat(self, page):
        if page.namespace() != 14:
            print("namespace", page.namespace())
            return
        year = re.findall('\d+', page.title(with_ns=False))
        print(year)
        if len(year) == 0:
            return
        year = int(year[0])
        if year % 10:
            return
        century = intToRomanNum(1 + int(year / 100))
        if year < 100:
            century_text = century
        else:
            century_text = "al %s-lea" % century
        text = """{{Commonscat|%ds}}

{{catmore}} {{Deceniu antet categorie|deceniu=%d}}

{{DEFAULTSORT:%d}}
[[Categorie:Ani după deceniu]] [[Categorie:Secolul %s]]""" % (year, year, year, century_text)
        #print(text)
        page.put(text, "Actualizez categoria anilor %d" % year)
        #exit(0)


if __name__ == "__main__":
    #bot = YearsCategories(2, 2020, 1, ns=14, site=pywikibot.getSite())
    #bot = YearsCategories(2021, 2099, 1, ns=14, site=pywikibot.getSite())
    bot = DecadesCategories(0, 2090, 10, ns=14, prefix="Anii", site=pywikibot.getSite())
    bot.run()
