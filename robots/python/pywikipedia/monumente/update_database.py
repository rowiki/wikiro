#!/usr/bin/python
# -*- coding: utf-8  -*-
'''Update the monuments database from some wiki page(s). The databases must be
made from 3 templates: one at the beginning, one at the end, and one for each
line in the database.

The script requires some configuration for each database the user wants to parse.
The global variable ''countries'' is a dictionary. The key is a tuple containing
the language and the database. The value is another dict containing the
following fields:
* project: the project family to work on
* lang: the language to work on
* namespaces: the namespaces that contain the pages to be parsed
* fields: The acceptable fields of rowTemplate; if some other field is found, an
           error message is thrown and that field is ignored
* pagePrefix: A list of page prefixes that we will work on; all other pages are
               ignored

Command line options:

-db	The database to work on. If none is specified, we parse all databases

'''
import sys, time, warnings, json, re
sys.path.append("wikiro/robots/python/pywikipedia")
import pywikibot
from pywikibot import pagegenerators
from pywikibot import config

import monumente

countries = {
	('ro', 'lmi') : {
		'project' : u'wikipedia',
		'lang' : u'ro',
		'rowTemplate' : u'ElementLMI',
		'namespaces' : [0],
		'fields' : [
			u'Cod',
			u'Denumire',
			u'Localitate',
			u'Adresă',
			u'Datare',
			u'Creatori',
			u'Lat',
			u'Lon',
			u'OsmLat',
			u'OsmLon',
			u'Imagine',
			u'Plan',
			u'Commons',
			u'NotăCod',
			u'FostCod',
			u'Cod92',
			u'CodRan',
			u'Copyright',
			u'RefCod',
			],
		'idField': u'Cod',
		'pagePrefix': {
			u'Lista monumentelor istorice din județul',
			u'Lista monumentelor istorice din București',
		}
	},
	('ro', 'ran') : {
		'project' : u'wikipedia',
		'lang' : u'ro',
		'rowTemplate' : u'ElementRAN',
		'namespaces' : [0],
		'fields' : {
			u'Cod': u'Cod',
			u'NotăCod': u'Notăcod',
			u'CodLMI': u'CodLMI',
			u'Nume': u'Nume',
			u'NumeAlternative': u'NumeAlternative',
			u'Categorie': u'Categorie',
			u'TipMonument': u'TipMonument',
			u'Localitate': u'Localitate',
			u'Adresă': u'Adresă',
			u'Cultura': u'Cultura',
			u'Faza': u'Faza',
			u'Datare': u'Datare',
			u'Lat': u'Lat',
			u'Lon': u'Lon',
			u'Descoperit': u'Descoperit',
			u'Descoperitor': u'Descoperitor',
			u'Datare': u'Datare',
			u'Imagine': u'Imagine',
			u'Commons': u'Commons',
			u'Index': u'Index',
			u'CodSIRUTA': u'CodSIRUTA',
			u'Stare': u'Stare',
			u'TipCod': u'TipCod',
			},
		'idField': u'Cod',
		'pagePrefix': {
			u'Lista siturilor arheologice din județul',
			u'Lista siturilor arheologice din București',
		}
	},
	('ro', 'wlemd') : {
		'project' : u'wikipedia',
		'lang' : u'ro',
		'rowTemplate' : u'Wikipedia:Wiki Loves Earth/Moldova/item',
		'namespaces' : [4],
		'fields' : [
			u'ID',
			u'denumire',
			u'proprietar',
			u'descriere',
			u'raion',
			u'ascunde_raion',
			u'amplasament',
			u'suprafata',
			u'latitudine',
			u'longitudine',
			u'tip',
			u'subtip',
			u'imagine',
			u'categorie',
			],
		'idField': u'ID',
		'pagePrefix': {
			u'Wikipedia:Wiki Loves Earth/Moldova/Lista',
		}
	},
}

