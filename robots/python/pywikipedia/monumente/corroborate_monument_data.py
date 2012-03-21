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
** în listă și articol coordonatele sunt diferite (diferențe mai mari de 0,01 grade sau ~35 de secunde de grad) - i
'''

import sys, time, warnings, json, string
import math, urlparse
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

_log = "link.err.log"
_flog = None

def initLog():
	global _flog, _log;
	_flog = open(_log, 'w+')
	
def closeLog():
	global _flog
	_flog.close()

def log(string):
	#wikipedia.output(string.encode("utf8") + "\n")
	_flog.write(string.encode("utf8") + "\n")
	
#TODO: Hardcoded order of parameters
def updateTableData(url, code, field, newvalue, upload = True, text = None):
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
		log(u"*''E'': ''[%s]'' Codul este prezent articolul [[%s]], dar nu și în listă" % (code, title))
		wikipedia.output(u"Weird code: %s" % code)
		return None
	else:
		wikipedia.output(u"\n" + str(params) + u"\n")
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
	if orig.strip() == new.strip():
		wikipedia.output("No change, nothing to upload!")
		return text
	#wikipedia.output("5")
	wikipedia.showDiff(orig, new)
	answer = wikipedia.input(u"Upload change? ([y]es/[n]o/[l]og)")
	if answer == 'y':
		(before, code, after) = text.partition(rawCode)
		after = after.replace(orig, new, 1)
		text = "".join((before, code, after))
		#wikipedia.output(text)
		if upload == True:
			comment = u"Actualizez câmpul %s în lista de monumente" % field
			page.put(text, comment)
			return None
	elif answer == 'l' or answer == '':
		new = new.replace("\n", "<br/>")
		log(u"*''W'': ''[%s]'' De verificat dacă înlocuirea câmpului ''%s'' cu ''%s'' este corectă (inclusiv legăturile adăugate)" % (code, orig, new))
	#wikipedia.output("6")
	return text
	
def main():
	f = open("db.json", "r+")
	wikipedia.output("Reading database file...")
	db = json.load(f)
	wikipedia.output("...done")
	f.close();
	
	f = open("ro_pages.json", "r+")
	wikipedia.output("Reading ro.wp pages file...")
	pages_ro = json.load(f)
	wikipedia.output("...done")
	f.close();
	
	f = open("ro_authors.json", "r+")
	wikipedia.output("Reading ro.wp authors file...")
	authors_ro = json.load(f)
	wikipedia.output("...done")
	f.close();
	
	f = open("commons_Category_pages.json", "r+")
	wikipedia.output("Reading commons categories file...")
	categories_commons = json.load(f)
	wikipedia.output("...done")
	f.close();
	
	f = open("commons_File_pages.json", "r+")
	wikipedia.output("Reading commons images file...")
	pages_commons = json.load(f)
	wikipedia.output("...done")
	f.close();
	
	initLog()
	lastSource = None
	
	#this is the big loop that should only happen once
	for monument in db:
		if monument["source"] <> lastSource:
			articleText = None
			lastSource = monument["source"]
		rawCode = monument["Cod"]
		regexp = re.compile("(([a-z]{1,2})-(i|ii|iii|iv)-([a-z])-([a-z])-([0-9]{5}(\.[0-9]{2})?))", re.I)
		result = re.findall(regexp, rawCode)
		if len(result) > 0:
			code = result[0][0]
		else:
			code = rawCode
		wikipedia.output(code)
		allPages = list()
		article = None
		picture = None
		#wikipedia.output(str(page))
		try:
			#wikipedia.output("OK: " + str(page[code]))
			if code in pages_ro:
				allPages.extend(pages_ro[code])
				if len(allPages) > 1:
					msg = u"*''E'': ''[%s]'' Codul este prezent în mai multe articole pe Wikipedia: " % code
					for page in allPages:
						msg += (u"[[:%s]] " % page["name"])
					log(msg)
				elif len(allPages) == 1:
					article = allPages[0]
			if code in pages_commons:
				if len(pages_commons[code]) == 1: #exactly one picture
					picture = pages_commons[code][0]["name"]
				elif monument["Imagine"] == "": #no image, multiple available
					picture = ""
					msg = u"*''I'': ''[%s]'' Există %d imagini disponibile la commons pentru acest cod: " % (code, len(pages_commons[code]))
					for pic in pages_commons[code]:
						msg += u"[[:%s]], " % pic["name"]
						if pic["quality"] == True:
							picture = pic["name"]
							break
					if picture == "": #no quality pictures
						log(msg)
				allPages.extend(pages_commons[code])
			if code in categories_commons:
				allPages.extend(categories_commons[code])
				if len(categories_commons[code]) > 1:
					msg = u"*''E'': ''[%s]'' Codului îi corespund mai multe categorii la Commons: " % code
					for page in categories_commons[code]:
						msg += (u"[[:%s]] " % page["name"])
					log(msg)
			#wikipedia.output(str(allPages))
		except Exception as e:
			wikipedia.output("Error: " + str(e))
			pass #ignore errors
		
		#monument name and link
		if article <> None and article["name"] <> None and article["name"] <> "":
			if monument["Denumire"].find("[[") == -1:
				link = u"[[" + article["name"] + "|" + monument["Denumire"] + "]]"
				#wikipedia.output(link)
				articleText = updateTableData(monument["source"], code, "Denumire", link, text=articleText)
			else: # check if the 2 links are the same
				link = strainu.extractLink(monument["Denumire"])
				if link == None:
					log(u"*''W'': ''[%s]'' De verificat legătura internă din câmpul Denumire" % code)
				else:
					page1 = wikipedia.Page(wikipedia.getSite(), link)
					page2 = wikipedia.Page(wikipedia.getSite(), article["name"])
					if page1 <> page2 and \
					(not page1.isRedirectPage() or page1.getRedirectTarget() <> page2) and \
					(not page2.isRedirectPage() or page2.getRedirectTarget() <> page1):
						log(u"*''W'': ''[%s]'' Câmpul Denumire are o legătură internă către [[%s]], dar articolul despre monument este [[%s]]" % (code, page1, page2))
					
		#author from article
		if article <> None and article["author"] <> None and article["author"].strip() <> "":
			author = strainu.stripLink(article["author"]).strip()
			if author == None or author == "":
				wikipedia.output("Wrong link: %s" % article["author"])
			elif monument["Arhitect"] == "":
				wikipedia.output(author)
				articleText = updateTableData(monument["source"], code, "Arhitect", author, text=articleText)
			else:
				a1 = author.split("<ref")[0].strip()
				a2 = strainu.stripLink(monument["Arhitect"]).strip()
				if a1 <> a2:
					log(u"*''W'': ''[%s]'' Câmpul Arhitect este \"%s\", dar articolul despre monument menționează \"%s\"" % (code, a2, a1))

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
				wikipedia.output(authors)
				articleText = updateTableData(monument["source"], code, "Arhitect", authors, text=articleText)
			else:
				wikipedia.output("The authors list is unchanged for %s: %s" % (code, authors))
		
		#image from Commons, none in the list
		if picture <> None and picture <> "" and monument["Imagine"] == "":
			#wikipedia.output("Upload?" + picture)
			if picture.find(':') < 0:#no namespace
				picture = "File:" + picture
			articleText = updateTableData(monument["source"], code, "Imagine", picture, text=articleText)
		
		#use image from article only if none is available (or was selected) 
		#from commons and we don't have a picture in the list
		if (picture == None or picture == "") and \
		article <> None and article["image"] <> None and \
		article["image"] <> "" and monument["Imagine"].strip() == "":
			wikipedia.output(monument["Imagine"])
			artimage = strainu.extractImageLink(article["image"]).strip()
			if artimage == None or artimage == "":
				wikipedia.output("Wrong link: %s" % article["image"])
			if artimage.find(':') < 0:#no namespace
				artimage = "File:" + artimage
			#wikipedia.output("Upload?" + artimage)
			articleText = updateTableData(monument["source"], code, "Imagine", artimage, text=articleText)
			
		#Commons category
		if code in categories_commons:
			cat = categories_commons[code][0]
			if monument["Commons"] == "":
				articleText = updateTableData(monument["source"], code, "Commons", "commons:" + cat["name"], text=articleText)
			elif monument["Commons"] <> ("commons:" + cat["name"]):
				log(u"*''E'': ''[%s]'' Există mai multe categorii pentru acest cod: [[:%s]] și [[:%s]]" % (code, "commons:" + cat["name"], monument["Commons"]))
		
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
					if math.fabs(artLat - page["lat"]) > 0.01 or math.fabs(artLong - page["long"]) > 0.01:
						log(u"*''E'': ''[%s]'' Coordonate diferite între [[:%s]] (%f,%f) și [[:%s]] (%f,%f)" % (code, page["name"], page["lat"], page["long"], artCoord, artLat, artLong))
						updateCoord = False
				else:
					artLat = page["lat"]
					artLong = page["long"]
					artCoord = page["name"]
					
		if lat == 0 and artLat <> 0 and updateCoord:
			wikipedia.output(str(artLat) + " " + str(artLong))
			articleText = updateTableData(monument["source"], code, "Lat", str(artLat), upload = False, text=articleText)
			articleText = updateTableData(monument["source"], code, "Lon", str(artLong), upload = True, text = articleText)
		
		if lat <> 0 and artLat <> 0 and (math.fabs(artLat - lat) > 0.01 or math.fabs(artLong - long) > 0.01):
			log(u"*''E'': ''[%s]'' Coordonate diferite între [[:%s]] (%f,%f) și listă (%f,%f)" % (code, artCoord, artLat, artLong, lat, long))
		
	closeLog()

if __name__ == "__main__":
	try:
		main()
	finally:
		wikipedia.stopme()
