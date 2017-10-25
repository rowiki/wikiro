#!/usr/bin/python
# -*- coding: utf-8  -*-
'''Parse the monument pages (articles and images) and put the output in a json
file with the following format:
dict{ code: list[dict{name, project, lat, lon, image, author},...],...}

The script requires some configuration for each database one plans to work on.
The global variable ''options'' is a dictionary. The key is a tuple containing
the language. The value is another dictionary containing the following fields:
* infoboxes: a list of dictionaries describing each infobox the we might encounter
	in the articles; these infoboxes can contain additional monument data
	For infobox we register the following information:
	- name: regex describing the infobox names; for speed, we group similar
		templates together
	- author: list of fields that can contain the creator of the monument
	- image: the field containing the monument image
	- one entry for each database we work on, containing the field of the
		code for that database
* qualityTemplates: list of templates that indicate that the page is a quality
	product of Wikimedia; this offers that page a bonus when filling the list
* one entry for each database we work on; this is a dictionary containing:
	- namespaces: the namespaces we should work on; these are parsed one at
		a time, not in parallel
	- codeTemplate: list of templates that mark the code in the page
	- codeTemplateParams: parameters of codeTemplate; these can be used to
		extract additional information from that template
	- codeRegexp: regular expression identifying the codes from the parsed
		database
	- templateRegexp: regular expression identifying the codeTemplate

Additionally, 'commons' has a key called 'validOccupations' that defines the
occupations we search for in the {{Creator}} templates and their Romanian
translations/ We use this to separate between the picture creator (e.g. painter,
photographer) and the monument creator.

Command line options:
-db		The database to work on; valid values can be found in the config
-incremental	Save the output files after each processed page; skipped pages
                 do not count
-nopreload	Do not preload pages, but retrieve them as we need them.
                 Default: false; true when using -parse:quick
-ns		Comma-separated list of namespaces to parse. Overrides the config
-parse		There are three possible values:
		* quick: All pages that are already in our database are skipped.
			Only new pages are parsed.
		* normal: New pages are parsed, as well as pages that were edited
			since the last script run
		* full: All pages are parsed
		Default: normal
'''

import sys, os
import time, datetime
import warnings
import json
import string
import cProfile
import re

import pywikibot
from pywikibot import pagegenerators
from pywikibot import config as user

sys.path.append('wikiro/robots/python/pywikipedia')
import strainu_functions as strainu

