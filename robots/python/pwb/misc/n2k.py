#!/usr/bin/python
#-*- coding:utf-8 -*-

import pywikibot
import re
import sqlite3

from collections import OrderedDict
from pywikibot.bot import SingleSiteBot
from transliterate import translit, get_available_language_codes
from transliterate.base import TranslitLanguagePack, registry

import wikiro.robots.python.pwb.wikidata as wikidata

class EL_ROLanguagePack(TranslitLanguagePack):
    language_code = 'el'
    language_name = 'Greek'
    mapping = (
    "avgdeziklmnxoprstyfoAVGDEZIKLMNXOPRSTYFO",
    "αβγδεζηκλμνξοπρστυφωΑΒΓΔΕΖΙΚΛΜΝΞΟΠΡΣΤΥΦΩ",
    )

    reversed_specific_mapping = (
    "ςάέήίόώϊϋΐωΩηΗϵϱι",
    "saeiiooiyiooiieri"
    )

    pre_processor_mapping = {
    u"th": u"θ",
    u"ch": u"χ",
    u"ps": u"ψ",
    u"Th": u"Θ",
    u"Ch": u"Χ",
    u"Ps": u"Ψ",
    }

    reversed_specific_pre_processor_mapping = {
    "ού": "ou",
    "ου": "ou",
    "αυθ": "afth",
    "ευθ": "efth",
    "ηυθ": "ifth",
    "αυκ": "afk",
    "ευκ": "efk",
    "ηυκ": "ifk",
    "αυξ": "afx",
    "ευξ": "efx",
    "ηυξ": "ifx",
    "αυπ": "afp",
    "ευπ": "efp",
    "ηυπ": "ifp",
    "αυσ": "afs",
    "ευσ": "efs",
    "ηυσ": "ifs",
    "αυτ": "aft",
    "ευτ": "eft",
    "ηυτ": "ift",
    "αυφ": "aff",
    "ευφ": "eff",
    "ηυφ": "iff",
    "αυχ": "afch",
    "ευχ": "efch",
    "ηυχ": "ifch",
    "αυψ": "afps",
    "ευψ": "efps",
    "ηυψ": "ifps",
    "αυ": "av",
    "ευ": "ev",
    "ηυ": "iv",
    "ΰ": "u",
    "ύ": "u"
    }


class BG_ROLanguagePack(TranslitLanguagePack):
    language_code = 'bg'
    language_name = 'Bulgarian'
    mapping = (
    u"abvgdeejziklmnoprstufhțîșăABVGDEEJZIKLMNOPRSTUFHȚÎȘĂ",
    u"абвгдеэжзиклмнопрстуфхцышъАБВГДЕЭЖЗИКЛМНОПРСТУФХЦыШЪ",
    )

    reversed_specific_mapping = (
    u"ьъЪйЙ",
    u"îăĂiI"
    )

    reversed_specific_pre_processor_mapping = {
            "чи": "ci",
            "че": "ce",
            "ч": "ci",
            "кî": "chi",
            "щ": "șt",
            "ю": "iu",
            "я": "ia",
            "Чи": "Ci",
            "Че": "Ce",
            "Ч": "Ci",
            "Кî": "Chi",
            "Щ": "Șt",
            "Ю": "Iu",
            "Я": "Ia"
    }


    pre_processor_mapping = {
    "e": "е",
    "E": "Е",
    u"ci": u"ч",
    u"chi": u"кî",
    u"șt": u"щ",
    u"iu": u"ю",
    u"ia": u"я",
    u"Ci": u"Ч",
    u"Chi": u"Кî",
    u"Șt": u"Щ",
    u"Iu": u"Ю",
    u"Ia": u"Я",
    u"Q": u"Я", # Bulgarians typers often use "Q" for "Я". Example: KNQZ => КНЯЗ
    u"q": u"Я", # Bulgarians typers often use "q" for "я". Example: pepelqshka => пепеляшка   
    }

