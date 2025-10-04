#!/usr/bin/python
#-*- coding:utf-8 -*-

import pywikibot
import re

from collections import OrderedDict
from pywikibot.bot import SingleSiteBot
from wikiro.robots.python.pwb import wikidata


class F1Articles(SingleSiteBot):
    def __init__(self, limit=None):
        super(F1Articles, self).__init__()
        self.limit = limit
        self.temporary_article = "{{dezvoltare}}\n{{Infocaseta Pilot de Formula 1}}\n[[Categorie:Piloți de Formula 1]]"

        self.inner_data = F1Query()
        self.article_template = """{{{{Infocaseta Pilot de Formula 1}}}}

'''{{{{subst:PAGENAME}}}}''' ({{{{date biografice}}}}) {{{{este}}}} un fost pilot de [[Formula 1]]. De-a lungul carierei a luat startul în {{{{plural|{start}|cursă|curse}}}} dintre care {pp} din [[Lista ocupanților de pole position în Formula 1|pole-position]]. A câștigat {{{{plural|{win}|cursă|curse}}}} și a terminat de {podium} ori pe podium, acumulând {{{{formatnum|{points}}}}} puncte<ref>Punctajul în Formula 1 s-a modificat de-a lungul timpului, așa încât numărul de puncte din sezoane diferite nu poate fi comparat.</ref> în întreaga activitate ca pilot de Formula 1.<ref>[https://www.statsf1.com/en/pilotes.aspx StatsF1 - Drivers]</ref>

== Note ==
<references />

{{{{Control de autoritate}}}}
{{{{DEFAULTSORT:{sort}}}}}
[[Categorie:Piloți de Formula 1]]
[[Categorie:Nașteri în {an}]]
[[Categorie:Nașteri pe {data}]]
[[Categorie:Articole despre piloți create automat]]
"""

    def skip_page(self, page) -> bool:
        if self.limit is not None and self.counter['read'] >= self.limit:
            return True
        return False
    
    def get_dates(self, claims):
        if "P569" not in claims:
            return None, None
        else:
            claim = wikidata.find_best_claim(claims["P569"])
            target = claim.getTarget()
            months = [" ianuarie", " februarie", " martie", " aprilie", " mai", " iunie",
                    " iulie", " august", " septembrie", " octombrie", " noiembrie", " decembrie"]
            day = str(target.day) + months[target.month-1]
            return day.strip(), str(target.year)



    def build_article(self, label, description, claims, idata):
        defaultsort = self.get_defaultsort(claims, label)
        data, an = self.get_dates(claims)

        text = self.article_template.format(nume=label,
                start = idata['start'],
                win = idata['win'] or "0",
                pp = idata['pp'] or "niciuna",
                podium = idata['podium'] or "0",
                points = idata['points'] or "0",
                sort = defaultsort,
                an=an,
                data=data)
        text = text.replace(" dintre care 0", ", niciuna")
        text = text.replace("A câștigat {{plural|0|cursă|curse}}", "Nu a câștigat nicio cursă")
        text = text.replace("a terminat de 0 ori pe podium", "nu a terminat niciodată pe podium")
        text = text.replace("a terminat de 1 ori pe podium", "a terminat o singură dată pe podium")
        text = text.replace("acumulând {{formatnum|0}} puncte", "neacumulând niciun punct")
        return text

    def get_defaultsort(self, claims, name) -> str:
        defaultsort = ""
        if 'P734' in claims:
            for surname_item in claims['P734']:
                if surname_item.getSnakType() != 'value':
                    continue
                surname_item = surname_item.getTarget()
                if surname_item.isRedirectPage():
                    surname_item = surname_item.getRedirectTarget()
                surname = wikidata.get_labels(surname_item)
                defaultsort += surname + " "
                name = name.replace(surname, "").strip()
            defaultsort = defaultsort.strip() + ", " + name
        elif "P53" in claims:
            for surname_item in claims['P53']:
                if surname_item.getSnakType() != 'value':
                    continue
                surname_item = surname_item.getTarget()
                if surname_item.isRedirectPage():
                    surname_item = surname_item.getRedirectTarget()
                surname = wikidata.get_labels(surname_item)
                defaultsort += surname + " "
                name = name.replace(surname, "").strip()
            defaultsort = defaultsort.strip() + ", " + name
        elif "P735" in claims:
            for firstname_item in claims['P735']:
                if firstname_item.getSnakType() != 'value':
                    continue
                firstname_item = firstname_item.getTarget()
                if firstname_item.isRedirectPage():
                    firstname_item = firstname_item.getRedirectTarget()
                firstname = wikidata.get_labels(firstname_item)
                defaultsort += firstname + " "
                name = name.replace(firstname, "").strip()
            defaultsort = name + ", " + defaultsort
        return defaultsort

    def get_inner_data(self, qid):
        for x in self.inner_data:
            print(x, flush=True)
            if x['item'].find(qid) > 0:
                return x


    def treat(self, page) -> None:
        if "rowiki" in page.sitelinks:
            pywikibot.output(f"{page.title()} already has rowiki sitelink")
            return False
        name = wikidata.get_labels(page)
        print(name)
        try:
            text = self.build_article(name, page.descriptions.get('ro'), page.claims, self.get_inner_data(page.title()))
            print(text)
            if not self.user_confirm("Upload?"):
                return
            ropage = pywikibot.Page(pywikibot.Site(), name)
            if ropage.exists():
                ropage = pywikibot.Page(pywikibot.Site(), name + " (pilot)")
                if ropage.exists():
                    pywikibot.output(f"Articolul {name} există deja.")
                    return False
            ropage.put(self.temporary_article, "Creez articol temporar pentru " + name)
            import time
            time.sleep(1)
            page.setSitelink(ropage)
            ropage.put(text, "Finalizez articolul despre " + name)
            if "P570" not in page.claims:
                tpage = ropage.toggleTalkPage()
                tpage.put("{{bpv}}", "Adaug {{bpv}}")
            return True
        except Exception as e:
            print(e)
            raise e
            return False