counties = {
u"București": { "coa": u"Fișier:Bucharest-Coat-of-Arms.png", "count": 0, "list": u"[[Lista monumentelor istorice din București]] (sector [[Lista monumentelor istorice din București, sector 1|1]], [[Lista monumentelor istorice din București, sector 2|2]], [[Lista monumentelor istorice din București, sector 3|3]], [[Lista monumentelor istorice din București, sector 4|4]], [[Lista monumentelor istorice din București, sector 5|5]], [[Lista monumentelor istorice din București, sector 6|6]], [[Lista monumentelor istorice din București cu sector necunoscut|necunoscut]])"},
u"Alba": { "coa": u"Fișier:Actual Alba county CoA.png", "count": 0},
u"Arad": { "coa": u"Fișier:Actual Arad county CoA.png", "count": 0},
u"Argeș": { "coa": u"Fișier:Actual Arges county CoA.png", "count": 0},
u"Bacău": { "coa": u"Fișier:Actual Bacau county CoA.png", "count": 0},
u"Bihor": { "coa": u"Fișier:Actual Bihor county CoA.png", "count": 0},
u"Bistrița-Năsăud": { "coa": u"Fișier:Actual Bistrita-Nasaud county CoA.png", "count": 0},
u"Botoșani": { "coa": u"Fișier:Stema judetului Botosani.svg", "count": 0},
u"Brașov": { "coa": u"Fișier:Actual Brasov county CoA.png", "count": 0},
u"Brăila": { "coa": u"Fișier:Stema judetului Braila.svg", "count": 0},
u"Buzău": { "coa": u"Fișier:Actual Buzau county CoA.png", "count": 0},
u"Caraș-Severin": { "coa": u"Fișier:Actual Caras-Severin county CoA.png", "count": 0},
u"Călărași": { "coa": u"Fișier:Actual Calarasi county CoA.png", "count": 0},
u"Cluj": { "coa": u"Fișier:Actual Cluj county CoA.png", "count": 0},
u"Constanța": { "coa": u"Fișier:Actual Constanta county CoA.png", "count": 0},
u"Covasna": { "coa": u"Fișier:Coa Romania County Kovászna.svg", "count": 0},
u"Dâmbovița": { "coa": u"Fișier:Actual Dambovita county CoA.png", "count": 0},
u"Dolj": { "coa": u"Fișier:RO Dolj county COA.svg", "count": 0},
u"Galați": { "coa": u"Fișier:Stema judetului Galati.svg", "count": 0},
u"Giurgiu": { "coa": u"Fișier:Actual Giurgiu county CoA.png", "count": 0},
u"Gorj": { "coa": u"Fișier:Stema jud Gorj.jpg", "count": 0},
u"Harghita": { "coa": u"Fișier:Actual Harghita county CoA.png", "count": 0},
u"Hunedoara": { "coa": u"Fișier:Actual Hunedoara county CoA.png", "count": 0},
u"Ialomița": { "coa": u"Fișier:Actual Ialomita county CoA.png", "count": 0},
u"Iași": { "coa": u"Fișier:Actual Iasi county CoA.svg", "count": 0},
u"Ilfov": { "coa": u"Fișier:Actual Ilfov county CoA.png", "count": 0},
u"Maramureș": { "coa": u"Fișier:Actual Maramures county CoA.png", "count": 0},
u"Mehedinți": { "coa": u"Fișier:Actual Mehedinti county CoA.png", "count": 0},
u"Mureș": { "coa": u"Fișier:Actual Mures county CoA.png", "count": 0},
u"Neamț": { "coa": u"Fișier:Actual Neamt county CoA.png", "count": 0},
u"Olt": { "coa": u"Fișier:Actual Olt county CoA.png", "count": 0},
u"Prahova": { "coa": u"Fișier:Actual Prahova county CoA.png", "count": 0},
u"Satu Mare": { "coa": u"Fișier:Actual Satu Mare county CoA.png", "count": 0},
u"Sălaj": { "coa": u"Fișier:Actual Salaj county CoA.png", "count": 0},
u"Sibiu": { "coa": u"Fișier:Actual Sibiu county CoA.png", "count": 0},
u"Suceava": { "coa": u"Fișier:Actual Suceava county CoA.png", "count": 0},
u"Teleorman": { "coa": u"Fișier:Actual Teleorman county CoA.png", "count": 0},
u"Timiș": { "coa": u"Fișier:Actual Timis county CoA.svg", "count": 0},
u"Tulcea": { "coa": u"Fișier:Actual Tulcea county CoA.png", "count": 0},
u"Vaslui": { "coa": u"Fișier:Actual Vaslui county CoA.png", "count": 0},
u"Vâlcea": { "coa": u"Fișier:Actual Valcea county CoA.png", "count": 0},
u"Vrancea": { "coa": u"Fișier:Actual Vrancea county CoA.png", "count": 0},
}

