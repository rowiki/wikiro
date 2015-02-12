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
#sys.path.append("..")
import pywikibot
from pywikibot import pagegenerators
from pywikibot import config

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
			u'Imagine',
			u'Commons',
			u'NotăCod',
			u'FostCod',
			u'Cod92',
			u'CodRan',
			u'Copyright',
			],
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
		'pagePrefix': {
			u'Lista siturilor arheologice din județul',
			u'Lista siturilor arheologice din București',
		}
	},
}

monuments_db = []

def processMonument(params, source, countryconfig, title):
	'''
	Process a single instance of a monument row template
	'''
	 
	# Get all the fields
	contents = {}
	# Add the source of information (permalink)
	contents['source'] = source
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
				if field == "Cod":
					contents[field] = re.sub(r'\s', '', value.encode("utf8")) # Do this somewhere else.replace("'", "\\'")
				else:
					contents[field] = value.encode("utf8") # Do this somewhere else.replace("'", "\\'")
			else:
				#FIXME: Include more information where it went wrong
				pywikibot.output(u'Found unknown field: %s on page %s' % (field, title) )
				pywikibot.output(u'Field: %s' % field)
				pywikibot.output(u'Value: %s' % value)
				pywikibot.output(u'Params: %s\n%s' % (params, param))
				#time.sleep(5)
	return contents

def processText(source, countryconfig, page=None):
	'''
	Process a text containing one or multiple instances of the monument row template
	'''
	templates = pywikibot.textlib.extract_templates_and_params(page.get())
	title = page.title(True)
	pywikibot.output(u'Working on page "%s"' % title)
	for (template, params) in templates:
		if template==countryconfig.get('rowTemplate'):
			monuments_db.append(processMonument(params, source, countryconfig, title))
			#time.sleep(5)
 

def processDatabase(countryconfig, dbname="lmi"):
	'''
	Process all the monuments of one database
	'''

	site = pywikibot.Site(countryconfig.get('lang'), countryconfig.get('project'))
	rowTemplate = pywikibot.Page(site, u'%s:%s' % (site.namespace(10), countryconfig.get('rowTemplate')))

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
			processText(page.permalink(), countryconfig, page=page)
			
	f = open("_".join([countryconfig.get('lang'), dbname, "db.json"]), "w+")
	json.dump(monuments_db, f, indent=2)
	f.close();


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

	if database:
		lang = pywikibot.Site().language()
		if not countries.get((lang, database)):
			pywikibot.output(u'I have no config for database "%s" in language "%s"' % (database, lang))
			return False
		pywikibot.output(u'Working on database "%s" in language "%s"' % (database, lang))
		processDatabase(countries.get((lang, database)), database)
	else:
		for (lang, database), countryconfig in countries.iteritems():
			pywikibot.output(u'Working on database "%s" in language "%s"' % (database, lang))
			processDatabase(countryconfig, database)
	

if __name__ == "__main__":
	try:
		main()
	finally:
		pywikibot.stopme()