options = {
	'wikidata':
	{
		'lmi': 'P1770',
		'ran': 'P2845',
	},
	'ro':
	{
		'lmi':#database we work on
		{
			'namespaces': [0, 6],
			#'namespaces': [6],
			'codeRegexp': re.compile("(([a-z]{1,2})-(i|ii|iii|iv)-([a-z])-([a-z])-([0-9]{5}(\.[0-9]{2,3})?))", re.I),
                        'templateRegexp': re.compile("\{\{(?:[a-z]*codLMI|Monument istoric)\|(([a-z]{1,2})-(i|ii|iii|iv)-([a-z])-([a-z])-([0-9]{5}(\.[0-9]{2,3})?))", re.I),
			'codeTemplate': ["codLMI", "Monument istoric"],
			'codeTemplateParams': 
			[
			],
			'geolimits': {
				'north': 48.3,
				'south': 43.6,
				'west':  20.27,
				'east':  29.7,
			},
		},
		'ran':#database we work on
		{
			'namespaces': [0, 6],
			#'namespaces': [6],
			'codeRegexp': re.compile("([0-9]{4,6}(\.[0-9][0-9]){1,3})", re.I),
			'templateRegexp': re.compile("\{\{codRAN\|([0-9]{4,6}(\.[0-9][0-9]){1,3})", re.I),
			'codeTemplate': ["codRAN"],
			'codeTemplateParams': 
			[
			],
			'geolimits': {
				'north': 48.3,
				'south': 43.6,
				'west':  20.27,
				'east':  29.7,
			},
		},
		'wlemd':#database we work on
		{
			'namespaces': [0],
			'codeRegexp': re.compile("((MD)-([a-z]{1,2})-([a-z]{2,3}(\.[a-z]{1,2})?)-([0-9]+))", re.I),
			'templateRegexp': re.compile("\{\{Monument natural MD\|((MD)-([a-z]{1,2})-([a-z]{2,3}(\.[a-z]{1,2})?)-([0-9]+))", re.I),
			'codeTemplate': ["Monument natural MD"],
			'codeTemplateParams': 
			[
			],
			'geolimits': {
				'north': 48.5,
				'south': 45.4,
				'west':  26.6,
				'east':  30.2,
			},
		},
		'infoboxes':
		[
		{
			'name': 'Infocaseta Monument|Cutie Monument',
			'author': ['artist', 'artist1', 'artist2', 'arhitect'],
			'image': 'imagine',
			# the databases we work on
			'ran': 'cod2',#TODO: this is a hack, we probably need to duplicate the entry
			'lmi': 'cod',
		},
		{
			'name': 'Clădire Istorică',
			'author': ['arhitect'],
			'image': 'imagine',
			# the databases we work on
			'ran': 'cod-ran',
			'lmi': 'cod-lmi',
		},
		{
			'name': 'Cutie Edificiu Religios|Infocaseta Edificiu religios|Infocaseta Teatru|Moschee',
			'author': ['arhitect'],
			'image': 'imagine',
			# the databases we work on
			'ran': '',#nada yet
			'lmi': '',
		},
		{
			'name': 'Castru|Infocaseta Castru|Infocaseta Villa rustica',
			'author': [],
			'image': 'imagine',
			# the databases we work on
			'ran': 'cod RAN',
			'lmi': 'cod LMI',
		},
		{
			'name': 'Infocasetă Davă|Infocaseta Davă|Infocaseta Cetate dacică',
			'author': [],
			'image': 'imagine',
			# the databases we work on
			'ran': 'ref:RO:RAN',
			'lmi': 'ref:RO:LMI',
		},
		{
			'name': 'Infocaseta Gară|Infocaseta Muzeu',
			'author': [],
			'image': 'imagine',
			# the databases we work on
			'ran': '',
			'lmi': '',
		},
		{
			'name': 'Infocaseta Biserică din lemn',
			'author': ['meșteri', 'zugravi'],
			'image': 'imagine',
			# the databases we work on
			'ran': 'cod RAN',
			'lmi': 'cod LMI',
		},
		{
			'name': 'Infocaseta Lăcaș de cult|Mănăstire',
			'author': ['arhitect', 'constructor', 'pictor'],
			'image': 'imagine',
			# the databases we work on
			'ran': 'codRAN',
			'lmi': 'codLMI',
		},
		{
			'name': 'Infocaseta clădire|Infobox cladire|Infobox building',
			'author': ['arhitect', 'firma_arhitectura', 'inginer', 'alti_designeri'],
			'image': 'image',
			# the databases we work on
			'ran': '',#nada yet
			'lmi': '',
		},
		],
		'qualityTemplates':
		[
			'Articol bun',
			'Articol de calitate',
			'Listă de calitate',
		],
	},
	'commons':
	{
		'lmi':
		{
			'namespaces': [14, 6],
			#'namespaces': [14],
			'codeRegexp': re.compile("(([a-z]{1,2})-(i|ii|iii|iv)-([a-z])-([a-z])-([0-9]{5}(\.[0-9]{2,3})?))", re.I),
                        'templateRegexp': re.compile("\{\{(?:Monument istoric|codLMI)\|(([a-z]{1,2})-(i|ii|iii|iv)-([a-z])-([a-z])-([0-9]{5}(\.[0-9]{2,3})?))", re.I),
			'codeTemplate': ["Monument istoric", "Monumente istorice", "codLMI"],
			'codeTemplateParams': 
			[
				'lmi92',
				'ran',
				'eroare',
			],
			'geolimits': {
				'north': 48.3,
				'south': 43.6,
				'west':  20.27,
				'east':  29.7,
			},
		},
		'ran':#database we work on
		{
			'namespaces': [14, 6],
			#'namespaces': [6],
			'codeRegexp': re.compile("([0-9]{4,6}(\.[0-9][0-9]){1,3})", re.I),
			'templateRegexp': re.compile("\{\{codRAN\|([0-9]{4,6}(\.[0-9][0-9]){1,3})", re.I),
			'codeTemplate': ["codRAN", "RAN"],
			'codeTemplateParams': 
			[
			],
			'geolimits': {
				'north': 48.3,
				'south': 43.6,
				'west':  20.27,
				'east':  29.7,
			},
		},
		'wlemd':#database we work on
		{
			'namespaces': [14, 6],
			'codeRegexp': re.compile("((MD)-([a-z]{1,2})-([a-z]{2,3}(\.[a-z]{1,2})?)-([0-9]+))", re.I),
			'templateRegexp': re.compile("\{\{Monument natural MD\|((MD)-([a-z]{1,2})-([a-z]{2,3}(\.[a-z]{1,2})?)-([0-9]+))", re.I),
			'codeTemplate': ["Monument natural MD"],
			'codeTemplateParams': 
			[
			],
			'geolimits': {
				'north': 48.5,
				'south': 45.4,
				'west':  26.6,
				'east':  30.2,
			},
		},
		'infoboxes': 
		[
			{
				#the format is actually {{Creator:Name}} without parameters
				'name': 'Creator',
				'author': ['_name'],
				'image': 'imagine',
				# the databases we work on
				'ran': '',
				'lmi': '',
			},
			{
				'name': 'codLMI|Monument istoric',
				'author': [],
				'image': 'imagine',
				# the databases we work on
				'ran': 'ran',
				'lmi': '1',#TODO
			},
		],
		'qualityTemplates':
		[
			'Valued image',
			'QualityImage',
			'Assessments',
			'Wiki Loves Monuments 2011 Europe nominee',
			'WLM finalist or winner image 2012',
			'WLM finalist or winner image',
			'Picture of the day',
			'Media of the day',
		],
		'validOccupations':
		{
			#we don't care about the creators of the 2D representation
			'architect': 'arhitect',
			'architectural painter': 'pictor arhitectural',
			'artist': 'artist',
			'artisan': 'artizan',
			'author': 'autor',
			'carpenter': 'tâmplar',
			'decorator': 'decorator',
			'engineer': 'inginer',
			'entrepreneur': 'întreprinzător',
			'ornamental painter': 'pictor ornamental',
			'sculptor': 'sculptor',
		},
	}
}