def F1Query():
    query = """SELECT ?item ?label ?description ?sitelinks ?start ?win ?podium ?pp ?points
WITH {
  SELECT *
  WHERE {
    ?item wdt:P106 wd:Q10841764 .
    ?item p:P1350 ?startp .
    ?startp ps:P1350 ?start .
    ?startp pq:P3831 wd:Q108886227 .
    FILTER ( ?start > 0 )
    OPTIONAL {
      ?item p:P1355 ?winp .
      ?winp ps:P1355 ?win .
      ?winp pq:P641 wd:Q2705092 .
    }
    OPTIONAL {
      ?item p:P1358 ?pointsp .
      ?pointsp ps:P1358 ?points .
      ?pointsp pq:P641 wd:Q2705092 .
    }
    OPTIONAL {
      ?item p:P10648?podiump .
      ?podiump ps:P10648 ?podium .
      ?podiump pq:P641 wd:Q2705092 .
    }
    OPTIONAL {
      ?item p:P10640 ?ppp .
      ?ppp ps:P10640 ?pp .
      ?ppp pq:P641 wd:Q2705092 .
    }
    FILTER NOT EXISTS {
      ?article schema:about ?item .
      ?article schema:inLanguage "ro" .
    }
    ?item wikibase:sitelinks ?sitelinks.
  }
  ORDER BY desc(?sitelinks)
  LIMIT 10000
} AS %i
WHERE {
  INCLUDE %i
  SERVICE wikibase:label { 
    bd:serviceParam wikibase:language "ro,mul,en" .
    ?item rdfs:label ?label . ?item schema:description ?description
  }
}
#GROUP BY ?item ?label ?description ?sitelinks ?est ?win ?
ORDER BY desc(?start)"""
    return list(wikidata.sparql_generator(query, pywikibot.Site()))

def F1Generator():
    pattern = re.compile("Q\d+")
    site = pywikibot.Site().data_repository()
    for elem in F1Query():
        qid = pattern.search(elem['item']).group(0)
        yield pywikibot.ItemPage(site, qid)


if __name__ == '__main__':
    bot = F1Articles(10000)
    bot.generator = F1Generator()
    bot.run()
