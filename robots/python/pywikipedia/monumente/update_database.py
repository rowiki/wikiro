#!/usr/bin/python
# -*- coding: utf-8  -*-
'''
Update the monuments database either from a text file or from some wiki page(s)

'''
import sys, time, warnings, json, re
#sys.path.append("..")
import pywikibot
from pywikibot import pagegenerators
from pywikibot import config

countries = {
	('ro', 'ro') : {
		'project' : u'wikipedia',
	    'lang' : u'ro',
	    'headerTemplate' : u'ÎnceputTabelLMI',
	    'rowTemplate' : u'ElementLMI',
		'footerTemplate' : u'SfârșitTabelLMI',
		'commonsTemplate' : u'Monument istoric',
		'commonsTrackerCategory' : u'Cultural heritage monuments in Romania with known IDs',
		'commonsCategoryBase' : u'Historical monuments in Romania',
		'unusedImagesPage' : u'User:Multichill/Unused Monument istoric',
		'imagesWithoutIdPage' : u'User:Multichill/Monument istoric without ID',
	    'namespaces' : [0],
	    'table' : u'monuments_ro_(ro)',
	    'truncate' : False, 
	    'primkey' : u'Cod',
	    'fields' : [
		    {
		    'source' : u'Cod',
		    'dest' : u'cod',
		    'conv' : u'',
		    },
		    {
		    'source' : u'Denumire',
		    'dest' : u'denumire',
		    'conv' : u'',
		    },
		    {
		    'source' : u'Localitate',
		    'dest' : u'localitate',
		    'conv' : u'',
		    },
			    {
		    'source' : u'Adresă',
		    'dest' : u'adresa',
		    'conv' : u'',
		    },
		    {
		    'source' : u'Datare',
		    'dest' : u'datare',
		    'conv' : u'',
		    },
		    {
		    'source' : u'Arhitect',
		    'dest' : u'arhitect',
		    'conv' : u'',
		    },
		    {
		    'source' : u'Lat',
		    'dest' : u'lat',
		    'conv' : u'',
		    },
		    {
		    'source' : u'Coordonate',
		    'dest' : u'',
		    'conv' : u'',
		    },
		    {
		    'source' : u'Lon',
		    'dest' : u'lon',
		    'conv' : u'',
		    },
		    {
		    'source' : u'Imagine',
		    'dest' : u'imagine',
		    'conv' : u'',
		    },
		    {
		    'source' : u'Commons',
		    'dest' : u'commons',
		    'conv' : u'',
		    },
		    {
		    'source' : u'NotăCod',
		    'dest' : u'notacod',
		    'conv' : u'',
		    },
		    {
		    'source' : u'FostCod',
		    'dest' : u'fostcod',
		    'conv' : u'',
		    },
		    {
		    'source' : u'Cod92',
		    'dest' : u'cod92',
		    'conv' : u'',
		    },
		    {
		    'source' : u'CodRan',
		    'dest' : u'codran',
		    'conv' : u'',
		    },
		    ],
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
	for field in countryconfig.get('fields'):
		contents[field.get(u'source')]=u''

	for param in params:
		# Remove leading or trailing spaces
		field = param
		value = params[param].split("<ref")[0].strip()
		
		#Check first that field is not empty
		if field:
				#Is it in the fields list?
				if field in contents:
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
	templates = pywikibot.extract_templates_and_params(page.get())
	title = page.title(True)
	pywikibot.output(u'Working on page "%s"' % title)
	for (template, params) in templates:
		if template==countryconfig.get('rowTemplate'):
			monuments_db.append(processMonument(params, source, countryconfig, title))
			#time.sleep(5)
 

def processCountry(countryconfig):
	'''
	Process all the monuments of one country
	'''

	site = pywikibot.getSite(countryconfig.get('lang'), countryconfig.get('project'))
	rowTemplate = pywikibot.Page(site, u'%s:%s' % (site.namespace(10), countryconfig.get('rowTemplate')))

	transGen = pagegenerators.ReferringPageGenerator(rowTemplate, onlyTemplateInclusion=True)
	filteredGen = pagegenerators.NamespaceFilterPageGenerator(transGen, countryconfig.get('namespaces'))
	pregenerator = pagegenerators.PreloadingGenerator(filteredGen)
	for page in pregenerator:
		if page.exists() and not page.isRedirectPage():
			# Do some checking
			processText(page.permalink(), countryconfig, page=page)
			
	f = open("lmi_db.json", "w+")
	json.dump(monuments_db, f, indent=2)
	f.close();


def processTextfile(textfile, countryconfig,):
	'''
	Process the contents of a text file containing one or more lines with the Tabelrij rijksmonument template
	'''
	file = open(textfile, 'r')
	for line in file:
		processText(line.decode('UTF-8').strip(), textfile, countryconfig)

def main():
	'''
	The main loop
	'''
	# First find out what to work on

	countrycode = u'ro'
	textfile = u''

	for arg in pywikibot.handleArgs():
		if arg.startswith('-countrycode:'):
			countrycode = arg [len('-countrycode:'):]
		elif arg.startswith('-textfile:'):
			textfile = arg [len('-textfile:'):]

	if countrycode:
		lang = pywikibot.getSite().language()
		if not countries.get((countrycode, lang)):
			pywikibot.output(u'I have no config for countrycode "%s" in language "%s"' % (countrycode, lang))
			return False
		pywikibot.output(u'Working on countrycode "%s" in language "%s"' % (countrycode, lang))
		if textfile:
			pywikibot.output(u'Going to work on textfile.')
			processTextfile(textfile, countries.get((countrycode, lang)))
		else:
			processCountry(countries.get((countrycode, lang)))
	else:
		for (countrycode, lang), countryconfig in countries.iteritems():
			pywikibot.output(u'Working on countrycode "%s" in language "%s"' % (countrycode, lang))
			processCountry(countryconfig,)
	'''


	generator = genFactory.getCombinedGenerator()
	if not generator:
		pywikibot.output(u'You have to specify what to work on. This can either be -textfile:<filename> to work on a local file or you can use one of the standard pagegenerators (in pagegenerators.py)')
	else:
		pregenerator = pagegenerators.PreloadingGenerator(generator)
		for page in pregenerator:
		if page.exists() and not page.isRedirectPage():
			# Do some checking
			processText(page.get(), page.permalink(), page=page)
	'''

if __name__ == "__main__":
	try:
		main()
	finally:
		pywikibot.stopme()
