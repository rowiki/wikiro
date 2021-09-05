#!/usr/bin/python
# -*- coding: utf-8  -*-

"""This script is used to create new historic monument articles on ro.wp
"""

import pywikibot
import json
import re
import sys

sys.path.append('wikiro/robots/python/pywikipedia')
import strainu_functions
import csvUtils

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

numbers = {
    1: "un",
    2: "două",
    3: "trei",
    4: "patru",
    5: "cinci",
    6: "șase",
    7: "șapte",
    8: "opt",
    9: "nouă",
}

j={}
l={}
m={}
ran={}

def splitCode(code):
    return code.split("-")
    
def extractCounty(code):
    s = splitCode(code)
    return counties[s[0]]
    
def articulate(text):
    return text.replace(u"sat", u"satului") \
                .replace(u"oraș", u"orașului") \
                .replace(u"municipiul", u"municipiului") \
                .replace(u"Sculptor", u"sculptorului") \
                .replace(u"Arhitect", u"arhitectului")

def extractCreator(monument):
    text = monument[u"Creatori"]
    if text == u"":
        return u""
    creators = text.split(u',')

    res = []
    for creator in creators:
        pywikibot.output(creator)
        if '(' not in creator:
            artist = creator
            if splitCode(monument[u"Cod"])[1] == "III":
                tip = u"Sculptor"
            else:
                tip = u"Arhitect"
        else:
            tip,artist = (item[:-1].strip() for item in creator.split('('))
            artist = artist[:-1]
            
        res.append((tip, artist))
    return res

def formatCreator(monument):
    outText = u""
    creators = extractCreator(monument)
    i = 1
    for (tip, artist) in creators:
        outText += u"""| tip_artist%d    = %s
| artist%d         = %s
""" % (i, tip, i, artist)
        i += 1
        
    return outText
    
def writeSubmonuments(submonuments):
    count = len(submonuments)
    if count == 0:
        return ""
    
    text = u"\nAnsamblul este format din %s monumente:\n" % (numbers.get(count) or str(count))
    for sub in submonuments:
        pywikibot.output(sub)
        text += u"* %s ({{codLMI|%s}})\n" %(sub["Denumire"], sub["Cod"])
        
    return text
    
def describeSubmonuments(submonuments):
    if len(submonuments) == 0:
        return ""
        
    global j, l, m, ran
        
    text = ""
    for sub in submonuments:
        added_text = ""
        if sub["Cod"] in j:
            added_text += addExternalData(j[sub["Cod"]], "europeana")
        if sub["Cod"] in l:
            added_text += addExternalData(l[sub["Cod"]], "lăcașe")
        if sub["Cod"] in m:
            added_text += addExternalData(m[sub["Cod"]], "muzee")
        if sub["Cod"] in ran:
            added_text += addExternalData(ran[sub["Cod"]], "ran")
        if added_text != "":
            text += "\n== %s ==\n" % sub["Denumire"]
            
            text += added_text
            text += "\n"
            
    return text
    
def formatRan(monument):
    outText = u""
    if monument["CodRan"]:
        outText = u"""| tip_cod2    = %s
| cod2         = %s""" % (u"[[Cod RAN]]", monument["CodRan"])
    return outText

def buildArticle(monument):
    article = u"""{{{{Infocaseta Monument
| nume            = {Denumire}
| imagine         = {Imagine}
| localitate      = {Localitate}
| tara            = {{{{ROM}}}}
| latd            = {Lat}
| longd           = {Lon}
| adresa          = {Address}
| data_inceperii_constructiei  = 
| data_finalizarii             = {Datare}
| data_restaurare              = 
| tip_cod         = [[Cod LMI]]
| cod             = {Cod}
{Ran}
{Artist}
}}}}

'''{{{{subst:PAGENAME}}}}''' este un {Tip} aflat pe teritoriul {Localitate2}{Artist2}.<ref>[https://patrimoniu.ro/monumente-istorice/lista-monumentelor-istorice Institutul Național al Patrimoniului - Lista Monumentelor Istorice]</ref>{Ran2}

"""
    tip = splitCode(monument[u"Cod"])[2]
    if tip == "s":
        monument[u"Tip"] = u"sit"
        if splitCode(monument[u"Cod"])[1] == "I":
            monument[u"Tip"] += u" arheologic"
    if tip == "m":
        monument[u"Tip"] = u"monument istoric"
    if tip == "a":
        monument[u"Tip"] = u"ansamblu de monumente istorice"
        
    creators = extractCreator(monument)
    artist2 = u""
    if len(creators) == 1:
        tip, artist = creators[0]
        artist2 += u", operă a " + articulate(tip) + u" " + artist
        
    ran = u""
    if monument["CodRan"]:
        ran = u" În [[Repertoriul Arheologic Național]], monumentul apare cu codul " + monument["CodRan"] + ".<ref name=\"ran" + monument["Cod"] + u"\">[http://ran.cimec.ro/ Repertoriul Arheologic Național], CIMEC</ref>"
        
    return article.format(
        Localitate = monument[u'Localitate'],
        Lat = monument[u'Lat'],
        Lon = monument[u'Lon'],
        Address = monument[u'Adresă'],
        Datare = monument[u'Datare'],
        Cod = monument[u'Cod'],
        Tip = monument[u'Tip'],
        Denumire = monument[u'Denumire'],
        Imagine = monument[u'Imagine'].replace(u"File:", u"") \
                                    .replace(u"Fișier:", u"") \
                                    .replace(u"Image:", u"") \
                                    .replace(u"Imagine:", u""),
        Localitate2 = articulate(monument[u'Localitate']).replace(u";", u","),
        Artist = formatCreator(monument),
        Artist2 = artist2,
        Ran = formatRan(monument),
        Ran2 = ran
    )
    
