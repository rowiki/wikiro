#!/usr/bin/python
# -*- coding: utf-8  -*-
'''
Based on the output from parse_monument_article.py and update_database.py 
identify different improvements and further errors in the monument pages and database
* să adauge linkuri către articolul despre un monument acolo unde există - d
* să adauge coordonate în listă dacă ele există în articol - d
* să adauge automat poza în listă dacă există una singură cu monumentul respectiv - d
* să raporteze la [[Proiect:Monumente istorice/Erori/Automate]] următoarele situații:
** în listă nu există codul respectiv, dar codul are formatul corect în articol/imagine
** în listă nu există imagine și există mai multe imagini disponibile la commons - i
** în listă și articol coordonatele sunt diferite (diferențe mai mari de 0,001 grade sau ~3,6 de secunde de grad) - i
'''

import sys, time, warnings, json, string, random, re
import math, urlparse
sys.path.append("..")
import strainu_functions as strainu

import pywikibot
from pywikibot import pagegenerators
from pywikibot import config as user

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
	'fields' : 
		[
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

_log = "link.err.log"
_flog = None

def initLog():
	global _flog, _log;
	_flog = open(_log, 'w+')

def closeLog():
	global _flog
	_flog.close()

def log(string):
	#pywikibot.output(string.encode("utf8") + "\n")
	_flog.write(string.encode("utf8") + "\n")
	
def rebuildTemplate(params):
	my_template = u"{{" + countries.get(('ro', 'ro')).get('rowTemplate') + u"\n"
	for name in [u"Cod", u"NotăCod", u"FostCod", u"CodRan", u"Cod92", u"Denumire", u"Localitate", u"Adresă", u"Datare", u"Arhitect", u"Lat", u"Lon", u"Imagine", u"Commons"]:
		if name in params and params[name] <> u"":
			my_template += u"| " + name + u" = " + params[name] + u"\n"
			
	my_template += u"}}\n"
	return my_template

#TODO: Hardcoded order of parameters
def updateTableData(url, code, field, newvalue, upload = True, text = None):
	pywikibot.output("Uploading %s for %s; value \"%s\"" % (field, code, newvalue))
	site = pywikibot.getSite()
	title = urlparse.parse_qs(urlparse.urlparse(str(url)).query)['title'][0].decode('utf8')
	page = pywikibot.Page(site, title)
	if text == None:
		pywikibot.output("Getting page contents")
		text = page.get()
	oldtext = text
	#templates = page.templatesWithParams()
	templates = pywikibot.extract_templates_and_params(text)
	codeFound = False
	last = None
	rawCode = None
	my_params = {}
	#pywikibot.output("1")
	for (template, params) in templates:
		if template==countries.get(('ro', 'ro')).get('rowTemplate'):
			for param in params:
				val = params[param]
				fld = param.strip()
				val = val.split("<ref")[0].strip()
				val2 = re.sub(r'\s', '', val)
				if fld == "Cod" and val2 == code:
					codeFound = True
					rawCode = val
					my_params = params
					break
			if codeFound:
				break
	#pywikibot.output("2")
	if not codeFound:
		log(u"*''E'': ''[%s]'' Codul nu este prezent în [[%s|listă]]" % (rawCode, title))
		pywikibot.output(u"Code not found: %s" % code)
		return None
	else:
		pywikibot.output(u"\n" + str(params) + u"\n")
	#pywikibot.output("3")
	orig = rebuildTemplate(my_params)
	my_params[field] = newvalue
	new = rebuildTemplate(my_params)
	#pywikibot.output("4")
	if orig.strip() == new.strip():
		pywikibot.output("No change, nothing to upload!")
		return text
	#pywikibot.output("5")
	pywikibot.showDiff(orig, new)
	answer = pywikibot.input(u"Upload change? ([y]es/[n]o/[l]og)")
	if answer == 'y':
		(before, code, after) = text.partition(rawCode)
		#we need to clean the whole template from both before and after
		clivb = before.rfind(u"{{" + countries.get(('ro', 'ro')).get('rowTemplate'))
		cliva = after.find(u"{{" + countries.get(('ro', 'ro')).get('rowTemplate'))
		if cliva >= 0 and clivb >= 0:
			after = after[cliva:]
			before = before[:clivb]
		else:
			pywikibot.output("Could not find the current template, aborting!")
			return text
		after = new + after
		text = "".join((before, after))
		#pywikibot.output(text)
		if upload == True:
			comment = u"Actualizez câmpul %s în lista de monumente" % field
			try:
				page.put(text, comment)
			except pywikibot.exceptions.Error:
				pywikibot.output("Some error occured, let's move on and hope for the best!")
			return None
	elif answer == 'l' or answer == '':
		new = new.replace("\n", "<br/>")
		log(u"*''W'': ''[%s]'' De verificat dacă înlocuirea câmpului ''%s'' cu ''%s'' este corectă (inclusiv legăturile adăugate)" % (code, orig, new))
	#pywikibot.output("6")
	return text

def main():
	f = open("lmi_db.json", "r+")
	pywikibot.output("Reading database file...")
	db = json.load(f)
	pywikibot.output("...done")
	f.close();

	f = open("ro_pages.json", "r+")
	pywikibot.output("Reading ro.wp pages file...")
	pages_ro = json.load(f)
	pywikibot.output("...done")
	f.close();

	f = open("ro_authors.json", "r+")
	pywikibot.output("Reading ro.wp authors file...")
	authors_ro = json.load(f)
	pywikibot.output("...done")
	f.close();

	f = open("commons_Category_pages.json", "r+")
	pywikibot.output("Reading commons categories file...")
	categories_commons = json.load(f)
	pywikibot.output("...done")
	f.close();

	f = open("commons_File_pages.json", "r+")
	pywikibot.output("Reading commons images file...")
	pages_commons = json.load(f)
	pywikibot.output("...done")
	f.close();

	initLog()
	lastSource = None

	#this is the big loop that should only happen once
	for monument in db:
		if monument["source"] <> lastSource:
			articleText = None
			lastSource = monument["source"]
		rawCode = monument["Cod"]
		regexp = re.compile("(([a-z]{1,2})-(i|ii|iii|iv)-([a-z])-([a-z])-([0-9]{5}(\.[0-9]{2,3})?))", re.I)
		result = re.findall(regexp, rawCode)
		if len(result) > 0:
			code = result[0][0]
		else:
			code = rawCode
		pywikibot.output(code)
		allPages = list()
		article = None
		picture = None
		pic_author = None
		lmi92 = None
		ran = None
		#pywikibot.output(str(page))
		try:
			#pywikibot.output("OK: " + str(page[code]))
			if code in pages_ro:
				allPages.extend(pages_ro[code])
				if len(allPages) > 1:
					msg = u"*''E'': ''[%s]'' Codul este prezent în mai multe articole pe Wikipedia: " % code
					for page in allPages:
						msg += (u"[[:%s]], " % page["name"])
					log(msg)
				elif len(allPages) == 1:
					article = allPages[0]
					ran = allPages[0]['ran']
			if code in pages_commons:
				if len(pages_commons[code]) == 1: #exactly one picture
					picture = pages_commons[code][0]["name"]
					if pic_author == None:
						pic_author = pages_commons[code][0]["author"]
					if "lmi92" in pages_commons[code]:
						lmi92 = pages_commons[code]["lmi92"]
				elif monument["Imagine"].strip() == "" or \
						monument["Arhitect"].strip() == "" or \
						monument["Cod92"].strip() == "": 
					#multiple images available, we need to parse them
					for pic in pages_commons[code]:
						if "lmi92" in pic:
							lmi92 = pic["lmi92"]
						msg = u"*''I'': ''[%s]'' Există %d imagini disponibile la commons pentru acest cod: " % (code, len(pages_commons[code]))
						author_list = ""
						if pic_author == None and author_list == "" and pic["author"] <> None:
							pic_author = pic["author"]
							author_list += u"[[:%s]], " % pic["name"]
							print pic_author
						elif pic["author"] <> None and pic_author <> pic["author"]:
							#multiple authors, ignore and report error
							author_list += u"[[:%s]], " % pic["name"]
							pic_author = None
							print "pic_author removed"
						msg += u"[[:%s]], " % pic["name"]
						if pic["quality"] == True: #choose the first quality picture
							picture = pic["name"]
							break
					if picture == None: #no quality pictures, but do not log - we'll choose a random one
						pass
						#log(msg)
					if pic_author == None and author_list <> "":
						log(u"*'''E''': ''[%s]'' În lista de imagini sunt trecuți mai multi autori: %s" % (code, author_list))
				allPages.extend(pages_commons[code])
			if code in categories_commons:
				allPages.extend(categories_commons[code])
				if len(categories_commons[code]) > 1:
					msg = u"*''E'': ''[%s]'' Codului îi corespund mai multe categorii la Commons: <span lang=\"x-sic\">" % code
					for page in categories_commons[code]:
						msg += (u"[[:commons:%s]], " % page["name"])
					msg += "</span>"
					log(msg)
			#pywikibot.output(str(allPages))
		except Exception as e:
			pywikibot.output("Error: " + str(e))
			pass #ignore errors
	
		#monument name and link
		if article <> None and article["name"] <> None and article["name"] <> "":
			if monument["Denumire"].find("[[") == -1:
				link = u"[[" + article["name"] + "|" + monument["Denumire"] + "]]"
				#pywikibot.output(link)
				articleText = updateTableData(monument["source"], code, "Denumire", link, text=articleText)
			else: # check if the 2 links are the same
				link = strainu.extractLink(monument["Denumire"])
				if link == None:
					log(u"*''W'': ''[%s]'' De verificat legătura internă din câmpul Denumire" % code)
				else:
					page1 = pywikibot.Page(pywikibot.getSite(), link)
					page2 = pywikibot.Page(pywikibot.getSite(), article["name"])
					if page1 <> page2 and \
					(not page1.isRedirectPage() or page1.getRedirectTarget() <> page2) and \
					(not page2.isRedirectPage() or page2.getRedirectTarget() <> page1):
						log(u"*''W'': ''[%s]'' Câmpul Denumire are o legătură internă către [[%s]], dar articolul despre monument este [[%s]]" % (code, page1, page2))
				
		#author from article
		if article <> None and article["author"] <> None and article["author"].strip() <> "":
			#author = strainu.stripLink(article["author"]).strip()
			author = article["author"].strip()
			if author == None or author == "":
				pywikibot.output("Wrong author link: \"%s\"@%s" % (article["author"], article["name"]))
			elif monument["Arhitect"] == "":
				pywikibot.output(author)
				articleText = updateTableData(monument["source"], code, "Arhitect", author, text=articleText)
			else:
				a1 = author.strip()
				a2 = monument["Arhitect"].strip()
				if a1 <> a2:
					articleText = updateTableData(monument["source"], code, "Arhitect", a1, text=articleText)
				#	log(u"*''W'': ''[%s]'' Câmpul Arhitect este \"%s\", dar articolul despre monument menționează \"%s\"" % (code, a2, a1))

		#add the author(s) extracted from author pages
		elif code in authors_ro:
			authors = monument["Arhitect"]
			for author in authors_ro[code]:
				if authors.find(author) == -1: #we don't already know the author
					if authors <> "":
						authors = author + ", " + authors
					else:
						authors = author
			if authors <> monument["Arhitect"]: # if something changed, update the text
				pywikibot.output(authors)
				articleText = updateTableData(monument["source"], code, "Arhitect", authors, text=articleText)
			else:
				pywikibot.output("The authors list is unchanged for %s: %s" % (code, authors))

		elif pic_author <> None:
			if pic_author <> strainu.stripLink(monument["Arhitect"]).strip():
				articleText = updateTableData(monument["source"], code, "Arhitect", pic_author, text=articleText)
	
		#image from Commons, none in the list
		if picture <> None and monument["Imagine"].strip() == "":
			pywikibot.output("Upload?" + picture)
			if picture.find(':') < 0:#no namespace
				picture = "File:" + picture
			articleText = updateTableData(monument["source"], code, "Imagine", picture, text=articleText)
	
			#use image from article only if none is available (or was selected) 
			#from commons and we don't have a picture in the list
			if picture == None and article <> None and article["image"] <> None and \
			article["image"] <> "" and monument["Imagine"].strip() == "":
				pywikibot.output(monument["Imagine"])
				artimage = strainu.extractImageLink(article["image"]).strip()
				if artimage == None or artimage == "":
					pywikibot.output("Wrong image link: \"%s\"@%s" % (article["image"], article["name"]))
				if artimage.find(':') < 0:#no namespace
					artimage = "File:" + artimage
				#pywikibot.output("Upload?" + artimage)
				articleText = updateTableData(monument["source"], code, "Imagine", artimage, text=articleText)

		if picture == None and monument["Imagine"].strip() == "":
			#use image from article only if none is available (or was selected) 
			#from commons and we don't have a picture in the list
			if article <> None and article["image"] <> None and article["image"] <> "":
				pywikibot.output(monument["Imagine"])
				artimage = strainu.extractImageLink(article["image"]).strip()
				if artimage == None or artimage == "":
					pywikibot.output("Wrong article image link: \"%s\"@%s" % (article["image"], article["name"]))
				if artimage.find(':') < 0:#no namespace
					artimage = "File:" + artimage
				#pywikibot.output("Upload?" + artimage)
				articleText = updateTableData(monument["source"], code, "Imagine", artimage, text=articleText)
			#final option: choose a random image from commons
			elif (code in pages_commons) and len(pages_commons[code]) > 0:
				artimage = random.sample(pages_commons[code],  1)[0]["name"]
				if artimage.find(':') < 0:#no namespace
					artimage = "File:" + artimage
				articleText = updateTableData(monument["source"], code, "Imagine", artimage, text=articleText)
		
		#Commons category
		if code in categories_commons:
			cat = categories_commons[code][0]
			if monument["Commons"] == "":
				articleText = updateTableData(monument["source"], code, "Commons", "commons:" + cat["name"], text=articleText)
			elif monument["Commons"].strip() <> ("commons:" + cat["name"].strip()):
				log(u"*''E'': ''[%s]'' Există mai multe categorii pentru acest cod: <span lang=\"x-sic\">[[:%s]] și [[:%s]]</span>" % (code, "commons:" + cat["name"], monument["Commons"]))
	
		#latitude and longitude
		if monument["Lat"] == "":
			lat = 0
		else:
			lat = float(monument["Lat"])
		if monument["Lon"] == "":
			long = 0
		else:
			long = float(monument["Lon"])
		artLat = 0
		artLong = 0
		artCoord = None
		updateCoord = True
		for page in allPages:
			if page["lat"] <> 0 and page["long"] <> 0:
				if artLat <> 0: #this also means artLong <> 0
					if math.fabs(artLat - page["lat"]) > 0.001 or math.fabs(artLong - page["long"]) > 0.001:
						log(u"*''E'': ''[%s]'' Coordonate diferite între [[:%s]] (%f,%f) și [[:%s]] (%f,%f)" % (code, page["name"], page["lat"], page["long"], artCoord, artLat, artLong))
						updateCoord = False
				else:
					artLat = page["lat"]
					artLong = page["long"]
					artCoord = page["name"]
				
		if lat == 0 and artLat <> 0 and updateCoord:
			pywikibot.output(str(artLat) + " " + str(artLong))
			articleText = updateTableData(monument["source"], code, "Lat", str(artLat), upload = False, text=articleText)
			articleText = updateTableData(monument["source"], code, "Lon", str(artLong), upload = True, text = articleText)
	
		if lat <> 0 and artLat <> 0 and (math.fabs(artLat - lat) > 0.01 or math.fabs(artLong - long) > 0.01):
			log(u"*''E'': ''[%s]'' Coordonate diferite între [[:%s]] (%f,%f) și listă (%f,%f)" % (code, artCoord, artLat, artLong, lat, long))
			
		if lmi92 <> None:
			articleText = updateTableData(monument["source"], code, "Cod92", lmi92, upload = True, text = articleText)
	
	closeLog()

if __name__ == "__main__":
	try:
		main()
	finally:
		pywikibot.stopme()
	
