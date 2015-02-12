#!/usr/bin/python
# -*- coding: utf-8  -*-
'''
Parse the author articles and put the output in a json file
with the following format:
dict{code, list[dict{author}, ...]}

'''

import sys, time, warnings, json, string, re
#sys.path.append("..")
import pywikibot
from pywikibot import pagegenerators
from pywikibot import config as user

options = {
	('ro', 'lmi'): 
	{
		'namespaces': [0],
		'codeTemplate': "codLMI",
		'authorTemplate': "autorCodLMI",
		'codeRegexp': re.compile("\{\{[aA]utorCodLMI\|(([a-z]{1,2})-(i|ii|iii|iv)-([a-z])-([a-z])-([0-9]{5}(\.[0-9]{2,3})?))", re.I),
	},
	('commons', 'lmi'):
	{
		'namespaces': [6],
		'codeTemplate': "Monument_istoric",
		'authorTemplate': "",
		'codeRegexp': "",
	}
}

fullDict = {}

def processArticle(text, page, conf):
	pywikibot.output(u'Working on "%s"' % page.title(True))
	regexp = conf['codeRegexp']
	results = re.findall(regexp, text)
	if results == None:
		return	
	else:
		print repr(results)
		for result in results:
			code = result[0]
			if code in fullDict:
				fullDict[code].append(page.title(withNamespace=False))
			else:
				fullDict[code] = [page.title(withNamespace=False)]
	
def main():
	lang = u'ro'
	db = u'lmi'
	textfile = u''

	for arg in pywikibot.handleArgs():
		if arg.startswith('-lang:'):
			lang = arg [len('-lang:'):]
			user.mylang = lang
		if arg.startswith('-family'):
			user.family = arg [len('-family:'):]
		if arg.startswith('-db'):
			db = arg [len('-db:'):]
	
	site = pywikibot.Site()
	lang = site.language()
	if not options.get((lang,db)):
		pywikibot.output(u'I have no options for language "%s"' % lang)
		return False
	
	langOpt = options.get((lang,db))
			
	authorTemplate = pywikibot.Page(site, u'%s:%s' % (site.namespace(10), langOpt.get('authorTemplate')))

	transGen = pagegenerators.ReferringPageGenerator(authorTemplate, onlyTemplateInclusion=True)
	filteredGen = pagegenerators.NamespaceFilterPageGenerator(transGen, langOpt.get('namespaces'), site)
	pregenerator = pagegenerators.PreloadingGenerator(filteredGen, 125)
	
	#page = pywikibot.Page(site, "File:Icoanei 56 (2).jpg")
	count = 0
	for page in pregenerator:
		if page.exists() and not page.isRedirectPage():
			# Do some checking
			processArticle(page.get(), page, langOpt)
			count += 1
	print count
	f = open("_".join([lang, db, "authors.json"]), "w+")
	json.dump(fullDict, f)
	f.close();

if __name__ == "__main__":
	try:
		main()
	finally:
		pywikibot.stopme()