errorRegexp = re.compile("eroare\s?=\s?([^0])", re.I)
geohackRegexp = re.compile("geohack\.php\?pagename=(.*?)&(amp;)?params=(.*?)&(amp;)?language=")
qualityRegexp = None
fullDict = {}
_log = "pages.err.log"
_flog = None
_trace = False
_db = "lmi"
_coordVariance = 0.001 #decimal degrees

class Trace:
	def __init__(self, name):
		if (_trace):
			pywikibot.output("=> " + name)
		self.name = name

	def __del__(self):
		if(_trace):
			pywikibot.output("<= " + str(self.name))

def initLog():
	global _flog, _log;
	_flog = open(_log, 'w+')

def closeLog():
	global _flog
	_flog.close()

def log(string):
	pywikibot.output(string + "\n")
	_flog.write(string + "\n")

def dms2dec(deg, min, sec, sign):
	return sign * (deg + (min / 60.0) + (sec / 3600.0))
		
def isCoor( ns, ew ):
	ns = ns.upper()
	ew = ew.upper()
	return ((ns == "N" or ns == "S") and
			(ew == "E" or ew == "W"))

def parseGeohackLinks(page, conf):
	trace = Trace(sys._getframe().f_code.co_name)
	output = pywikibot.comms.http.request(page.site, "/w/api.php?action=parse&format=json&page=" +
			page.title(asUrl=True) + "&prop=externallinks&uselang=ro")
	#pywikibot.output("<= Retrieved external links")
	linksdb = json.loads(output)
	#print linksdb
	try:
		title = linksdb["parse"]["title"]
		links = linksdb["parse"]["externallinks"]
	except: return 0,0
	global geohackRegexp
	geohack_match = None
	for item in links:
		#print "External link " + item
		geohack_match = geohackRegexp.search(item)
		if geohack_match != None:
			link = geohack_match.group(3)
			#print geohack_match.group(3)
			break
	if geohack_match == None or link == None or link == "":
		#pywikibot.output("No geohack link found in article")
		return 0,0
	#valid formats:
	# D_M_S_N_D_M_S_E
	# D_M_N_D_M_E
	# D_N_D_E
	# D;D
	# D_N_D_E_to_D_N_D_E
	# (see https://wiki.toolserver.org/view/GeoHack#params for details)
	link = link.replace(",", ".") #make sure we're dealing with US-style numbers
	tokens = link.split('_')
	#print u"Tokens are " + str(tokens)
	#sanitize non-standard strings
	l = tokens[:]
	for token in l:
		if token.strip() == '' or token.find(':') > -1 or \
				token.find('{{{') > -1:
			tokens.remove(token)
	numElem = len(tokens)
	if tokens[0] == link: #no _
		tokens = link.split(';')
		if float(tokens[0]) and float(tokens[1]): # D;D
			lat = tokens[0]
			long = tokens[1]
		else:
			log("*''E'': [[:%s]] Problemă (1) cu legătura Geohack: nu pot \
				identifica coordonatele în grade zecimale: %s" % (title, link))
			return 0,0
	elif numElem >= 8 and isCoor(tokens[3], tokens[7]): #D_M_S_N_D_M_S_E_something
		if numElem == 9:
			pywikibot.output("*[[%s]] We should ignore parameter 9: %s (%s)" %
						(title, tokens[8], link))
		deg1 = float(tokens[0])
		min1 = float(tokens[1])
		sec1 = float(tokens[2])
		sign1 = strainu.geosign(tokens[3],'N','S')
		deg2 = float(tokens[4])
		min2 = float(tokens[5])
		sec2 = float(tokens[6])
		sign2 = strainu.geosign(tokens[7],'E','V')
		lat = dms2dec(deg1, min1, sec1, sign1)
		long = dms2dec(deg2, min2, sec2, sign2)
		if sec1 == 0 and sec2 == 0:
			log("*''W'': [[:%s]] ar putea avea nevoie de actualizarea \
				coordonatelor - valoarea secundelor este 0" % title)
	elif numElem >= 9 and isCoor(tokens[1], tokens[3]) and isCoor(tokens[6], tokens[8]): # D_N_D_E_to_D_N_D_E
		lat1 = float(tokens[0]) * strainu.geosign(tokens[1], 'N', 'S')
		long1 = float(tokens[2]) * strainu.geosign(tokens[3], 'E', 'V')
		lat2 = float(tokens[5]) * strainu.geosign(tokens[6], 'N', 'S')
		long2 = float(tokens[7]) * strainu.geosign(tokens[8], 'E', 'V')
		if lat1 == 0 or long1 == 0 or lat2 == 0 or long2 == 0:
			#TODO: one of them is 0; this is also true for equator and GMT
			log("*''E'': [[:%s]] Problemă (2) cu legătura Geohack: - \
				una dintre coordonatele de bounding box e 0: %s" %
				(title, link))
			return 0,0
		lat = (lat1 + lat2) / 2
		long = (long1 + long2) / 2
	elif numElem >= 6 and isCoor(tokens[2], tokens[5]): # D_M_N_D_M_E
		deg1 = float(tokens[0])
		min1 = float(tokens[1])
		sec1 = 0.0
		sign1 = strainu.geosign(tokens[2],'N','S')
		deg2 = float(tokens[3])
		min2 = float(tokens[4])
		sec2 = 0.0
		sign2 = strainu.geosign(tokens[5],'E','V')
		lat = dms2dec(deg1, min1, sec1, sign1)
		long = dms2dec(deg2, min2, sec2, sign2)
		log("*''E'': [[:%s]] are nevoie de actualizarea coordonatelor" \
			" nu sunt disponibile secundele" % title)
	elif numElem >= 4 and isCoor(tokens[1], tokens[3]): # D_N_D_E
		deg1 = float(tokens[0])
		sign1 = strainu.geosign(tokens[1],'N','S')
		deg2 = float(tokens[2])
		sign2 = strainu.geosign(tokens[3],'E','V')
		lat = sign1 * deg1
		long = sign2 * deg2
	else:
		log("*''E'': [[:%s]] Problemă (3) cu legătura Geohack: nu pot" \
			" identifica nicio coordonată: %s" % (title, link))
		return 0,0
	if lat < conf[_db]['geolimits']['south'] or lat > conf[_db]['geolimits']['north'] or \
		long < conf[_db]['geolimits']['west'] or long > conf[_db]['geolimits']['east']:
		log("*''E'': [[:%s]] Coordonate invalide pentru țară: %f,%f" \
			" (extrase din %s)" % (title, lat, long, link))
		return 0,0
	return lat,long

