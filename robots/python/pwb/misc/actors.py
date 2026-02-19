#!/usr/bin/python
#-*- coding:utf-8 -*-

import re

import requests

import pywikibot
import wikiro.robots.python.pwb.wikidata as wikidata
from pywikibot import ItemPage
from pywikibot.bot import SingleSiteBot

#country = "Q183"  # Germany
#country_name = "Germania"
#country_people = "german"
#country = "Q142"  # France
#country_name = "Franța"
#country_people = "francez"
#country = "Q36"  # Poland
#country_name = "Polonia"
#country_people = "polonez"
#country = "Q218"  # Romania
#country_name = "România"
#country_people = "român"
#country = "Q29"  # Spania
#country_name = "Spania"
#country_people = "spaniol"
#country = "Q38"  # Italia
#country_name = "Italia"
#country_people = "italian"
#country = "Q28"  # Ungaria
#country_name = "Ungaria"
#country_people = "maghiar"
#country = "Q34"  # Suedia
#country_name = "Suedia"
#country_people = "suedez"
#country = "Q33"  # Finlanda
#country_name = "Finlanda"
#country_people = "finlandez"
#country = "Q222"  # Albania
#country_name = "Albania"
#country_people = "albanez"
country = "Q55" # Olanda
country_name = "Țările de Jos"
country_people = "neerlandez"


def get_gender_data(claims, masc=None, fem=None):
    if "P21" in claims:
        item = claims["P21"][0].getTarget()
        if item.labels.get("ro") == "femeie":
            return fem or "ă"
    return masc or ""

def get_defaultsort(claims, name) -> str:
    defaultsort = ""
    try:
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
    except Exception as e:
        print(e)
    finally:
        return defaultsort or name


class ActorArticles(SingleSiteBot):
    def __init__(self, limit=None):
        super(ActorArticles, self).__init__()
        self.limit = limit
        self.article_template = """{{{{Infocaseta Actor}}}}
'''{name}''' ({{{{date biografice}}}}) {{{{este}}}} {{{{subst:un/o}}}} {{{{Listă de la Wikidata|P106|sep=,&#32;|conj=și|ill=1}}}} """ + country_people + """{{{{subst:un/o||ă}}}}. 

== Biografie ==
{name} s-a născut în {{{{subst:Dată|{{{{subst:#property:P569}}}}|notimetag=y}}}} în {{{{subst:#if:{{{{subst:#property:P19}}}}|{{{{subst:#property:P19}}}}|[[""" + country_name + """]]}}}}{{{{subst:#if:{{{{subst:#property:P69}}}}|&#32;și a fost educat{{{{subst:un/o||ă}}}} la {{{{Listă de la Wikidata|P69|sep=,&#32;|conj=și|limit=3}}}}}}}}. {{{{subst:#if:{{{{subst:#property:P26}}}}|A fost căsătorit{{{{subst:un/o||ă}}}} {{{{subst:#ifeq:{{{{subst:Date înlănțuite de la Wikidata|P26|count}}}}|1|o dată|de {{{{Date înlănțuite de la Wikidata|P26|count}}}} ori}}}}{{{{subst:#if:{{{{subst:#property:P40}}}}|&#32;și a avut {{{{plural|{{{{Date înlănțuite de la Wikidata|P40|count}}}}|copil|copii}}}}}}}}.}}}} {{{{subst:#if:{{{{subst:#property:P551}}}}|Locuiește în {{{{Listă de la Wikidata|P551|sep=,&#32;|conj=și|ill=1}}}}.}}}}

== Filmografie selectată ==
Lista de mai jos conține filmele prezente în Wikidata și {filmography_type} {name}.

{{{{Wikidata list
|sparql={filmography_query}
|columns=label,?year
|autolist=fallback
|row_template=subst:Wikidata list/filme
|skip_table=1
|col_count=2
}}}}
{{{{Wikidata list end}}}}

== Note ==
<references />

== Vezi și ==
* [[{see_also_list}]]

== Legături externe ==
* {{{{commonscat-inline}}}}
* {{{{Nume IMDb}}}}

{{{{Control de autoritate}}}}
{{{{DEFAULTSORT:{defaultsort}}}}}
{{{{subst:#if:{{{{subst:Dată|{{{{subst:#property:P569}}}}|format=j F|notimetag=y}}}}|[[Categorie:Nașteri pe {{{{subst:Dată|{{{{subst:#property:P569}}}}|format=j F|notimetag=y}}}}]]}}}}
{{{{subst:#if:{{{{subst:Dată|{{{{subst:#property:P569}}}}|format=Y|notimetag=y}}}}|[[Categorie:Nașteri în {{{{subst:Dată|{{{{subst:#property:P569}}}}|format=Y|notimetag=y}}}}]]}}}}
{{{{subst:#if:{{{{subst:Dată|{{{{subst:#property:P570}}}}|format=j F|notimetag=y}}}}|[[Categorie:Decese pe {{{{subst:Dată|{{{{subst:#property:P570}}}}|format=j F|notimetag=y}}}}]]}}}}
{{{{subst:#if:{{{{subst:Dată|{{{{subst:#property:P570}}}}|format=Y|notimetag=y}}}}|[[Categorie:Decese în {{{{subst:Dată|{{{{subst:#property:P570}}}}|format=Y|notimetag=y}}}}]]|}}}}
[[Categorie:{category}]]
[[Categorie:Articole despre actori și regizori create automat]]"""

    def skip_page(self, page) -> bool:
        if self.limit is not None and self.counter['read'] >= self.limit:
            return True
        return False

    def build_article(self, page, name, article_type):
        defaultsort = get_defaultsort(page.claims, name)
        qid = page.title()
        if article_type == "director":
            filmography_query = ("SELECT DISTINCT ?item (YEAR(?date) AS ?year) { ?item wdt:P57 wd:"
             + qid
             + ". ?item wdt:P577 ?date } ORDER BY ?date ?item")
            filmography_type = "regizate de"
            category = "Regizori " + country_people + "i"
        elif article_type == "actor":
            filmography_query = ("SELECT DISTINCT ?item (YEAR(?date) AS ?year) { ?item wdt:P161 wd:"
             + qid
             + ". ?item wdt:P577 ?date } ORDER BY ?date ?item")
            filmography_type = "în care a jucat"
            category = "Actori " + country_people + "i"
            if get_gender_data(page.claims) == "ă":
                category = "Actrițe " + country_people + "e"
        else:
            raise ValueError("Unknown article type")
        text = self.article_template.format(
            defaultsort = defaultsort,
            name = name,
            category = category,
            filmography_query = filmography_query,
            filmography_type = filmography_type,
            see_also_list = f"Listă de " + category.lower(),
        )
        text = text.replace("italiane", "italiene").replace("italiani", "italieni")
        return text


    def treat(self, page: ItemPage) -> None:
        if "rowiki" in page.sitelinks:
            pywikibot.output(f"{page.title()} already has rowiki sitelink")
            #requests.get("https://listeria.toolforge.org/index.php?action=update&lang=ro&page=" + page.sitelinks["rowiki"].title)
            return
        name = page.labels.get("mul") or page.labels.get("ro") or page.labels.get("en")
        if name is None or len(name) < 1:
            pywikibot.output(f"Could not get name for {page.title()}")
            return
        article_type = decide_article_type(page)
        if article_type == "unknown":
            pywikibot.output(f"Could not decide article type for {name} ({page.title()})")
            return
        print(name)
        try:
            text = self.build_article(page, name, article_type)
            #print(text)
            answer = self.user_confirm("Upload article?")
            if not answer:
                return
            ropage = pywikibot.Page(pywikibot.Site(), name)
            if ropage.exists():
                alt_name = name
                if article_type == "director":
                    alt_name = name + " (" + get_gender_data(page.claims, "regizor", "regizoare") + ")"
                elif article_type == "actor":
                    alt_name = name + " (" + get_gender_data(page.claims, "actor", "actriță") + ")"
                pywikibot.output(f"Articolul {ropage.title()} există deja, încerc cu {alt_name}.")
                ropage = pywikibot.Page(pywikibot.Site(), alt_name)
            if ropage.exists():
                pywikibot.output(f"Articolul {ropage.title()} există deja.")
                return
            ropage.put(text, "Creez articol pentru " + name)
            import time
            time.sleep(1)
            page.setSitelink(ropage)
            ropage.put(text, "Finalizez articolul despre " + name)
            requests.get("https://listeria.toolforge.org/index.php?action=update&lang=ro&page=" + ropage.title(as_url=True))
        except Exception as e:
            print(e)
            raise e


