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
		'project' : 'wikipedia',
		'lang' : 'ro',
		'rowTemplate' : 'ElementLMI',
		'namespaces' : [0],
		'fields' : [
			'Cod',
			'Denumire',
			'Localitate',
			'Adresă',
			'Datare',
			'Creatori',
			'Lat',
			'Lon',
			'OsmLat',
			'OsmLon',
			'Imagine',
			'Plan',
			'Commons',
			'NotăCod',
			'FostCod',
			'Cod92',
			'CodRan',
			'Copyright',
			'RefCod',
			],
		'idField': 'Cod',
		'pagePrefix': {
			'Lista monumentelor istorice din județul',
			'Lista monumentelor istorice din București',
		}
	},
	('ro', 'ran') : {
		'project' : 'wikipedia',
		'lang' : 'ro',
		'rowTemplate' : 'ElementRAN',
		'namespaces' : [0],
		'fields' : {
			'Cod': 'Cod',
			'NotăCod': 'Notăcod',
			'CodLMI': 'CodLMI',
			'Nume': 'Nume',
			'NumeAlternative': 'NumeAlternative',
			'Categorie': 'Categorie',
			'TipMonument': 'TipMonument',
			'Localitate': 'Localitate',
			'Adresă': 'Adresă',
			'Cultura': 'Cultura',
			'Faza': 'Faza',
			'Datare': 'Datare',
			'Lat': 'Lat',
			'Lon': 'Lon',
			'Descoperit': 'Descoperit',
			'Descoperitor': 'Descoperitor',
			'Datare': 'Datare',
			'Imagine': 'Imagine',
			'Commons': 'Commons',
			'Index': 'Index',
			'CodSIRUTA': 'CodSIRUTA',
			'Stare': 'Stare',
			'TipCod': 'TipCod',
			},
		'idField': 'Cod',
		'pagePrefix': {
			'Lista siturilor arheologice din județul',
			'Lista siturilor arheologice din București',
		}
	},
	('ro', 'wlemd') : {
		'project' : 'wikipedia',
		'lang' : 'ro',
		'rowTemplate' : 'Wikipedia:Wiki Loves Earth/Moldova/item',
		'namespaces' : [4],
		'fields' : [
			'ID',
			'denumire',
			'proprietar',
			'descriere',
			'raion',
			'ascunde_raion',
			'amplasament',
			'suprafata',
			'latitudine',
			'longitudine',
			'tip',
			'subtip',
			'imagine',
			'categorie',
			],
		'idField': 'ID',
		'pagePrefix': {
			'Wikipedia:Wiki Loves Earth/Moldova/Lista',
		}
	},
}