def commaRepl(matchobj):
	trace = Trace(sys._getframe().f_code.co_name)
	if matchobj.group(1) == "și":
		return "și "
	else:
		return ", "

def formatAuthor(author):
	trace = Trace(sys._getframe().f_code.co_name)
	ref = ""
	if author.find("<ref") > -1:
		ref = "".join(re.findall("<ref.*>", author))#TODO: this is oversimplified
		author = author.split("<ref")[0]
	author = strainu.stripNamespace(author.strip())
	author = re.sub("((,|și)??)\s*<br\s*\/?\s*>\s*", commaRepl, author, flags=re.I)
	#print author
	author = author.replace(" și", ",")
	#print author
	authors = author.split(",")
	author = ""
	for i,a in enumerate(authors):
		a = a.strip()
		parsed = strainu.extractLinkAndSurroundingText(a)
		if parsed != None:
			author += parsed[0] + "[[" + parsed[1] + "]]" + parsed[2]
		else:
			author += "[[" + a + "]]"
		if i != len(authors) - 1:
			author += ", "
		#print author
	return author + ref

#commons-specific
def processCreatorTemplate(name, conf):
	trace = Trace(sys._getframe().f_code.co_name)
	site = pywikibot.Site()
	creator = pywikibot.Page(site, name)
	if creator.exists() == False:
		return ""
	while creator.isRedirectPage():
		creator = creator.getRedirectTarget()
	tls = pywikibot.extract_templates_and_params(creator.get(), strip=True)
	for (template,params) in tls:
		#print(params)
		if template != "Creator":
			continue
		occupation = params.get("Occupation")
		if not occupation:
			continue
		for valid in conf['validOccupations']:
			if occupation.find(valid) > -1:
				#print occupation
				return formatAuthor(name) + " (" + conf['validOccupations'][valid] + ")"
	return ""

