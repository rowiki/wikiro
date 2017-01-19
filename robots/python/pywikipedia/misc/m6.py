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
from pywikibot import catlib

sys.path.append(".")
import strainu_functions as sf


stations = [
{"name": u"Pajura", "previous": u"[[1 Mai (stație de metrou)|1 Mai]]", "title": u"Pajura (stație de metrou pe M6)", "location": u"în lungul căii ferate, de-a lungul străzii Băiculești"},
{"name": u"Expoziției",  "location": u"între Bulevardul Expoziției, Bulevardul Poligrafiei și strada Parcului, lângă [[complexul expozițional Romexpo]]"},
{"name": u"Piața Montreal",  "location": u"de-a lungul Bulevardului Mărăști, în apropierea [[Parcul Herăstrău|Parcului Herăstrău]] și a [[Casa Presei Libere|Casei Presei Libere]]"},
{"name": u"Gara Băneasa",  "location": u"pe espalanada din fața [[Gara Băneasa|Gării CFR Băneasa]]"},
{"name": u"Aeroportul Băneasa",  "location": u"între [[DN1]] și [[Aeroportul Internațional București Băneasa - Aurel Vlaicu]]"},
{"name": u"Tokyo",  "location": u"între [[DN1]] și Complexul Comercial Băneasa"},
{"name": u"Washington",  "location": u"în zona Institutului de Meteorologie"},
{"name": u"Paris",  "location": u"pe [[DN1]] în apropierea [[Liceul Francez „Anna de Noailles” din București|Liceului Francez „Anna de Noailles”]]"},
{"name": u"Bruxelles", "city": u"orașul [[Otopeni]]",  "location": u"la nord de [[DNCB|Centura București]], lângă pasajul rutier"},
{"name": u"Otopeni", "city": u"orașul [[Otopeni]]",  "location": u"la intersecția dintre Calea Bucureştilor și strada 23 August"},
{"name": u"Ion I.C. Brătianu", "city": u"orașul [[Otopeni]]", "location": u"între străzile Zborului și Nicolae Grigorescu"},
{"name": u"Aeroportul Otopeni", "next": u"Terminus", "city": u"orașul [[Otopeni]]", "location": u"sub parcarea aferentă [[Aeroportul Internațional Henri Coandă București|aeroportului]]"},
]

ref = u"<ref name=\"govro\">{{Citation|url=http://gov.ro/ro/guvernul/sedinte-guvern/magistrala-6-de-metrou-gara-de-nord-aeroport-otopeni-indicatorii-tehnico-economici-au-fost-aprobati|title=Magistrala 6 de metrou – Gara de Nord – Aeroport Otopeni – indicatorii tehnico-economici au fost aprobați|date=2016-12-08|author=Gov.ro}}</ref>"

def article(**kwargs):
    infobox = u"""{{{{Cutie stație metrou București
| nume = {name}
| precedentM6 = {previous}
| următorM6 = {next}
| locație = {location}
| sector = {city}
| peroane = 
}}}}""".format(**kwargs)
    text = u"'''{name}''' este o stație de [[Metroul din București|metrou din București]]. Se va afla în {city}, {location}. Termenul estimat de punere în funcțiune este a doua jumătate a anului 2021.{ref}".format(**kwargs)
    text = infobox + "\n\n" + text
    text += u"""

== Note ==
<references/>

{{ciot-București}}
{{Metrou M6}}

[[Categorie:Stații ale liniei 6 de metrou din București]]
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
            station["city"] = u"[[Sectorul 1]]"
        if "previous" not in station:
            station["previous"] = u"[[" + stations[i-1]["title"] + u"|" + stations[i-1]["name"] + u"]]"
        if "next" not in station:
	    station["next"] = u"[[" + stations[i+1]["name"] + u" (stație de metrou)|" + stations[i+1]["name"] + u"]]"
        station["ref"] = ref
        article(**station)
        i+=1

if __name__ == "__main__":
    processList()
	

