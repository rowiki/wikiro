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
** în listă și articol coordonatele sunt diferite (diferențe mai mari de 0,001 grade sau ~3,6 de secunde de grad)
'''

import sys, time, warnings, json, string, random, re
import math, urlparse
sys.path.append("..")
import strainu_functions as strainu

import pywikibot
from pywikibot import pagegenerators
from pywikibot import config as user

countries = {
	('ro', 'lmi') : {
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
		'fields' : {
                    u'Cod': u'Cod',
                    u'Denumire': u'Denumire',
                    u'Localitate': u'Localitate',
                    u'Adresă': u'Adresă',
                    u'Datare': u'Datare',
                    u'Arhitect': u'Creatori',
                    u'Creatori': u'Creatori',
                    u'Lat': u'Lat',
                    u'Coordonate': u'Coordonate',
                    u'Lon': u'Lon',
                    u'Imagine': u'Imagine',
                    u'Commons': u'Commons',
                    u'NotăCod': u'Notăcod',
                    u'FostCod': u'FostCod',
                    u'Cod92': u'Cod92',
                    u'CodRan': u'CodRan',
             },
	},
}

_log = "link.err.log"
_flog = None

_coordVariance = 0.001 #decimal degrees

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
	my_template = u"{{" + countries.get(('ro', 'lmi')).get('rowTemplate') + u"\n"
	for name in [u"Cod", u"NotăCod", u"FostCod", u"CodRan", u"Cod92", u"Denumire", u"Localitate", u"Adresă", u"Datare", u"Creatori", u"Lat", u"Lon", u"Imagine", u"Commons"]:
		if name in params and params[name] <> u"":
			my_template += u"| " + countries.get(('ro', 'lmi')).get('fields')[name] + u" = " + params[name] + u"\n"
	my_template += u"}}\n"
	return my_template

def updateTableData(url, code, field, newvalue, upload = True, text = None):
	pywikibot.output("Uploading %s for %s; value \"%s\"" % (field, code, newvalue))
	site = pywikibot.getSite()
	title = urlparse.parse_qs(urlparse.urlparse(str(url)).query)['title'][0].decode('utf8')
	page = pywikibot.Page(site, title)
	
	if text == None:
		pywikibot.output("Getting page contents")
		text = page.get()
		
	oldtext = text
	codeFound = False
	last = None
	rawCode = None
	my_params = {}
	rowTemplate = countries.get(('ro', 'lmi')).get('rowTemplate')
	
	templates = pywikibot.extract_templates_and_params(text)
	for (template, params) in templates:
		if template == rowTemplate:
			for param in params:
				val = params[param]
				val = val.split("<ref")[0].strip()
				val2 = re.sub(r'\s', '', val)
				
				fld = param.strip()
				
				if fld == "Cod" and val2 == code:
					codeFound = True
					rawCode = val
					my_params = params
					break
					
			if codeFound:
				break
	else: #for .. else
		log(u"*''E'': ''[%s]'' Codul nu este prezent în [[%s|listă]]" % (rawCode, title))
		pywikibot.output(u"Code not found: %s" % code)
		return None

	pywikibot.output(my_params)
	
	orig = rebuildTemplate(my_params)
	my_params[field] = newvalue
	new = rebuildTemplate(my_params)
	
	if orig.strip() == new.strip():
		pywikibot.output("No change, nothing to upload!")
		return text
	
	pywikibot.output(orig)
	pywikibot.output(new)
	pywikibot.showDiff(orig, new)
	
	answer = pywikibot.input(u"Upload change? ([y]es/[n]o/[l]og)")
	if answer == 'y':
		(before, code, after) = text.partition(rawCode)
		
		# we need to clean the whole template from both before and after
		clivb = before.rfind(u"{{" + rowTemplate)
		cliva = after.find(u"{{" + rowTemplate)
		# if we cannot find another template after the current, we
		# are most likely the last template on the page
		if cliva < 0:
			cliva = after.find(u"{{" + countries.get(('ro', 'lmi')).get('footerTemplate'))
		if cliva >= 0 and clivb >= 0:
			after = after[cliva:]
			before = before[:clivb]
		else:
			pywikibot.output("Could not find the current template, aborting!")
			return text
		
		# rebuild the page with the new text
		after = new + after
		text = "".join((before, after))
		
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
	
	return text
	
def readJson(filename, what):
	f = open(filename, "r+")
	pywikibot.output("Reading " + what + " file...")
	db = json.load(f)
	pywikibot.output("...done")
	f.close();
	return db
	
def readRan(filename):
	f = open(filename, "r+")
	pywikibot.output("Reading archeological monuments file...")
	ran = json.load(f)
	pywikibot.output("...done")
	f.close();
	
	db = {}
	for site in ran:
		if u"CodLMI" in site and site[u"CodLMI"] != u"":
			lmi = site[u"CodLMI"]
			if lmi not in db:
				db[lmi] = []
			db[lmi].append(site)
	return db
	
def parseArticleCoords(code, allPages):
	artLat = 0
	artLon = 0
	artSrc = None
	updateCoord = False
	for page in allPages:
		if page["lat"] <> 0 and page["long"] <> 0:
			if artLat <> 0: #this also means artLong <> 0
				if math.fabs(artLat - page["lat"]) > _coordVariance or \
					math.fabs(artLon - page["long"]) > _coordVariance:
					log(u"*''E'': ''[%s]'' Coordonate diferite între "
						u"[[:%s]] (%f,%f) și [[:%s]] (%f,%f)" % \
						(code, page["name"], page["lat"], page["long"], artSrc, artLat, artLon)
						)
					updateCoord = False
			else:
				artLat = page["lat"]
				artLon = page["long"]
				artSrc = page["name"]
				updateCoord = True
	return (artLat, artLon, artSrc, updateCoord)
	
def parseRanCoords(code, ran_data):
	#search in RAN database
	ranLat = 0
	ranLon = 0
	ranCod = u""
	updateCoord = False
	for site in ran_data:
		if site[u"Lat"] == u"" or site[u"Lon"] == u"":
			continue
		if ranLat == 0:
			ranLat = float(site[u"Lat"])
			ranLon = float(site[u"Lon"])
			ranCod = site[u"Cod"]
			updateCoord = True
		elif math.fabs(ranLat - float(site["Lat"])) > _coordVariance or \
			 math.fabs(ranLon - float(site["Lon"])) > _coordVariance:
				log(u"*''E'': ''[%s]'' Coordonate diferite între RAN:%s (%f,%f) și RAN:%s (%f,%f)" % \
					(code, site["Cod"], float(site["Lat"]), float(site["Lon"]), ranCod, ranLat, ranLon))
				updateCoord = False
	return (ranLat, ranLon, u"RAN: " + ranCod, updateCoord)
	
def parseOtherCoords(code, other_data):
	#search in external data
	updateCoord = False
	try:
		otherLat = float(other_data["lat"])
		otherLong = float(other_data["long"])
		updateCoord = True
	except ValueError:
		pass
	return (otherLat, otherLong, u"External data", updateCoord)

def main():
	db =				readJson("lmi_db.json", "database")
	pages_ro =			readJson("ro_pages.json", "ro.wp pages")
	authors_ro =		readJson("ro_authors.json", "ro.wp authors")
	files_ro =			readJson("ro_Fișier_pages.json", "ro.wp files")
	categories_commons =readJson("commons_Category_pages.json", "commons categories")
	pages_commons =		readJson("commons_File_pages.json", "commons images")
	other_data =		readJson("other_monument_data.json", "other data")
	
	ran_data = readRan("ran_db.json")

	initLog()
	lastSource = None

	#this is the big loop that must only happen once
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
		
		try:
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
					msg = u"*''I'': ''[%s]'' Există %d imagini disponibile la commons pentru acest cod: " % (code, len(pages_commons[code]))
					for pic in pages_commons[code]:
						if "lmi92" in pic:
							lmi92 = pic["lmi92"]
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

		elif monument["Imagine"].strip() == "":
			#use image from article only if none is available (or was selected) 
			#from commons and we don't have a picture in the list
			if article <> None and article["image"] <> None and article["image"] <> "":
				pywikibot.output(article["Imagine"])
				artimage = strainu.extractImageLink(article["image"]).strip()
				if artimage == None or artimage == "":
					pywikibot.output("Wrong article image link: \"%s\"@%s" % (article["image"], article["name"]))
				if artimage.find(':') < 0:#no namespace
					artimage = "File:" + artimage
				articleText = updateTableData(monument["source"], code, "Imagine", artimage, text=articleText)
			#next option: an image from the RAN database
			elif code in ran_data:
				for site in ran_data[code]:
					if site[u"Imagine"] != u"":
						ranimage = site[u"Imagine"] # it MUST have the namespace prefix
						articleText = updateTableData(monument["source"], code, 
											"Imagine", ranimage, text=articleText)
						break
			#next option: choose a random image from commons
			elif (code in pages_commons) and len(pages_commons[code]) > 0:
				tries = 0
				while True:
					artimage = random.sample(pages_commons[code],  1)[0]["name"]
					tries += 1
					#be picky and don't choose a detail picture; also stop on some arbitrary condition
					if artimage.find("detali") == -1 or tries == len(pages_commons[code]):
						break
				if artimage.find(':') < 0:#no namespace
					artimage = "File:" + artimage
				articleText = updateTableData(monument["source"], code, "Imagine", artimage, text=articleText)
			#final option: perhaps we have a local image?
			elif (code in files_ro) and len(files_ro[code]) > 0:
				localimage = random.sample(files_ro[code],  1)[0]["name"]
				if localimage.find(':') < 0:#nonamespace
					localimage = "File:" + localimage
				articleText = updateTableData(monument["source"], code, "Imagine", localimage, text=articleText)
		
		#Commons category
		if code in categories_commons:
			cat = categories_commons[code][0]
			if monument["Commons"] == "":
				articleText = updateTableData(monument["source"], code,
										"Commons", "commons:" + cat["name"], text=articleText)
			elif monument["Commons"].strip() <> ("commons:" + cat["name"].strip()):
				log(u"*''E'': ''[%s]'' Există mai multe categorii pentru acest cod: <span lang=\"x-sic\">[[:%s]] și [[:%s]]</span>" % (code, "commons:" + cat["name"], monument["Commons"]))
		#next option: a category from the RAN database
		elif code in ran_data:
			for site in ran_data[code]:
				if site[u"Commons"] != u"":
					rancat = site[u"Commons"] #it MUST have the namespace prefix
					articleText = updateTableData(monument["source"], code, 
										"Commons", rancat, text=articleText)
					break

		#latitude and longitude
		try:
			lat = float(monument["Lat"])
		except ValueError:
			lat = 0
		try:
			long = float(monument["Lon"])
		except ValueError:
			long = 0
			
		otherCoords = []
		otherCoords.append(parseArticleCoords(code, allPages))
		if code in ran_data:
			otherCoords.append(parseRanCoords(code, ran_data[code]))
		if code in other_data:
			otherCoords.append(parseOtherCoords(code, other_data[code]))

		for (otherLat, otherLong, otherSrc, otherValid) in otherCoords:
			if lat <> 0 and \
				otherValid and \
				otherLat <> 0 and \
				( \
				 math.fabs(otherLat - lat) > _coordVariance or \
				 math.fabs(otherLong - long) > _coordVariance \
				):
					log(u"*''E'': ''[%s]'' Coordonate diferite între %s (%f,%f) și listă (%f,%f)" % \
						(code, otherSrc, otherLat, otherLong, lat, long))

			elif lat == 0 and otherValid:
				print otherCoords
				pywikibot.output(u"Valid coord found:\n"
								u"\tSource: " + otherSrc + "\n" 
								u"\tLatitude: " + str(otherLat) + "\n" 
								u"\tLongitude: " + str(otherLong))
				articleText = updateTableData(monument["source"], code, "Lat", str(otherLat), 
								upload = False, text = articleText)
				articleText = updateTableData(monument["source"], code, "Lon", str(otherLong), 
								upload = True, text = articleText)
			
		#Codes from 1992
		if lmi92 <> None:
			articleText = updateTableData(monument["source"], code, "Cod92", lmi92, upload = True, text = articleText)
	
	closeLog()

if __name__ == "__main__":
	try:
		main()
	finally:
		pywikibot.stopme()
	