monuments_db = []
monuments_counts = {}

def processMonument(params, source, countryconfig, title, previous):
	'''
	Process a single instance of a monument row template
	'''
	 
	# Get all the fields
	contents = {}
	# Add the source of information (permalink)
	contents['source'] = source
	contents['previous'] = previous
	fields = countryconfig.get('fields')
	for field in fields:
		contents[field] = u''

	for param in params:
		# Remove leading or trailing spaces
		field = param
		value = params[param].split("<ref")[0].strip()
		
		#Check first that field is not empty
		if field:
			#Is it in the fields list?
			if field in fields:
				#Load it with Big fucking escape hack. Stupid mysql lib
				if field == countryconfig.get('idField'):
					contents[field] = re.sub(r'\s', '', value.encode("utf8")) # Do this somewhere else.replace("'", "\\'")
				else:
					contents[field] = value.encode("utf8") # Do this somewhere else.replace("'", "\\'")
			else:
				#FIXME: Include more information on where it went wrong
				pywikibot.output(u'Found unknown field: %s on page %s' % (field, title) )
				pywikibot.output(u'Field: %s' % field)
				pywikibot.output(u'Value: %s' % value)
				pywikibot.output(u'Params: %s\n%s' % (params, param))
				#time.sleep(5)
	return monumente.filterOne(contents, countryconfig), contents[countryconfig.get('idField')]

def processText(source, countryconfig, text, title):
	'''
	Process a text containing one or multiple instances of the monument row template
	'''
	templates = pywikibot.textlib.extract_templates_and_params(text)
	pywikibot.output(u'Working on page "%s"' % title)
	prevCode = None
	count = 0
	for (template, params) in templates:
		if template==countryconfig.get('rowTemplate'):
			ret, prevCode = processMonument(params, source, countryconfig, title, prevCode)
			monuments_db.append(ret)
			#time.sleep(5)
			count += 1
	return count
 
def writeOutput(filename):
	global monuments_db
	f = open(filename, "w+")
	json.dump(monuments_db, f, indent=2)
	f.close();
	monuments_db = []

def processDatabase(countryconfig, dbname="lmi"):
	'''
	Process all the monuments of one database
	'''

	global monuments_db

	site = pywikibot.Site(countryconfig.get('lang'), countryconfig.get('project'))
	rowTemplate = countryconfig.get('rowTemplate')
	if rowTemplate.find(':') == -1:
		rowTemplate = u'%s:%s' % (site.namespace(10), countryconfig.get('rowTemplate'))
	rowTemplate = pywikibot.Page(site, rowTemplate)

	transGen = pagegenerators.ReferringPageGenerator(rowTemplate, onlyTemplateInclusion=True)
	filteredGen = pagegenerators.NamespaceFilterPageGenerator(transGen, countryconfig.get('namespaces'))
	pregenerator = pagegenerators.PreloadingGenerator(filteredGen)

	for page in pregenerator:
		prefixes = countryconfig.get('pagePrefix')
		for prefix in prefixes:
			if page.title().find(prefix) > -1:
				break
		else:
			pywikibot.output(u"Page " + page.title() + u" is unknown.")
			continue
		if page.exists() and not page.isRedirectPage():
			# Do some checking
			monuments_counts[page.title()] = processText(page.permalink(), countryconfig, page.get(), page.title(True))

	writeOutput("_".join([countryconfig.get('lang'), dbname, "db.json"]))