def invalidCount(count, title, db, list=None):
	if count == 0:
		log("*''E'': [[:%s]] nu conține niciun cod %s valid" % (title, db))
	else:
		log("*''I'': [[:%s]] conține %d coduri %s" \
			" distincte: %s." % (title, count, db, list))
		
#TODO:still database specific
def checkMultipleMonuments(codes, separator='.'):
	if len(codes) == 0:
		return False
	last = codes[0][strainu.findDigit(codes[0]):strainu.rfindOrLen(codes[0], separator)]
	for code in codes:
		code = code[strainu.findDigit(code):strainu.rfindOrLen(code, separator)]
		#print code
		#print last
		if code != last:
			return True
	return False

def getWikidataProperty(page, prop):
	default_returns = {
		"P625": (0,0),
		"P1770": None,
		"P18": None,
	}
	#print prop
	if page.namespace() != 0:
		return default_returns.get(prop)
	try:
		item = page.data_item()
	except:
		print(("Could not obtain wikidata item for " + page.title()))
		return default_returns.get(prop)
	#print item.claims
	if prop in item.claims:
		claim = item.claims[prop][0]
		try:
			target = claim.getTarget()
			#print target
			if isinstance(target, pywikibot.Coordinate):
					if target.precision < _coordVariance:
						return target.lat, target.lon
					else:
						return 0,0
			else:
				return target
		except Exception as e:
			print(("Wikidata Exception " + repr(e)))
	return default_returns.get(prop)

