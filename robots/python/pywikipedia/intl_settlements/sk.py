#!/usr/bin/python3

import re
import sys

import pywikibot
import pywikibot.data.sparql as sparql
sys.path.append("wikiro/robots/python/pywikipedia")
import strainu_functions as sf

listQuery = """SELECT ?item ?itemLabel WHERE {
  ?item wdt:P31 ?instance.
  VALUES (?instance) {
    (wd:Q6784672)
  }

	# look for articles (sitelinks) in ro
  	OPTIONAL { ?sitelink schema:about ?item . ?sitelink schema:inLanguage "ro" }
	# but select items with no such article
	FILTER (!BOUND(?sitelink))
  
  SERVICE wikibase:label { bd:serviceParam wikibase:language "sk". }
}"""

fullListQuery = """SELECT ?item ?itemLabel WHERE {
  ?item wdt:P31 ?instance.
  VALUES (?instance) {
    (wd:Q6784672)
  }
  
  SERVICE wikibase:label { bd:serviceParam wikibase:language "sk". }
}  ORDER BY ?itemLabel"""

infobox = "{{Infocaseta Așezare}}"
history = """

== Istoric ==
Localitatea {nume} este atestată documentar din [[{aninit}]]."""
references = """

== Note ==
<references />"""
extlinks = """

== Legături externe ==
{siteoficial}
{commonscat}"""
categories = """

{{Informații bibliotecare}}
[[Categorie:Localități din Slovacia]]"""

article = """
'''{nume}''' este {articol} [[Comunele Slovaciei|{tip} slovac{sufix}]], aflat{sufix} {district}{sup} {regiune}{sup2}{apa}. Localitatea se află la {{{{alt|{alt}}}}}, se întinde pe o suprafață de {{{{dim|{area}|km<sup>2</sup>}}}} și {anpop} {{{{subst:plural|{pop}|locuitor}}}}.{ref} {listavecini}{listafrati}"""

def listAsString(sprefix, pprefix, suffix, _list):
	if len(_list) == 0:
		return ""
	elif len(_list) == 1:
		out = sprefix
	else:
		out = pprefix
	for elem in _list:
		if elem == _list[-1] and len(_list) > 1:
			out +=" și " + elem
		elif elem == _list[0]:
			out += elem
		else:
			out += ", " + elem
	out += suffix
	return out

reg = re.compile("(?P<name>.+) \((?P<prefix>[^\s]+)\s(?P<sup>.*)\)")
def mangleName(name):
	if name.find("ill") > -1:
		return name
	#if it exists, slink is guaranteed to be a valid page on ro.wp
	slink = sf.extractLink(name)
	sname = sf.stripLink(name)
	if not sname:
		return name
	global reg
	prefix = ["okres", "districtul", "regiunea"]
	result = reg.match(sname)
	#print(name, slink, sname, result)
	if result:
		if result.group('prefix').strip().lower() in prefix:
			return "[[" + (slink or result.group('name') + ", " + result.group('sup')) + "|" + result.group('name') + "]]"
		else:
			return "[[" + (slink or sname) + "|" + result.group('name') + "]]"
	if sname[:sname.find(' ')].lower() in prefix:
		return "[[" + (slink or sname) + "|" + sname[sname.find(' ')+1:] + "]]"
	return name
		


def appendInfobox(text):
	if text and len(text):
		return text + "\n" + infobox
	else:
		return infobox

def appendHistory(text, name, year):
	return text + history.format(**{'nume':name, 'aninit':year})

def appendReferences(text):
	return text + references

def appendLinks(text, site, commonscat):
	if site and len(site):
		site = "* {{siteoficial}}"
	else:
		site = ""
	if commonscat and len(commonscat):
		commonscat = "* {{commonscat-inline}}"
	else:
		commonscat = ""
	return text + extlinks.format(**{'siteoficial':site, 'commonscat':commonscat})

def appendCategories(text):
	return text + categories

