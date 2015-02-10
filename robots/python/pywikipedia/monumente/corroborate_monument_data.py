#!/usr/bin/python
# -*- coding: utf-8  -*-
'''Based on the output from parse_monument_article.py and update_database.py 
identify different improvements and further errors in the monument pages and
database.

Improvements:
* add a link to the article about the monument (where such article exists)
* add the coordinates of the monument if they can be extracted from the 
  available data (list/article/pictures)
* add a picture if available
  - if the article has a picture, add that one
  - if there is only one picture, add it
  - if there are several pictures, add the one which has some quality
    recognition or a random one
   
Errors reported to a log file:
* The list does not have that code, but it has the coorect format in the
  article/image
* there are more than one categories/articles for a certain code
* the list/article/pictures have different coordinates (difference larger than
  0.001 grade or ~3,6 degree seconds)

Example: "python corroborate_monument_data.py -lang:ro -db:lmi -updateArticle"

Command-line arguments:

-import         Name of the file containing additional data; this can be either
                a JSON or a CSV file. The JSON keys or CSV columns must match
                the fields from the config

-db             Together with "-lang" specifies the config to be used.
                Default is "lmi"

-addRan         [ro specific] Add RAN codes and use RAN db to update monuments

-updateArticle  Update the link to the article if available

-updateImage    Update the image if available

-updateCoord    Update the coords if available

-updateCreator  Update the creator(s) if available

-updateCommons  Update the link to Wikimedia Commons if available
'''

import sys, time, warnings, json, string, random, re
import math, urlparse, os
import codecs
import collections
sys.path.append("..")
import strainu_functions as strainu
import csvUtils

import pywikibot
from pywikibot import pagegenerators
from pywikibot import config as user

class Changes:
	none	= 0x000
	article = 0x010
	coord   = 0x020
	image   = 0x040
        creator = 0x080
	commons = 0x100
	other	= 0x200
	all	= 0xFFF

countries = {
	('ro', 'lmi') : {
		'project' : u'wikipedia',
		'lang' : u'ro',
		'headerTemplate' : u'ÎnceputTabelLMI',
		'rowTemplate' : u'ElementLMI',
		'footerTemplate' : u'SfârșitTabelLMI',
		'namespaces' : [0],
		'codeRegexp' : "(([a-z]{1,2})-(i|ii|iii|iv)-([a-z])-([a-z])-([0-9]{5}(\.[0-9]{2,3})?))",
		'fields' : collections.OrderedDict([
						(u'Cod', {'code': Changes.all, }),
						(u'NotăCod', {'code': Changes.all, }),
						(u'FostCod', {'code': Changes.other, }),
						(u'CodRan', {'code': Changes.other, }),
						(u'Cod92', {'code': Changes.other, }),
						(u'Denumire', {'code': Changes.article, }),
						(u'Localitate', {'code': Changes.all, }),
						(u'Adresă', {'code': Changes.all, }),
						(u'Datare', {'code': Changes.all, }),
						(u'Creatori', {'code': Changes.creator, }),
						(u'Lat', {'code': Changes.coord, }),
						(u'Lon', {'code': Changes.coord, }),
						(u'Imagine', {'code': Changes.image, }),
						(u'Commons', {'code': Changes.commons, }),
						(u'Copyright', {'code': Changes.all, }),
					]),
	},
}

_flog = None
_coordVariance = 0.001 #decimal degrees
_changes = Changes.none
_lang ='ro'
_db = 'lmi'
_log = _lang + _db + "_link.err.log"


def initLog():
	global _flog, _log;
	_flog = open(_log, 'w+')

def closeLog():
	global _flog
	_flog.close()

def log(string):
	pywikibot.output(string.encode("utf8"))
	_flog.write(string.encode("utf8") + "\n")
	
def rebuildTemplate(params):
	my_template = u"{{" + countries.get((_lang, _db)).get('rowTemplate') + u"\n"
	for name in countries.get((_lang, _db)).get('fields'):
		if name in params and params[name] <> u"":
			my_template += u"| " + name + u" = " + params[name] + u"\n"
	my_template += u"}}\n"
	return my_template

