#!/usr/bin/python3

import re

import pywikibot
from pywikibot import pagegenerators as pg
from pywikibot.bot import SingleSiteBot
from pywikibot.date import intToRomanNum

import wikiro.robots.python.pwb.wikidata as wikidata

months = ["ianuarie", "februarie", "martie", "aprilie", "mai", "iunie",
        "iulie", "august", "septembrie", "octombrie", "noiembrie", "decembrie"]

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
        self.always = False
        if suffix in ["î.e.n.", "î.Hr."]:
            self.direction = -1
        else:
            self.direction = 1

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

class YearsArticles(YearsBot):
    def treat(self, page):
        if page.namespace() != 0:
            print("namespace", page.namespace())
            return
        oldtext = text = page.get()
        try:
            year = page.title(with_ns=False)
            space = year.find(" ")
            year = year[:space]
            year = int(year)
            if space > -1:
                year = 1-year
        except:
            return

        text = self.antet(text)
        text = self.nasteri(text, year)
        text = self.decese(text, year)
        if oldtext != text:
            pywikibot.output(page.title())
            pywikibot.showDiff(oldtext, text)
            answer = pywikibot.input_choice("Încarc?", [("Yes", "y"), ("No", "n"), ("Always", "a")], default='y', force=self.always)
            if answer == "a":
                self.always = True
            if answer == "y" or self.always == True:
                page.put(text, f"Actualizez conținutul pentru {page.title()}") 

    def antet(self, text):
        if self.direction == -1:
            antet = "{{Anul fără cronologie tematică}}{{Anul în alte calendare}}\n"
        else:
            antet = "{{Anul}}{{Anul în alte calendare}}\n"
        loc = text.find("__FARACUPRINS__")
        if (loc == -1):
            return text
        elif loc == 0:
            text = antet + text
        else:
            text = text.replace(text[:loc], antet)
        return text

    def nasteri(self, text, year):
        evenimente = text.find("== Nașteri ==\n\n[[Categ")
        if (evenimente == -1):
            evenimente = text.find("== Nașteri ==\n\n*\n\n[[Categ")
        if (evenimente == -1):
            evenimente = text.find("== Nașteri ==\n\n*\n\n==")
        if (evenimente == -1):
            evenimente = text.find("== Nașteri ==\n\n==")
        if (evenimente == -1):
            return text

        if self.direction == 1:
            diff_year = f"""FILTER (?est >= "{str(year)}-01-01T00:00:00Z"^^xsd:dateTime && ?est < "{str(year+1)}-01-01T00:00:00Z"^^xsd:dateTime)"""
        else:
            diff_year = f"""FILTER (?est > "{str(year-1)}-01-01T00:00:00Z"^^xsd:dateTime && ?est <= "{str(year)}-01-01T00:00:00Z"^^xsd:dateTime)"""

        oldout = output = "== Nașteri ==\n"

        query = f"""SELECT ?item ?label ?description ?sitelinks
WITH {{
  SELECT *
  WHERE {{
    ?item wdt:P569 ?est .
    {diff_year}
    FILTER EXISTS {{
      ?article schema:about ?item .
      ?article schema:inLanguage "ro" .
    }}
    ?item wikibase:sitelinks ?sitelinks.
  }}
  ORDER BY desc(?sitelinks)
  LIMIT 100
}} AS %i
WHERE {{
  INCLUDE %i
  SERVICE wikibase:label {{ 
    bd:serviceParam wikibase:language "ro,mul" .
    ?item rdfs:label ?label . ?item schema:description ?description
  }}
}} GROUP BY ?item ?label ?description ?sitelinks
ORDER BY desc(?sitelinks)"""
        pattern = re.compile("Q\d+")
        site = pywikibot.Site().data_repository()
        for elem in wikidata.sparql_generator(query, pywikibot.Site()):
            print(elem, flush=True)
            qid = pattern.search(elem['item']).group(0)
            item = pywikibot.ItemPage(site, qid)
            if 'rowiki' not in item.sitelinks:
                continue
            date = item.claims['P569'][0].getTarget()
            if date is None:
                continue
            if date.precision < 10:
                elem['n'] = "''dată necunoscută''"
            elif date.precision < 11:
                elem['n'] = months[date.month-1]
            else:
                elem['n'] = "[[" + str(date.day) + " " + months[date.month-1] + "]]"
            if 'P570' in item.claims:
                date = item.claims['P570'][0].getTarget()
            else:
                date = None
            if date is None or date.precision < 9:
                elem['d'] = '?'
            else:
                if date.year < 0:
                    an = str(-date.year) + " î.Hr."
                else:
                    an = str(date.year)
                elem['d'] = f"[[{an}]]"
            output += f"* {elem['n']}: [[{item.getSitelink('rowiki')}]] {elem['description'] or ''} (d. {elem['d'] or '?'})\n"

        return text.replace(oldout, output)


    def decese(self, text, year):
        evenimente = text.find("== Decese ==\n\n[[Categ")
        if (evenimente == -1):
            evenimente = text.find("== Decese ==\n\n*\n\n[[Categ")
        if (evenimente == -1):
            evenimente = text.find("== Decese ==\n\n*\n\n==")
        if (evenimente == -1):
            evenimente = text.find("== Decese ==\n\n==")
        if (evenimente == -1):
            return text

        if self.direction == 1:
            diff_year = f"""FILTER (?est >= "{str(year)}-01-01T00:00:00Z"^^xsd:dateTime && ?est < "{str(year+1)}-01-01T00:00:00Z"^^xsd:dateTime)"""
        else:
            diff_year = f"""FILTER (?est > "{str(year-1)}-01-01T00:00:00Z"^^xsd:dateTime && ?est <= "{str(year)}-01-01T00:00:00Z"^^xsd:dateTime)"""

        oldout = output = "== Decese ==\n"

        query = f"""SELECT ?item ?label ?description ?sitelinks
WITH {{
  SELECT *
  WHERE {{
    ?item wdt:P570 ?est .
    {diff_year}
    FILTER EXISTS {{
      ?article schema:about ?item .
      ?article schema:inLanguage "ro" .
    }}
    ?item wikibase:sitelinks ?sitelinks.
  }}
  ORDER BY desc(?sitelinks)
  LIMIT 100
}} AS %i
WHERE {{
  INCLUDE %i
  SERVICE wikibase:label {{ 
    bd:serviceParam wikibase:language "ro,mul" .
    ?item rdfs:label ?label . ?item schema:description ?description
  }}
}} GROUP BY ?item ?label ?description ?sitelinks
ORDER BY desc(?sitelinks)"""
        pattern = re.compile("Q\d+")
        site = pywikibot.Site().data_repository()
        for elem in wikidata.sparql_generator(query, pywikibot.Site()):
            qid = pattern.search(elem['item']).group(0)
            item = pywikibot.ItemPage(site, qid)
            if 'rowiki' not in item.sitelinks:
                continue
            date = item.claims['P570'][0].getTarget()
            if date is None:
                continue
            if date.precision < 10:
                elem['d'] = "''dată necunoscută''"
            elif date.precision < 11:
                elem['d'] = months[date.month-1]
            else:
                elem['d'] = "[[" + str(date.day) + " " + months[date.month-1] + "]]"
            if 'P569' in item.claims:
                date = item.claims['P569'][0].getTarget()
            else:
                date = None
            if date is None or date.precision < 9:
                elem['n'] = '?'
            else:
                if date.year < 0:
                    an = str(-date.year) + " î.Hr."
                else:
                    an = str(date.year)
                elem['n'] = f"[[{an}]]"
            output += f"* {elem['d']}: [[{item.getSitelink('rowiki')}]] {elem['description'] or ''} (n. {elem['n'] or '?'})\n"

        return text.replace(oldout, output)


if __name__ == "__main__":
    #bot = YearsCategories(2, 2020, 1, ns=14, site=pywikibot.Site())
    #bot = YearsCategories(2021, 2099, 1, ns=14, site=pywikibot.Site())
    #bot = DecadesCategories(0, 2090, 10, ns=14, prefix="Anii", site=pywikibot.Site())
    bot = YearsArticles(1, 1888, 1, ns=0, site=pywikibot.Site())
    bot = YearsArticles(291, 444, 1, suffix="î.Hr.", ns=0, site=pywikibot.Site())
    bot.run()