class ProtectedArea:
    def __init__(self, l = None):
        self.columns = [None]*13
        self.columns = list(l[0])
        #print(l)
        for elem in l:
            for x in range(len(elem)):
                if self.columns[x] == elem[x]:
                    continue
                else:
                    if type(self.columns[x]) != list:
                        self.columns[x] = [self.columns[x]]
                    self.columns[x].append(elem[x])

    def get_sitecode(self):
        return self.columns[0]

    def get_sitename(self):
        ret = self.columns[1]
        if ret.isupper() == True:
            ret = ret.title()
        return ret

    def get_sitetype(self):
        return self.columns[2]

    def get_sci_date(self):
        return self.columns[3]

    def get_sci_ref(self):
        return ""

    def get_spa_date(self):
        return self.columns[4]

    def get_spa_ref(self):
        return self.columns[5]

    def get_sac_date(self):
        return self.columns[6]

    def get_sac_ref(self):
        return self.columns[7]

    def get_coord(self,sep=","):
        if sep is None:
            return (self.columns[8], self.columns[9])
        return sep.join((self.columns[8], self.columns[9]))

    def get_marine_pct(self):
        return self.columns[10]

    def get_area(self):
        return self.columns[11]

    def get_country(self):
        if self.columns[12] is None:
            c = self.columns[0][:2]
            return f"{{{{ISO 3166-1|{c}}}}}"
        return self.columns[12]

    def get_bioregion(self):
        dictionary = {
            "Alpine": "alpină",
            "Atlantic": "atlantică",
            "Marine Atlantic": "marină Atlantică",
            "Black Sea": "Marea Neagră",
            "Marine Black Sea": "marină a Mării Negre",
            "Boreal": "boreală",
            "Continental": "continentală",
            "Macaronesian": "macaroneziană",
            "Marine Macaronesian": "marină macaroneziană",
            "Marine": "marină", 
            "Marine Baltic": "marină baltică",
            "Macaronesian": "",
            "Mediterranean": "mediteraneană",
            "Marine Mediterranean": "marină mediteraneană",
            "Pannonian": "panonică",
            "Steppic": "de stepă",
        }
        if self.columns[13] is None:
            return []
        if type(self.columns[13]) != list:
            self.columns[13] = [self.columns[13]]
        return [dictionary[x] for x in self.columns[13]]

    def get_directive(self):
        return self.columns[14]