def updateTableData(url, code, field, newvalue, upload = True, text = None, ask = True):
	"""
	:param url: The wiki page that will be updated
	:type url: string
	:param code: The LMI code that will be updated
	:type code: string
	:param field: The field that will be updated
	:type field: string
	:param newvalue: The new value that will be used for the field
	:type newvalue: string
	:param upload: How to handle the upload of the modified page. False means do not upload; 
		True means upload if we have a change; 
		None means upload even if the text has not changed;
		This option is for empty or previous changes; it will probalby be used with ask=False
	:type upload: Three-state boolean (True, False, None)
	:param text: The text that will be updated (None means get the page from the server)
	:type text: string
	:param ask: Whether to ask the user before uploading
	:type ask: boolean
	:return: The modified text of the page
	:rtype: string
	"""
	if (countries.get((_lang, _db)).get('fields')[field]['code'] & _changes) == Changes.none:
		pywikibot.output("Skipping %s for %s" % (field, code))
		return
	pywikibot.output("Uploading %s for %s; value \"%s\"" % (field, code, newvalue))
	site = pywikibot.Site()
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
	rowTemplate = countries.get((_lang, _db)).get('rowTemplate')
	
	templates = pywikibot.textlib.extract_templates_and_params(text)
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
		log(u"* [Listă] ''E'': ''[%s]'' Codul nu este prezent în [[%s|listă]]" % (rawCode, title))
		pywikibot.output(u"Code not found: %s" % code)
		return None
	
	orig = rebuildTemplate(my_params)
	my_params[field] = newvalue
	new = rebuildTemplate(my_params)
	
	if orig.strip() == new.strip() and upload != None:
		pywikibot.output("No change, nothing to upload!")
		return text
	
	pywikibot.output(orig)
	pywikibot.output(new)
	pywikibot.showDiff(orig, new)
	
	if ask:
		answer = pywikibot.input(u"Upload change? ([y]es/[n]o/[l]og)")
	else:
		answer = 'y'
	if answer == 'y':
		(before, code, after) = text.partition(rawCode)
		
		# we need to clean the whole template from both before and after
		clivb = before.rfind(u"{{" + rowTemplate)
		cliva = after.find(u"{{" + rowTemplate)
		# if we cannot find another template after the current, we
		# are most likely the last template on the page
		if cliva < 0:
			cliva = after.find(u"{{" + countries.get((_lang, _db)).get('footerTemplate'))
		if cliva >= 0 and clivb >= 0:
			after = after[cliva:]
			before = before[:clivb]
		else:
			pywikibot.output("Could not find the current template, aborting!")
			return text
		
		# rebuild the page with the new text
		after = new + after
		text = "".join((before, after))
		
		if upload == True or upload == None:
			comment = u"Actualizez câmpul %s în lista de monumente" % field
			try:
				page.put(text, comment)
			except pywikibot.exceptions.Error:
				pywikibot.output("Some error occured, let's move on and hope for the best!")
			return None
			
	elif answer == 'l':
		orig = orig.replace("\n", "<br/>")
		new = new.replace("\n", "<br/>")
		log(u"*''W'': ''[%s]'' De verificat dacă înlocuirea câmpului ''%s'' cu ''%s'' este corectă (inclusiv legăturile adăugate)" % (code, orig, new))
	
	return text
	
def readJson(filename, what):
	try:
		f = open(filename, "r+")
		pywikibot.output("Reading " + what + " file...")
		db = json.load(f)
		pywikibot.output("...done")
		f.close();
		return db
	except IOError:
		pywikibot.output("Failed to read " + filename + ". Trying to do without it.")
	
def readOtherData(filename):
	if not os.path.exists(filename):
		pywikibot.output("Could not find file " + filename)
		return {}
	pywikibot.output("Reading other data file...")
	if filename.endswith(".json"):
		f = open(filename, "r+")
		db = json.load(f)
		f.close()
	elif filename.endswith(".csv"):
		db = csvUtils.csvToJson(filename)
	else:
		pywikibot.output("Could not recognise file type")
		return {}
	pywikibot.output("...done")
	return db
	
def readRan(filename):
	"""
	This function is specific to ('ro','lmi'). We need to find a way to remove the need for it
	"""
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
					log(u"* [WPCOM] ''E'': ''[%s]'' Coordonate diferite între "
						u"[[:%s]] (%f,%f) și [[:%s]] (%f,%f)" % \
						(code, page["name"], page["lat"], page["long"], artSrc[5:], artLat, artLon)
						)
					updateCoord = False
			else:
				artLat = page["lat"]
				artLon = page["long"]
				artSrc = u"WP : " + page["name"]
				updateCoord = True
	return (artLat, artLon, artSrc, updateCoord)

