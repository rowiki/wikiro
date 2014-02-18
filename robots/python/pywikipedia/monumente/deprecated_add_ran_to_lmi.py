#!/usr/bin/python
# -*- coding: utf-8  -*-
'''
Parse a csv containing a RAN code and the corresponding LMI code and
 update the database with the correct RAN code.

Due to the weak quality of RAN data, we also make some guesswork and
 adjustments
'''

import sys, time, warnings, json, string, random
import math, urlparse, csv
import cProfile
sys.path.append("..")
import wikipedia, re, pagegenerators
import config as user
import strainu_functions as strainu

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
		],
	},
}

origtext = None

def log(string):
	wikipedia.output(string.encode("utf8") + "\n")
	#_flog.write(string.encode("utf8") + "\n")

#TODO: Hardcoded order of parameters
def updateTableData(url, code, field, newvalue, upload = True, text = None):
	global origtext
	wikipedia.output("Uploading %s for %s; value \"%s\"" % (field, code, newvalue))
	site = wikipedia.getSite()
	title = urlparse.parse_qs(urlparse.urlparse(url).query)['title'][0]
	page = wikipedia.Page(site, title)
	if text == None:
		wikipedia.output("Getting page contents")
		text = page.get()
	oldtext = text
	templates = page.templatesWithParams(thistxt=text)
	codeFound = False
	orig = None
	last = None
	rawCode = None
	#wikipedia.output("1")
	for (template, params) in templates:
		if template==countries.get(('ro', 'ro')).get('rowTemplate'):
			for param in params:
				(fld, sep, val) = param.partition(u'=')
				fld = fld.strip()
				val = val.split("<ref")[0].strip()
				val2 = re.sub(r'\s', '', val)
				if fld == "Cod" and val2 == code:
					codeFound = True
					rawCode = val
				elif fld == field:
					orig = param.strip()
				elif codeFound and orig == None and param == params[-1]: #keep the last element
					last = param.strip()
			if codeFound:
				break
			else:
				orig = None
	#wikipedia.output("2")
	if not codeFound:
		log(u"*''E'': ''[%s]'' Codul nu este prezent în [[%s|listă]]" % (code, title))
		wikipedia.output(u"Code not found: %s" % code)
		return None
	else:
		#wikipedia.output(u"\n" + str(params) + u"\n")
		pass
	#wikipedia.output("3")
	if orig != None:
		new = field + " = " + newvalue
	elif last != None:
		orig = last
		new = last + "\n| " + field + " = " + newvalue
	else: #No orig, no last? Something wrong!
		wikipedia.output("I don't have enough information to modify this template!")
		return None
	#wikipedia.output("4")
	if orig.strip() == new.strip() and not upload:
		wikipedia.output("No change, nothing to upload!")
		return text
	#wikipedia.output("5")
	(before, code, after) = text.partition(rawCode)
	after = after.replace(orig, new, 1)
	text = "".join((before, code, after))
	if upload == True and origtext <> text:
		wikipedia.showDiff(origtext, text)
		answer = 'y'
		#answer = wikipedia.input(u"Upload change? ([y]es/[n]o/[l]og)")
		if answer == 'y':
			#wikipedia.output(text)
			comment = u"Actualizez câmpul %s în lista de monumente" % field
			page.put(text, comment)
			return None
		elif answer == 'l' or answer == '':
			new = new.replace("\n", "<br/>")
			#log(u"*''W'': ''[%s]'' De verificat dacă înlocuirea câmpului ''%s'' cu ''%s'' este corectă (inclusiv legăturile adăugate)" % (code, orig, new))
	else:
		wikipedia.showDiff(orig, new)
	#wikipedia.output("6")
	return text

def main():
	global origtext
	f = open("lmi_db.json", "r+")
	wikipedia.output("Reading database file...")
	db = json.load(f)
	wikipedia.output("...done")
	f.close();

	sources = {}
	articles = {}
	for monument in db:
		sources[monument["Cod"]] = monument["source"]

	simple_ran = csv.reader(open("ran_lmi.csv", "r"))

	regexp = re.compile("(([a-z]{1,2})-(i|ii|iii|iv)-([a-z])-([a-z])-([0-9]{5}(\.[0-9]{2})?))", re.I)
	for monument in simple_ran:
		if monument[1] == "" or monument[0] == "":#we have LMI
			continue

		ran = monument[0].strip()
		codes = monument[1].split(',')
		for lmi in codes:
			lmi = lmi.replace(lmi[0:2], lmi[0:2].upper(), 1)
			lmi = lmi.replace(' ', '').replace('--','-').replace('..','.').replace('_','-').replace('i','I')
			lmi = re.sub("([IV])([msa])", "\g<1>-\g<2>", lmi, count=1)
			lmi = re.sub("([AB])([0-9])", "\g<1>-\g<2>", lmi, count=1)
			lmi = re.sub("\.$", "", lmi, count=1)
			lmi = re.sub("([msa])\.?([AB])", "\g<1>-\g<2>", lmi, count=1)
			lmi = re.sub("([A-Z][A-Z])\.-?I", "\g<1>-I", lmi, count=1)
			if regexp.search(lmi) == None:
				print "Codul %s nu are formatul asteptat" % lmi
				continue
			print lmi
			if lmi in sources:
				if sources[lmi] in articles:
					if lmi in articles[sources[lmi]]:
						articles[sources[lmi]][lmi] += [ran]
					else:
						articles[sources[lmi]][lmi] = [ran]
				else:
					articles[sources[lmi]] = {lmi: [ran]}
			else:
				print "Codul %s nu e in baza de date" % lmi
	count = 0
	for county in articles:
		site = wikipedia.getSite()
		title = urlparse.parse_qs(urlparse.urlparse(county).query)['title'][0]
		page = wikipedia.Page(site, title)
		origtext = page.get()
		text = None
		for monument in articles[county]:
			if len(articles[county][monument]) >= 1:
				newvalue = ", ".join(articles[county][monument])
				text = updateTableData(county, monument, "CodRan", newvalue, upload = False, text = text)
		text = updateTableData(county, monument, "CodRan", newvalue, upload = True, text = text)
	

if __name__ == "__main__":
	try:
		cProfile.run('main()', 'profiling/ranprofile.txt')
		#main()
	finally:
		wikipedia.stopme()
	