def update(database):
	if database:
		lang = pywikibot.Site().language()
		if not countries.get((lang, database)):
			pywikibot.output(u'I have no config for database "%s" in language "%s"' % (database, lang))
			return False
		pywikibot.output(u'Working on database "%s" in language "%s"' % (database, lang))
		processDatabase(countries.get((lang, database)), database)
		updateCounts(database)
	else:
		for (lang, database), countryconfig in countries.iteritems():
			pywikibot.output(u'Working on database "%s" in language "%s"' % (database, lang))
			processDatabase(countryconfig, database)
			updateCounts(database)

def updateCounts(database):
	if database != u"lmi":
		return
	global monuments_counts
	section = u"""==Lista monumentelor pe județ==
<!--lista totală e prea lungă pentru a fi păstrată într-o singură pagină. Editați listele pe fiecare județ-->
Puteți căuta în lista de monumente folosind formularul de mai jos, sau puteți naviga prin paginile cu monumente aferente fiecărui județ.
<inputbox>
type=fulltext
prefix=Lista monumentelor istorice din
break=no
width=30
searchbuttonlabel=Căutare în liste
</inputbox>
{|class="wikitable sortable" width="100%"
!Listă
!Județ
!Număr de monumente
|-
"""
	prev_county = u""
	total_count = 0
	for county in sorted(monuments_counts.keys()):
		if county.find(u"sector") > -1:
			current_county = u"București"
		else:
			current_county = county.replace(u"Lista monumentelor istorice din județul ", u"")
			if current_county.find(" - ") > -1:
				current_county = current_county[:current_county.find("-") - 1]
			if current_county not in counties:
				continue
		if current_county != prev_county:
			if prev_county in counties:
				if "list" in counties[prev_county]:
					section += u"|" + counties[prev_county]["list"] + \
						u"\n|[[" + counties[prev_county]["coa"] + \
						u"|20px]][[județul " + prev_county + u"|" + \
						prev_county + u"]]\n|{{subst:formatnum:" + \
						str(counties[prev_county]["count"]) + u"}}\n|-\n"
				else:
					section += u"|[[Lista monumentelor istorice din județul " + prev_county + \
						u"]]\n|[[" + counties[prev_county]["coa"] + \
						u"|20px]][[județul " + prev_county + u"|" + \
						prev_county + u"]]\n|{{subst:formatnum:" + \
						str(counties[prev_county]["count"]) + u"}}\n|-\n"
			prev_county = current_county

		counties[current_county]["count"] += monuments_counts[county]
		total_count += monuments_counts[county]
	section += u"|[[Lista monumentelor istorice din județul " + prev_county + \
		u"]]\n|[[" + counties[prev_county]["coa"] + \
		u"|20px]][[județul " + prev_county + u"|" + \
		prev_county + u"]]\n|{{subst:formatnum:" + \
		str(counties[prev_county]["count"]) + u"}}\n|-\n"
	section = section.replace(u"județul București", u"București")
	section += "|}\n\n"
	page = pywikibot.Page(pywikibot.getSite(), u"Lista_monumentelor_istorice_din_România#Lista_monumentelor_pe_jude.C8.9B")
	page.get()
	pywikibot.getSite().editpage(page,text=section, section=2)
	pywikibot.output("Saved the monument counts per county")
	monuments_counts = {}
	print total_count
	
def main():
	'''
	The main loop
	'''
	# First find out what to work on

	database = None
	textfile = u''

	for arg in pywikibot.handleArgs():
		if arg.startswith('-db:'):
			database = arg [len('-db:'):]
			
	update(database)

if __name__ == "__main__":
	try:
		main()
	finally:
		pywikibot.stopme()
