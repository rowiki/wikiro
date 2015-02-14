#!/usr/bin/python
# -*- coding: utf-8  -*-

import sys, json
sys.path.append("..")
import pywikibot
from pywikibot import pagegenerators
from pywikibot import config as user
'''
This script creates redirects in the Cod namespace on ro.wp based on the monument
database. We create a link to the list in the main article and, if an article
exists, another redirect is created at a page defined by the configuration option.

The script requires some configuration. The 'options' global variable is a 
dictionary containing one entry for each database we can work on. The following
settings can be configured:
- database: 		the file containing the database we work on
- articles: 		the file containing the existing articles related to the 
			database
- shortcutPrefix: 	the prefix used to identify each database's links; this
			contains the namespace ("Cod") and perhaps some other 
			means of isolating the different databases; the link to
			the list contains this prefix and the id from the database
- articleSuffix:	suffix to be added to the previously described page to
			obtain the redirect to the article

Example: 
- prefix: "Cod:LMI:"
- suffix: "/articol"
- code: 10
Redirect to the database is created at: Cod:LMI:10
Redirect to the article is created at: Cod:LMI:10/articol

See [[ro:Wikipedia:Coduri]] (ro) for details on the usage of the Cod namespace.
'''

options = {
	'lmi': {
		'database': 'ro_lmi_db.json',
		'articles': 'ro_lmi_pages.json',
		'shortcutPrefix': u'Cod:LMI:',
		'articleSuffix': u'/articol',
	},
}
_db = 'lmi'

def main():
	f = open(options[_db]['database'], "r+")
	pywikibot.output("Reading database file...")
	db = json.load(f)
	pywikibot.output("...done")
	f.close();
	
	f = open(options[_db]['articles'], "r+")
	pywikibot.output("Reading articles file...")
	pages_ro = json.load(f)
	pywikibot.output("...done")
	f.close();
	
	site = pywikibot.Site()
	
	for code in pages_ro:
		page = pywikibot.Page(site, options[_db]['shortcutPrefix'] + code + options[_db]['articleSuffix'])
		pywikibot.output(page.title())
		if page.exists() and not page.isRedirectPage():
				pywikibot.output(u"Page %s is not a redirect" % page.title())
		elif not page.exists():
			page.put(u"#redirecteaza[[%s]]" % pages_ro[code][0]["name"], "Redirecting code to the Wikipedia article")
			
	for monument in db:
		#if not monument["Cod"] in pages_ro:
		#	continue
		page = pywikibot.Page(site, options[_db]['shortcutPrefix'] + monument["Cod"])
		pywikibot.output(page.title())

		source_page = monument["source"][monument["source"].find(u'=')+1:monument["source"].find(u'&')]
		pywikibot.output(source_page)
		source_page = pywikibot.Page(site, source_page)
		page_text = u"#redirect [[{0}#{1}]]".format(source_page.title(), monument["Cod"])
		if not page.exists():# or page.get(False, True) <> page_text:
			page.put(page_text, "Redirecting code to the Wikipedia list")

if __name__ == "__main__":
	try:
		main()
	finally:
		pywikibot.stopme()

