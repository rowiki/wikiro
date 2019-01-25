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

The script requires some configuration for each database one plans to work on.
The global variable ''cfg'' is a dictionary. The key is a tuple containing
the language and the database. The value is another dict containing the
following fields:
* headerTemplate: The template that marks the beginning of the list in the page
* rowTemplate: The template that contains a row from the list; this is the 
		actual template containing the data
* footerTemplate: The template that marks the end of the list in the page
* codeRegexp: Regular expression alowing easy identification of the code.
	       This is needed in order to make sure the codes in the database/
	       pages/files are correct
* fields: An ordered dictionary of all the fields the rowTemplate can contain.
           They are used to maintain the same order of parameters for all
           instances of rowTemplate and to decide wether the current config
           requires the field to be updated (see below for details)
* geolimits: the limits of the area containing the monuments. It it a dictionary
		containing integer values for north, south, east and west

The script allows the user to choose which fields to update in the current run
by using the "update..." command line parameters described below. By default,
all the changes are uploaded. The actual fields that are updated are controlled
by the value of the "fields" dictionary in the config. Each one is a constant
from the Changes class:
* article - this field should be updated when the updateArticle param is given
* image - this field should be updated when the updateImage param is given
* coord - this field should be updated when the updateCoord param is given
* creator - this field should be updated when the updateCreator param is given
* commons - this field should be updated when the updateCommons param is given
* all - this field should be updated no matter what command line params are
         given; this should also be used for fields that are not automatically
         updated by this script

Example: "python corroborate_monument_data.py -lang:ro -db:lmi -updateArticle"

Command-line arguments:

-addRan         [ro specific] Add RAN codes and use RAN db to update monuments

-code           The LMI code we want to work on. Can also be a code prefix.

-county         The 2-letter code of the county (the same as the second part of
                the ISO 3166-2 code). Limits the monuments we work on to the
                ones from the specified county

-db             Together with "-lang" specifies the config to be used.
                Default is "lmi"

-enforceBlacklist Do not use images from blacklist. Default behavior is to use
				them as last resort

-import         Name of the file containing additional data; this can be either
                a JSON or a CSV file. The JSON keys or CSV columns must match
                the fields from the config

-force          Force the update of the fields even if we already have a value

-updateArticle  Update the link to the article if available

-updateCommons  Update the link to Wikimedia Commons if available

-updateCoord    Update the coords if available

-updateCreator  Update the creator(s) if available