def addExternalData(data, source="europeana"):
    if len(data["descr"]) < 100:
        #ignoring short descriptions
        return ""
    
    descr = data["descr"].replace("\n", "\n\n")
    if source == "europeana":
        added_text = descr + u"<ref name=\"europeana" + data["cod"] + u"\">" + data["url"] + u"</ref>\n\n"
    if source == "muzee":
        added_text = descr + u"<ref name=\"muzeu" + data["cod"] + u"\">[http://ghidulmuzeelor.cimec.ro/ Ghidul Muzeelor], CIMEC</ref>\n\n"
    if source == "lăcașe":
        added_text = descr + u"<ref name=\"biserici" + data["cod"] + u"\">[http://cimec.ro/Monumente/LacaseCult/default_ro.htm Lăcașe de cult], CIMEC</ref>\n\n"
    if source == "ran":
        #added_text = descr + u"<ref name=\"ran" + data["cod"] + u"\">[http://ran.cimec.ro/ Repertoriul Arheologic Național], CIMEC</ref>\n\n"
        added_text = descr + u"<ref name=\"ran" + data["cod"] + u"\" />\n\n"
    return added_text

def addVillageToTitles(titles, village):
    ret = set(titles)
    for title in titles:
        if title.find(u" din ") == -1 and (not village or title.find(village) == -1):
            title = title.strip()
            title = re.sub(r'Ansamblul (.*)', r'Ansamblul \1 din %s' % village, title)
            title = re.sub(r'Biseric(ă|a) (.*)', r'Biserica \2 din %s' % village, title)
            title = re.sub(r'Capela (.*)', r'Capela \1 din %s' % village, title)
            title = re.sub(r'Basilica (.*)', r'Basilica \1 din %s' % village, title)
            title = re.sub(r'Statuia (.*)', r'Statuia \1 din %s' % village, title)
            title = re.sub(r'Centrul (.*)', r'Centrul \1 din %s' % village, title)
            title = re.sub(r'Bustul (.*)', r'Bustul \1 din %s' % village, title)
            title = re.sub(r'Monumentul (.*)', r'Monumentul \1 din %s' % village, title)
            title = re.sub(r'Palatul (.*)', r'Palatul \1 din %s' % village, title)
            title = re.sub(r'Castelul (.*)', r'Castelul \1 din %s' % village, title)
            title = re.sub(r'Conacul (.*)', r'Conacul \1 din %s' % village, title)
            title = re.sub(r'Podul (.*)', r'Podul \1 din %s' % village, title)
            title = re.sub(r'Cazinou(.*)', r'Cazinoul\1 din %s' % village, title)
            title = re.sub(r'Hotel(.*)', r'Hotelul\1 din %s' % village, title)

        if title.find(u" din ") == -1 and (not village or title.find(village) == -1):
            title = title + (" din %s" % village)
        ret.add(title)
    return ret

def generateList(seq):
    seen = set()
    seen_add = seen.add
    return sorted([ x.strip() for x in seq if not (x in seen or seen_add(x))])

def expandSaints(ret):
    i = 0
    lret = list(ret)
    for x in lret:
        if u"Sf." not in x:
            continue
        if any([y in x for y in ["Mihail și Gavril", "Mihail si Gaviil", "Petru și Pavel", "Mucenici", "Apostoli"]]):
            ret.append(x.replace(u"Sf.", u"Sfinții"))
        elif any([y in x for y in ["Nicolae", "Andrei", "Gheorghe", "Dumitru"]]):
            ret.append(x.replace(u"Sf.", u"Sfântul"))
        elif any([y in x for y in ["Maria", "Parascheva", "Paraschiva", "Treime"]]):
            ret.append(x.replace(u"Sf.", u"Sfânta"))
        else:
            ret.append(x.replace(u"Sf.", u"Sfinții"))
            ret.append(x.replace(u"Sf.", u"Sfântul"))
            ret.append(x.replace(u"Sf.", u"Sfânta"))
    return ret
    
