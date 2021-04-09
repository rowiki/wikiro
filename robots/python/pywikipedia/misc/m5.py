#!/usr/bin/python
# -*- coding: utf-8  -*-

'''
'''
#
# (C) Strainu 2016
#
# Distributed under the terms of the MIT license.
#

import re
import sys
from collections import OrderedDict as od

import pywikibot
from pywikibot import pagegenerators
from pywikibot import config as user

sys.path.append("wikiro/robots/python/pywikipedia")
import strainu_functions as sf


stations = [
        {"name": u"Calea Moșilor", "deep": "-14,40", "previous": u"[[Universitate (stație de metrou)|Universitate]]", "location": u"în intersecția dintre [[Bulevardul Regele Carol I|bd. Carol I]] și [[Calea Moșilor]]"},
        {"name": u"Traian", "deep": "-12,10", "next": u"[[Piața Iancului (stație de metrou)|Piața Iancului]]", "location": u"pe Bulevardul Pache Protopopescu, în apropierea [[Colegiul Național Mihai Viteazul din București|Liceului Mihai Viteazul]]"},
        {"name": u"Victor Manu", "deep": "-13,50", "previous": u"[[Piața Iancului (stație de metrou)|Piața Iancului]]", "location": u"pe Șoseaua Iancului"},
        {"name": u"Național Arena", "deep": "-14,70", "location": u"în apropierea intersecției dintre Șoseaua Iancului și Șoseaua Pantelimon"},
        {"name": u"Chișinău", "deep": "-15,20", "location": u"în apropierea intersecției dintre Bd. Chișinău și Șoseaua Pantelimon"},
        {"name": u"Morarilor", "deep": "-14,00", "location": u"în apropierea intersecției dintre Șoseaua Morarilor și Șoseaua Pantelimon"},
        {"name": u"Sfântul Pantelimon", "deep": "-13,00", "location": u"în zona spitalului Sfântul Pantelimon"},
        {"name": u"Vergului", "deep": "-9,00",  "next": u"[[Pantelimon (stație de metrou)|Depoul Pantelimon 2]]", "location": u"în apropierea intersecției dintre Șoseaua Vergului și Șoseaua Pantelimon"},
]

ref = u"<ref name=\"govro\">{{Citation|url=https://gov.ro/fisiere/subpagini_fisiere/NF_HG_374-2019.pdf|title=Notă de fundamentare a HG 374/2019 pentru reaprobarea indicatorilor tehnico-economici ai M5|date=2019-06-03|author=GOV.ro}}</ref>"

def article(**kwargs):
    infobox = u"""{{{{viitor}}}}
{{{{Cutie stație metrou București
| nume = {name}
| precedentM5 = {previous}
| următorM5 = {next}
| locație = {location}
| sector = {city}
| peroane = 
}}}}""".format(**kwargs)
    text = u"'''{name}''' este o stație planificată de [[Metroul din București|metrou din București]]. Se va afla în {city}, {location}, la o adâncime de {{{{dim|{deep}|m}}}}.{ref}".format(**kwargs)
    text = infobox + "\n\n" + text
    text += u"""

== Note ==
<references/>

{{ciot-București}}
{{Metrou M5}}

[[Categorie:Sector 2]]
[[Categorie:Stații ale liniei 5 de metrou din București]]
"""
    page = pywikibot.Page(pywikibot.getSite(), title=kwargs["title"])
    if not page.exists():
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
            station["city"] = u"[[Sectorul 2]]"
        if "previous" not in station:
            station["previous"] = u"[[" + stations[i-1]["title"] + u"|" + stations[i-1]["name"] + u"]]"
        if "next" not in station:
            station["next"] = u"[[" + stations[i+1]["name"] + u" (stație de metrou)|" + stations[i+1]["name"] + u"]]"
        station["ref"] = ref
        article(**station)
        i+=1

if __name__ == "__main__":
    processList()
	