class Natura2000Articles(SingleSiteBot):
    def __init__(self, limit=None):
        super(Natura2000Articles, self).__init__()
        self.limit = limit
        self.connect_sqlite()

    def connect_sqlite(self):
        self.con = sqlite3.connect("wikiro/data/natura2000")

    def get_main_data(self, code = None):
        query = """SELECT n2k.SITECODE,
       n2k.SITENAME,
       n2k.SITETYPE,
       n2k.DATE_PROP_SCI AS date_sci,
       n2k.DATE_SPA AS date_spa,
       n2k.SPA_LEGAL_REFERENCE AS ref_spa,
       n2k.DATE_SAC AS date_sac,
       n2k.SAC_LEGAL_REFERENCE as ref_sac,
       n2k.LATITUDE,
       n2k.LONGITUDE,
       n2k.MARINE_AREA_PERCENTAGE,
       n2k.AREAHA,
       eunis.[Member state] AS country,
       eunis.[Biogeographical region],
       eunis.Directive
  FROM n2k
       LEFT JOIN
       eunis ON eunis.SITECODE = n2k.SITECODE
       """
        cur = self.con.cursor()
        if code is None:
            res = cur.execute(query)
        else:
            res = cur.execute(query + "WHERE n2k.SITECODE = \"" + code + "\"")
        return res.fetchall()

    def get_species(self, area: ProtectedArea, group, other=False):
        if group is None:
            group = "not NULL"
        if other == True:
            table = "otherspecies"
        else:
            table = "species"
        query = f"SELECT SPECIESNAME, SPECIESRO from {table} where SITECODE = \"{area.get_sitecode()}\" and SPGROUP is {group} group by SPECIESNAME"
        #print(query)
        cur = self.con.cursor()
        res = cur.execute(query)
        return res.fetchall()

    def get_habitats(self, area: ProtectedArea):
        query = f"SELECT DESCRIPTIONRO from habitat where SITECODE = \"{area.get_sitecode()}\""
        cur = self.con.cursor()
        res = cur.execute(query)
        return res.fetchall()

    def skip_page(self, page) -> bool:
        if self.limit is not None and self.counter['read'] >= self.limit:
            return True
        return False

    def build_ref(self, name, content):
        if name not in self.sources and content is not None:
            self.sources[name] = content
        return "@@" + name + "@@"

    def populate_refs(self, text):
        for name in self.sources:
            content = self.sources[name]
            text = text.replace("@@" + name + "@@", "<ref name=\"" + name + "\">" + content + "</ref>", 1)
            text = text.replace("@@" + name + "@@", "<ref name=\"" + name + "\" />")

        text = re.sub(r"@@.*@@", "", text)

        return text

    def build_date_tl(self, date):
        date_elem = date.split("-")
        if len(date_elem) != 3:
            return date
        ret = "{{dată|" + date_elem[0] + "|" + date_elem[1]
        if (int(date_elem[2]) != 1):
            ret += "|" + date_elem[2]
        ret += "}}"
        return ret

    def build_intro(self, area: ProtectedArea):
        #type
        tip = []
        if area.get_sac_date() != "":
            sac = "arie specială de conservare &mdash; SAC"
            if area.get_sac_ref() != "":
                sac += self.build_ref("sac", area.get_sac_ref())
            tip.append(sac)
        if area.get_sci_date() != "" or area.get_sitetype() in ["B", "C"]:
            sci = "sit de importanță comunitară &mdash; SCI"
            if area.get_sci_ref() != "":
                sci += self.build_ref("sci", area.get_sci_ref())
            tip.append(sci)
        if area.get_spa_date() != "" or area.get_sitetype() in ["A", "C"]:
            spa = "arie de protecție specială avifaunistică &mdash; SPA"
            if area.get_spa_ref() != "":
                spa += self.build_ref("spa", area.get_spa_ref())
            tip.append(spa)
        tipuri = ", ".join(tip)

        #country
        tara = area.get_country()
        aria = area.get_area()

        #marin
        try:
            pct = int(area.get_marine_pct())
        except:
            pct = 0
        if pct == 100:
            marin = "integral marine"
        elif pct > 0:
            marin = f"din care {{{{dim|{pct}|%}}}} marine"
        else:
            marin = "integral pe uscat"

        return f"{{{{Infocaseta Arie protejată}}}}\n'''{{{{subst:PAGENAME}}}}''' este o arie protejată ({tipuri}) din [[{tara}]] întinsă pe o suprafață de {{{{dim|{aria}|[[hectar|ha]]}}}}, {marin}."

    def build_loc(self, area: ProtectedArea):
        return "== Localizare ==\nCentrul sitului {{subst:PAGENAME}} este situat la coordonatele {{coord|" + area.get_coord("|") + "|display=i}}."

    def build_establishment(self, area: ProtectedArea):
        #first
        names = {
            "sac": "arie specială de conservare",
            "sci": "sit de importanță comunitară",
            "spa": "arie de protecție specială avifaunistică"
        }
        links = []
        dates = []
        dates_int = []
        refs  = []
        for t in ["sac", "sci", "spa"]:
            attr = "get_" + t + "_date"
            if hasattr(area, attr):
                date = getattr(area, attr)()
                #print(attr, date)
                if date == "":
                    continue
                date_tl = self.build_date_tl(date)
                idx = len(dates)
                for i in range(idx):
                    if int(date.replace("-","")) < dates_int[i]:
                        dates.insert(i, date_tl)
                        dates_int.insert(i, int(date.replace("-","")))
                        links.insert(i, names[t])
                        refs.insert(i, self.build_ref(t, None))
                        break
                else:
                    dates.insert(0, date_tl)
                    dates_int.insert(0, int(date.replace("-","")))
                    links.insert(0, names[t])
                    refs.insert(0, self.build_ref(t, None))

        #backup
        if len(links) == 0:
            if area.get_sitetype() in ["A", "C"]:
                i = len(links)
                links.insert(i, names["spa"])
                dates.insert(i, "baza directivei 79/409 din 1979 referitoare la conservarea păsărilor sălbatice")
                refs.insert(i, "")
            if area.get_sitetype() in ["B", "C"]:
                i = len(links)
                dates.insert(i, "baza directivei 92/43 din 1992 referitoare la conservarea habitatelor naturale și a faunei și florei sălbatice")
                if area.get_sci_ref() != "":
                    links.insert(i, names["sci"])
                    refs.insert(i, self.build_ref("sci", area.get_sci_ref()))
                elif area.get_sac_ref() != "":
                    links.insert(i, names["sac"])
                    refs.insert(i, self.build_ref("sac", area.get_sac_ref()))
                else:
                    links.insert(i, names["sci"])
                    refs.insert(i, "")

        if len(links) > 2:
            other = " Alte tipuri de protecție:" 
            for i in range(1, len(links)):
                other += f"\n* {links[i]} (în {dates[i]}){refs[i]}"
        elif len(links) > 1:
            other = f" Situl a fost protejat și ca {links[1]} în {dates[1]}{refs[1]}."
        else:
            other = ""


        #protected
        plants = self.get_species(area, "\"plante\"")
        animals = self.get_species(area, "not \"plante\"")

        protected = ""
        if len(plants) > 0:
            protected += f"{{{{subst:plural|{len(plants)}|specie|specii}}}} de plante"
        if len(animals) > 0:
            if len(protected) > 0:
                protected += " și "
            protected += f"{{{{subst:plural|{len(animals)}|specie|specii}}}} de animale"
        if protected == "":
            protected = "un număr necunoscut de specii din flora spontană și fauna sălbațică aflate în arealul zonei"

        #refs
        ref_n2k = self.build_ref("n2k", "{{Citat web|url=https://natura2000.eea.europa.eu/Natura2000/SDF.aspx?site=" + area.get_sitecode() + "&release=33|titlu=Natura 2000 Standard Data Form for " + \
                area.get_sitename() + "|accessdate=2024-05-30}}")
        ref_biodiv = self.build_ref("biodiversity", "{{Citat web|url=https://biodiversity.europa.eu/sites/natura2000/" + area.get_sitecode() + "|titlu=" + \
                area.get_sitename() + "|publisher=biodiversity.europa.eu|accessdate=2024-05-29}}")

        return f"== Înființare ==\nSitul {{{{subst:PAGENAME}}}} a fost declarat [[Sit Natura 2000|{links[0]}]] în {dates[0]}{refs[0]} pentru a proteja {protected}.{other}{ref_n2k}{ref_biodiv}"

    def build_biodiversity(self, area: ProtectedArea):
        groups = ["plante", "păsări", "amfibieni", "mamifere", "nevertebrate", "pești", "reptile"]

        reglist = area.get_bioregion()
        #print(reglist)
        if len(reglist) == 0:
            ecoreg = "ecoregiune"
            ecoreglist = "necunoscută"
        elif len(reglist) == 1:
            ecoreg = "ecoregiunea"
            ecoreglist = reglist[0]
        if len(reglist) > 1:
            ecoreg = "ecoregiunile"
            ecoreglist = ", ".join(reglist[:-1])
            ecoreglist = ecoreglist + " și " + reglist[-1]
        habitatlist = [x[0] for x in self.get_habitats(area)]
        nohabitat = len(habitatlist)
        habitatlist = "'', ''".join(habitatlist)
        intro = "== Biodiversitate ==\n"
        intro += f"Situată în [[Ecoregiune|{ecoreg}]] {ecoreglist}, "
        if nohabitat > 0:
            intro += f"aria protejată conține {{{{subst:plural|{nohabitat}|[[habitat]] natural|[[Habitat|habitate]] naturale}}}}: ''{habitatlist}''."
        else:
            intro += f"aria protejată nu include niciun habitat protejat."
        intro += self.build_ref("biodiversity", None)

        biodiversity = "La baza desemnării sitului se află mai multe specii protejate:" + self.build_ref("n2k", None)
        group_count = 0
        total_count = 0
        for group in groups:
            species = self.get_species(area, f"\"{group}\"")
            if len(species) > 0:
                group_count = group_count + 1
                total_count = total_count + len(species)
                species_list = []
                for sp in species:
                    if sp[1] == sp[0]:
                        species_list.append(f"''{sp[0]}''")
                    else:
                        species_list.append(f"{sp[1].lower()} (''{sp[0]}'')")

                biodiversity += f"\n* [[{group}]] ({len(species)}): " + ", ".join(species_list)
        if group_count == 0:
            biodiversity = "La baza desemnării sitului se află un număr nedeclarat de specii protejate." + self.build_ref("n2k", None)
        elif group_count == 1:
            biodiversity = biodiversity.replace(":", "", 1)
            biodiversity = biodiversity.replace("\n*", " de ", 1)
            if total_count == 1:
                biodiversity = biodiversity.replace("mai multe specii protejate", "o specie protejată", 1)
                biodiversity = biodiversity.replace(" (1)", "", 1)
            biodiversity = biodiversity + "."

        otherspecies = "Pe lângă speciile protejate, pe teritoriul sitului au mai fost identificate"
        for group in groups:
            species = self.get_species(area, f"\"{group}\"", True)
            if len(species) > 0:
                otherspecies += f" {{{{subst:plural|{len(species)}|specie|specii}}}} de {group},"
        if otherspecies[-1] == ",":
            otherspecies = otherspecies[:-1] + "."
            otherspecies += self.build_ref("n2k", None)
        else:
            otherspecies = ""

        return "\n\n".join([x for x in [intro, biodiversity, otherspecies] if x != ""])

    def build_footer(self, area: ProtectedArea):
        return """== Note ==
<references />

== Vezi și ==
* [[Natura 2000]]
* [[Lista siturilor Natura 2000 din {country}]]

{{{{Control de autoritate}}}}

[[Categorie:Natura 2000 în {country}]]""".format(country=area.get_country())

    def build_article(self, area: ProtectedArea):
        self.sources = {}
        ret = self.build_intro(area)
        ret += "\n\n" + self.build_loc(area)
        ret += "\n\n" + self.build_establishment(area)
        ret += "\n\n" + self.build_biodiversity(area)
        ret += "\n\n" + self.build_footer(area)

        return self.populate_refs(ret)

    def treat(self, page) -> None:
        print(page.title())
        if page.claims is not None and "P3425" in page.claims:
            name = page.labels.get('ro')
            #print(page.claims["P3425"])
            data = None
            i = 0
            while (data is None or len(data) < 1) and i < len(page.claims["P3425"]):
                data = self.get_main_data(page.claims["P3425"][i].getTarget())
                i = i + 1
                #print(data)
                if data is None or len(data) < 1:
                    continue
                else:
                    break
            else:
                print("code", page.claims["P3425"][0].getTarget())
                return
            area = ProtectedArea(data)
            if name is None:
                if area.get_country() == "Bulgaria":
                    bgname = page.labels.get('bg')
                    if bgname is not None:
                        name = translit(bgname, 'bg', reversed=True)
                        print("translit bg", bgname, name)
                    if bgname is not None and bgname.find("ъ") == -1:
                        return False
                if area.get_country() == "Grecia":
                    grname = page.labels.get('el')
                    if grname is not None:
                        name = translit(grname, 'el', reversed=True)
                        print("translit el", grname, name)
            if name is None:
                name = area.get_sitename()
            text = self.build_article(area)
            #print(text)

            # name cleanup
            origname = name
            name = name.replace("<", "(").replace(">",")")
            if name != origname:
                text = f"{{{{Titlu corect|{origname}}}}}\n" + text

            ropage = pywikibot.Page(pywikibot.Site(), name)
            tip = "Natura 2000"
            if ropage.exists() and ropage.isDisambig():
                if name.find("(") == -1:
                    if area.get_sitetype() == "A":
                        tip = "SPA"
                    elif area.get_sitetype() == "B":
                        tip = "SCI"
                    name = name + " (sit " + tip + ")"
                    ropage = pywikibot.Page(pywikibot.Site(), name)
            if ropage.exists():
                print(name, "already exists")
                return False
            ropage.text = text
            ropage.save("Creez articolul despre " + name)
            description = page.descriptions.get('ro')
            if description is None:
                description = "arie protejată " + tip + " din " + area.get_country()
                page.editDescriptions({'ro': description})
            import time
            time.sleep(1)
            page.setSitelink(ropage)
            return True
        else:
            return False

