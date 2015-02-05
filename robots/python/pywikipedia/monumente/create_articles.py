#!/usr/bin/python
# -*- coding: utf-8  -*-

import pywikibot
import json
import re
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
        print creator
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
    if len(submonuments) == 0:
        return ""
    
    text = u"\nAnsamblul este format din următoarele monumente:\n"
    for sub in submonuments:
        print sub
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

'''{{{{subst:PAGENAME}}}}''' este un {Tip} aflat pe teritoriul {Localitate2}{Artist2}.<ref>[http://arhiva.cultura.ro/Files/GenericFiles/LMI-2010.pdf Ministerul Culturii - Lista Monumentelor Istorice]</ref>{Ran2}

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

def addVillageToTitle(title, village):
    if title.find(u" din ") == -1 and (not village or title.find(village) == -1):
	title = title.strip()
        title = re.sub(ur'Ansamblul (.*)', ur'Ansamblul \1 din %s' % village, title)
        title = re.sub(ur'Biseric(ă|a) (.*)', ur'Biserica \2 din %s' % village, title)
        title = re.sub(ur'Capela (.*)', ur'Capela \1 din %s' % village, title)
        title = re.sub(ur'Basilica (.*)', ur'Basilica \1 din %s' % village, title)
        title = re.sub(ur'Statuia (.*)', ur'Statuia \1 din %s' % village, title)
        title = re.sub(ur'Centrul (.*)', ur'Centrul \1 din %s' % village, title)
        title = re.sub(ur'Bustul (.*)', ur'Bustul \1 din %s' % village, title)
        title = re.sub(ur'Monumentul (.*)', ur'Monumentul \1 din %s' % village, title)
        title = re.sub(ur'Palatul (.*)', ur'Palatul \1 din %s' % village, title)
        title = re.sub(ur'Conacul (.*)', ur'Conacul \1 din %s' % village, title)
        title = re.sub(ur'Podul (.*)', ur'Podul \1 din %s' % village, title)
        title = re.sub(ur'Cazinou(.*)', ur'Cazinoul\1 din %s' % village, title)
        title = re.sub(ur'Hotel(.*)', ur'Hotelul\1 din %s' % village, title)
    return title

def generateList(seq):
    seen = set()
    seen_add = seen.add
    return [ x for x in seq if not (x in seen or seen_add(x))]
    
def cleanupTitle(monument):
    ret = set()
    title = monument["Denumire"]
    village = monument["Localitate"]
    address = monument[u"Adresă"]
    print title
    village = strainu_functions.extractLink(village)
    if village and village.find(",") > -1:
        village = village[:village.find(",")]

    title = re.sub(ur'(.*?)?"(.*?)"', ur'\1„\2”', title)
    ret.add(title)
    ret.add(title.strip(u" „”"))

    if title.find(u"„") > -1:
        ret.add(title[title.find(u"„")+1:title.find(u"”")])

    title2 = title.replace(u'Ansamblul bisericii', u'Biserica').replace(u'Ansamblul conacului', u'Conacul').replace(u'Ansamblul castelului', u'Castelul').replace(u'Ansamblul cetății', u'Ctatea')
    if title2 != title:
        ret.add(title2)
        ret.add(title2.strip(u"„”"))

    title3 = addVillageToTitle(title, village)
    if title3 != title:
	ret.add(title3)
        ret.add(title3.replace(u"„", u"").replace(u"”", u""))
    title3 = addVillageToTitle(title2, village)
    if title3 != title2:
	ret.add(title3)
        ret.add(title3.replace(u"„", u"").replace(u"”", u""))
        
    if title == u'Casă':
        title = u'Casă din %s (%s)' % (village, address)
	ret.add(title)
    if title == u'Gară':
        title = u'Gara din %s' % (village)
	ret.add(title)

    if title.find(u"(") > -1:
        title = title[:title.find(u"(")]
        ret.add(title)
        title3 = addVillageToTitle(title, village)
        if title3 != title:
	    ret.add(title3)
            ret.add(title3.replace(u"„", u"").replace(u"”", u""))
    if title.find(u",") > -1:
        title = title[:title.find(u",")]
        ret.add(title)
        title3 = addVillageToTitle(title, village)
        if title3 != title:
	    ret.add(title3)
            ret.add(title3.replace(u"„", u"").replace(u"”", u""))
    if title.find(u";") > -1:
        title = title[:title.find(u";")]
        ret.add(title)
        title3 = addVillageToTitle(title, village)
        if title3 != title:
	    ret.add(title3)
            ret.add(title3.replace(u"„", u"").replace(u"”", u""))
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
            no = len(imageList) / 2
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
        com = u"\n{{Commonscat|" + monument["Commons"].replace("commons:Category:", "") + u"}}\n"
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
    f = open("lmi_db.json", "r+")
    pywikibot.output("Reading database file...")
    db = json.load(f, encoding="utf8")
    pywikibot.output("...done")
    f.close()
    f = open("ro_pages.json", "r+")
    pywikibot.output("Reading articles file...")
    art = json.load(f)
    pywikibot.output("...done")
    f.close()
    f = open("commons_File_pages.json", "r+")
    pywikibot.output("Reading images file...")
    images = json.load(f)
    pywikibot.output("...done")
    f.close()
    f = open("ran_orig.json", "r+")
    pywikibot.output("Reading RAN file...")
    ran = json.load(f)
    pywikibot.output("...done")
    f.close()
    
    l = csvUtils.csvToJson("monumente/date_externe/lacase_cimec_descriere.csv", field="cod")
    m = csvUtils.csvToJson("monumente/date_externe/muzeu_cimec_descriere.csv", field="cod")

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
        #pywikibot.output("* [[Cod:LMI:" + monument["Cod"] + "]]: " + monument["Denumire"])
        #continue
        
        submonuments = []
        subcode = splitCode(monument["Cod"])
        for suffix in range(1, 100):
            for nature in ['I', 'II', 'III', 'IV']:
                for clasif in ['a', 's', 'm']:
                    for importance in ['A', 'B']:
                        check = subcode[0] + "-" + nature + "-" + clasif + "-" + importance + "-" + subcode[4] + "." + ("%02d" % suffix)
                        #print check
                        if check in newdb:
                            submonuments.append(newdb[check])
                            break
                    else:
                       continue
                    break
                else:
                    continue
                break
            else:
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
        