def director_generator(country: str):
    query = """SELECT ?item ?label ?description ?linkcount WHERE
{
    ?item wdt:P106 wd:Q2526255.
    ?item wdt:P27 wd:""" + country + """.
  SERVICE wikibase:label {
    bd:serviceParam wikibase:language "ro,en,mul".
    ?item rdfs:label ?label;
      schema:description ?description.
  }
  ?item wikibase:sitelinks ?linkcount .
  FILTER (?linkcount >= 1) . # only include items with 1 or more sitelinks
}  
"""
    yield from generic_generator(query)

def director_list(qid: str):
    query = ("SELECT DISTINCT ?item (YEAR(?date) AS ?year) { ?item wdt:P57 wd:"
             + qid
             + ". ?item wdt:P577 ?date } ORDER BY ?date ?item")
    yield from generic_generator(query)

def actor_generator(country: str):
    query = """SELECT ?item ?label ?description ?linkcount WHERE
{
    ?item wdt:P106 wd:Q10800557.
    #?item wdt:P106 wd:Q2526255.
    ?item wdt:P27 wd:""" + country + """.
  SERVICE wikibase:label {
    bd:serviceParam wikibase:language "ro,en,mul".
    ?item rdfs:label ?label;
      schema:description ?description.
  }
  ?item wikibase:sitelinks ?linkcount .
  FILTER (?linkcount >= 1) . # only include items with 1 or more sitelinks
}   
"""
    yield from generic_generator(query)

def actor_list(qid: str):
    query = ("SELECT DISTINCT ?item (YEAR(?date) AS ?year) { ?item wdt:P161 wd:"
             + qid
             + ". ?item wdt:P577 ?date } ORDER BY ?date ?item")
    yield from generic_generator(query)

def generic_generator(query):
    pattern = re.compile("Q\d+")
    site = pywikibot.Site().data_repository()
    for elem in wikidata.sparql_generator(query, pywikibot.Site()):
        qid = pattern.search(elem['item']).group(0)
        yield pywikibot.ItemPage(site, qid)

def decide_article_type(page: pywikibot.ItemPage) -> str:
    qid = page.title()
    actor_count = len(list(actor_list(qid)))
    director_count = len(list(director_list(qid)))
    if director_count < 2 and actor_count < 2:
        return "unknown"
    if director_count > actor_count:
        return "director"
    else:
        return "actor"

def combined_generator(country: str):
    yield from actor_generator(country)
    yield from director_generator(country)

if __name__ == '__main__':
    bot = ActorArticles(2_000_000)
    bot.generator = combined_generator(country)
    bot.run()
