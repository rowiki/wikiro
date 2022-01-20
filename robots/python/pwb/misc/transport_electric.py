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

sys.path.append("wikiro/robots/python/pwb")
import strainu_functions as sf


stations = [
  {
    "title": "Troleibuzele din Baia Mare",
    "oras": "Baia Mare",
    "deschidere": "16 februarie 1996",
    "inchidere": "",
    "activ": 1,
    "text": ""
  },
  {
    "title": "Troleibuzele din Brăila",
    "oras": "Brăila",
    "deschidere": "23 august 1989",
    "inchidere": 1999,
    "activ": 0,
    "text": ""
  },
  {
    "title": "Troleibuzele din Brașov",
    "oras": "Brașov",
    "deschidere": "1 mai 1959",
    "inchidere": "",
    "activ": 1,
    "text": ""
  },
  {
    "title": "Troleibuzele din București",
    "oras": "București",
    "deschidere": "10 noiembrie 1949",
    "inchidere": "",
    "activ": 1,
    "text": ""
  },
  {
    "title": "Troleibuzele din Cluj-Napoca",
    "oras": "Cluj-Napoca",
    "deschidere": "7 noiembrie 1959",
    "inchidere": "",
    "activ": 1,
    "text": ""
  },
  {
    "title": "Troleibuzele din Constanța",
    "oras": "Constanța",
    "deschidere": "5 iulie 1959",
    "inchidere": "3 decembrie 2010",
    "activ": 0,
    "text": ""
  },
  {
    "title": "Troleibuzele din Craiova",
    "oras": "Craiova",
    "deschidere": "9 mai 1943",
    "inchidere": "octombrie 1944",
    "activ": 0,
    "text": ""
  },
  {
    "title": "Troleibuzele din Galați",
    "oras": "Galați",
    "deschidere": "23 august 1989",
    "inchidere": "",
    "activ": 1,
    "text": ""
  },
  {
    "title": "Troleibuzele din Iași",
    "oras": "Iași",
    "deschidere": "1 mai 1985",
    "inchidere": "4 martie 2006",
    "activ": 0,
    "text": ""
  },
  {
    "title": "Troleibuzele din Mediaș",
    "oras": "Mediaș",
    "deschidere": "23 august 1989",
    "inchidere": "",
    "activ": 1,
    "text": ""
  },
  {
    "title": "Troleibuzele din Piatra Neamț",
    "oras": "Piatra Neamț",
    "deschidere": "22 decembrie 1995",
    "inchidere": "",
    "activ": 1,
    "text": "Sistemul include linii către Dumbrava Roșie și Săvinești."
  },
  {
    "title": "Troleibuzele din Ploiești",
    "oras": "Ploiești",
    "deschidere": "1 septembrie 1997",
    "inchidere": "",
    "activ": 1,
    "text": ""
  },
  {
    "title": "Troleibuzele din Satu Mare",
    "oras": "Satu Mare",
    "deschidere": "15 noiembrie 1994",
    "inchidere": 2005,
    "activ": 0,
    "text": ""
  },
  {
    "title": "Troleibuzele din Sibiu",
    "oras": "Sibiu",
    "deschidere": 1983,
    "inchidere": 2009,
    "activ": 0,
    "text": ""
  },
  {
    "title": "Troleibuzele din Slatina",
    "oras": "Slatina",
    "deschidere": "30 mai 1996",
    "inchidere": "31 decembrie 2005",
    "activ": 0,
    "text": ""
  },
  {
    "title": "Troleibuzele din Suceava",
    "oras": "Suceava",
    "deschidere": "15 august 1987",
    "inchidere": "2 aprilie 2006",
    "activ": 0,
    "text": ""
  },
  {
    "title": "Troleibuzele din Târgoviște",
    "oras": "Târgoviște",
    "deschidere": "4 ianuarie 1995",
    "inchidere": 2005,
    "activ": 0,
    "text": ""
  },
  {
    "title": "Troleibuzele din Târgu Jiu",
    "oras": "Târgu Jiu",
    "deschidere": "20 iunie 1995",
    "inchidere": "",
    "activ": 1,
    "text": "Sistemul include linie interurbană către Bârsești."
  },
  {
    "title": "Troleibuzele din Timișoara",
    "oras": "Timișoara",
    "deschidere": "15 noiembrie 1942",
    "inchidere": "",
    "activ": 1,
    "text": ""
  },
  {
    "title": "Troleibuzele din Vaslui",
    "oras": "Vaslui",
    "deschidere": "1 mai 1994",
    "inchidere": "",
    "activ": 1,
    "text": ""
  }
]
ref = u"<ref name=\"govro\">{{Citation|url=http://gov.ro/ro/guvernul/sedinte-guvern/magistrala-6-de-metrou-gara-de-nord-aeroport-otopeni-indicatorii-tehnico-economici-au-fost-aprobati|title=Magistrala 6 de metrou – Gara de Nord – Aeroport Otopeni – indicatorii tehnico-economici au fost aprobați|date=2016-12-08|author=Gov.ro}}</ref>"

def article(**kwargs):
    text = u"""{{{{Infocaseta Transport în comun
| nume = Rețeaua de troleibuz din {oras}
| tip = [[troleibuz]]
| localități = {oras}
| deschidere = {deschidere}
| închidere = {inchidere}
}}}}

'''Rețeaua de troleibuz din {oras}''' {asigura} transportul electric din oraș. Rețeaua a fost inaugurată în {deschidere}{text_inchidere}. {text}

{{{{Troleibuze din România}}}}

[[Categorie:Troleibuzele din România]]
[[Categorie:{oras}]]""".format(**kwargs)

    page = pywikibot.Page(pywikibot.getSite(), title=kwargs["title"])
    if page.exists():
        return
    else:
        page.put(text, u"Articol despre o rețea de troleibuz")
        import time
        time.sleep(5)

def processList():
    global stations, ref
    i = 0
    for station in stations:
        if station["inchidere"] != u"":
            station["text_inchidere"] = " și a fost închisă în %s" %  station["inchidere"]
        else:
            station["text_inchidere"] = ""
        if station["activ"]:
            station["asigura"] = u"asigură"
        else:
            station["asigura"] = u"a asigurat"
        article(**station)
        i+=1

if __name__ == "__main__":
    processList()
	

