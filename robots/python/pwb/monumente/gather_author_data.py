#!/usr/bin/python
# -*- coding: utf-8  -*-
'''
Parse the author articles and put the output in a json file
with the following format:
dict{code, list[dict{author}, ...]}

The script requires some configuration for each database one plans to work on.
The global variable ''options'' is a dictionary. The key is a tuple containing
the language and the database. The value is another dict containing the
following fields:
* namespaces: the namespaces to scan for author data
* authorTemplate: The template that marks the code from the database
* codeRegexp: regular expression used for searching for the codes

Command line parameters:
-db	The database to work with. Together with -lang they give the configuration
         to be used for retrieving data
'''

import json
import re

# sys.path.append("..")
import pywikibot
from pywikibot import config as user
from pywikibot import pagegenerators

options = {
	('ro', 'lmi'): 
	{
		'namespaces': [0],
		'authorTemplate': "autorCodLMI",
		'codeRegexp': re.compile("\{\{[aA]utorCodLMI\|(([a-z]{1,2})-(i|ii|iii|iv)-([a-z])-([a-z])-([0-9]{5}(\.[0-9]{2,3})?))", re.I),
	},
	('commons', 'lmi'):
	{
		'namespaces': [6],
		'authorTemplate': "",
		'codeRegexp': "",
	}
}

fullDict = {}

def getYearsFromWikidata(page):
	wdpage = page.data_item()
	data = wdpage.get()
	#print(data['claims'])
	if 'P570' in data['claims']:
		if len(data['claims']['P570']) > 1:
			print(data['claims'])
			return None
		claim = data['claims']['P570'][0]
		return claim.getTarget().year 
	return None

def processArticle(text, page, conf):
	pywikibot.output(u'Working on "%s"' % page.title())
	regexp = conf['codeRegexp']
	results = re.findall(regexp, text)
	if results == None:
		return	
	else:
		print(repr(results))
		year = getYearsFromWikidata(page)
		for result in results:
			code = result[0]
			if code in fullDict:
				fullDict[code].append({"Creatori": page.title(with_ns=False),
									   "Copyright": year})
			else:
				fullDict[code] = [{"Creatori": page.title(with_ns=False),
								   "Copyright":	year}]
	
def main():
	lang = u'ro'
	db = u'lmi'
	textfile = u''

	for arg in pywikibot.handle_args():
		if arg.startswith('-lang:'):
			lang = arg [len('-lang:'):]
			user.mylang = lang
		if arg.startswith('-family'):
			user.family = arg [len('-family:'):]
		if arg.startswith('-db'):
			db = arg [len('-db:'):]
	
	site = pywikibot.Site()
	lang = site.lang
	if not options.get((lang,db)):
		pywikibot.output(u'I have no options for language "%s"' % lang)
		return False
	
	langOpt = options.get((lang,db))
			
	authorTemplate = pywikibot.Page(site, u'%s:%s' % (site.namespace(10), langOpt.get('authorTemplate')))

	transGen = authorTemplate.getReferences(only_template_inclusion=True)
	filteredGen = pagegenerators.NamespaceFilterPageGenerator(transGen, langOpt.get('namespaces'), site)
	pregenerator = pagegenerators.PreloadingGenerator(filteredGen, 50)
	
	#page = pywikibot.Page(site, "File:Icoanei 56 (2).jpg")
	count = 0
	for page in pregenerator:
		if page.exists() and not page.isRedirectPage():
			# Do some checking
			processArticle(page.get(), page, langOpt)
			count += 1
	print(count)
	f = open("_".join([lang, db, "authors.json"]), "w+")
	json.dump(fullDict, f)
	f.close();

if __name__ == "__main__":
	try:
		main()
	finally:
		pywikibot.stopme()