class NewNatura2000Articles(Natura2000Articles):
    def __init__(self, limit=None):
        super(NewNatura2000Articles, self).__init__(limit)

    def init_page(self, site):
        # obtain area data and page object
        data = self.get_main_data(site[1])
        #print(site[1], data)
        self.area = ProtectedArea(data)

        name = self.area.get_sitename()
        page = pywikibot.Page(pywikibot.Site(), name)
        return super().init_page(page)

    def skip_page(self, page):
        if page.exists():
            print(f"Skipping {page.title()} as it already has an article")
            return True
        return super().skip_page(page)

    def build_item(self, page, area: ProtectedArea):
        repo = self.site.data_repository()

        new_item = pywikibot.ItemPage(repo)
        new_item.editLabels(labels={"ro": area.get_sitename()}, summary="Setting labels")
        description = "arie protejată din " + area.get_country()
        new_item.editDescriptions({'ro': description}, summary="Setting description")
        new_item.setSitelink(page, summary="Setting sitelink")

        claim = pywikibot.Claim(repo, 'P3425')
        claim.setTarget(area.get_sitecode())
        new_item.addClaim(claim, summary="Add claim P3425")
        claim = pywikibot.Claim(repo, 'P31')
        claim.setTarget(pywikibot.ItemPage(repo, "Q15069452"))
        new_item.addClaim(claim, summary="Add claim P31")

        coordinateclaim  = pywikibot.Claim(repo, u'P625')
        lat, lon = area.get_coord(None)
        coordinate = pywikibot.Coordinate(lat=lat, lon=lon, precision=0.0001, site=repo)
        coordinateclaim.setTarget(coordinate)
        new_item.addClaim(coordinateclaim, summary=u'Adding coordinate claim')
        return new_item.getID()

    def treat(self, page) -> None:
        print("*** " + page.title(), flush=True)
        text = self.build_article(self.area)
        print(text)
        page.text = text
        page.save("Creez articolul despre " + page.title())
        import time
        time.sleep(1)
        self.build_item(page, self.area)
        return True


