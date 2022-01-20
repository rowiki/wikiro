#!/usr/bin/python
# -*- coding: utf-8  -*-

'''
'''
#
# (C) Strainu 2020
#
# Distributed under the terms of the MIT license.
#

import re
import sys
from collections import OrderedDict as od

import pywikibot
from pywikibot import pagegenerators
from pywikibot import config as user

sys.path.append("wikiro/robots/python/pwb")
import strainu_functions as sf


stations = [
        {"name": u"Știrbei Vodă", "city": "[[Sectorul 1]]", "previous": u"[[Gara de Nord (stație de metrou)|Gara de Nord]]", "location": u"la intersecția dintre Strada Berzei și Strada Știrbei Vodă"},
{"name": u"Hașdeu",  "location": u""},
{"name": u"Uranus", "city": "[[Sectorul 5]]", "location": u"în apropierea [[Grand Hotel Marriott]]"},
{"name": u"George Rozorea", "city": "[[Sectorul 5]]", "location": u"în Piața Dr. Jose Rizal, la intersecția Șoselei Panduri cu Calea 13 Septembrie"},
{"name": u"Chirigiu", "city": "[[Sectorul 5]]", "location": u"în Piața Chirigiu"},
{"name": u"Filaret", "city": "[[Sectorul 5]]", "location": u"în apropierea [[Gara Filaret|Autogării Filaret]]"},
{"name": u"Eroii Revoluției",  "location": u""},
{"name": u"George Bacovia",  "location": u"în apropierea intersecției dintre Șoseaua Giurgiului și strada George Bacovia"},
{"name": u"Toporași", "location": u"în apropierea intersecției dintre Șoseaua Giurgiului și strada Toporași"},
{"name": u"Nicolae Cajal", "location": u"în zona [[Cimitirul evreiesc Giurgiului din București|cimitirului evreiesc Giurgiului]]"},
{"name": u"Luică", "location": u"în apropierea intersecției dintre Șoseaua Giurgiului si strada Alexandru Anghel"},
{"name": u"Giurgiului", "location": u"pe Șoseaua Giurgiului"},
{"name": u"Gara Progresul", "next": u"Terminus", "location": u"la [[Gara Progresul]]"},
]

ref = u"<ref name=\"spf\">{{Citation|url=https://ro.scribd.com/document/459611539/SPF-SF-Mag-4-Gara-de-Nord-Progresu|title=SPF și SF pentru construcția liniei 4 de metrou: Lac Străulești - Gara Progresu, tronsonul Gara de Nord - Gara Progresu}}</ref>"

def article(**kwargs):
    infobox = u"""{{{{viitor}}}}
{{{{Cutie stație metrou București
| nume = {name}
| precedentM6 = {previous}
| următorM6 = {next}
| locație = {location}
| sector = {city}
| peroane = 
}}}}""".format(**kwargs)
    text = u"'''{name}''' este o stație planificată de [[Metroul din București|metrou din București]]. Se va afla în {city}, {location}.{ref}".format(**kwargs)
    text = infobox + "\n\n" + text
    text += u"""

== Note ==
<references/>

{{ciot-București}}
{{Metrou M4}}

[[Categorie:Stații ale liniei 4 de metrou din București]]
"""
    page = pywikibot.Page(pywikibot.getSite(), title=kwargs["title"])
    if page.exists():
        return
    else:
        page.put(text, u"Articol despre o stație de metrou")
        import time
        time.sleep(5)

def processList():
    global stations, ref
    i = 0
    for station in stations:
        if "title" not in station:
            station["title"] = station["name"] + u" (stație de metrou)"
        if "city" not in station:
            station["city"] = u"[[Sectorul 4]]"
        if "previous" not in station:
            station["previous"] = u"[[" + stations[i-1]["title"] + u"|" + stations[i-1]["name"] + u"]]"
        if "next" not in station:
            station["next"] = u"[[" + stations[i+1]["name"] + u" (stație de metrou)|" + stations[i+1]["name"] + u"]]"
        station["ref"] = ref
        article(**station)
        i+=1

if __name__ == "__main__":
    processList()
	

