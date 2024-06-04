#!/usr/bin/python
#-*- coding:utf-8 -*-

import pywikibot
import re

from collections import OrderedDict
from pywikibot.bot import SingleSiteBot


import wikiro.robots.python.pwb.wikidata as wikidata

class PlaneArticles(SingleSiteBot):
    def __init__(self, limit=None):
        super(PlaneArticles, self).__init__()
        self.limit = limit
        self.temporary_article = "{{dezvoltare}}\n{{Infocaseta Aeroport}}\n[[Categorie:Aeroporturi]]"

        self.etichete_tp = " ({{Etichete native}})"
        self.zona_tp = " ce deservește zona {{#invoke:Wikidata|getOneValue|P931|ill=1}}"
        self.calatori_tp = " Aeroportul a fost tranzitat în {{Date înlănțuite de la Wikidata|@P3872|_P585}} de {{plural|{{Date înlănțuite de la Wikidata|@P3872}}|pasager|pasageri}}.{{Date înlănțuite de la Wikidata|@P3872|ref}}"
        self.deschis_tp = "deschis în {{subst:#invoke:Wikidata|getOneValue|P1619}}"
        self.numit_tp = "numit după {{subst:#ifeq:{{subst:Date înlănțuite de la Wikidata|P931|raw}}|{{subst:Date înlănțuite de la Wikidata|P138|raw}}|zona deservită|{{subst:#invoke:Wikidata|getOneValue|P138|ill=1}}}}"
        self.open_tp =  " A fost {deschis}{conj1}{numit}."
        self.piste_tp = "dispune de {{plural|{{subst:#invoke:Wikidata|getBestValuesCount|P529}}|pistă|piste}}"
        self.opista_tp = "dispune de o singură pistă"
        self.arie_tp = "ocupă o suprafață de {{subst:#invoke:Wikidata|getOneValue|P2046|ill=1|si=1}}"
        self.altitudine_tp = "Situat la o altitudine de {{subst:#invoke:Wikidata|getOneValue|P2044|ill=1|si=1}}, "
        self.aeroport = "{{subst:#if:{{{{#property:P2044}}}}|aeroportul|Aeroportul}}"
        self.operator_tp = " Este operat de {{subst:#invoke:Wikidata|getOneValue|P137|ill=1}}."
        self.terminale_tp = "Aerogara are în componență {{plural|{{subst:#invoke:Wikidata|getBestValuesCount|P527}}|terminal|terminale}}, {{subst:#invoke:Wikidata|getValueListWithSeparator|2=P527|conj=și|ill=1}}. "
        self.conjunction = " și "

        self.article_template = """{{{{Infocaseta Aeroport}}}}
'''{nume}'''{etichete} {{{{Cod aeroport}}}} este un aeroport din {{{{subst:#invoke:Wikidata|getLocationChain|ill=1}}}}{zona}.{calatori}{open}

{altitudine}{aeroport} {piste}{conj2}{arie}.{operator}

== Infrastructură ==
=== Piste ===
{{{{subst:#invoke:Runway|fromFrame|output=string}}}}

=== Terminale ===
{terminale}

== Note ==
<references/>

== Vezi și ==
{{{{Ordered list|list_style_type=disc|[[Listă de aeroporturi]]|{{{{subst:#invoke:Wikidata|getValueListWithSeparator|{{{{subst:!}}}}|P1889|limit=3|ill=1}}}}}}}}

{{{{ciot-aeroport}}}}
{{{{Control de autoritate}}}}
{{{{DEFAULTSORT:{sort}}}}}
[[Categorie:Aeroporturi {{{{subst:#if:{{{{#property:P17}}}}|din {{{{subst:#property:P17}}}}|}}}}]]
[[Categorie:Articole despre aeroporturi create automat]]
"""

    def skip_page(self, page) -> bool:
        if self.limit is not None and self.counter['read'] >= self.limit:
            return True
        return False

    def build_article(self, label, claims, defaultsort):
        if "P1705" not in claims:
            etichete = ""
        else:
            etichete = self.etichete_tp
        if "P931" not in claims:
            zona = ""
        else:
            zona = self.zona_tp
        if "P3872" not in claims:
            calatori = ""
        else:
            calatori = self.calatori_tp
        if "P1619" not in claims:
            deschis = ""
        else:
            deschis = self.deschis_tp
        if "P138" not in claims:
            numit = ""
        else:
            numit = self.numit_tp
        if deschis != "" or numit != "":
            if deschis != "" and numit != "":
                conj1 = self.conjunction
            else:
                conj1 = ""
            open = self.open_tp.format(deschis=deschis, conj1=conj1,  numit=numit)
        else:
            open = ""
        if "P529" not in claims:
            piste = self.opista_tp # large airports have their runways spelled out in WD
        else:
            piste = self.piste_tp
        if "P2046" not in claims:
            arie = ""
        else:
            arie = self.arie_tp
        if piste != "" and arie != "":
            conj2 = self.conjunction
        else:
            conj2 = ""
        if "P2044" not in claims:
            altitudine = ""
        else:
            altitudine = self.altitudine_tp
        if "P137" not in claims:
            operator = ""
        else:
            operator = self.operator_tp
        if "P527" not in claims:
            terminale = ""
        else:
            terminale = self.terminale_tp
        return self.article_template.format(nume=label, etichete=etichete, zona=zona, calatori=calatori, open=open, altitudine=altitudine, aeroport=self.aeroport, piste=piste, conj2=conj2, arie=arie, operator=operator, terminale=terminale, sort=defaultsort)



    def treat(self, page) -> None:
        page.get()
        name = page.labels.get('ro')
        defaultsort = name.replace("Aeroportul Internațional ", "").replace("Aeroportul ", "")
        defaultsort += ", aeroport"
        text = self.build_article(name, page.claims, defaultsort)
        print(text)
        #return
        ropage = pywikibot.Page(pywikibot.Site(), name)
        if ropage.exists():
                return
        ropage.put(self.temporary_article, "Creez articol temporar pentru " + name)
        page.setSitelink(ropage)
        ropage.put(text, "Finalizez articolul despre " + name)


def planesGenerator():
    query = """SELECT ?item ?label ?code ?linkcount WHERE {
    ?item wdt:P238 ?code.
    FILTER NOT EXISTS {
      ?item wdt:P3999 ?inchis.
    }
    ?item wikibase:sitelinks ?linkcount .
  FILTER (?linkcount >= 1) . # only include items with 1 or more sitelinks
  FILTER NOT EXISTS {
    ?article schema:about ?item .
    ?article schema:inLanguage "ro" .
    ?article schema:isPartOf <https://ro.wikipedia.org/>
  }
  SERVICE wikibase:label { 
    bd:serviceParam wikibase:language "ro".
    ?item rdfs:label ?label;
      schema:description ?description.
  }
  FILTER(EXISTS {
   ?item rdfs:label ?lang_label.
   FILTER(LANG(?lang_label) = "ro")
 })
}
GROUP BY ?item ?label ?code ?linkcount
ORDER BY DESC(?linkcount)
"""
    pattern = re.compile("Q\d+")
    site = pywikibot.Site().data_repository()
    for elem in wikidata.sparql_generator(query, pywikibot.Site()):
        qid = pattern.search(elem['item']).group(0)
        yield pywikibot.ItemPage(site, qid)


if __name__ == '__main__':
    bot = PlaneArticles(1000)
    bot.generator = planesGenerator()
    bot.run()