def appendArticle(text, nume, tip, sup, sup2, apa, alt, area, anpop, pop, listavecini, listafrati):
	if tip == "oraș":
		articol = "un"
		sufix = ""
	else:
		articol = "o"
		sufix = "ă"

	if apa and len(apa):
		rn = sf.stripLink(apa)
		if rn.lower().find("râu") == 0:
			apa = "[[" + sf.extractLink(apa) + "|" + rn[rn.find(" ")+1:] + "]]"
		apa = ", pe malul râului %s" % apa
	else:
		apa = ""

	ref = ""
	if anpop and len(anpop):
		if anpop == "2017":
			ref = "<ref>[https://slovak.statistics.sk/wps/portal/ext/themes/demography/population/indicators/ Počet obyvateľov SR k 31. 12. 2017]</ref>"
		anpop = "în %s număra" % anpop
	else:
		anpop = "numără"

	if listavecini and len(listavecini):
		listavecini = listAsString("Se învecinează cu comuna ", "Se învecinează cu ", ".", listavecini)
	else:
		listavecini = ""

	if listafrati and len(listafrati):
		listafrati = "\n\n" + listAsString("Localitatea este înfrățită cu ", "Localitatea este înfrățită cu ", ".", listafrati)
	else:
		listafrati = ""

	if sup:
		district = "în districtul "
	else:
		district = ""
		sup = ""

	if sup2:
		regiune = "din regiunea "
	else:
		regiune = ""
		sup2 = ""

	dic = {'nume':nume, 'articol':articol, 'tip':tip, 'sup':sup,
		'sup2':sup2, 'apa':apa, 'alt':alt, 'area':area, 
		'anpop':anpop, 'pop':pop, 'listavecini':listavecini,
		'listafrati':listafrati, 'sufix':sufix, 'ref':ref,
		'district':district, 'regiune':regiune}
	return text + article.format(**dic)

def getLabels(item):
	item.get()
	return item.labels.get('ro') or item.labels.get('sk') or item.labels.get('en') or ''

def toString(target, link=True):
	if type(target) == pywikibot.ItemPage:
		site = pywikibot.getSite().data_repository()
		item = pywikibot.ItemPage(site, target.id)
		text = getLabels(item)
		if link == False:
			return text
		if pywikibot.getSite().dbName() in item.sitelinks:
			localpage = item.sitelinks[pywikibot.getSite().dbName()]
			return "[[%s|%s]]" % (localpage, text)
		return "{{ill-wd|%s}}" % target.id
	if type(target) == pywikibot.WbQuantity:
		r = str(target.amount)
		if r.find('.') > -1:
			r = r.replace('.',',')
		return r
	if type(target) == pywikibot.WbTime:
		return str(target.year)
	if type(target) == pywikibot.WbMonolingualText:
		if target.language not in ['sk', 'en', 'ro']:
			return None
		return str(target.text)
	return str(target)


def getValues(item, prop, link=True, mangle=False):
	item.get()
	quals = None
	if type(prop) == list:
		quals = prop[1] #TODO: handle more qualifiers
		prop = prop[0]
	ret = []
	if prop not in item.claims:
		return ret
	for value in item.claims[prop]:
		if quals:
			value = value.qualifiers.get(quals)
			if not value:
				return ret
			value = value[0]
		value = toString(value.getTarget(), link)
		if not value:
			continue
		if mangle:
			value = mangleName(value)
		ret.append(value)
	return ret

def getValue(item, prop, mangle=False):
	ret = getValues(item, prop, mangle=mangle)
	if len(ret):
		ret = ret[0]
	return ret


def buildPage(item):
	return buildPageText(getItemValues(item))

def getItemValues(item):
	nume = getValue(item, "P1448", mangle=True)
	if len(nume) == 0:
		nume = sf.stripLink(mangleName(getLabels(item)))
		#print(nume)
	tip = getValues(item, "P31", link=False)
	if "oraș" in tip:
		tip = "oraș"
	else:
		tip = "comună"
	sup = getValue(item, "P131", mangle=True)
	if sup:
		supitem = item.claims["P131"][0].getTarget()
		sup2 = getValue(supitem, "P131", mangle=True)
	else:
		#print(item)
		sup2 = None
	apa = getValue(item, "P206")
	alt = getValue(item, "P2044")
	area = getValue(item, "P2046")
	anpop = getValue(item, ["P1082", "P585"])
	pop = getValue(item, "P1082")
	listavecini = getValues(item, "P47", mangle=True)
	listafrati = getValues(item, "P190", mangle=True)
	an = getValue(item, "P1249")
	site = getValue(item, "P856")
	commonscat = getValue(item, "P373")

	return nume, tip, sup, sup2, apa, alt, area, anpop, pop, listavecini, listafrati, an, site, commonscat
	
