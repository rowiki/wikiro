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
        self.temporary_article = "{{dezvoltare}}\n{{Infocaseta Sportiv}}\n[[Categorie:Medaliați olimpici]]"

        self.conjunction = " și "
        self.feminine = "ă"

        self.article_template = """{{{{Infocaseta Sportiv}}}}
'''{nume}''' ({{{{date biografice}}}}) {{{{este}}}} {un} {ocupatie}, medaliat{fem} olimpic{fem}. A participat la {{{{subst:plural|{num}|ediție a|ediții ale}}}} [[Jocurile Olimpice|Jocurilor Olimpice]] ({lista}) în care a câștigat {aur}{argint}{bronz}.

== Participări ==
Lista de mai jos prezintă principalele competiții la care a participat {nume} de-a lungul carierei.
* {{{{Listă de la Wikidata|P1344|sep=</li><li>}}}}

== Note ==
<references/>

{{{{ciot-sport}}}}
{{{{Control de autoritate}}}}
{{{{DEFAULTSORT:{sort}}}}}
[[Categorie:Nașteri în {an}]]
[[Categorie:Nașteri pe {data}]]
{categorie_aur}
{categorie_argint}
{categorie_bronz}
{categorie_sport}
[[Categorie:Articole despre sportivi create automat]]
"""

    def get_gender_data(self, claims, masc, fem):
        if "P21" in claims:
            item = claims["P21"][0].getTarget()
            if item.labels.get("ro") == "femeie":
                return fem or "ă"
        return masc or ""

    def get_sport(self, description, claims):
        
        female = self.get_gender_data(claims, False, True)
        #print("Female", female)
        if female:
            result = "sportivă"
            cat = "[[Categorie:Sportive]]"
        else:
            result = "sportiv"
            cat = "[[Categorie:Sportivi]]"
        
        if "P106" in claims:
            item = claims["P106"][0].getTarget()
            if female and "P2521" in item.claims:
                for name in item.claims["P2521"]:
                    if name.getTarget().language == "ro":
                        result = name.getTarget().text
                        break
            else:
                result = item.labels.get("ro") or result
            if "P910" in item.claims:
                cat_item = item.claims["P910"][0].getTarget()
                if "rowiki" in cat_item.sitelinks:
                    cat = "[[" + cat_item.getSitelink("rowiki") + "]]"

        if "P1532" in claims or "P27" in claims:
            country = claims.get("P1532") or claims.get("P27")
            country = country[0].getTarget()
            #for demonim in country.claims.get("P1549") or []:
            #    target = demonim.getTarget()
            #    if target.language == "ro":
            #        part = demonim.qualifiers.get("P518")
            #        if part is None:
            #            continue
            #        part = part[0].getTarget().title()
            #        if female and part != "Q1775415":
            #            continue
            #        if not female and part != "Q499327":
            #            continue
            #         result += " " + target.text
            result += " ce a reprezentat [[" + (country.getSitelink("rowiki") or wikibase.get_labels(country) or "o țară necunoscută") + "]]"
        
        if description is not None:
            return description, cat
        else:
            return result, cat

    def skip_page(self, page) -> bool:
        if self.limit is not None and self.counter['read'] >= self.limit:
            return True
        return False

    def get_participation_years(self, claims):
        lista = set([])
        medals = {
                "aur": 0,
                "argint": 0,
                "bronz": 0
                }
        if "P1344" not in claims:
            return lista
        for participation in claims["P1344"]:
            item = participation.getTarget()
            item.get()
            for isa in item.claims["P31"]:
                #print(isa.getTarget().title())
                if isa.getTarget().title() == "Q159821":
                    year = item.labels.get("ro")[-4:]
                    lista.add(year)
                elif isa.getTarget().title() == "Q82414":
                    year = item.labels.get("ro")[-4:]
                    lista.add(year)
                elif isa.getTarget().title() == "Q18536594" or \
                        isa.getTarget().title() == "Q26132862":
                    #print(isa)
                    qual = isa.qualifiers.get("P642") or item.claims.get("P361")
                    if qual is None:
                        continue
                    qual = qual[0]
                    #print(qual)
                    year = qual.getTarget().labels.get("ro")[-4:]
                    lista.add(year)
            if "P166" in participation.qualifiers:
                honor = wikidata.find_best_claim(participation.qualifiers["P166"])
                if honor.getTarget().title() == "Q15243387":
                    medals["aur"] += 1
                if honor.getTarget().title() == "Q15889641":
                    medals["argint"] += 1
                if honor.getTarget().title() == "Q15889643":
                    medals["bronz"] += 1
        return sorted(lista), medals

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

    def build_article(self, label, description, claims):
        lista, medalii = self.get_participation_years(claims)
        defaultsort = self.get_defaultsort(claims, label)
        data, an = self.get_dates(claims)
        #print(data, an)
        aur = ""
        if medalii["aur"] > 0:
            aur = f"{{{{subst:plural|{medalii['aur']}|medalie|medalii}}}} de aur"
        argint = ""
        if medalii["argint"] > 0:
            if medalii["aur"] > 0:
                argint = " și "
                if medalii["bronz"] > 0:
                    argint = ", "
            argint += f"{{{{subst:plural|{medalii['argint']}|medalie|medalii}}}} de argint"
        bronz = ""
        if medalii["bronz"] > 0:
            if medalii["aur"] > 0 or medalii["argint"] > 0:
                bronz = " și "
            bronz += f"{{{{subst:plural|{medalii['bronz']}|medalie|medalii}}}} de bronz"
        if aur != "":
            categorie_aur = "[[Categorie:Medaliați olimpici cu aur]]"
        else:
            categorie_aur = ""
        if argint != "":
            categorie_argint = "[[Categorie:Medaliați olimpici cu argint]]"
        else:
            categorie_argint = ""
        if bronz != "":
            categorie_bronz = "[[Categorie:Medaliați olimpici cu bronz]]"
        else:
            categorie_bronz = ""
        ocupatie,cat_sport=self.get_sport(description, claims)

        text = self.article_template.format(nume=label,
                un=self.get_gender_data(claims, "un", "o"),
                ocupatie=ocupatie,
                fem=self.get_gender_data(claims, "", "ă"), 
                num=len(lista),
                lista=", ".join(lista),
                aur=aur,
                argint=argint,
                bronz=bronz,
                sort=defaultsort,
                an=an,
                data=data,
                categorie_aur=categorie_aur,
                categorie_argint=categorie_argint,
                categorie_bronz=categorie_bronz,
                categorie_sport=cat_sport)
        text = text.replace("\n\n[[Categorie", "\n[[Categorie") #categorii
        text = text.replace("\n\n\n[[Categorie", "\n[[Categorie") #categorii
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



    def treat(self, page) -> None:
        if "rowiki" in page.sitelinks:
            pywikibot.output(f"{page.title()} already has rowiki sitelink")
            return False
        name = wikidata.get_labels(page)
        print(name)
        try:
            text = self.build_article(name, page.descriptions.get('ro'), page.claims)
            #print(text)
            #return
            ropage = pywikibot.Page(pywikibot.Site(), name)
            if ropage.exists():
                ropage = pywikibot.Page(pywikibot.Site(), name + " (sportiv)")
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
            #raise e
            return False


def olympiansGenerator():
    query = """SELECT DISTINCT ?item ?itemLabel ?itemDescription WHERE {
      ?item wdt:P31 wd:Q5 .
      ?item p:P1344 [ pq:P166 ?medal ] .
      ?medal wdt:P279 wd:Q636830 .
      SERVICE wikibase:label { bd:serviceParam wikibase:language "ro,en". }
      OPTIONAL { ?sitelink schema:about ?item . ?sitelink schema:inLanguage "ro" }
      FILTER(!BOUND(?sitelink))
    }
"""
    pattern = re.compile("Q\d+")
    site = pywikibot.Site().data_repository()
    for elem in wikidata.sparql_generator(query, pywikibot.Site()):
        qid = pattern.search(elem['item']).group(0)
        yield pywikibot.ItemPage(site, qid)


if __name__ == '__main__':
    bot = OlympiansArticles(10000)
    bot.generator = olympiansGenerator()
    bot.run()
