#!/usr/bin/python
#-*- coding:utf-8 -*-

import pywikibot
import re

from collections import OrderedDict
from pywikibot.bot import SingleSiteBot
from wikiro.robots.python.pwb import wikidata


import wikiro.robots.python.pwb.wikidata as wikidata

class OlympiansArticles(SingleSiteBot):
    def __init__(self, limit=None):
        super(OlympiansArticles, self).__init__()
        self.limit = limit
        self.temporary_article = "{{dezvoltare}}\n{{articol NGC}}"
        self.article_template = "{{{{subst:articol NGC|număr={nr}}}}}"


    def skip_page(self, page) -> bool:
        if self.limit is not None and self.counter['read'] >= self.limit:
            return True
        return False

    def build_article(self, ngc):
        text = self.article_template.format(nr=ngc)
        return text


    def treat(self, page) -> None:
        if page.title() in ["Q1040318","Q1043153","Q1044012","Q1044670","Q1045350","Q1045356","Q1045368","Q1045433","Q1045460","Q1045503","Q1045509","Q1045569","Q1045707","Q1045771","Q1045865","Q1046046","Q1046142", "Q1046213", "Q1046296", "Q1046425", "Q1046460", "Q1046472", "Q1046651", "Q1046979", "Q1047015", "Q1047081", "Q1047099", "Q1047126", "Q1047216", "Q1047221", "Q1047275", "Q1047348", "Q1047530", "Q1048793", "Q1049622", "Q1049944", "Q1050730", "Q1051339", "Q1066544", "Q1101939", "Q1102171", "Q1103527", "Q1105376", "Q1105495", "Q1106050", "Q1114607", "Q111536923", "Q1115532", "Q1116695", "Q1117932", "Q1118634", "Q1118650", "Q1118990", "Q1122174", "Q1122383", "Q1145556", "Q1146251", "Q1146322", "Q1146349", "Q1146501", "Q1146864", "Q1147623", "Q1148165", "Q1149335", "Q1149344", "Q1149507", "Q1149542", "Q1149760", "Q1149904", "Q1159824", "Q1167444", "Q1168518", "Q1470285", "Q205266", "Q2085330", "Q2085707", "Q2088169", "Q2089329", "Q211449", "Q3310210", "Q3315001", "Q4795541", "Q588998", "Q599942", "Q629111", "Q682838", "Q682838", "Q736577", "Q742005", "Q786756", "Q86858034", "Q88855516", "Q92016202", "Q932566", "Q980981"
]:
            pywikibot.output(f"Skipping {page.title()} due to blacklist")
            return False
        if "rowiki" in page.sitelinks:
            pywikibot.output(f"{page.title()} already has rowiki sitelink")
            return False
        ngc = page.claims.get("P3208")
        if ngc is None or len(ngc) < 1:
            pywikibot.output(f"Could not get NGC id for {page.title()}")
            return False
        ngc = ngc[0].getTarget()
        name = "NGC " + str(ngc)
        print(name)
        try:
            text = self.build_article(ngc)
            print(text)
            #return
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
            #raise e
            return False


def olympiansGenerator():
    query = """SELECT DISTINCT ?item ?ngc ?label ?description {
  ?item wdt:P3208 ?ngc .
  ?item wdt:P31/wdt:P279* wd:Q318 . 
  SERVICE wikibase:label {
    bd:serviceParam wikibase:language "ro,en" .
    ?item rdfs:label ?label . ?item schema:description ?description
  }
  # look for articles (sitelinks) in ro
  OPTIONAL { ?sitelink schema:about ?item . ?sitelink schema:inLanguage "ro" }
  # but select items with no such article
  FILTER (!BOUND(?sitelink))
}
"""
    pattern = re.compile("Q\d+")
    site = pywikibot.Site().data_repository()
    for elem in wikidata.sparql_generator(query, pywikibot.Site()):
        qid = pattern.search(elem['item']).group(0)
        yield pywikibot.ItemPage(site, qid)


if __name__ == '__main__':
    bot = OlympiansArticles(4000)
    bot.generator = olympiansGenerator()
    bot.run()