def processArticle(text, page, conf):
	trace = Trace(sys._getframe().f_code.co_name)
	title = page.title()
	pywikibot.output('Working on "%s"' % title)
	global _db

	#skip pictures under copyright (not available on commons)
	#TODO: rowp-specific
	tl = strainu.extractTemplate(text, "Material sub drepturi de autor")
	if tl != None:
		pywikibot.output("Skipping page containing copyrighted material")
		return
	if re.search(errorRegexp, text) != None:
		log("*''E'': [[:%s]] a fost marcat de un editor ca având o eroare în" \
			" codul %s" % (title, _db))
		return

	code = None
	codes = re.findall(conf[_db]['codeRegexp'], text)
	#print (codes)
	if len(codes) > 1 and checkMultipleMonuments([res[0] for res in codes]): #more than one code, juse use the one that is marked with the template
		tlCodes = re.findall(conf[_db]['templateRegexp'], text)
		#print(tlCodes)
		if len(tlCodes) == 1:
			code = tlCodes[0][0]
		codes = tlCodes
		# if no or more than one code was found, we'll try extracting the correct one from the templates in the page
	elif len(codes) == 0:
		pass
	else:#exactly 1 code or several codes of the same monument
		code = codes[0][0]
	if not code:
		code = getWikidataProperty(page, options.get('wikidata').get(_db))
	#print code
	if qualityRegexp != None and re.search(qualityRegexp, text) != None:
		quality = True
	else:
		quality = False
	
	try:		
		coor = page.coordinates(True)
		if coor:
			#print coor
			lat = coor.lat
			long = coor.lon
		else:
			lat, long = parseGeohackLinks(page, conf)
	except KeyError as e:
		print(("KeyError " + repr(e)))
		lat, long = parseGeohackLinks(page, conf)
	except Exception as e:
		print(("Exception " + repr(e)))
		lat = long = 0
	if lat == 0:
		try:
			lat, long = getWikidataProperty(page, "P625")
		except:
			print("Coord exception")

	dictElem = {'name': title,
		    'project': user.mylang,
		    'lat': lat, 'long': long,
		    'quality': quality,
		    'lastedit': pywikibot.Timestamp.fromISOformat(page._timestamp).totimestampformat(),
		    'code': code,
		}
	#print dictElem
	for key in conf['infoboxes'][0]:
		if key not in dictElem:
			dictElem[key] = None

	#print conf['infoboxes']
	for box in conf['infoboxes']:
		#pywikibot.output("Searching for template %s" % box['name'])
		tl = strainu.extractTemplate(text, box['name'])
		if tl == None:
			#pywikibot.output("Template %s not found" % box['name'])
			continue
		(_dict, _keys) = strainu.tl2Dict(tl)
		#print _dict
		author = ""
		for author_key in box['author']:
			if (not author_key in _dict) or _dict[author_key].strip() == "":
				#empty author, ignore
				continue
			author_key_type = "tip_" + author_key
			if author_key_type in _dict:
				author_type =  _dict[author_key_type].strip().lower()
			else:
				author_type = author_key
			if author_type.find("_name") != -1:
				author += processCreatorTemplate(_dict[author_key], conf) + ", "
			else:
				author += formatAuthor(_dict[author_key]) + " (" + author_type + "), "
		if author == "":
			author = None
		else:
			author = author[:-2] #remove the final comma
			#pywikibot.output(author)
		if dictElem['author'] == None:
			dictElem['author'] = author
		for key in box:
			#print key
			#try to identify the correct code
			if dictElem['code'] == None and key == _db and box[key] in _dict:
				infoCodes = re.findall(conf[_db]['codeRegexp'], _dict[box[key]])
				#print(infoCodes)
				if len(infoCodes) != 1 and checkMultipleMonuments([res[0] for res in infoCodes]): # more or less than one code is marked; just ignore
					invalidCount(len(codes), title, _db, [res[0] for res in codes])#count comes from the first search
					return
				else:
					code = dictElem['code'] = infoCodes[0][0]
					#print dictElem

			#TODO: second condition borks for anything else but strings
			if dictElem[key] == None and \
			    str(box[key]) in _dict and \
			    _dict[box[key]].strip() != "":
				dictElem[key] = _dict[box[key]]
				#pywikibot.output(key + u"=" + dictElem[key])

	#print dictElem['code']
	if dictElem['code'] == None:
		invalidCount(len(codes), title, _db, [res[0] for res in codes])#count comes from the first search
		return

	if dictElem.get('image') == None:
	# if there are images in the article, try an image from Wikidata
	# if not available, use the first image from the article
	# I'm deliberately skipping images in templates (they have been treated
	# above) and galleries, which usually contain non-selected images
		img = getWikidataProperty(page, "P18")
		if img == None:
			img = strainu.linkedImages(page)
			if len(img):
				dictElem['image'] = img[0].title()
		else:
			dictElem['image'] = img.title()
	if dictElem.get('image') and dictElem.get('image').find(':') < 0: #no namespace
		dictElem['image'] = page.site.namespace(6) + ":" + dictElem['image'].strip()

	#print dictElem

	if len(conf[_db]['codeTemplateParams']):
		i = 0
		while tl == None and i < len(conf[_db]['codeTemplate']):
			tl = strainu.extractTemplate(text, conf[_db]['codeTemplate'][i])
			i += 1
		if tl == None:
			print("Cannot find any valid templates!")
			return
		(tlcont, tlparam) = strainu.tl2Dict(tl)

		for param in conf[_db]['codeTemplateParams']:
			if param in tlcont:
				dictElem[param] = tlcont[param]
		
	if code in fullDict:
		for elem in fullDict[code]:
			if elem['name'] == title:
				fullDict[code].remove(elem)
				break
		fullDict[code].append(dictElem)
	else:
		fullDict[code] = [dictElem]