def cleanupTitle(monument):
    ret = set()
    title = monument["Denumire"]
    village = monument["Localitate"]
    address = monument[u"Adresă"]
    pywikibot.output("***" + title)
    village = strainu_functions.extractLink(village)
    if village and village.find(",") > -1:
        village = village[:village.find(",")]

    title = re.sub(r'<br\s?\/>', r' ', title)
    title = re.sub(r'(.*?)?"(.*?)"', r'\1„\2”', title)
    #print title
    ret.add(title)

    if title.find(u"„") > -1:
        #print title[title.find(u"„")+1:title.find(u"”")]
        ret.add(title[title.find(u"„")+1:title.find(u"”")])

    title2 = title.replace(u'Ansamblul conacului', u'Conacul').\
        replace(u'Ansamblul castelului', u'Castelul').\
        replace(u'Ansamblul cetății', u'Cetatea').\
        replace(u'Ansamblul curții', u'Curtea').\
        replace(u'Ansamblul capelei', u'Capela')
    title2 = re.sub(r'Ansamblul bisericii ([\w\-]*)e', r'Biserica \1ă', title2)
    title2 = re.sub(r'evanghelică fortificate', r'evanghelică fortificată', title2)
    title2 = re.sub(r'Ansamblul bisericii', r'Biserica', title2)
    title2 = re.sub(r'Fosta [mM]ănăstire( a?)', r'Mănăstirea', title2)
    title2 = re.sub(r'Biserica de lemn(.*)', r'Biserica de lemn din %s' % village, title2)
    title2 = title2.replace(u' dă ', u' de ')
    if title2 != title:
        #print title2
        ret.add(title2)
    ret.add(title2.replace(u"„", u"").replace(u"”", u""))
    title3 = re.sub(r'evanghelică C\.?\s?A\.?', r'evanghelică', title2)
    if title3 != title2:
        #print(title3)
        ret.add(title3)    

    if title == u'Casă':
        title = u'Casă din %s (%s)' % (village, address)
        #print title
        ret.add(title)
    if title == u'Gară':
        title = u'Gara din %s' % (village)
        #print title
        ret.add(title)

    if title.find(u"(") > -1:
        title2 = title[:title.find(u"(")]
        #print title2
        ret.add(title2)
        title2 = title[title.find(u"(")+1:title.find(u")")]
        ret.add(title2)
    if title.find(u",") > -1:
        title2 = title[:title.find(u",")]
        #print title2
        ret.add(title2)
    if title.find(u";") > -1:
        title2 = title[:title.find(u";")]
        #print title2
        ret.add(title2)

    if title.find(u"azi ") > -1:
        title2 = title[:title.find(u"azi ")]
        if title2.endswith(u', '):
            title2 = title2[:-2]
        #print title2
        ret.add(title2)
        title3 = title[title.find(u"azi ")+len(u"azi"):]
        #print title3
        ret.add(title3)
    elif title.find(u"în prezent ") > -1:
        title2 = title[:title.find(u"în prezent ")]
        if title2.endswith(u', '):
            title2 = title2[:-2]
        #print title2
        ret.add(title2)
        title3 = title[title.find(u"în prezent ")+len(u"în prezent"):]
        #print title3
        ret.add(title3)
    print(ret)
    ret = set(expandSaints(list(ret)))
    print(ret)
    ret = addVillageToTitles(ret, village)
    #print ret
    return generateList(ret)

def generateGallery(imageList, articleImage):
    no = -1
    start = u""
    end = u""
    text = u""
    if len(imageList) > 4: 
        if len(imageList) <= 8:
            no = 4
        elif len(imageList) <= 16:
            no = int(len(imageList) / 2)
        else:
            no = 8
        start = u"==Galerie==\n<gallery>\n"
        end = u"</gallery>\n"

    text += start
    while no >= 0:
        no -= 1
        if imageList[no]["name"] == articleImage:
            continue
        text += imageList[no]["name"] + "\n"

    text += end

    return text


def addSuffix(text, monument, images):
    cats = u"""
== Note ==
<references /> 
{Commons}
{Gallery}

[[Categorie:Monumente istorice din {County}]]
    """
    com = u""
    if monument[u"Commons"] != u"":
        com = u"\n==Legături externe=="
        com += u"\n* {{Commonscat-inline|" + monument["Commons"].replace("commons:Category:", "") + u"}}\n"
    img = []
    if monument["Cod"] in images:
        img = images[monument["Cod"]]
    return text + cats.format(
        Commons = com,
        County = extractCounty(monument["Cod"]),
        Gallery = generateGallery(img, monument[u'Imagine']),
    )

