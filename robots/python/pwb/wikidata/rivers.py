#!/usr/bin/python
# -:- coding: utf8 -:-

#
# (C) Strainu 2023
#
# Distributed under the terms of the MIT license.
#
import pywikibot
from pywikibot import config as user
from pywikibot import pagegenerators as pg
from pywikibot import textlib
import re
from robot_romania import ItemUtils, WikidataBot, WorkItem
import wikiro.robots.python.pwb.strainu_functions as sf

config = {
    'properties': {
        #alias: (prop, preferred, type)
        'cod râu': ('P11888', False, 'string'),
        'emisar': ('P403', False, 'wikibase-item'),
        'afluent': ('P974', False, 'wikibase-item'),
        'coord': ('P625', False, 'globe-coordinate'),
        'lungime': ('P2043', False, 'quantity'),
        'arie': ('P2053', False, 'quantity'),
        'debit': ('P2225', False, 'quantity'),
    }
}

all_rivers_sparql = """SELECT ?item ?label ?gura ?guraLabel ?cod ?_image ?sitelink WHERE {
  ?item wdt:P31 wd:Q4022.
  ?item wdt:P17 wd:Q218.
  OPTIONAL {
    ?sitelink schema:about ?item. # sitelink about the item
    ?sitelink schema:inLanguage "ro".
    ?sitelink schema:isPartOf <https://ro.wikipedia.org/>
  }
  SERVICE wikibase:label {
    bd:serviceParam wikibase:language "ro" . 
    ?item rdfs:label ?label.
    ?gura rdfs:label ?guraLabel.
  }
 
  OPTIONAL { ?item wdt:P403 ?gura. }
  OPTIONAL { ?item wdt:P11888 ?cod. }
  OPTIONAL { ?item wdt:P18 ?_image. }
}
"""

class ImportRiverData(ItemUtils, WorkItem):
    def __init__(self, config=None, always=False):
        super(ImportRiverData, self).__init__(config, always)

    def update(self, key, _values, _type, _data):
        # set "preluat din" -> "Wikipedia în română"
        sources = {key: []}
        source_claim = pywikibot.Claim(self.item.repo, "P143", datatype='wikibase-item')
        source_claim.setTarget(pywikibot.ItemPage(self.item.repo, "Q199864"))
        for i in range(len(_values)):
            sources[key].append([source_claim])

        if _type == "string":
            self.updateProperty(key, {key: _values}, sources=sources)
        elif _type == "wikibase-item":
            if _data is not None and type(_data) != dict:
                pywikibot.output("Wrong extra data for wikibase-item")
                return

            downstream_values = []
            for _value in _values:
                try:
                    _link_page = pywikibot.Page(pywikibot.Site("ro","wikipedia"), _value)
                    while _link_page.isRedirectPage():
                        _link_page = _link_page.getRedirectTarget()
                    _value = _link_page.data_item()
                except pywikibot.exceptions.NoPageError:
                    pywikibot.error(f"Could not find page [[:ro:{_value}]]")
                    return
                except Exception:
                    pywikibot.error(f"Could not extract page from {_value}")
                    import traceback
                    traceback.print_exc()
                    return        

            # set qualifiers
            if _data is not None:
                qualifiers = {key: []}
                for i in range(len(_values)):
                    qualifiers[key].append([])
                for qual_prop in _data.keys():
                    qual_claim = pywikibot.Claim(self.item.repo, qual_prop, datatype = 'wikibase-item')
                    qual_claim.setTarget(pywikibot.ItemPage(self.item.repo, _data[qual_prop]))
                    for i in range(len(_values)):
                        qualifiers[key][i].append(qual_claim)
            else:
                qualifiers=None
            if key == "afluent":
                force = None
            else:
                force = False
            self.updateProperty(key, {key: downstream_values}, force=None, qualifiers=qualifiers, sources=sources)
        elif _type == "globe-coordinate":
            pass
        elif _type == "quantity":
            if type(_data) != str:
                pywikibot.output("Wrong extra data for quantity")
                return
            self.updateProperty(key, {key: [(_value, _data) for _value in _values]}, sources=sources)
        else:
            pywikibot.error(f"Unknown property type {_type}, skipping")

    def get_multiple_links(self, value:str, first_only:bool):
        if first_only:
            return [sf.extractLink(value)]

        values = []
        ret = sf.extractLinkAndSurroundingText(value)
        if ret != None:
            pre, link, post = ret
            values.append(link)
            values += self.get_multiple_links(post, first_only)
        return values

    def import_ro_data(self):
        try:
            article = self.item.getSitelink("rowiki")
            article_page = pywikibot.Page(pywikibot.Site("ro","wikipedia"), article)
            if not article_page.exists():
                raise Exception
            while article_page.isRedirectPage():
                article_page = article_Page.getRedirectTarget()
        except Exception as e:
            print(f"Nu găsesc pagina {e}", flush=True)
            return

        try:
            text = article_page.get()
            #templates = textlib.extract_templates_and_params(article_page.get())
            param_dict, param_names = sf.tl2Dict(sf.extractTemplate(text, "Infocaseta Râu"))
        except Exception as e:
            pywikibot.error(f"Nu pot extrage Infocaseta Râu. Exception {e}")
            print(text)
            return
        #param_dict = None
        #for template,params in templates:
        #    if template == "Infocaseta Râu":
        #        param_dict = params
        #        break
        #else:
        if param_dict is None:
            print(f"Nu găsesc Infocaseta Râu în {article}: {text}", flush=True)
            return

        params_to_property = {
            # template param: (alias, first value only?, extra_data)
            #"cod-râu": ("cod râu", None, None),
            "afluenți": ("afluent", False, None),
            "afl-dreapta": ("afluent", False, {"P3871": "Q25303601"}),
            "afl-stânga": ("afluent", False, {"P3871": "Q27834806"}),
            "coord-vărsare": ("coord", None, {"P518": "Q1233637"}),
            "lungime": ("lungime", None, "Q828224"),
            "supr-bazin": ("arie", None, "Q712226"),
            "debit-mediu": ("debit", None, "Q794261"),
            "nume-emisar": ("emisar", True, None),
        }

        for param in params_to_property:
            if param not in param_dict.keys():
                continue
            code, first_only, extra_data = params_to_property[param]
            _property, _, _type = config['properties'][code]
            if first_only is not None:
                _val = self.get_multiple_links(param_dict[param], first_only)
            else:
                _val = [param_dict[param].strip()]
            self.update(code, _val, _type, extra_data)
            
    def doWork(self, page, item):
        self.setItem(item)
        self.import_ro_data()
                   