-updateImage    Update the image if available
'''

import sys, time, warnings, json, string, random, re
import math
import urllib
import os
import codecs
import collections
sys.path.append("wikiro/robots/python/pywikipedia")
import strainu_functions as strainu
import csvUtils

import monumente
from monumente import Changes

import pywikibot
from pywikibot import pagegenerators
from pywikibot import config as user

_flog = None
_coordVariance = 0.001 #decimal degrees
_changes = Changes.none
_lang ='ro'
_db = 'lmi'
_log = _lang + _db + "_link.err.log"
_differentCoords = {}
_allowBlacklist = True

def initLog():
	global _flog, _log;
	_flog = open(_log, 'w+')

def closeLog():
	global _flog
	_flog.close()

def log(string):
	pywikibot.output(string.encode("utf8"))
	_flog.write(string + "\n")

def getCfg(_lang, _db):
	return monumente.config.get(_lang).get(_db)

def getFields(_lang, _db):
	return getCfg(_lang, _db).get('fields')
	
def rebuildTemplate(params, skipEmpty=True):
	my_template = u"{{" + getCfg(_lang, _db).get('rowTemplate') + u"\n"
	for name in getFields(_lang, _db):
		if skipEmpty and name in params and params[name] != u"":
			my_template += u"| " + name + u" = " + params[name].strip() + u"\n"
		elif not skipEmpty and name in params:
			my_template += u"| " + name + u" = " + params[name].strip() + u"\n"
	my_template += u"}}\n"
	return my_template

def isNullorEmpty(s):
	return bool(not s or s.strip() == u"")

def updateTableData(url, code, field, newvalue, olddata, upload = True, text = None, ask = True, source = None):
	"""
	:param url: The wiki page that will be updated
	:type url: string
	:param code: The LMI code that will be updated
	:type code: string
	:param field: The field that will be updated
	:type field: string
	:param newvalue: The new value that will be used for the field
	:type newvalue: string
	:param olddata: The old values from the list for the current code
	:type newvalue: array
	:param upload: How to handle the upload of the modified page. False means do not upload; 
		True means upload if we have a change; 
		None means upload even if the text has not changed;
		This option is for empty or previous changes; it will probalby be used with ask=False
	:type upload: Three-state boolean (True, False, None)
	:param text: The text that will be updated (None means get the page from the server)
	:type text: string
	:param ask: Whether to ask the user before uploading
	:type ask: boolean
	:param source: The source of the date; it is used in the commit description
	:type source: string
	:return: The modified text of the page
	:rtype: string
	"""
	if not field:
		return
	if (field not in getFields(_lang, _db) or \
	getFields(_lang, _db)[field]['code'] & _changes) == Changes.none:
		pywikibot.output("Skipping updating %s for %s" % (field, code))
		return
	if hasDependencyCycles(field, newvalue, getFields(_lang, _db)[field]['code'], olddata):
		pywikibot.output("Skipping updating %s for %s" % (field, code))
		return

	site = pywikibot.Site()
	title = urllib.parse.parse_qs(urllib.parse.urlparse(str(url)).query)['title'][0]
	page = pywikibot.Page(site, title)
	
	if text == None:
		text = page.get()
		
	oldtext = text
	codeFound = False
	last = None
	rawCode = None
	my_params = {}
	rowTemplate = getCfg(_lang, _db).get('rowTemplate')
	
	templates = pywikibot.textlib.extract_templates_and_params(text, strip=True)
	for (template, params) in templates:
		if template == rowTemplate:
			#params = { k.strip(): v for k,v in params.items() }
			old_params = dict(params)
			params = monumente.filterOne(params, getCfg(_lang, _db))
			for param in params:
				val = params[param]
				val = val.split("<ref")[0].strip()
				val2 = re.sub(r'\s', '', val)
				
				fld = param.strip()
				
				if fld == getCfg(_lang, _db).get('idField') and val2 == code:
					codeFound = True
					rawCode = old_params[param]
					my_params = params
					break
			
			if codeFound:
				break
	else: #for .. else
		log(u"* [Listă] ''E'': ''[%s]'' Codul nu este prezent în [[%s|listă]]" % (rawCode, title))
		pywikibot.output(u"updateTableData: Code %s not found, giving up" % code)
		return None
	
	orig = rebuildTemplate(my_params, not getCfg(_lang, _db).get('keepEmptyFields')) 
	my_params[field] = newvalue
	new = rebuildTemplate(my_params, not getCfg(_lang, _db).get('keepEmptyFields'))
	
	if orig.strip() == new.strip() and upload != None:
		#pywikibot.output("No change in field %s for %s, nothing to upload!" % (field, code))
		return text

	pywikibot.output("Updating %s for %s to value \"%s\"" % (field, code, newvalue))
	
	pywikibot.output(orig)
	pywikibot.output(new)
	pywikibot.showDiff(orig, new)
	
	if ask:
		answer = pywikibot.input(u"Upload change? ([y]es/[n]o/[l]og)")
	else:
		answer = 'y'
	if answer == 'y':
		if code == u"MD-GL-rs-005":
			import pdb
			pdb.set_trace()
		(before, code, after) = text.partition(rawCode)
		
		# we need to clean the whole template from both before and after
		clivb = before.rfind(u"{{" + rowTemplate)
		cliva = after.find(u"{{" + rowTemplate)
		# if we cannot find another template after the current, we
		# are most likely the last template on the page
		if cliva < 0:
			cliva = after.find(u"{{" + getCfg(_lang, _db).get('footerTemplate'))
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
			if source:
				s_text = u" folosind date din %s" % source
			else:
				s_text = u""
			comment = u"Actualizez câmpul %s în lista de monumente%s" % (field, s_text)
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
		f.close()
		return db
	except IOError:
		pywikibot.error("Failed to read " + filename + ". Trying to do without it.")
		return {}
	
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
		pywikibot.error("Could not recognise file type")
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
	global _differentCoords
	for page in allPages:
		if page["lat"] != 0 and page["long"] != 0:
			if artLat != 0: #this also means artLong != 0
				if math.fabs(artLat - page["lat"]) > _coordVariance or \
					math.fabs(artLon - page["long"]) > _coordVariance:
					if page["name"] not in _differentCoords:
						_differentCoords[page["name"]] = [page["lat"], page["long"]]
					if artSrc[5:] not in _differentCoords:
						_differentCoords[artSrc[5:]] = [artLat, artLon]
					updateCoord = False
			else:
				artLat = page["lat"]
				artLon = page["long"]
				artSrc = u"WP : " + page["name"]
				updateCoord = True
	return (artLat, artLon, artSrc, updateCoord, {"prefix": artSrc})

def parseRanCoords(code, ran_data):
	"""
	search in RAN database
	"""
	ranLat = 0
	ranLon = 0
	ranCod = u""
	updateCoord = False
	prefix = u"RAN: "
	global _differentCoords
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
				if site["Cod"] not in _differentCoords:
					_differentCoords[site["Cod"]] = [float(site["Lat"]), float(site["Lon"])]
				if ranCod not in _differentCoords:
					_differentCoords[ranCod] = [ranLat, ranLon]
				updateCoord = False
	return (ranLat, ranLon, prefix + ranCod, updateCoord, {"prefix": prefix})
	
def parseOtherCoords(code, other_data, fields):
	"""
	search in external data
	"""
	updateCoord = False
	otherLat = 0
	otherLong = 0
	otherSrc = "N/A "
	if fields.get("lat", 0) in other_data and fields.get("lon", 0) in other_data:
		try:
			otherLat = float(other_data[fields["lat"]])
			otherLong = float(other_data[fields["lon"]])
			updateCoord = True
		except ValueError:
			pass
	#print fields
	if fields["prefix"]:
		otherSrc = u"%s (date externe)" % fields["prefix"]
	return (otherLat, otherLong, otherSrc, updateCoord, fields)
	
def addRanData(code, monument, ran_data, articleText):
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

def getImageType(image):
	""" 
	This function decides if an image should be uploaded or not, and if yes,
	what field it should be uploaded to

	:return: None if it should not be uploaded, otherwise the field it should
		be uploaded to
	:type: string or None
	"""	
	search = image.lower()
	#print(search)
	fields = getFields(_lang, _db)
	for field in fields:
		#print("*" + field)
		if fields[field]['code'] == Changes.image:
			for skip in fields[field]['blacklist']:
				#print("**" + skip)
				if search.find(skip) != -1:
					#print("break")
					break
			else:
				return field

	return None

def chooseImagePicky(files):
	#print(files)
	tries = 0
	randimagelist = random.sample(files,  len(files))
	print(randimagelist)
	while tries < len(files):
		artimage = randimagelist[tries]["name"]
		tries += 1
		#be picky and don't choose a detail picture; also stop on some arbitrary condition
		imageType =  getImageType(artimage)
		if imageType:
			return (artimage, imageType)
	if _allowBlacklist:
		return (artimage, getImageType(""))
	return None, None
	
def checkNewMonuments(other_data, db):
	f = codecs.open(_db + "_monumente_noi.wiki", "w", "utf8")
	for monument in other_data:
		if monument in db:
			continue
		outText = rebuildTemplate(other_data[monument])
		f.write(outText)
	f.close()

def getYearsFromWikidata(page):
	try:
		wdpage = page.data_item()
	except:#no wikidata item? Perhaps a new page?
		return None
	data = wdpage.get()
	#print data['claims']
	if 'P570' in data['claims']:
		if len(data['claims']['P570']) > 1:
			log(u"* [WD] '''W''': ''[%s]'' are mai multe date ale decesului la Wikidata" % (page.title()))
			return None
		claim = data['claims']['P570'][0]
		return claim.getTarget().year
	return None

def extractCopyrightField(creators):
	last_death = 0
	#print creators
	post = creators
	while post:
		parsed = strainu.extractLinkAndSurroundingText(post)
		if parsed:
			(pre, link, post) = parsed
		else:
			break
		page = pywikibot.Page(pywikibot.Site(), link)
		if not page.exists():
			continue
		while page.isRedirectPage():
			page = page.getRedirectTarget()
		year = getYearsFromWikidata(page)
		#print page.title()
		#print year
		if year and year > last_death:
			last_death = year
	return last_death

def hasDependencyCycles(field, value, type, monument):
	"""
	This function checks if the value already exists in another field of
	the same type (such as OsmLat for Lat, Plan for Image etc.)

	Checking this breaks dependency cycles between the list, the articles
	and Wikidata.
	"""
	fields = getFields(_lang, _db)
	for otherField in fields:
		#print "*" + field
		if fields[otherField]['code'] & type and \
			field != otherField and \
			monument.get(otherField) == value:
			pywikibot.output(u"Dependency cycle detected:\nmonument[%s]=%s\nmonument[%s]=%s" % (field, value, otherField, monument[otherField]))
			return True
	return False

def getFieldsWithAliases():
	fields = getFields(_lang, _db)
	return {field: fields[field]['alias'] for field in fields if fields[field].get('alias')}

def getMiscDataFromPages(dataSource):
	data = {}
	fields = getFieldsWithAliases()
	for field in fields:
		alias = fields[field]
		if alias in dataSource:
			data[field] = dataSource[alias]
	return data

def main():
	otherFile = "other_monument_data.csv"
	addRan = False
	force = False
	codePrefix = None
	global _changes, _db, _differentCoords, _allowBlacklist
	
	for arg in pywikibot.handleArgs():
		if arg.startswith('-import:'):
			otherFile = arg [len('-import:'):]
		if arg.startswith('-addRan'):
			addRan = True
		if arg.startswith('-db'):
			_db = arg [len('-db:'):]
		if arg.startswith('-force'):
			force = True
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
		if arg.startswith('-enforceBlacklist'):
			_allowBlacklist = False
		for prefixParam in ['-code:', '-county:']:
			if arg.startswith(prefixParam):
				codePrefix = arg [len(prefixParam):]

	if _changes == Changes.none:
		_changes = Changes.all

	_lang = pywikibot.Site().lang
	if getCfg(_lang, _db) == None:
		pywikibot.output("Couldn't find the config for lang %s, database %s. Did you miss a parameter?" % (_lang, _db))
		pywikibot.output("----")
		pywikibot.showHelp()
		return

	if force and _changes == Changes.all:
		answer = pywikibot.inputChoice("Are you sure you want to force update ALL the fields? This will cause a lot of changes.", ["Yes", "No"], ["y", "n"])
		if answer != 'y':
			return
	
	db_json =				readJson("_".join(filter(None, [_lang, _db, "db.json"])), "database")
	pages_local =			readJson("_".join(filter(None, [_lang, _db, "pages.json"])), _lang + ".wp pages")
	authors_local =			readJson("_".join(filter(None, [_lang, _db, "authors.json"])), _lang + ".wp authors")
	files_local =			readJson("_".join(filter(None, [_lang, _db,  pywikibot.Site().namespace(6), "pages.json"])), _lang + ".wp files")
	categories_commons =	readJson("_".join(filter(None, ["commons", _db, "Category_pages.json"])), "commons categories")
	pages_commons =			readJson("_".join(filter(None, ["commons", _db, "File_pages.json"])), "commons images")

	other_data = readOtherData(otherFile)
	
	checkNewMonuments(other_data, db_json)
	
	if addRan:
		ran_data = readRan("ro_ran_db.json")
	else:
		ran_data = {}
	articleField = u"Denumire"
	fields = getFields(_lang, _db)
	for field in fields:
		if fields[field]['code'] == Changes.article:
			articleField = field
			break
	creatorField = u"Creatori"
	for field in fields:
		if fields[field]['code'] == Changes.creator:
			creatorField = field
			break
	imageField = u"Imagine"
	for field in fields:
		if fields[field]['code'] == Changes.image:
			imageField = field
			break
	latField = None
	lonField = None
	for field in fields:
		if fields[field]['code'] == Changes.coord:
			#TODO: maybe use the first letters? We currently hardcode the order
			if not latField:
				latField = field
			elif not lonField:
				lonField = field

	initLog()
	lastSource = None

	#this is the big loop that must only happen once
	for index, monument in enumerate(db_json):
		if monument["source"] != lastSource:
			articleText = None
			lastSource = monument["source"]

		rawCode = monument[getCfg(_lang, _db).get('idField')]
		regexp = re.compile(getCfg(_lang, _db).get('codeRegexp'), re.I)
		result = re.findall(regexp, rawCode)
		if len(result) > 0:
			code = result[0][0]
		else:
			code = rawCode
		if codePrefix and not code.startswith(codePrefix):
			continue
		pywikibot.output(code)

		allPages = list()
		article = None
		picture = None
		pic_author = None
		otherData = []
		ran = None
		_differentCoords = {}
		last_death = 0
		
		try:
			copyright = monument.get(u"Copyright") or u"yes"
			if monument.get(creatorField) != "" and (copyright == u"" or force):
				last_death = extractCopyrightField(monument.get(creatorField))
			elif len(copyright) == 4:#hack based on the valid values of Copyright
				last_death = int(copyright)

			if code in pages_local:
				allPages.extend(pages_local[code])
				if len(allPages) > 1:
					msg = u"* [WP] ''E'': ''[%s]'' Codul este prezent în mai multe articole pe Wikipedia: " % code
					for page in allPages:
						msg += (u"[[:%s]], " % page["name"])
					log(msg)
				elif len(allPages) == 1:
					article = allPages[0]
					ran = allPages[0].get('ran')
			if code in pages_commons:
				if len(pages_commons[code]) == 1: #exactly one picture
					#picture = pages_commons[code][0]["name"]
					if pic_author == None:
						pic_author = pages_commons[code][0]["author"]
					otherData.append(getMiscDataFromPages(pages_commons[code][0]))
				elif isNullorEmpty(monument.get(imageField)) or \
						isNullorEmpty(monument.get(creatorField)) or \
						any([isNullorEmpty(monument.get(field)) for field in getFieldsWithAliases()]):
					#multiple images available, we need to parse them
					msg = u"*''I'': ''[%s]'' Există %d imagini disponibile la commons pentru acest cod: " % (code, len(pages_commons[code]))
					for pic in pages_commons[code]:
						otherData.append(getMiscDataFromPages(pic))
						author_list = ""
						if pic_author == None and author_list == "" and pic["author"] != None:
							pic_author = pic["author"]
							author_list += u"[[:%s]], " % pic["name"]
							#print u"Pic Author: " + pic_author
						elif pic["author"] != None and pic_author != pic["author"]:
							#multiple authors, ignore and report error
							author_list += u"[[:%s]], " % pic["name"]
							pic_author = None
							#print "pic_author removed"
						msg += u"[[:%s]], " % pic["name"]
						if pic["quality"] == True: #choose the first quality picture
							picture = pic["name"]
							break
					if picture == None: #no quality pictures, but do not log - we'll choose a random one
						pass
						#log(msg)
					if pic_author == None and author_list != "":
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
			import traceback
			traceback.print_exc()
			raise
			pass #ignore errors

		#RAN
		if addRan:
			articleText = addRanData(code, monument, ran_data, articleText)
	
		#monument name and link
		if article != None and article["name"] != None and article["name"] != "":
			if monument[articleField].find("[[") == -1:
				link = u"[[" + article["name"] + "|" + monument[articleField] + "]]"
				#pywikibot.output(link)
				articleText = updateTableData(monument["source"], code, articleField, link, monument, text=articleText)
			else: # check if the 2 links are the same
				link = strainu.extractLink(monument[articleField])
				if link == None:
					log(u"* [Listă] ''W'': ''[%s]'' De verificat legătura internă din câmpul Denumire - e posibil să existe o problemă de închidere a tagurilor" % code)
				else:
					page1 = pywikibot.Page(pywikibot.Link(link, pywikibot.Site()))
					page2 = pywikibot.Page(pywikibot.Site(), article["name"])
					if force and page1.title() != page2.title():
						field = "".join(strainu.stripLinkWithSurroundingText(monument[articleField]))
						link = u"[[" + article["name"] + "|" + field + "]]"
						articleText = updateTableData(monument["source"], code, articleField, link, monument, text=articleText)
					elif page1.title() != page2.title() and \
					(not page1.isRedirectPage() or page1.getRedirectTarget() != page2) and \
					(not page2.isRedirectPage() or page2.getRedirectTarget() != page1):
						log(u"* [WPListă]''W'': ''[%s]'' Câmpul Denumire are o legătură internă către %s, dar articolul despre monument este %s" % (code, page1, page2))

		#author from article
		if article != None and article["author"] != None and article["author"].strip() != "":
			#print "Author from article"
			#author = strainu.stripLink(article["author"]).strip()
			author = article["author"].strip()
			if author == None or author == "":
				pywikibot.output("Wrong author link: \"%s\"@%s" % (article["author"], article["name"]))
			elif monument[creatorField].strip() == "":
				pywikibot.output(author)
				articleText = updateTableData(monument["source"], code, creatorField, author, monument, text=articleText)
			else:
				a1 = author.strip()
				a2 = monument[creatorField].strip()
				if a1 != a2 and strainu.extractLink(a1) != strainu.extractLink(a2) and force:
					articleText = updateTableData(monument["source"], code, creatorField, a1, monument, text=articleText)
				#	log(u"*''W'': ''[%s]'' Câmpul Creatori este \"%s\", dar articolul despre monument menționează \"%s\"" % (code, a2, a1))

		#add the author(s) extracted from author pages
		elif code in authors_local:
			#print "Author from author pages"
			authors = monument[creatorField].strip()
			for author in authors_local[code]:
				if authors.find(author["name"]) == -1: #we don't already know the author
					if authors != "":
						authors = author["name"] + ", " + authors
					else:
						authors = author["name"]
				if author["dead"] and author["dead"] > last_death:
					log(u"* [WPListă]''W'': ''[%s]'' Se pare că nu toți creatorii sunt menționați în listă. În articol, [[%s]] este menționat ca autor." % (code, author["name"]))
					last_death = author["dead"]
			a2 = monument[creatorField].strip()
			if a2 == u"" or authors != a2  and strainu.extractLink(authors) != strainu.extractLink(a2): # if something changed, update the text
				if force or a2 == u"":
					pywikibot.output(authors)
					articleText = updateTableData(monument["source"], code, creatorField, authors, monument, text=articleText)

		elif pic_author != None and pic_author.strip() != "":
			#print "Author from commons"
			if strainu.stripLink(pic_author) != strainu.stripLink(monument[creatorField]).strip():
				if force or monument[creatorField].strip() == u"":
					articleText = updateTableData(monument["source"], code, creatorField, pic_author, monument, text=articleText)

		#try to find the author in external data
		elif code in other_data and creatorField in other_data[code]:
			#print "Author from other data"
			authors = monument[creatorField].strip()
			author = other_data[code][creatorField]
			if authors != u"" and authors.find(author) == -1: #we don't already know the author
				authors = author + ", " + authors
			elif authors == u"":
				authors = author
			else:
				log("* [Listă] ''W'': ''[%s]'' Lista are creatorii %s, iar în fișierul importat apare %s" % \
						code, authors, author)
			if authors != monument[creatorField]: # if something changed, update the text
				if force:
					pywikibot.output(authors)
					articleText = updateTableData(monument["source"], code, creatorField, authors, monument, text=articleText)

		# --- Copyright ---
		if last_death > 0:
			articleText = updateTableData(monument["source"], code, u'Copyright', str(last_death), monument, text=articleText)
	
		# --- Choose an image ---

		#we will only consider other types of image if no picture exists
		if isNullorEmpty(monument.get(imageField)) or force:
			#image from Commons, none in the list
			if picture != None:
				#pywikibot.output("We're uploading a selected picture from Commons: " + picture)
				if picture.find(':') < 0:#no namespace
					picture = "File:" + picture
				picture, pictureType = chooseImagePicky([{"name": picture}])
				articleText = updateTableData(monument.get("source"), code, pictureType, picture, monument, text=articleText)
			elif force:
				pass# when forced, only upload quality images
			#use image from article only if none is available (or was selected) 
			#from commons and we don't have a picture in the list
			elif article != None and article["image"] != None and article["image"] != "":
				#pywikibot.output("We're uploading image " + article["image"] + " from the article")
				artimage = strainu.extractImageLink(article["image"]).strip()
				if artimage == None or artimage == "":
					pywikibot.output("Wrong article image link: \"%s\"@%s" % (article["image"], article["name"]))
				if artimage.find(':') < 0:#no namespace
					artimage = "File:" + artimage
				artimage,artimageType = chooseImagePicky([{"name": artimage}])
				articleText = updateTableData(monument["source"], code, artimageType, artimage, monument, text=articleText)
			#next option: an image from the RAN database
			elif code in ran_data:
				for site in ran_data[code]:
					if site[u"Imagine"] != u"":
						ranimage = site[u"Imagine"] # it MUST have the namespace prefix
						ranimage,ranimageType = chooseImagePicky([{"name": artimage}])
						articleText = updateTableData(monument["source"], code, 
											ranimageType, ranimage,
											monument, text=articleText)
						break
			#next option: choose a random image from commons
			elif (code in pages_commons) and len(pages_commons[code]) > 0:
				artimage,artimageType = chooseImagePicky(pages_commons[code])
				if artimage.find(':') < 0:#no namespace
					artimage = "File:" + artimage
				articleText = updateTableData(monument["source"], code, artimageType, artimage, monument, text=articleText)
			#final option: perhaps we have a local image?
			elif (code in files_local) and len(files_local[code]) > 0:
				localimage,localimageType = chooseImagePicky(files_local[code])
				if localimage.find(':') < 0:#nonamespace
					localimage = "File:" + localimage
				articleText = updateTableData(monument["source"], code, localimageType, localimage, monument, text=articleText)
			#bonus option for ensembles: try to get images from the first submonument
			else:
				newcode = code.replace(u"-a-", u"-m-") + u".01"
				if code.find(u"-a-") > -1 and \
					db_json[index+1][getCfg(_lang, _db).get('idField')] == newcode and \
					not isNullorEmpty(db_json[index+1].get(imageField)):
					pywikibot.output(u"Importing image from submonument %s" % newcode)
					articleText = updateTableData(monument["source"], code, imageField, db_json[index+1].get(imageField), monument, text=articleText)

		# --- Commons category ---
		if code in categories_commons:
			cat = categories_commons[code][0]
			if isNullorEmpty(monument.get("Commons")) or force:
				articleText = updateTableData(monument["source"], code,
								"Commons", "commons:" + cat["name"],
								monument, text=articleText)
			elif monument.get("Commons") and monument.get("Commons").strip() != ("commons:" + cat["name"].strip()):
				log(u"* [COM] ''E'': ''[%s]'' Există mai multe categorii pentru acest cod: <span lang=\"x-sic\">[[:%s]] și [[:%s]]</span>" % (code, "commons:" + cat["name"], monument["Commons"]))
		#next option: a category from the RAN database
		elif code in ran_data:
			for site in ran_data[code]:
				if site[u"Commons"] != u"":
					rancat = site[u"Commons"] #it MUST have the namespace prefix
					articleText = updateTableData(monument["source"], code, 
									"Commons", rancat,
									monument, text=articleText)
					break

		# --- Coordinates ---
		try:
			lat = float(monument.get(latField))
		except ValueError:
			lat = 0
		try:
			long = float(monument.get(lonField))
		except ValueError:
			long = 0
			
		otherCoords = []
		otherCoords.append(parseArticleCoords(code, allPages))
		if code in ran_data:
			otherCoords.append(parseRanCoords(code, ran_data[code]))
		if code in other_data:
			#print other_data[code]
			otherCoords.append(parseOtherCoords(code, other_data[code], {"prefix": other_data[code].get("Source")}))
			otherCoords.append(parseOtherCoords(code, other_data[code], {"lat": u"OsmLat", "lon": u"OsmLon", "prefix": u"OSM"}))

		for (otherLat, otherLong, otherSrc, otherValid, otherFields) in otherCoords:
			if (otherLat > 0 and strainu.getSec(otherLat) == 0) and \
				(otherLong > 0 and strainu.getSec(otherLong) == 0):
				if not otherSrc:
					otherSrc = u"sursă externă necunoscută"
				log(u"* [%s] ''W'': ''[%s]'' Coordonatele (%f,%f) din %s au nevoie de o verificare - secundele sunt 0" \
				 %(otherSrc, code, otherLat, otherLong, otherSrc))
				otherValid = False
				continue
			elif (otherLat > 0 or otherLong > 0) and \
				(otherLat > getCfg(_lang, _db).get('geolimits').get('north') or \
				otherLat < getCfg(_lang, _db).get('geolimits').get('south') or \
				otherLong > getCfg(_lang, _db).get('geolimits').get('east') or \
				otherLong < getCfg(_lang, _db).get('geolimits').get('west')) :
				log(u"* [%s] ''W'': ''[%s]'' Coordonatele (%f,%f) au nevoie de o verificare - nu par " \
						u"a fi din regiunea căutată" %	(otherSrc[:3], code, otherLat, otherLong))
				otherValid = False
				continue
			if lat != 0 and \
				otherValid and \
				otherLat != 0 and \
				( \
				 math.fabs(otherLat - lat) > _coordVariance or \
				 math.fabs(otherLong - long) > _coordVariance \
				):
					if otherSrc not in _differentCoords:
						_differentCoords[otherSrc[5:]] = [otherLat, otherLong]
					if monument["source"] not in _differentCoords:
						src = strainu.convertUrlToWikilink(monument["source"])
						_differentCoords[src] = [lat, long]

			elif (lat == 0 or force) and otherValid:
				pywikibot.output(u"Valid coord found:\n"
								u"\tSource: " + otherSrc + "\n" 
								u"\tLatitude: " + str(otherLat) + "\n" 
								u"\tLongitude: " + str(otherLong))
				ask = True
				#if otherSrc[5:] == monument["CodRan"]:
				#	ask = False
				uploadlat = ("Lon" in monument and monument["Lon"] != "")
				articleText = updateTableData(monument["source"], code, otherFields.get("lat") or latField, str(otherLat), monument,
								upload = uploadlat, text = articleText, ask = ask, source = otherSrc)
				articleText = updateTableData(monument["source"], code, otherFields.get("lon") or lonField, str(otherLong), monument,
								upload = True, text = articleText, ask = ask, source = otherSrc)
		if len(_differentCoords) > 0:
			text = u"* ''E'': ''[%s]'' Coordonate diferite între " % code
			for src in _differentCoords:
				text += u"<br/>[[:%s]] (%f, %f), " % (src, _differentCoords[src][0], _differentCoords[src][1])
			log(text)
	
		#Other data
		for entry in otherData:
			for field in entry:
				#print entry[field]
				if not isNullorEmpty(entry[field]) and (isNullorEmpty(monument[field]) or force):
					articleText = updateTableData(monument["source"], code, field, entry[field], monument, text = articleText)
	
	closeLog()

if __name__ == "__main__":
	try:
		main()
	finally:
		pywikibot.stopme()
	