def n2k_generator():
    query = """SELECT ?item ?label ?code ?linkcount {
      ?item wdt:P3425 ?code .
  SERVICE wikibase:label {
    bd:serviceParam wikibase:language "ro" .
    ?item rdfs:label ?label . ?item schema:description ?description
  }
  FILTER NOT EXISTS {
    ?article schema:about ?item .
    ?article schema:inLanguage "ro" .
    ?article schema:isPartOf <https://ro.wikipedia.org/>
  }
  ?item wikibase:sitelinks ?linkcount .
}
"""
    pattern = re.compile("Q\d+")
    site = pywikibot.Site().data_repository()
    for elem in wikidata.sparql_generator(query, pywikibot.Site()):
        qid = pattern.search(elem['item']).group(0)
        yield pywikibot.ItemPage(site, qid)

def db_generator():
    query = """SELECT ?item ?label ?code {
      ?item wdt:P3425 ?code .
  SERVICE wikibase:label {
    bd:serviceParam wikibase:language "ro" .
    ?item rdfs:label ?label . ?item schema:description ?description
  }
}
"""
    #site = pywikibot.Site().data_repository()
    codes = set()
    #for elem in wikidata.sparql_generator(query, pywikibot.Site()):
    #    codes.add(elem['code'])
    con = sqlite3.connect("wikiro/data/natura2000")
    query = """SELECT DISTINCT SITENAME, SITECODE FROM n2k where n2k.COUNTRY_CODE LIKE 'RO';"""
    res = con.execute(query)
    sites = [x for x in res.fetchall()]
    proc = set()
    for site in sites:
        if site[1] in codes:
    #        print(f"Skipping {site[1]} as it already has an item")
            continue
        proc.add(site[1])
        yield site
    print(len(sites), len(codes), len(proc))



if __name__ == '__main__':
    #print(get_available_language_codes())
    print(registry.register(BG_ROLanguagePack, force=True))
    print(registry.register(EL_ROLanguagePack, force=True))
    #print(registry.get('bg'))
    #print(registry.get('el'))
    #print(get_available_language_codes())
    #print(translit("Кресна", 'bg', reversed=True))
    #print(translit("Сребърна", 'bg', reversed=True))a
    #while True:
    #    bgname = pywikibot.input("Bg")
    #    print(translit(bgname, 'bg', reversed=True))

    bot = Natura2000Articles(None)
    bot.generator = n2k_generator()
    #bot = NewNatura2000Articles(None)
    #bot.generator = db_generator()
    bot.run()

