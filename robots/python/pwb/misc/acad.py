#!/usr/bin/python
#-*- coding:utf-8 -*-
import csv
import re

import pywikibot
import wikiro.robots.python.pwb.wikidata as wikidata
from pywikibot import ItemPage
from pywikibot.bot import SingleSiteBot

plural_types = {
    "membru": "", # prefix already in template
    "membru corespondent": "corespondenți",
    "membru de onoare": "de onoare",
    "membru titular": "titulari",
    "membru fondator": "fondatori",
    "membru post-mortem": "post-mortem"
}

class AcadArticles(SingleSiteBot):
    def __init__(self, limit=None):
        super(AcadArticles, self).__init__()
        self.limit = limit
        self.temporary_article = "{{dezvoltare}}\n{{ArticolMembruAcad}}"
        self.article_template = "{{{{subst:ArticolMembruAcad|nume={nume}|tip_membru={tip}|ocupație={ocupatie}|an={an}|tip_membru_plural={tip_plural}|sort={sort} }}}}"
        with open('acad.csv', newline='', encoding='utf-8') as csvfile:
            reader = csv.reader(csvfile, delimiter=',', quotechar='"')
            self.data = {}
            for row in reader:
                if row[2] not in self.data and row[11] != "":
                    self.data[row[2]] = row

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
        else:
            names = name.split(" ")
            if len(names) == 2:
                defaultsort = names[1] + ", " + names[0]
            else:
                defaultsort = name[-1] + ", " + " ".join(names[:-1])
        return defaultsort

    def skip_page(self, page) -> bool:
        if self.limit is not None and self.counter['read'] >= self.limit:
            return True
        return False

    def build_article(self, page, line):
        defaultsort = self.get_defaultsort(page.claims,
           page.labels.get('ro', page.labels.get('mul', page.labels.get('en'))))
        idx = line[9].rfind(',')
        if idx != -1:
            line[9] = "[[" + line[9][:idx] + ']] și [[' + line[9][idx+2:] + ']]'
        else:
            line[9] = "[[" + line[9] + "]]"
        line[9] = line[9].replace(', ', ']], [[')
        text = self.article_template.format(
            nume=line[7],
            tip=line[10],
            ocupatie=line[9],
            an=line[11],
            tip_plural=plural_types.get(line[10], ""),
            sort=defaultsort
        )
        return text


    def treat(self, page: ItemPage) -> None:
        if "rowiki" in page.sitelinks:
            pywikibot.output(f"{page.title()} already has rowiki sitelink")
            return
        data = self.data.get(page.title())
        if data is None or data[11] == "":
            pywikibot.output(f"No data for {page.title()}")
            return
        labels = page.labels
        name = labels.get('ro', labels.get('mul', labels.get('en'))) or data[7]
        print(name)
        try:
            text = self.build_article(page, data)
            print(text)
            #print(page.get_best_claim('P27').getTarget().labels.get('ro'))
            #return
            ropage = pywikibot.Page(pywikibot.Site(), name)
            if ropage.exists():
                pywikibot.output(f"Articolul {name} există deja.")
                return
            ropage.put(self.temporary_article, "Creez articol temporar pentru " + name)
            import time
            time.sleep(1)
            page.setSitelink(ropage)
            ropage.put(text, "Finalizez articolul despre " + name)
            if name != data[7]:
                redir = pywikibot.Page(pywikibot.Site(), data[7])
                if redir.exists():
                    pywikibot.output(f"Redirect {data[7]} există deja.")
                    return
                redir.text = f"#REDIRECT [[{name}]]"
                redir.save("Creez redirect către " + name)
            pywikibot.output(f"Articolul {name} creat cu succes.")
        except Exception as e:
            print(e)
            #raise e
            return


def acad_generator():
    query = """SELECT DISTINCT ?item ?itemLabel WHERE {
  SERVICE wikibase:label { bd:serviceParam wikibase:language "ro,mul,en". }
  {
      ?item p:P463 ?statement0.
      ?statement0 (ps:P463) wd:Q901677.
    OPTIONAL { ?sitelink schema:about ?item . ?sitelink schema:inLanguage "ro" }
	FILTER (!BOUND(?sitelink)) 
  }
}
"""
    pattern = re.compile("Q\d+")
    site = pywikibot.Site().data_repository()
    for elem in wikidata.sparql_generator(query, pywikibot.Site()):
        qid = pattern.search(elem['item']).group(0)
        yield pywikibot.ItemPage(site, qid)


if __name__ == '__main__':
    bot = AcadArticles(500)
    bot.generator = acad_generator()
    bot.run()
