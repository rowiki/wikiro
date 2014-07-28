#!/usr/bin/python
# -*- coding: utf-8  -*-

import pywikibot
import json

counties = {
"B": u"București",
"AB": u"județul Alba",
"AR": u"județul Arad",
"AG": u"județul Argeș",
"BC": u"județul Bacău",
"BH": u"județul Bihor",
"BN": u"județul Bistrița-Năsăud",
"BT": u"județul Botoșani",
"BV": u"județul Brașov",
"BR": u"județul Brăila",
"BZ": u"județul Buzău",
"CS": u"județul Caraș-Severin",
"CL": u"județul Călărași",
"CJ": u"județul Cluj",
"CT": u"județul Constanța",
"CV": u"județul Covasna",
"DB": u"județul Dâmbovița",
"DJ": u"județul Dolj",
"GL": u"județul Galați",
"GR": u"județul Giurgiu",
"GJ": u"județul Gorj",
"HR": u"județul Harghita",
"HD": u"județul Hunedoara",
"IL": u"județul Ialomița",
"IS": u"județul Iași",
"IF": u"județul Ilfov",
"MM": u"județul Maramureș",
"MH": u"județul Mehedinți",
"MS": u"județul Mureș",
"NT": u"județul Neamț",
"OT": u"județul Olt",
"PH": u"județul Prahova",
"SM": u"județul Satu Mare",
"SJ": u"județul Sălaj",
"SB": u"județul Sibiu",
"SV": u"județul Suceava",
"TR": u"județul Teleorman",
"TM": u"județul Timiș",
"TL": u"județul Tulcea",
"VS": u"județul Vaslui",
"VL": u"județul Vâlcea",
"VN": u"județul Vrancea",
}

def splitCode(code):
    return code.split("-")
    
def extractCounty(code):
    s = splitCode(code)
    return counties[s[0]]
    
def articulate(text):
    return text.replace(u"sat", u"satului") \
                .replace(u"oraș", u"orașului") \
                .replace(u"municipiul", u"municipiului")

def buildArticle(monument):
    #TODO: arhitect
    #TODO: submonumente
    article = u"""{{{{Infocaseta Monument
| nume            = {{{{subst:PAGENAME}}}}
| imagine         = {Imagine}
| localitate      = {Localitate}
| tara            = {{{{ROM}}}}
| latd            = {Lat}
| longd           = {Lon}
| adresa          = {Address}
| data_inceperii_constructiei  = 
| data_finalizarii             = {Datare}
| data_restaurare              = 
| tip_cod            = [[Cod LMI]]
| cod                = {Cod}
}}}}

'''{{{{subst:PAGENAME}}}}''' este un {Tip} aflat pe teritoriul {Localitate2}.<ref>[http://arhiva.cultura.ro/Files/GenericFiles/LMI-2010.pdf Ministerul Culturii - Lista Monumentelor Istorice]</ref> 

"""
    tip = splitCode(monument[u"Cod"])[2]
    if tip == "s":
        monument[u"Tip"] = u"sit"
    if tip == "m":
        monument[u"Tip"] = u"monument istoric"
    if tip == "a":
        monument[u"Tip"] = u"ansamblu de monumente istorice"
        
    return article.format(
        Localitate = monument[u'Localitate'],
        Lat = monument[u'Lat'],
        Lon = monument[u'Lon'],
        Address = monument[u'Adresă'],
        Datare = monument[u'Datare'],
        Cod = monument[u'Cod'],
        Tip = monument[u'Tip'],
        Imagine = monument[u'Imagine'].replace(u"File:", u"") \
                                    .replace(u"Fișier:", u"") \
                                    .replace(u"Image:", u"") \
                                    .replace(u"Imagine:", u""),
        Localitate2 = articulate(monument[u'Localitate']),
    )
    
def addExternalData(text, data):
    if len(data["descr"]) < 100:
        #ignoring short descriptions
        return text
    
    descr = data["descr"].replace("\n", "\n\n")
    added_text = descr + u"<ref name=\"europeana\">" + data["url"] + u"</ref>\n\n"
    return text + added_text
    
def addCategories(text, monument):
    #TODO: gallery
    cats = u"""
== Note ==
<references /> 

{Commons}

[[Categorie:Monumente istorice din {County}]]
    """
    com = u""
    if monument[u"Commons"] != u"":
        com = u"{{Commonscat|" + monument["Commons"].replace("commons:Category:", "") + u"}}"
    return text + cats.format(
        Commons = com,
        County = extractCounty(monument["Cod"])
    )

if __name__ == "__main__":
    f = open("europeana_monuments.json", "r")
    pywikibot.output("Reading europeana file...")
    j = json.load(f)
    pywikibot.output("...done")
    f.close()
    f = open("lmi_db.json", "r+")
    pywikibot.output("Reading database file...")
    db = json.load(f, encoding="utf8")
    pywikibot.output("...done")
    f.close();
    f = open("ro_pages.json", "r+")
    pywikibot.output("Reading articles file...")
    art = json.load(f)
    pywikibot.output("...done")
    f.close()
    
    site = pywikibot.getSite()
    
    for monument in db:
        talkPage = False
        if monument["Cod"] in art:
            #we already have an article about this monument
            continue
            
        text = buildArticle(monument)
        if monument["Cod"] in j:
            text = addExternalData(text, j[monument["Cod"]])
            talkPage = True
        elif monument[u"Imagine"] == "" or splitCode(monument[u"Cod"])[3] == "B":
            #only create A articles with pictures
            continue
            
        text = addCategories(text, monument)
        
        pywikibot.output(text)
        newtitle = monument["Denumire"]
        answer = pywikibot.input(u"Upload change to article %s? ([y]es/[n]o/[r]ename)" % newtitle)
        if answer == 'n' or answer == '':
            continue
        if answer == 'r':
            pywikibot.output(u"The monument is in: %s." % monument["Localitate"])
            newtitle = pywikibot.input("Please input another title:")
        
        newtitle = "Utilizator:Strainu/3"
        page = pywikibot.Page(site, newtitle)
        #TODO:if not page.exists():
        page.put(text, "Scriu un articol nou despre un monument istoric")
        
        if talkPage:
            talk = page.toggleTalkPage()
            if not talk.exists():
                talk.put(u"Pagina conține [%s metadate] din proiectul [[Europeana]], disponibile sub licența [[Creative Commons|CC0]]. Pentru detalii, vedeți [%s această pagină].--~~~~" % (j[monument["Cod"]]["url"], "http://www.europeana.eu/portal/rights/metadata-usage-guidelines.html"), "Disclaimer Europeana")
        