def parseRanCoords(code, ran_data):
	"""
	search in RAN database
	"""
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
				log(u"* [RAN] ''E'': ''[%s]'' Coordonate diferite între RAN:%s (%f,%f) și RAN:%s (%f,%f)" % \
					(code, site["Cod"], float(site["Lat"]), float(site["Lon"]), ranCod, ranLat, ranLon))
				updateCoord = False
	return (ranLat, ranLon, u"RAN: " + ranCod, updateCoord)
	
def parseOtherCoords(code, other_data):
	"""
	search in external data
	"""
	updateCoord = False
	otherLat = 0
	otherLong = 0
	if u"Lat" in other_data and u"Lon" in other_data:
		try:
			otherLat = float(other_data["Lat"])
			otherLong = float(other_data["Lon"])
			updateCoord = True
		except ValueError:
			pass
	return (otherLat, otherLong, u"ALT: External data", updateCoord)
	
def addRan(code, monument, ran_data, articleText):
	if code in ran_data:
		sites = []
		for site in ran_data[code]:
			sites.append(site[u"Cod"])
		sites = list(set(sites))
		sites.sort()
		#print sites
		sites = ", ".join(sites)
		articleText = updateTableData(monument["source"], code, "CodRan", sites, \
								 text=articleText, ask=False)
	return articleText
	
def chooseImagePicky(files):
	blacklist = [#all lowercase
				u'detali',#detaliu, detalii
				u'pisani',#pisanie, pisanii
				u'interio',#interior, interioare
				u'plac',#placa, placă
				]
	tries = 0
	while tries < len(files):
		artimage = random.sample(files,  1)[0]["name"]
		tries += 1
		#be picky and don't choose a detail picture; also stop on some arbitrary condition
		for cond in blacklist:
			if (artimage.lower().find(cond) != -1):
				break
		else:
			return artimage
	return artimage
	
def checkNewMonuments(other_data, db):
	f = codecs.open(_db + "_monumente_noi.wiki", "w", "utf8")
	for monument in other_data:
		if monument in db:
			continue
		outText = rebuildTemplate(other_data[monument])
		f.write(outText)
	f.close()