def rlinput(prompt, prefill=''):
   readline.set_startup_hook(lambda: readline.insert_text(prefill))
   try:
      return raw_input(prompt)
   finally:
      readline.set_startup_hook()

if __name__ == "__main__":
    f = open("europeana_monuments.json", "r")
    pywikibot.output("Reading europeana file...")
    j = json.load(f)
    pywikibot.output("...done")
    f.close()
    f = open("ro_lmi_db.json", "r+")
    pywikibot.output("Reading database file...")
    db = json.load(f, encoding="utf8")
    pywikibot.output("...done")
    f.close()
    f = open("ro_lmi_pages.json", "r+")
    pywikibot.output("Reading articles file...")
    art = json.load(f)
    pywikibot.output("...done")
    f.close()
    f = open("commons_lmi_File_pages.json", "r+")
    pywikibot.output("Reading images file...")
    images = json.load(f)
    pywikibot.output("...done")
    f.close()
    f = open("ran_orig.json", "r+")
    pywikibot.output("Reading RAN file...")
    ran = json.load(f)
    pywikibot.output("...done")
    f.close()
    
    l = csvUtils.csvToJson("lacase_cimec_descriere.csv", field="cod")
    m = csvUtils.csvToJson("muzeu_cimec_descriere.csv", field="cod")

    site = pywikibot.Site()
    
    newdb = {}
    for monument in db:
        newdb[monument["Cod"]] = monument
    
    for monument in db:
        talkPage = False
        if monument["Cod"] in art:
            #we already have an article about this monument
            continue
        if u"." in monument["Cod"]:
            #no article for submonuments
            continue

        #generate articles for A-grade monuments with pics or 
        #for monuments with external data
        if monument["Cod"] not in j and (monument[u"Imagine"] == "" or splitCode(monument[u"Cod"])[3] == "B"):
            continue
        if monument["Denumire"].find("[[") > -1:
            continue
        if monument["Denumire"].find("Casă") > -1:
            continue
        if monument["Denumire"].find("Casa ") > -1:
            continue
        if monument["Denumire"].find("Cruce") > -1:
            continue
        #pywikibot.output("* [[Cod:LMI:" + monument["Cod"] + "]]: " + monument["Denumire"])
        #continue
        
        submonuments = []
        subcode = splitCode(monument["Cod"])
        for suffix in range(1, 400):
            for nature in ['I', 'II', 'III', 'IV']:
                for clasif in ['a', 's', 'm']:
                    for importance in ['A', 'B']:
                        check = subcode[0] + "-" + nature + "-" + clasif + "-" + importance + "-" + subcode[4] + "." + ("%02d" % suffix)
                        #pywikibot.output(check)
                        if check in newdb:
                            submonuments.append(newdb[check])
                            break
                    else:
                        continue
                    break
                else:
                    continue
                break

        text = buildArticle(monument)
        if monument["Cod"] in j:
            text += addExternalData(j[monument["Cod"]])
            talkPage = True
        if monument["Cod"] in l:
            text += addExternalData(l[monument["Cod"]], "lăcașe")
            talkPage = True
        if monument["Cod"] in m:
            text += addExternalData(m[monument["Cod"]], "muzee")
            talkPage = True
        if monument["Cod"] in ran:
            text += addExternalData(ran[monument["Cod"]], "ran")
            talkPage = True
        
        text += writeSubmonuments(submonuments)
        text += describeSubmonuments(submonuments)
        text = addSuffix(text, monument, images)
        
        pywikibot.output(text)
        newtitles = cleanupTitle(monument)
        versions = "[s]kip\n[r]ename\n"
        for x in range(len(newtitles)):
            versions += "[%d]: %s\n" % (x, newtitles[x])
        answer = pywikibot.input(u"Choose a name for the article?\n%s" % versions)
        if answer == 's' or answer == '':
            continue
        if answer == 'r':
            pywikibot.output(u"The monument is in: %s." % monument["Localitate"])
            newtitle = pywikibot.input(u"Please input another title:")
        elif int(answer) < len(newtitles):
            newtitle = newtitles[int(answer)]
        else:
            continue
            
        #newtitle = "Utilizator:Strainu/3"
        page = pywikibot.Page(site, newtitle)
        if not page.exists():
            page.put(text, "Scriu un articol nou despre un monument istoric")

        if talkPage:
            talk = page.toggleTalkPage()
            if not talk.exists():
                if monument["Cod"] in j:
                    talk.put(u"{{dateCimec}}\n\nPagina conține [%s metadate] din proiectul [[Europeana]], disponibile sub licența [[Creative Commons|CC0]]. Pentru detalii asupra licenței, vedeți [%s această pagină].--~~~~" % (j[monument["Cod"]]["url"], "http://www.europeana.eu/portal/rights/metadata-usage-guidelines.html"), "Disclaimer Europeana")
                else:
                    talk.put(u"{{dateCimec}}")
        
