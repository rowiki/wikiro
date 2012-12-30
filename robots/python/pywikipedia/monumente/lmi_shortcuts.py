#!/usr/bin/python
# -*- coding: utf-8  -*-

import sys, json
sys.path.append("..")
import wikipedia
'''
This script creates LMI links in the Cod namespace on ro.wp basted on 
the monument database.

See [[Wikipedia:Coduri]] (ro) for details on the Cod namespace.
'''

def split_code(code):
	parts = code.split('-')
	if len(parts) < 3:
		print parts
		return (None, None, None, None)
	return (parts[0], parts[1], parts[2], parts[3])

def main():
	f = open("lmi_db.json", "r+")
	wikipedia.output("Reading database file...")
	db = json.load(f)
	wikipedia.output("...done")
	f.close();
	
	f = open("ro_pages.json", "r+")
	wikipedia.output("Reading articles file...")
	pages_ro = json.load(f)
	wikipedia.output("...done")
	f.close();
	
	site = wikipedia.getSite()
	
	for code in pages_ro:
		page = wikipedia.Page(site, u"Cod:LMI:" + code)
		wikipedia.output(page.title())
		#if page.exists() and not page.isRedirect():
		#		wikipedia.output(u"Page %s is not a redirect" % page.title())
		#else:
		page.put(u"#redirect [[%s]]" % pages_ro[code][0]["name"], "Redirecting code to the Wikipedia article")
			
	for monument in db:
		if monument["Cod"] in pages_ro:
			continue
		page = wikipedia.Page(site, u"Cod:LMI:" + monument["Cod"])
		wikipedia.output(page.title())
		#if page.exists() and not page.isRedirect():
		#		wikipedia.output(u"Page %s is not a redirect" % page.title())
		#else:
		if not page.exists():
			source_page = wikipedia.url2link(monument["source"][monument["source"].find(u'=')+1:monument["source"].find(u'&')], site, site)
			#wikipedia.output(source_page)
			source_page = wikipedia.Page(site, source_page)
			page.put(u"#redirect [[{0}#{1}]]".format(source_page.title(), monument["Cod"]), "Redirecting code to the Wikipedia article")

if __name__ == "__main__":
	try:
		main()
	finally:
		wikipedia.stopme()