class FixRiverLabels(ItemUtils, WorkItem):
    def __init__(self, config=None, always=False):
        super(FixRiverLabels, self).__init__(config, always)

    def update_description(self, description="râu din România"):
        try:
            if "ro" not in self.item.descriptions:
                answer = self.userConfirm(f"Set description to {description} for {self.label}?")
                if answer:
                    self.item.editDescriptions({'ro': description})
        except pywikibot.exceptions.OtherPageSaveError as e:
            #import pdb
            #pdb.set_trace()
            if e.reason.info.find(" associated with language code ro, using the same description text.") > -1:	
                emissary_item = self.getUniqueClaim("emisar", True)
                if emissary_item is not None and "ro" in emissary_item.labels:
                    emissary_label = emissary_item.labels["ro"]
                    if emissary_label.find(",") > -1:
                        emissary_label = emissary_label.split(",")[0]
                    description += ", afluent al râului " + emissary_label
                    self.update_description(description)

    def doWork(self, page, item):
        self.setItem(item)
        if "ro" not in self.item.labels and \
            "en" in self.item.labels and \
            "river" not in self.item.labels["en"].lower() and \
            "creek" not in self.item.labels["en"].lower():
            self.update_label("ro", self.item.labels["en"], force=True)
        
        self.update_description()
		           

class FixRiversCode(ItemUtils, WorkItem):
    def __init__(self, config=None, always=False):
        super(Rivers, self).__init__(config, always)

    def doWork(self, page, item):
        self.setItem(item)
        #self.fix_code()
        self.check_emissary_code()

    def check_emissary_code(self):
        code = self.getUniqueClaim("cod râu", True)
        if code is None:
            return
        emissary_item = self.getUniqueClaim("emisar", True)
        if emissary_item is not None:
            emissary = ItemUtils(self.config, False, emissary_item)
            emissary_code = emissary.getUniqueClaim("cod râu", True)
            if emissary_code is not None and \
                emissary_code != "XIV-1" and \
                code.find(emissary_code) != 0:
                pywikibot.error(f"Cod emisar invalid {emissary_code} în [[{emissary_item.title()}|{emissary.extractLabel()}]]. Am comparat cu codul {code} din [[{self.item.title()}|{self.extractLabel()}]]")
        
    def fix_code(self):
        old_code = code = self.getUniqueClaim("cod râu", True)
        if code is None:
            return
        while re.search("(.*)\.[0]+$", code) != None:
            print(code)
            code = re.search("(.*)\.[0]+$", code).group(1)
        code = re.sub(r"\.[0]+([1-9]+)", r".\1", code)
        code = re.sub(r"\.$", r"", code)
        if code != old_code:
            self.updateProperty("cod râu", {"cod râu": code}, force=True)
        if code is None or re.search("([XVI]+)-([0-9]*[a-z]?\.?){1,6}", code) != None:
            return
        if code.lower().find("necodificat") > -1:
            self.updateProperty("cod râu", {"cod râu": []}, force=True)
        elif re.search("([XVI]+)\.([0-9]*[a-z]?\.?){1,6}", code) != None:
            code = code.replace(".", "-", 1)
            self.updateProperty("cod râu", {"cod râu": code}, force=True)

    def description(self):
        return "Bot that correct the information about Romanian rivers on Wikidata"


if __name__ == "__main__":
    pywikibot.handle_args()
    user.mylang = 'wikidata'
    user.family = 'wikidata'

    page = pywikibot.Page(pywikibot.Site(), "P11888", ns=120)
    #generator = page.getReferences(follow_redirects=True, content=False)
    generator = pg.WikidataSPARQLPageGenerator(all_rivers_sparql, site=pywikibot.Site())

    bot = WikidataBot(site=True, generator=generator)

    #bot.workers.append(FixRiversCode(config, False))
    #bot.workers.append(FixRiverLabels(config, False))
    bot.workers.append(ImportRiverData(config, False))

    bot.run()

