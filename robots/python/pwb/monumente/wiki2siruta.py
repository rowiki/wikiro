#!/usr/bin/python
# -*- coding: utf-8  -*-

import sys, time, warnings, json, string, random, re, csv
import math, urlparse
import sirutalib
sys.path.append("..")
import strainu_functions as strainu

import pywikibot
from pywikibot import pagegenerators
from pywikibot import config as user

def processLink(link):
	if link.find(',') > -1:
		village = link[:link.find(',')].strip()
		county = link[link.find(',') + 2:].strip()
	else:
		village = link.strip()
		county = None
	if village.find('(') > -1:
		village = village[:village.find('(')].strip()
		commune = village[village.find('(') + 1:village.find(')')].strip()
	else:
		commune = None
	return village

def main():
	f = open("lmi_db.json", "r+")
	pywikibot.output("Reading database file...")
	db = json.load(f)
	pywikibot.output("...done")
	f.close();
	
	data = open("siruta_wikipedia.csv", "r")
	w2s = csv.reader(data)
	w3s = {}
	for line in w2s:
		w3s[line[4]] = line
	out = {}
	txt = u""
	siruta = sirutalib.SirutaDatabase()
	siruta.set_diacritics_params(cedilla=False, acircumflex=True, nodia=True)
	errors = 0
	
	for monument in db:
		if not monument["Cod"] in w3s:
			continue
		line = w3s[monument["Cod"]]
		village = county = commune = None
		link = strainu.extractLink(monument["Localitate"][monument["Localitate"].find("[["):monument["Localitate"].find("]]")+2])
		if link == None:
			link = monument["Localitate"]
		village = processLink(link)
		if village != unicode(line[1], "utf8"):
			if line[0] == '':
				sname = u""
			else:
				sname = siruta.get_name(int(line[0]))
			if sname == None:
				sname = u""
			if village.upper() != sname:
				page = pywikibot.Page(pywikibot.getSite(), link)
				try:
					if page.exists() and page.isRedirectPage():
						page = page.getRedirectTarget()
						link = page.title()
						village = processLink(link)
				except:
					pass
				village = village.replace(u"Î", u"I")
				village = village.replace(u"Â", u"A")
				village = village.replace(u"Ă", u"A")
				village = village.replace(u"Ș", u"S")
				village = village.replace(u"Ț", u"T")
				village = village.replace(u"î", u"i")
				village = village.replace(u"â", u"a")
				village = village.replace(u"ă", u"a")
				village = village.replace(u"ș", u"s")
				village = village.replace(u"ț", u"t")
				village = village.replace(u"-", u" ")
				sname = sname.replace(u"-", u" ")
				if village.upper() != sname:
					errors += 1
					pywikibot.output(u"* Monumentul '''[[Cod:LMI:" + line[4] + u"|" + line[4] + "]]''' este trecut pe Wikipedia " + \
					u"în localitatea [[" + link + u"]], iar în RAN este SIRUTA ''" + line[0] + \
					u"'', adică localitatea ''" + line[1].decode("utf8") + u" (" + line[2].decode("utf8") + \
					u"), " + line[3].decode("utf8") + u"''")
					continue
		out[line[0]] = link
		#print line[0] + ": " + link
			
	print out
	print "Errors: " + str(errors)
	
if __name__ == "__main__":
	main()
