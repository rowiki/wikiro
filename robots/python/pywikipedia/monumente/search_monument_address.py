#!/usr/bin/python
# -*- coding: utf-8  -*-
'''
Read & parse file

'''
import sys, time, warnings, json, re
sys.path.append(".")
from geo import overpass

import strainu_functions

outname = "bucharest2_addresses.csv"
polygon = u"București"

def searchOsm(wikidata):
    for adresa in wikidata:
         print adresa
         strd = adresa["strada"]
         nr = adresa["nr"]
         cod = adresa["cod"]
	 oras = adresa["oras"]
         filters = {
             u"a": {
                 u"addr:street": (strd.strip(), True),
                 u"addr:city": (oras, True),
                 u"addr:housenumber": (nr, True),
             },
         }
         bot = overpass.OverpassRequest(poly=polygon, filters=filters, output="json")
         js = bot.fetchNode()
	 #js = bot.fetchWay()
         print "-------------------------------"
	 try:
             obj = json.loads(js, "utf8")
	 except:
             print "Trouble with the latest address"
	     print js
	     continue
         #outfile = open("bucharest_addresses.csv", "a")
         outfile = open(outname, "a")
         print obj
	 #continue
         for element in obj["elements"]:
             if element["tags"]["addr:housenumber"] != nr:
                 continue
             outfile.write(cod + u"," + str(element["lat"]) + u"," + str(element["lon"]) + u"," + str(element["id"]) + "\n")                
         outfile.close()
         print "-------------------------------"


def writeAddress():
    ret = []
    start = False
    for monument in db:
        cod = monument["Cod"]
        #if cod == "MS-II-m-B-15500":
        #    start = True
        start = True
        if not start:
	    continue
#TODO - adrese f.n.        
        if cod[0:2] == "B-":
            m = re.match(ur"^(?i)(Șos.|Str.|Calea|Bd.|Aleea|Intr.|Piața|Splaiul)([^0-9]+)(\d+-?\d*?)[^0-9]+sector[^0-9]*(\d)(.*)", monument[u"Adresă"])
            #m = re.match(ur"^(?i)(Șos.|Str.|Calea|Bd.|Aleea|Intr.|Piața|Splaiul)([^0-9]+)(\d+(-\d*)?)", monument[u"Adresă"])
            if m:
                tempStr = m.group(1).strip().lower()
                if tempStr == u"str.":
                    tempStr = u"Strada"
                if tempStr == u"șos.":
                    tempStr = u"Șoseaua"
                if tempStr == u"intr.":
                    tempStr = u"Intrarea"
                if tempStr == u"bd.":
                    tempStr = u"Bulevardul"
                print u"Strada=" + m.group(2)
                print u"Număr=" + m.group(3)
                #print u"Sector=" + m.group(6)
                #print monument[u"Adresă"]
                key = ""
                ret.append({
                        "cod": cod,
                        "strada": tempStr + u" " + m.group(2).replace("nr.", "").strip(),
                        "nr":m.group(3),
			"oras": strainu_functions.stripLink(monument[u"Localitate"]),
                })

    return ret

f = open("ro_lmi_db.json", "r+")
db = json.load(f)
f.close()
searchOsm(writeAddress())