def main():
	trace = Trace(sys._getframe().f_code.co_name)
	PARSE_QUICK = 0
	PARSE_NORMAL = 1
	PARSE_EXTENDED = 2
	PARSE_FULL = 3
	lang = 'ro'
	textfile = ''
	parse_type = PARSE_EXTENDED
	preload = True
	incremental = False
	namespaces = None

	global _log
	global fullDict
	global qualityRegexp
	global _db

	for arg in pywikibot.handleArgs():
		if arg.startswith('-lang:'):
			lang = arg [len('-lang:'):]
			user.mylang = lang
		if arg.startswith('-family'):
			user.family = arg [len('-family:'):]
		if arg.startswith('-db:'):
			_db = arg [len('-db:'):]
		if arg.startswith('-nopreload'):
			preload = False
		if arg.startswith('-incremental'):
			incremental = True
		if arg.startswith('-ns'):
			namespaces = [int(x) for x in arg[len('-ns:'):].split(',')]
		if arg.startswith('-parse'):
			if  arg [len('-parse:'):] == "full":
				parse_type = PARSE_FULL
			elif arg [len('-parse:'):] == "quick":
				parse_type = PARSE_QUICK
				preload = False
			elif arg [len('-parse:'):] == "normal":
				parse_type = PARSE_NORMAL
	
	site = pywikibot.Site()
	lang = user.mylang
	if not options.get(lang):
		pywikibot.output('I have no options for language "%s"' % lang)
		return False

	langOpt = options.get(lang)
	if not namespaces:
		namespaces = langOpt.get(_db).get('namespaces')

	rowTemplate = pywikibot.Page(site, '%s:%s' % (site.namespace(10), \
								langOpt.get(_db).get('codeTemplate')[0]))
	_log = "_".join([lang, _db, _log]);
	initLog()

	qReg = "\{\{("
	for t in langOpt.get('qualityTemplates'):
		qReg = qReg + t + "|"
	qReg = qReg[:-1]
	qReg += ")(.*)\}\}"
	#print qReg
	qualityRegexp = re.compile(qReg, re.I)
	site.login()

	for namespace in namespaces:
		transGen = pagegenerators.ReferringPageGenerator(rowTemplate,
									onlyTemplateInclusion=True, content=False)
		#filteredGen = transGen = pagegenerators.CategorizedPageGenerator(catlib.Category(site, u"Category:1690s churches in Romania"))
		filteredGen = pagegenerators.NamespaceFilterPageGenerator(transGen,
									[namespace], site)
		if preload:
			pregenerator = pagegenerators.PreloadingGenerator(filteredGen, 500)
		else:
			pregenerator = filteredGen

		count = 0
		if namespace == 0:
			namespaceName = ""
		else:
			namespaceName = site.namespace(namespace)
		#no need to parse everything if we're gonna go through all the pages
		reworkedDict = {}
		filename = "_".join([_f for _f in [lang, _db, namespaceName, "pages.json"] if _f])
		tempfile = "." + filename
		if parse_type != PARSE_FULL:
			try:
				if incremental and os.path.exists(tempfile):
					f = open(tempfile, "r+")
				else:
					f = open(filename, "r+")
				print((f.name))
				jsonFile = json.load(f)
				f.close();
			except:
				jsonFile = {}
			if parse_type == PARSE_QUICK:
				fullDict = jsonFile
				pywikibot.output("Importing %d values from input file" % len(jsonFile))
			#pre-calculate as much as possible of the information we'll need
			vallist = list(jsonFile.values()) # extract list of values
			valCount = len(vallist)
			#print vallist
			for i in range(valCount):
				for j in range(len(vallist[i])):
					reworkedDict[vallist[i][j]["name"]] = vallist[i][j]
			del vallist
			del jsonFile

		for page in pregenerator:
			#page = pywikibot.Page(site, u"File:Bucuresti punte 1837.jpg")
			content = None
			try:
				pageTitle = page.title()
				if pageTitle in reworkedDict:
					#on quick parse, we just use the previous values, even 
					# if the page has changed 
					if parse_type == PARSE_QUICK:
						pywikibot.output('Skipping "%s"' % page.title())
						continue #fullDict already contains the relevant information
					else:
						content = reworkedDict[pageTitle]
				useCache = False
				#on normal parse, we first check if the page has changed
				if content and (parse_type == PARSE_NORMAL or parse_type == PARSE_EXTENDED):
					if 'lastedit' in content:
						lastedit = content['lastedit']
					else:
						lastedit = 0
					if 'code' in content:
						code = content['code']
					else:
						code = 0
					# if we preloaded the page, we already have the time
					if parse_type == PARSE_NORMAL:
						pageEdit = page.editTime().totimestampformat()
					elif parse_type == PARSE_EXTENDED:
						pageEdit = pywikibot.Timestamp.fromISOformat(page._timestamp).totimestampformat()
					if int(pageEdit) <= int(lastedit):
						useCache = True
				if useCache:
					if code in fullDict:
						fullDict[code].append(content)
					else:
						fullDict[code] = [content]
					#pywikibot.output('Skipping "%s"' % page.title())
					#continue
				elif page.exists() and not page.isRedirectPage():
					print((page.title()))
					processArticle(page.get(), page, langOpt)
					count += 1
					if incremental:
						f = open(tempfile, "w+")
						json.dump(fullDict, f, indent = 2)
						f.close();
			except Exception as e:
				pywikibot.output("Exception: " + repr(e))
				import traceback
				traceback.print_exc()
				#this sucks, but we shouldn't stop
				#keep the data we have and carry on
				if content:
					if code in fullDict:
						fullDict[code].append(content)
					else:
						fullDict[code] = [content]
				continue
		print(count)
		#print fullDict
		f = open(filename, "w+")
		json.dump(fullDict, f, indent = 2)
		f.close();
		if incremental and os.path.exists(tempfile):
			os.unlink(tempfile)
		fullDict = {}
	closeLog()

if __name__ == "__main__":
	try:
		#cProfile.run('main()', 'profiling/parseprofile.txt')
		#import pdb
		#pdb.set_trace()
		main()
	finally:
		pywikibot.stopme()