counties = {
"București": { "coa": "Fișier:Bucharest-Coat-of-Arms.png", "count": 0, "list": "[[Lista monumentelor istorice din București]] (sector [[Lista monumentelor istorice din București, sector 1|1]], [[Lista monumentelor istorice din București, sector 2|2]], [[Lista monumentelor istorice din București, sector 3|3]], [[Lista monumentelor istorice din București, sector 4|4]], [[Lista monumentelor istorice din București, sector 5|5]], [[Lista monumentelor istorice din București, sector 6|6]], [[Lista monumentelor istorice din București cu sector necunoscut|necunoscut]])"},
"Alba": { "coa": "Fișier:Actual Alba county CoA.png", "count": 0},
"Arad": { "coa": "Fișier:Actual Arad county CoA.png", "count": 0},
"Argeș": { "coa": "Fișier:Actual Arges county CoA.png", "count": 0},
"Bacău": { "coa": "Fișier:Actual Bacau county CoA.png", "count": 0},
"Bihor": { "coa": "Fișier:Actual Bihor county CoA.png", "count": 0},
"Bistrița-Năsăud": { "coa": "Fișier:Actual Bistrita-Nasaud county CoA.png", "count": 0},
"Botoșani": { "coa": "Fișier:Stema judetului Botosani.svg", "count": 0},
"Brașov": { "coa": "Fișier:Actual Brasov county CoA.png", "count": 0},
"Brăila": { "coa": "Fișier:Stema judetului Braila.svg", "count": 0},
"Buzău": { "coa": "Fișier:Actual Buzau county CoA.png", "count": 0},
"Caraș-Severin": { "coa": "Fișier:Actual Caras-Severin county CoA.png", "count": 0},
"Călărași": { "coa": "Fișier:Actual Calarasi county CoA.png", "count": 0},
"Cluj": { "coa": "Fișier:Actual Cluj county CoA.png", "count": 0},
"Constanța": { "coa": "Fișier:Actual Constanta county CoA.png", "count": 0},
"Covasna": { "coa": "Fișier:Coa Romania County Kovászna.svg", "count": 0},
"Dâmbovița": { "coa": "Fișier:Actual Dambovita county CoA.png", "count": 0},
"Dolj": { "coa": "Fișier:RO Dolj county COA.svg", "count": 0},
"Galați": { "coa": "Fișier:Stema judetului Galati.svg", "count": 0},
"Giurgiu": { "coa": "Fișier:Actual Giurgiu county CoA.png", "count": 0},
"Gorj": { "coa": "Fișier:Stema jud Gorj.jpg", "count": 0},
"Harghita": { "coa": "Fișier:Actual Harghita county CoA.png", "count": 0},
"Hunedoara": { "coa": "Fișier:Actual Hunedoara county CoA.png", "count": 0},
"Ialomița": { "coa": "Fișier:Actual Ialomita county CoA.png", "count": 0},
"Iași": { "coa": "Fișier:Actual Iasi county CoA.svg", "count": 0},
"Ilfov": { "coa": "Fișier:Actual Ilfov county CoA.png", "count": 0},
"Maramureș": { "coa": "Fișier:Actual Maramures county CoA.png", "count": 0},
"Mehedinți": { "coa": "Fișier:Actual Mehedinti county CoA.png", "count": 0},
"Mureș": { "coa": "Fișier:Actual Mures county CoA.png", "count": 0},
"Neamț": { "coa": "Fișier:Actual Neamt county CoA.png", "count": 0},
"Olt": { "coa": "Fișier:Actual Olt county CoA.png", "count": 0},
"Prahova": { "coa": "Fișier:Actual Prahova county CoA.png", "count": 0},
"Satu Mare": { "coa": "Fișier:Actual Satu Mare county CoA.png", "count": 0},
"Sălaj": { "coa": "Fișier:Actual Salaj county CoA.png", "count": 0},
"Sibiu": { "coa": "Fișier:Actual Sibiu county CoA.png", "count": 0},
"Suceava": { "coa": "Fișier:Actual Suceava county CoA.png", "count": 0},
"Teleorman": { "coa": "Fișier:Actual Teleorman county CoA.png", "count": 0},
"Timiș": { "coa": "Fișier:Actual Timis county CoA.svg", "count": 0},
"Tulcea": { "coa": "Fișier:Actual Tulcea county CoA.png", "count": 0},
"Vaslui": { "coa": "Fișier:Actual Vaslui county CoA.png", "count": 0},
"Vâlcea": { "coa": "Fișier:Actual Valcea county CoA.png", "count": 0},
"Vrancea": { "coa": "Fișier:Actual Vrancea county CoA.png", "count": 0},
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
		contents[field] = ''

	for param in params:
		# Remove leading or trailing spaces
		field = param.strip()
		value = params[param].split("<ref")[0].strip()
		
		#Check first that field is not empty
		if field:
			#Is it in the fields list?
			if field in fields:
				#Load it with Big fucking escape hack. Stupid mysql lib
				if field == countryconfig.get('idField'):
					contents[field] = re.sub(r'\s', '', value) # Do this somewhere else.replace("'", "\\'")
				else:
					contents[field] = value # Do this somewhere else.replace("'", "\\'")
			else:
				#FIXME: Include more information on where it went wrong
				pywikibot.output('Found unknown field: %s on page %s' % (field, title) )
				pywikibot.output('Field: %s' % field)
				pywikibot.output('Value: %s' % value)
				pywikibot.output('Params: %s\n%s' % (params, param))
				#time.sleep(5)
	return monumente.filterOne(contents, countryconfig), contents[countryconfig.get('idField')]

def processText(source, countryconfig, text, title):
	'''
	Process a text containing one or multiple instances of the monument row template
	'''
	templates = pywikibot.textlib.extract_templates_and_params(text)
	pywikibot.output('Working on page "%s"' % title)
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
		rowTemplate = '%s:%s' % (site.namespace(10), countryconfig.get('rowTemplate'))
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
			pywikibot.output("Page " + page.title() + " is unknown.")
			continue
		if page.exists() and not page.isRedirectPage():
			# Do some checking
			monuments_counts[page.title()] = processText(page.permalink(), countryconfig, page.get(), page.title(True))

	writeOutput("_".join([countryconfig.get('lang'), dbname, "db.json"]))

def update(database):
	if database:
		lang = pywikibot.Site().language()
		if not countries.get((lang, database)):
			pywikibot.output('I have no config for database "%s" in language "%s"' % (database, lang))
			return False
		pywikibot.output('Working on database "%s" in language "%s"' % (database, lang))
		processDatabase(countries.get((lang, database)), database)
		updateCounts(database)
	else:
		for (lang, database), countryconfig in countries.items():
			pywikibot.output('Working on database "%s" in language "%s"' % (database, lang))
			processDatabase(countryconfig, database)
			updateCounts(database)

def updateCounts(database):
	if database != "lmi":
		return
	global monuments_counts
	section = """==Lista monumentelor pe județ==
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
	prev_county = ""
	total_count = 0
	for county in sorted(monuments_counts.keys()):
		if county.find("sector") > -1:
			current_county = "București"
		else:
			current_county = county.replace("Lista monumentelor istorice din județul ", "")
			if current_county.find(" - ") > -1:
				current_county = current_county[:current_county.find("-") - 1]
			if current_county not in counties:
				continue
		if current_county != prev_county:
			if prev_county in counties:
				if "list" in counties[prev_county]:
					section += "|" + counties[prev_county]["list"] + \
						"\n|[[" + counties[prev_county]["coa"] + \
						"|20px]][[județul " + prev_county + "|" + \
						prev_county + "]]\n|{{subst:formatnum:" + \
						str(counties[prev_county]["count"]) + "}}\n|-\n"
				else:
					section += "|[[Lista monumentelor istorice din județul " + prev_county + \
						"]]\n|[[" + counties[prev_county]["coa"] + \
						"|20px]][[județul " + prev_county + "|" + \
						prev_county + "]]\n|{{subst:formatnum:" + \
						str(counties[prev_county]["count"]) + "}}\n|-\n"
			prev_county = current_county

		counties[current_county]["count"] += monuments_counts[county]
		total_count += monuments_counts[county]
	section += "|[[Lista monumentelor istorice din județul " + prev_county + \
		"]]\n|[[" + counties[prev_county]["coa"] + \
		"|20px]][[județul " + prev_county + "|" + \
		prev_county + "]]\n|{{subst:formatnum:" + \
		str(counties[prev_county]["count"]) + "}}\n|-\n"
	section = section.replace("județul București", "București")
	section += "|}\n\n"
	page = pywikibot.Page(pywikibot.getSite(), "Lista_monumentelor_istorice_din_România#Lista_monumentelor_pe_jude.C8.9B")
	page.get()
	pywikibot.getSite().editpage(page,text=section, section=2)
	pywikibot.output("Saved the monument counts per county")
	monuments_counts = {}
	print(total_count)
	
def main():
	'''
	The main loop
	'''
	# First find out what to work on

	database = None
	textfile = ''

	for arg in pywikibot.handleArgs():
		if arg.startswith('-db:'):
			database = arg [len('-db:'):]
			
	update(database)

if __name__ == "__main__":
	try:
		main()
	finally:
		pywikibot.stopme()
