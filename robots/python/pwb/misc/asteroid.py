#!/usr/bin/python
#-*- coding:utf-8 -*-

import pywikibot
import re

from collections import OrderedDict
from pywikibot.bot import SingleSiteBot
from wikiro.robots.python.pwb import wikidata


import wikiro.robots.python.pwb.wikidata as wikidata

class AsteroidArticles(SingleSiteBot):
    def __init__(self, limit=None):
        super(AsteroidArticles, self).__init__()
        self.limit = limit
        self.temporary_article = "{{dezvoltare}}\n{{articol asteroid}}"
        self.article_template = "{{{{subst:articol asteroid|număr={nr}|nume={name}}}}}\n[[Categorie:Articole despre asteroizi create automat]]"


    def skip_page(self, page) -> bool:
        if self.limit is not None and self.counter['read'] >= self.limit:
            return True
        return False

    def build_article(self, name):
        nr = name.split(" ")[0]
        text = self.article_template.format(nr=nr, name=name)
        return text


    def treat(self, page) -> None:
        if page.title() in ["Q111593049", "Q985936"]:
            pywikibot.output(f"Skipping {page.title()} due to blacklist")
            return False
        if "rowiki" in page.sitelinks:
            pywikibot.output(f"{page.title()} already has rowiki sitelink")
            return False
        name = page.labels.get("mul") or page.labels.get("ro") or page.labels.get("en")
        if name is None or len(name) < 1:
            pywikibot.output(f"Could not get name for {page.title()}")
            return False
        print(name)
        try:
            text = self.build_article(name)
            print(text)
            answer = self.user_confirm("Upload article?")
            if answer != True:
                return
            ropage = pywikibot.Page(pywikibot.Site(), name)
            if ropage.exists():
                pywikibot.output(f"Articolul {name} există deja.")
                return False
            ropage.put(self.temporary_article, "Creez articol temporar pentru " + name)
            import time
            time.sleep(1)
            page.setSitelink(ropage)
            ropage.put(text, "Finalizez articolul despre " + name)
            return True
        except Exception as e:
            print(e)
            raise e
            return False


def asteroidGenerator():
    query = """SELECT ?item {
      ?item wdt:P31 wd:Q3863 .
      #?item wdt:P31 wd:Q6592 .
      ?item wdt:P793 wd:Q25488743 .
    #FILTER NOT EXISTS {
    #  ?article schema:about ?item .
    #  ?article schema:inLanguage "ro" .
    #}
    #?item wikibase:sitelinks ?sitelinks.
  SERVICE wikibase:label {
    bd:serviceParam wikibase:language "mul,ro,en" .
    ?item rdfs:label ?label . ?item schema:description ?description .
  } 
} ORDER BY ?item LIMIT 10000 OFFSET 20000 
"""
    pattern = re.compile("Q\d+")
    site = pywikibot.Site().data_repository()
    for elem in wikidata.sparql_generator(query, pywikibot.Site()):
        qid = pattern.search(elem['item']).group(0)
        yield pywikibot.ItemPage(site, qid)


if __name__ == '__main__':
    bot = AsteroidArticles(10000)
    bot.generator = asteroidGenerator()
    bot.run()
