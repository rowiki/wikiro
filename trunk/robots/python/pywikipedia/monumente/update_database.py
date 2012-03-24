#!/usr/bin/python
# -*- coding: utf-8  -*-
'''
Update the monuments database either from a text file or from some wiki page(s)

'''
import sys, time, warnings, json
sys.path.append("..")
import wikipedia, config, re, pagegenerators

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
		#Split at =
		(field, sep, value) = param.partition(u'=')
		# Remove leading or trailing spaces
		field = field.strip()
		value = value.split("<ref")[0].strip()
		
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
					wikipedia.output(u'Found unknown field: %s on page %s' % (field, title) )
					wikipedia.output(u'Field: %s' % field)
					wikipedia.output(u'Value: %s' % value)
					wikipedia.output(u'Params: %s\n%s' % (params, param))
					#time.sleep(5)
	return contents

def processText(text, source, countryconfig, page=None):
	'''
	Process a text containing one or multiple instances of the monument row template
	'''
	
	if not page:
		site = site = wikipedia.getSite(countryconfig.get('lang'), countryconfig.get('project'))
		page = wikipedia.Page(site, u'User:Multichill/Zandbak')
	templates = page.templatesWithParams(thistxt=text)
	wikipedia.output(u'Working on page "%s"' % page.title(True))
	for (template, params) in templates:
		if template==countryconfig.get('rowTemplate'):
			#print template
			#print params
			monuments_db.append(processMonument(params, source, countryconfig, page.title(True)))
			#time.sleep(5)
 

def processCountry(countryconfig):
	'''
	Process all the monuments of one country
	'''

	site = wikipedia.getSite(countryconfig.get('lang'), countryconfig.get('project'))
	rowTemplate = wikipedia.Page(site, u'%s:%s' % (site.namespace(10), countryconfig.get('rowTemplate')))

	transGen = pagegenerators.ReferringPageGenerator(rowTemplate, onlyTemplateInclusion=True)
	filteredGen = pagegenerators.NamespaceFilterPageGenerator(transGen, countryconfig.get('namespaces'))
	pregenerator = pagegenerators.PreloadingGenerator(filteredGen)
	for page in pregenerator:
		if page.exists() and not page.isRedirectPage():
			# Do some checking
			processText(page.get(), page.permalink(), countryconfig, page=page)
			
	f = open("db.json", "w+")
	json.dump(monuments_db, f)
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

	for arg in wikipedia.handleArgs():
		if arg.startswith('-countrycode:'):
			countrycode = arg [len('-countrycode:'):]
		elif arg.startswith('-textfile:'):
			textfile = arg [len('-textfile:'):]

	if countrycode:
		lang = wikipedia.getSite().language()
		if not countries.get((countrycode, lang)):
			wikipedia.output(u'I have no config for countrycode "%s" in language "%s"' % (countrycode, lang))
			return False
		wikipedia.output(u'Working on countrycode "%s" in language "%s"' % (countrycode, lang))
		if textfile:
			wikipedia.output(u'Going to work on textfile.')
			processTextfile(textfile, countries.get((countrycode, lang)))
		else:
			processCountry(countries.get((countrycode, lang)))
	else:
		for (countrycode, lang), countryconfig in countries.iteritems():
			wikipedia.output(u'Working on countrycode "%s" in language "%s"' % (countrycode, lang))
			processCountry(countryconfig,)
	'''


	generator = genFactory.getCombinedGenerator()
	if not generator:
		wikipedia.output(u'You have to specify what to work on. This can either be -textfile:<filename> to work on a local file or you can use one of the standard pagegenerators (in pagegenerators.py)')
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
		wikipedia.stopme()
