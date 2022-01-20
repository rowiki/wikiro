#!/usr/bin/python
# -*- coding: utf-8  -*-

import json
import string

def fix_house(house):
  house = house.replace("Jud.", "județul")
  house = house.replace("jud.", "județul")
  house = house.replace("sec.", "secolul")
  return house

f = open("wikiro/js/msat.json", "r")
j = json.load(f)
f.close()

text = ""
colors = {
"Gospodarii": "orange",
"Instalatii": "lightgrey",
"Biserici & troite": "pink",
"Anexe": "#ebaf4c",
"Sectorul nou": "lightblue",
"Hanul „La barieră”": "white"
}

for section in j:
  count = 0
  text += "==" + section + "==\n"
  s = j[section]["section"]
  color = colors[section]
  for house in s:
    count += 1
    h = s[house]["house"]
    url = s[house]["link"]
    casa = fix_house(h["name"])
    if section == "Gospodarii":
      title = "Gospodărie din " + casa
    elif section == "Sectorul nou":
      title = "Casă din " + casa
    else:
      title = casa
    text += "===" + title + "===\n"
    t = h["text"].strip()
    t = t.split("\n")[0]
    if len(t) < 90:
      description = fix_house(t)
    else:
      description = "&nbsp;"
    #description = string.capwords(description,' ')
    table = """{{| width="100%" style="border:solid black 1px;"
|width="200px" | [[Fișier:No image available.svg|faracadru|200px]]
|style="text-align:left;" | 
{{{{resize|1.2em|{{{{background color|{3}|'''&nbsp;{4}&nbsp;'''}}}} '''{0}'''}}}}
<br/>{{{{plain link|{1}|Pagina muzeului}}}}
<br/>{{{{coord|44.47|26.078}}}}

{2}
|}}

"""
    text += table.format(title, url, description, color, count)


f = open("wikiro/data/msat/msat.wiki", "w+")
f.write(text)
f.close()