def buildPageText(nume, tip, sup, sup2, apa, alt, area, anpop, pop, listavecini, listafrati, an, site, commonscat):	
	supret = ""
	if sup and sup.find("ill") == -1:
		supret = sf.stripLink(sup)

	text = ""
	text = appendInfobox(text)
	text = appendArticle(text, nume, tip, sup, sup2, apa, alt, area, anpop, pop, listavecini, listafrati)
	if an and len(an):
		text = appendHistory(text, nume, an)
	text = appendReferences(text)
	text = appendLinks(text, site, commonscat)
	text = appendCategories(text)

	return nume, supret, text

def processPage(text, page, item):
	description = "Creez articol despre o localitate din Slovacia #sknew"
	upload = True
	#if (text.find("ill-wd") > 0 and (text.find("învecinează") > 0 or text.find("înfrățită") > 0)) or text.find("Regiunea") < 0:
	#	upload = False
	#print(upload)
	print(text)
	#upload = upload and pywikibot.input_yn("Create page %s with content above?" % page.title())
	if upload:
		page.put(text, description)
		item.setSitelink(page)
		#null-edit to refresh infobox
		page.put(text, description)

def guessTitle(site, name, sup, pl):
	page = pywikibot.Page(site, name)
	if page not in pl:
		page2 = pywikibot.Page(site, name + ", " + sup)
		if page2 not in pl:
			page3 = pywikibot.Page(site, name + ", Slovacia")
			if page3 not in pl:
				return page2.title(), None
			else:
				page = page3
				name = page3.title()
		else:
			page = page2
			name = page2.title()
	return name, page

def getWikidataSettlements(query):
	q = sparql.SparqlQuery()
	return q.select(query)

def getSettlementListLine(item):
	nume, _, _, sup2, _, _, _, _, _, _, _, _, _, _ = getItemValues(item)
	ret = "* %s {{small|(%s)}}\n" % (mangleName(toString(item)), sup2)
	pywikibot.output(ret)
	return ret

def parse_wikidata_safe():
	site = pywikibot.getSite()
	wdsite = site.data_repository()
	pagelist = pywikibot.Page(site, "Comunele Slovaciei").linkedPages()
	pl = [page for page in pagelist]
	res = getWikidataSettlements(listQuery)
	start = 0
	for r in res:
		qv = r['item'][r['item'].rfind('/')+1:]
		#if r['itemLabel'].find("okres") == -1:
		#	continue
		item = pywikibot.ItemPage(wdsite, qv)
		name, sup, text = buildPage(item)
		name, page = guessTitle(site, name, sup, pl)
		if not page:
			print("Skipping", name)
			continue
		if page.exists():
			print(name, "already exists")
			continue

		processPage(text, page, item)

def parse_wikidata_full():
	site = pywikibot.getSite()
	wdsite = site.data_repository()
	res = getWikidataSettlements(listQuery)
	for r in res:
		qv = r['item'][r['item'].rfind('/')+1:]
		#if r['itemLabel'].find("okres") == -1:
		#	continue
		item = pywikibot.ItemPage(wdsite, qv)
		name, sup, text = buildPage(item)
		name = name + ", " + sup
		page = pywikibot.Page(site, name)
		if page.exists():
			print(name, "already exists")
			continue

		processPage(text, page, item)

def build_settlement_list():
	site = pywikibot.getSite()
	wdsite = site.data_repository()
	ret = "{{div col}}\n"
	res = getWikidataSettlements(fullListQuery)
	for r in res:
		qv = r['item'][r['item'].rfind('/')+1:]
		#if r['itemLabel'].find("okres") == -1:
		#	continue
		item = pywikibot.ItemPage(wdsite, qv)
		ret += getSettlementListLine(item)
	ret += "{{div col end}}"
	return ret

if __name__ == "__main__":
	print(build_settlement_list())
