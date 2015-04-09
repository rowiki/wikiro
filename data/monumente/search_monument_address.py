#!/usr/bin/python
# -*- coding: utf-8  -*-
'''
Read & parse file

'''
import sys, time, warnings, json, re
from geo import overpass

def searchOsm(wikidata):
    for adresa in wikidata:
         print adresa
         strd = adresa["strada"]
         nr = adresa["nr"]
         cod = adresa["cod"]
         filters = {
             u"a": {
                 u"addr:street": (strd.strip(), True),
                # u"addr:city": (u"București", True),
                 u"addr:housenumber": (nr, True),
             },
         }
         bot = overpass.OverpassRequest(poly=u"București", filters=filters, output="json")
         js = bot.fetchNode()
         print "-------------------------------"
         obj = json.loads(js, "utf8")
         outfile = open("bucharest_addresses.csv", "a")
         print obj["elements"]
         for element in obj["elements"]:
             if element["tags"]["addr:housenumber"] != nr:
                 continue
             outfile.write(cod + u"," + str(element["lat"]) + u"," + str(element["lon"]) + u"," + str(element["id"]) + "\n")                
         outfile.close()
         print "-------------------------------"


def writeAddress():
    ret = []
    for monument in db:
        cod = monument["Cod"]
#TODO - adrese f.n.        
        if cod[0:2] == "B-":
            m = re.match(ur"^(?i)(Șos.|Str.|Calea|Bd.|Aleea|Intr.|Piața|Splaiul)([^0-9]+)(\d+-?\d*?)[^0-9]+sector[^0-9]*(\d)(.*)", monument[u"Adresă"])
            if m:
                tempStr = m.group(1).strip()
                if tempStr == u"Str.":
                    tempStr = u"Strada"
                if tempStr == u"Șos.":
                    tempStr = u"Șoseaua"
                if tempStr == u"Intr.":
                    tempStr = u"Intrarea"
                if tempStr == u"Bd.":
                    tempStr = u"Bulevardul"
                #print u"Strada=" + tempStr + m.group(2)
                #print u"Număr=" + m.group(3)
                #print u"Sector=" + m.group(4)
                print monument[u"Adresă"]
                key = ""
                ret.append({
                        "cod": cod,
                        "strada": tempStr + u" " + m.group(2).strip(),
                        "nr":m.group(3)
                })
    return ret

f = open("ro_lmi_db.json", "r+")
db = json.load(f)
f.close()
searchOsm(writeAddress())