def main():
	otherFile = "other_monument_data.csv"
	addRan = False
	global _changes
	
	for arg in pywikibot.handleArgs():
		if arg.startswith('-import:'):
			otherFile = arg [len('-import:'):]
		if arg.startswith('-addRan'):
			addRan = True
		if arg.startswith('-db'):
			_db = arg [len('-db:'):]
		if arg.startswith('-updateArticle'):
			_changes = _changes | Changes.article
		if arg.startswith('-updateImage'):
			_changes = _changes | Changes.image
		if arg.startswith('-updateCoord'):
			_changes = _changes | Changes.coord
		if arg.startswith('-updateCreator'):
			_changes = _changes | Changes.creator
		if arg.startswith('-updateCommons'):
			_changes = _changes | Changes.commons

	if _changes == Changes.none:
		_changes = Changes.all

	_lang = pywikibot.Site().lang
	if countries.get((_lang, _db)) == None:
		pywikibot.output("Couldn't find the config for lang %s, database %s. Please check the help below." % (_lang, _db))
		pywikibot.output("----")
		pywikibot.showHelp()
		return
	
	db_json =				readJson(_db + "_db.json", "database")
	pages_local =			readJson(_lang + "_pages.json", _lang + ".wp pages")
	authors_local =			readJson(_lang + "_authors.json", _lang + ".wp authors")
	files_local =			readJson(_lang + "_" + pywikibot.Site().namespace(6) + "_pages.json", _lang + ".wp files")
	categories_commons =	readJson("commons_Category_pages.json", "commons categories")
	pages_commons =			readJson("commons_File_pages.json", "commons images")
	
	other_data = readOtherData(otherFile)
	
	checkNewMonuments(other_data, db_json)
	
	if addRan:
		ran_data = readRan("ran_db.json")
	else:
		ran_data = {}

	initLog()
	lastSource = None

	#this is the big loop that must only happen once
	for monument in db_json:
		if monument["source"] <> lastSource:
			articleText = None
			lastSource = monument["source"]

		rawCode = monument["Cod"]
		regexp = re.compile(countries.get((_lang, _db)).get('codeRegexp'), re.I)
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
			if code in pages_local:
				allPages.extend(pages_local[code])
				if len(allPages) > 1:
					msg = u"* [WP] ''E'': ''[%s]'' Codul este prezent în mai multe articole pe Wikipedia: " % code
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
						monument["Creatori"].strip() == "" or \
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
						log(u"* [COM] '''E''': ''[%s]'' În lista de imagini sunt trecuți mai multi autori: %s" % (code, author_list))
				allPages.extend(pages_commons[code])
			if code in files_local:
				allPages.extend(files_local[code])
			if code in categories_commons:
				allPages.extend(categories_commons[code])
				if len(categories_commons[code]) > 1:
					msg = u"* [COM] ''E'': ''[%s]'' Codului îi corespund mai multe categorii la Commons: <span lang=\"x-sic\">" % code
					for page in categories_commons[code]:
						msg += (u"[[:commons:%s]], " % page["name"])
					msg += "</span>"
					log(msg)
			#pywikibot.output(str(allPages))
		except Exception as e:
			pywikibot.output("Error: " + str(e))
			pass #ignore errors

		#RAN
		if addRan:
			articleText = addRanData(code, monument, ran_data, articleText)
	
		#monument name and link
		if article <> None and article["name"] <> None and article["name"] <> "":
			if monument["Denumire"].find("[[") == -1:
				link = u"[[" + article["name"] + "|" + monument["Denumire"] + "]]"
				#pywikibot.output(link)
				articleText = updateTableData(monument["source"], code, "Denumire", link, text=articleText)
			else: # check if the 2 links are the same
				link = strainu.extractLink(monument["Denumire"])
				if link == None:
					log(u"* [Listă] ''W'': ''[%s]'' De verificat legătura internă din câmpul Denumire" % code)
				else:
					page1 = pywikibot.Page(pywikibot.Site(), link)
					page2 = pywikibot.Page(pywikibot.Site(), article["name"])
					if page1 <> page2 and \
					(not page1.isRedirectPage() or page1.getRedirectTarget() <> page2) and \
					(not page2.isRedirectPage() or page2.getRedirectTarget() <> page1):
						log(u"* [WPListă]''W'': ''[%s]'' Câmpul Denumire are o legătură internă către [[%s]], dar articolul despre monument este [[%s]]" % (code, page1, page2))

		#author from article
		if article <> None and article["author"] <> None and article["author"].strip() <> "":
			print "autor1"
			#author = strainu.stripLink(article["author"]).strip()
			author = article["author"].strip()
			if author == None or author == "":
				pywikibot.output("Wrong author link: \"%s\"@%s" % (article["author"], article["name"]))
			elif monument["Creatori"].strip() == "":
				pywikibot.output(author)
				articleText = updateTableData(monument["source"], code, "Creatori", author, text=articleText)
			else:
				a1 = author.strip()
				a2 = monument["Creatori"].strip()
				if a1 <> a2 and strainu.extractLink(a1) <> strainu.extractLink(a2):
					articleText = updateTableData(monument["source"], code, "Creatori", a1, text=articleText)
				#	log(u"*''W'': ''[%s]'' Câmpul Creatori este \"%s\", dar articolul despre monument menționează \"%s\"" % (code, a2, a1))

		#add the author(s) extracted from author pages
		elif code in authors_local:
			print "autor2"
			authors = monument["Creatori"]
			for author in authors_local[code]:
				if authors.find(author) == -1: #we don't already know the author
					if authors <> "":
						authors = author + ", " + authors
					else:
						authors = author
			a2 = monument["Creatori"].strip()
			if authors <> a2  and strainu.extractLink(authors) <> strainu.extractLink(a2): # if something changed, update the text
				pywikibot.output(authors)
				articleText = updateTableData(monument["source"], code, "Creatori", authors, text=articleText)
			else:
				pywikibot.output("The authors list is unchanged for %s: %s" % (code, authors))

		elif pic_author <> None and pic_author.strip() <> "":
			print "autor3"
			if strainu.stripLink(pic_author) <> strainu.stripLink(monument["Creatori"]).strip():
				articleText = updateTableData(monument["source"], code, "Creatori", pic_author, text=articleText)

		#try to find the author in external data
		elif code in other_data and "Creatori" in other_data[code]:
			print "autor4"
			authors = monument["Creatori"].strip()
			author = other_data[code]["Creatori"]
			if authors <> u"" and authors.find(author) == -1: #we don't already know the author
				authors = author + ", " + authors
			elif authors == u"":
				authors = author
			else:
				log("* [Listă] ''W'': ''[%s]'' Lista are creatorii %s, iar în fișierul importat apare %s" % \
						code, authors, author)
			if authors <> monument["Creatori"]: # if something changed, update the text
				pywikibot.output(authors)
				articleText = updateTableData(monument["source"], code, "Creatori", authors, text=articleText)
			else:
				pywikibot.output("The authors list is unchanged for %s: %s" % (code, authors))
	
		#image from Commons, none in the list
		if picture <> None and monument["Imagine"].strip() == "":
			#pywikibot.output("Upload?" + picture)
			if picture.find(':') < 0:#no namespace
				picture = "File:" + picture
			articleText = updateTableData(monument["source"], code, "Imagine", picture, text=articleText)

		elif monument["Imagine"].strip() == "":
			#use image from article only if none is available (or was selected) 
			#from commons and we don't have a picture in the list
			if article <> None and article["image"] <> None and article["image"] <> "":
				pywikibot.output(article["image"])
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
				artimage = chooseImagePicky(pages_commons[code])
				if artimage.find(':') < 0:#no namespace
					artimage = "File:" + artimage
				articleText = updateTableData(monument["source"], code, "Imagine", artimage, text=articleText)
			#final option: perhaps we have a local image?
			elif (code in files_local) and len(files_local[code]) > 0:
				localimage = chooseImagePicky(files_local[code])
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
				log(u"* [COM] ''E'': ''[%s]'' Există mai multe categorii pentru acest cod: <span lang=\"x-sic\">[[:%s]] și [[:%s]]</span>" % (code, "commons:" + cat["name"], monument["Commons"]))
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
			print code
			otherCoords.append(parseOtherCoords(code, other_data[code]))

		for (otherLat, otherLong, otherSrc, otherValid) in otherCoords:
			if (otherLat > 0 and strainu.getSec(otherLat) == 0) and \
				(otherLong > 0 and strainu.getSec(otherLong) == 0):
				log(u"* [%s] ''W'': ''[%s]'' Coordonatele (%f,%f) au nevoie de o verificare - secundele sunt 0" \
				 %	(otherSrc[:3], code, otherLat, otherLong))
				otherValid = False
				continue
			elif (otherLat > 0 or otherLong > 0) and \
				(otherLat > 48.3 or otherLat < 43.6 or \
				otherLong > 29.7 or otherLong < 20.27) :
				log(u"* [%s] ''W'': ''[%s]'' Coordonatele (%f,%f) au nevoie de o verificare - nu par " \
						"a fi din Romania" %	(otherSrc[:3], code, otherLat, otherLong))
				otherValid = False
				continue
			if lat <> 0 and \
				otherValid and \
				otherLat <> 0 and \
				( \
				 math.fabs(otherLat - lat) > _coordVariance or \
				 math.fabs(otherLong - long) > _coordVariance \
				):
					log(u"* [Listă] ''E'': ''[%s]'' Coordonate diferite între %s (%f,%f) și listă (%f,%f)" % \
						(code, otherSrc, otherLat, otherLong, lat, long))

			elif lat == 0 and otherValid:
				print otherCoords
				pywikibot.output(u"Valid coord found:\n"
								u"\tSource: " + otherSrc + "\n" 
								u"\tLatitude: " + str(otherLat) + "\n" 
								u"\tLongitude: " + str(otherLong))
				ask = True
				#if otherSrc[5:] == monument["CodRan"]:
				#	ask = False
				uploadlat = False
				if "Lon" in monument and monument["Lon"] != "":
					uploadlat = True
				articleText = updateTableData(monument["source"], code, "Lat", str(otherLat), 
								upload = uploadlat, text = articleText, ask = ask)
				articleText = updateTableData(monument["source"], code, "Lon", str(otherLong), 
								upload = True, text = articleText, ask = ask)
			
		#Codes from 1992
		if lmi92 <> None:
			articleText = updateTableData(monument["source"], code, "Cod92", lmi92, upload = True, text = articleText)
	
	closeLog()

if __name__ == "__main__":
	try:
		main()
	finally:
		pywikibot.stopme()
	
