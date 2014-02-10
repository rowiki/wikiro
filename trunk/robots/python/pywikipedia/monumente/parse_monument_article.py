#!/usr/bin/python
# -*- coding: utf-8  -*-
'''
Parse the monument pages (articles and images) and put the output in a json file
with the following format:
dict{code, list[dict{name, project, lat, lon, image, author}, ...]}

'''

import sys
import time, datetime
import warnings
import json
import string
import cProfile
import re

import pywikibot
from pywikibot import pagegenerators
from pywikibot import config as user
from pywikibot import catlib

sys.path.append("..")
import strainu_functions as strainu

options = {
	'ro':
	{
		'namespaces': [0, 6],
		#'namespaces': [6],
		'templateRegexp': re.compile("\{\{[a-z]*codLMI\|(([a-z]{1,2})-(i|ii|iii|iv)-([a-z])-([a-z])-([0-9]{5}(\.[0-9]{2,3})?))", re.I),
		'codeTemplate': ["codLMI"],
		'codeTemplateParams': 
		[
		],
		'infoboxes':
		[
		{
			'name': u'Infocaseta Monument|Cutie Monument',
			'author': [u'artist', u'artist1', u'artist2', u'arhitect'],
			'image': u'imagine',
			'ran': u'',#TODO
		},
		{
			'name': u'Clădire Istorică',
			'author': [u'arhitect'],
			'image': u'imagine',
			'ran': u'cod-ran',
		},
		{
			'name': u'Cutie Edificiu Religios|Infocaseta Edificiu religios|Infocaseta Teatru|Moschee',
			'author': [u'arhitect'],
			'image': u'imagine',
			'ran': u'',#nada yet
		},
		{
			'name': u'Castru|Infocaseta Castru|Infocaseta Cetate dacică|Infocaseta Villa rustica',
			'author': [],
			'image': u'imagine',
			'ran': u'cod RAN',
		},
		{
			'name': u'Infocasetă Davă|Infocaseta Davă',
			'author': [],
			'image': u'imagine',
			'ran': u'ref:RO:RAN',
		},
		{
			'name': u'Infocaseta Gară|Mănăstire',
			'author': [],
			'image': u'imagine',
			'ran': u'',
		},
		{
			'name': u'Infocaseta Biserică din lemn',
			'author': [u'meșteri'],
			'image': u'imagine',
			'ran': u'cod RAN'
		},
		{
			'name': u'Infocaseta Lăcaș de cult',
			'author': [u'arhitect'],
			'image': u'imagine',
			'ran': u'codRAN'
		},
		{
			'name': u'Infocaseta clădire|Infobox cladire|Infobox building',
			'author': [u'arhitect'],
			'image': u'image',
			'ran': u''#nada yet
		},
		],
		'qualityTemplates':
		[
			u'Articol bun',
			u'Articol de calitate',
			u'Listă de calitate',
		],
	},
	'commons':
	{
		'namespaces': [14, 6],
		#'namespaces': [6],
		'templateRegexp': re.compile("\{\{Monument istoric\|(([a-z]{1,2})-(i|ii|iii|iv)-([a-z])-([a-z])-([0-9]{5}(\.[0-9]{2,3})?))", re.I),
		'codeTemplate': ["Monument istoric", "codLMI"],
		'codeTemplateParams': 
		[
			u'lmi92',
			u'ran',
		],
		'infoboxes': 
		[
			{
				#the format is actually {{Creator:Name}} without parameters
				'name': u'Creator',
				'author': [u'_name'],
				'image': u'imagine',
				'ran': u'',
			},
		],
		'qualityTemplates':
		[
			u'Valued image',
			u'QualityImage',
			u'Assessments',
			u'Wiki Loves Monuments 2011 Europe nominee'
		],
		'validOccupations':
		{
			#we don't care about the creators of the 2D representation
			u'architect': u'arhitect',
			u'architectural painter': u'pictor arhitectural',
			u'artist': u'artist',
			u'artisan': u'artizan',
			u'author': u'autor',
			u'engineer': u'inginer',
			u'entrepreneur': u'întreprinzător',
			u'ornamental painter': u'pictor ornamental',
		},
	}
}


codeRegexp = re.compile("(([a-z]{1,2})-(i|ii|iii|iv)-([a-z])-([a-z])-([0-9]{5}(\.[0-9]{2,3})?))", re.I)
errorRegexp = re.compile("eroare\s?=\s?([^0])", re.I)
geohackRegexp = re.compile("geohack\.php\?pagename=(.*?)&(amp;)?params=(.*?)&(amp;)?language=")
qualityRegexp = None
fullDict = {}
_log = "pages.err.log"
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

def dms2dec(deg, min, sec, sign):
	return sign * (deg + (min / 60.0) + (sec / 3600.0))

def geosign(check, plus, minus):
	if check == plus:
		return 1
	elif check == minus:
		return -1
	else:
		return 0 #this should really never happen

def parseGeohackLinks(page):
	#title = page.title()
	#html = page.site().getUrl( "/wiki/" + page.urlname(), True)
	output = pywikibot.comms.http.request(page.site, "/w/api.php?action=parse&format=json&page=" +
			page.title(asUrl=True) + "&prop=externallinks&uselang=ro")
	linksdb = json.loads(output)
	title = linksdb["parse"]["title"]
	links = linksdb["parse"]["externallinks"]
	global geohackRegexp
	geohack_match = None
	for item in links:
		geohack_match = geohackRegexp.search(item)
		if geohack_match <> None:
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
	#print tokens
	#sanitize non-standard strings
	l = tokens[:]
	for token in l:
		if token.strip() == '' or string.find(token, ':') > -1 or \
				string.find(token, '{{{') > -1:
			tokens.remove(token)
	numElem = len(tokens)
	if tokens[0] == link: #no _
		tokens = tokens[0].split(';')
		if float(tokens[0]) and float(tokens[1]): # D;D
			lat = tokens[0]
			long = tokens[1]
		else:
			log(u"*''E'': [[:%s]] Problemă (1) cu legătura Geohack: nu pot \
				identifica coordonatele în grade zecimale: %s" % (title, link))
			return 0,0
	elif numElem == 9: # D_N_D_E_to_D_N_D_E or D_M_S_N_D_M_S_E_something
		if tokens[4] <> "to":
			pywikibot.output(u"*[[%s]] We should ignore parameter 9: %s (%s)" %
							(title, tokens[8], link))
			deg1 = float(tokens[0])
			min1 = float(tokens[1])
			sec1 = float(tokens[2])
			sign1 = geosign(tokens[3],'N','S')
			deg2 = float(tokens[4])
			min2 = float(tokens[5])
			sec2 = float(tokens[6])
			sign2 = geosign(tokens[7],'E','V')
			lat = dms2dec(deg1, min1, sec1, sign1)
			long = dms2dec(deg2, min2, sec2, sign2)
			if sec1 == 0 and sec2 == 0:
				log(u"*''W'': [[:%s]] ar putea avea nevoie de actualizarea \
					coordonatelor - valoarea secundelor este 0" % title)
		else:
			lat1 = float(tokens[0]) * geosign(tokens[1], 'N', 'S')
			long1 = float(tokens[2]) * geosign(tokens[3], 'E', 'V')
			lat2 = float(tokens[5]) * geosign(tokens[6], 'N', 'S')
			long2 = float(tokens[7]) * geosign(tokens[8], 'E', 'V')
			if lat1 == 0 or long1 == 0 or lat2 == 0 or long2 == 0:
				#TODO: one of them is 0; this is also true for equator and GMT
				log(u"*''E'': [[:%s]] Problemă (2) cu legătura Geohack: - \
					una dintre coordonatele de bounding box e 0: %s" %
					(title, link))
				return 0,0
			lat = (lat1 + lat2) / 2
			long = (long1 + long2) / 2
	elif numElem == 8: # D_M_S_N_D_M_S_E
		deg1 = float(tokens[0])
		min1 = float(tokens[1])
		sec1 = float(tokens[2])
		sign1 = geosign(tokens[3],'N','S')
		deg2 = float(tokens[4])
		min2 = float(tokens[5])
		sec2 = float(tokens[6])
		sign2 = geosign(tokens[7],'E','V')
		lat = dms2dec(deg1, min1, sec1, sign1)
		long = dms2dec(deg2, min2, sec2, sign2)
		if sec1 == 0 and sec2 == 0:
			log(u"*''W'': [[:%s]] ar putea avea nevoie de actualizarea" \
				u" coordonatelor - valoarea secundelor este 0" % title)
	elif numElem == 6: # D_M_N_D_M_E
		deg1 = float(tokens[0])
		min1 = float(tokens[1])
		sec1 = 0.0
		sign1 = geosign(tokens[2],'N','S')
		deg2 = float(tokens[3])
		min2 = float(tokens[4])
		sec2 = 0.0
		sign2 = geosign(tokens[5],'E','V')
		lat = dms2dec(deg1, min1, sec1, sign1)
		long = dms2dec(deg2, min2, sec2, sign2)
		log(u"*''E'': [[:%s]] are nevoie de actualizarea coordonatelor" \
			u" nu sunt disponibile secundele" % title)
	elif numElem == 4: # D_N_D_E
		deg1 = float(tokens[0])
		sign1 = geosign(tokens[1],'N','S')
		deg2 = float(tokens[2])
		sign2 = geosign(tokens[3],'E','V')
		lat = sign1 * deg1
		long = sign2 * deg2
	else:
		log(u"*''E'': [[:%s]] Problemă (3) cu legătura Geohack: nu pot" \
			u" identifica nicio coordonată: %s" % (title, link))
		return 0,0
	if lat < 43 or lat > 48.25 or long < 20 or long > 29.67:
		log(u"*''E'': [[:%s]] Coordonate invalide pentru România: %f,%f" \
			u" (extrase din %s)" % (title, lat, long, link))
		return 0,0
	return lat,long

def checkAllCodes(result, title, logMsg = True):
	if len(result) == 0:
		if logMsg:
			log(u"*''E'': [[:%s]] nu conține niciun cod LMI valid" % title)
			return None
	elif len(result) > 1:
		code = result[0][0]
		if (code.rfind('.') > -1):
			c1 = code[-8:code.rfind('.')]
		else:
			c1 = code[-5:]
		for res in result:
			if code != res[0]:
				point = res[0].rfind('.')
				if (point > -1):
					c2 = res[0][-8:point]
				else:
					c2 = res[0][-5:]
				if c1 != c2: #they're NOT sub-monuments
					if logMsg:
						log(u"*''W'': [[:%s]] conține mai multe coduri LMI" \
						u" distincte: %s, %s. În mod normal, acest lucru" \
						u" este o greșeală" % (title, code, res[0]))
					return ""
		return code
	else:
		return result[0][0]#first regular expression

def commaRepl(matchobj):
	if matchobj.group(1) == u"și":
		return u"și "
	else:
		return u", "

def formatAuthor(author):
	ref = ""
	if author.find("<ref") > -1:
		ref = "".join(re.findall("<ref.*>", author))#TODO: this is oversimplified
		author = author.split("<ref")[0]
	author = strainu.stripNamespace(author.strip())
	author = re.sub(u"((,|și)??)\s*<br\s*\/?\s*>\s*", commaRepl, author, flags=re.I)
	#print author
	author = author.replace(u" și", u",")
	#print author
	authors = author.split(u",")
	author = u""
	for i,a in enumerate(authors):
		a = a.strip()
		parsed = strainu.extractLinkAndSurroundingText(a)
		if parsed != None:
			author += parsed[0] + u"[[" + parsed[1] + u"]]" + parsed[2]
		else:
			author += u"[[" + a + u"]]"
		if i != len(authors) - 1:
			author += u", "
		#print author
	return author + ref

#commons-specific
def processCreatorTemplate(name, conf):
	site = pywikibot.Site()
	creator = pywikibot.Page(site, name)
	if creator.exists() == False:
		return u""
	while creator.isRedirectPage():
		creator = creator.getRedirectTarget()
	tls = pywikibot.extract_templates_and_params(creator.get())
	for (template,params) in tls:
		if template != u"Creator":
			continue
		occupation = params[u"Occupation"]
		for valid in conf['validOccupations']:
			if occupation.find(valid) > -1:
				#print occupation
				return formatAuthor(name) + u" (" + conf['validOccupations'][valid] + u")"
	return u""
		

def processArticle(text, page, conf):
	title = page.title()
	pywikibot.output(u'Working on "%s"' % title)
	global codeRegexp
	code = checkAllCodes(re.findall(codeRegexp, text), title)
	if code is None: #no valid code in page
		pywikibot.output("No valid code in page " + title)
		return
	elif code == "": #more than one code, juse use the one that is marked as {{codLMI|code}}
		code = checkAllCodes(re.findall(conf['templateRegexp'], text), title, False)
		if code is None or code == "": # either no code or more than one code is marked; just ignore
			   pywikibot.output("Too many codes in page " + title)
			   return
	if re.search(errorRegexp, text) <> None:
		log(u"*''E'': [[:%s]] a fost marcat de un editor ca având o eroare în" \
			" codul LMI %s" % (title, code))
		return

	if qualityRegexp <> None and re.search(qualityRegexp, text) <> None:
		quality = True
	else:
		quality = False
	
	#lat, long = parseGeohackLinks(page)
	
	try:
		coor = page.coordinates(True)
		#print type(coor)
		lat = coor['latitude']
		long = coor['longitude']
	except KeyError as e:
		#print "KeyError " + repr(e)
		lat, long = parseGeohackLinks(page)
	except Exception as e:
		#print "Exception " + repr(e)
		lat = long = 0
	
	

	author = None
	image = None
	ran = None

	for box in conf['infoboxes']:
		tl = strainu.extractTemplate(text, box['name'])
		if tl == None:
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
				author_type = author_key.lower()
			if author_type.find("_name") != -1:
				author += processCreatorTemplate(_dict[author_key], conf) + u", "
			else:
				author += formatAuthor(_dict[author_key]) + " (" + author_type + "), "
		if author == u"":
			author = None
		else:
			author = author[:-2] #remove the final comma
			#pywikibot.output(author)
		if box['image'] in _dict and _dict[box['image']].strip() <> "":
			#TODO:prefix with the namespace
			image = _dict[box['image']]
			#pywikibot.output(image)
		if box['ran'] in _dict and _dict[box['ran']].strip() <> "":
			ran = _dict[box['ran']]
			#pywikibot.output(ran)
		if author <> None and image <> None and ran <> None:
			break # stop only if we have all the information we need

	if image == None:
	# if there are images in the article, use the first image
	# I'm deliberately skipping images in templates (they have been treated
	# above) and galleries, which usually contain non-selected images
	#	for img in page.imagelinks(total=1):
		for img in strainu.linkedImages(page):
			#print img
			image = img.title()
			break
				
	dictElem = {'name': title,
				'project': user.mylang,
				'lat': lat, 'long': long,
				'author': author, 'image': image,
				'quality': quality,
				'lastedit': page.editTime().totimestampformat(),
				'code': code,
				'ran': ran
			   }

	if len(conf['codeTemplateParams']):
		i = 0
		while tl == None and i < len(conf['codeTemplate']):
			tl = strainu.extractTemplate(text, conf['codeTemplate'][i])
			i += 1
		if tl == None:
			print "Cannot find any valid templates!"
			return
		(tlcont, tlparam) = strainu.tl2Dict(tl)
							
		for param in conf['codeTemplateParams']:
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

def processImagePage(text, page, conf):
	pass

def main():
	PARSE_QUICK = 0
	PARSE_NORMAL = 1
	PARSE_FULL = 2
	lang = u'ro'
	textfile = u''
	parse_type = PARSE_NORMAL
	preload = True

	for arg in pywikibot.handleArgs():
		if arg.startswith('-lang:'):
			lang = arg [len('-lang:'):]
			user.mylang = lang
		if arg.startswith('-family'):
			user.family = arg [len('-family:'):]
		if arg.startswith('-nopreload'):
			preload = False
		if arg.startswith('-parse'):
			if  arg [len('-parse:'):] == "full":
				parse_type = PARSE_FULL
			elif arg [len('-parse:'):] == "quick":
				parse_type = PARSE_QUICK
				preload = False

	site = pywikibot.getSite()
	lang = user.mylang
	if not options.get(lang):
		pywikibot.output(u'I have no options for language "%s"' % lang)
		return False

	langOpt = options.get(lang)

	rowTemplate = pywikibot.Page(site, u'%s:%s' % (site.namespace(10), \
								langOpt.get('codeTemplate')[0]))
	global _log
	global fullDict
	global qualityRegexp
	_log = lang + "_" + _log;
	initLog()

	qReg = u"\{\{("
	for t in langOpt.get('qualityTemplates'):
		qReg = qReg + t + "|"
	qReg = qReg[:-1]
	qReg += ")(.*)\}\}"
	#print qReg
	qualityRegexp = re.compile(qReg, re.I)

	for namespace in langOpt.get('namespaces'):
		transGen = pagegenerators.ReferringPageGenerator(rowTemplate,
									onlyTemplateInclusion=True, step=1000)
		#transGen = pagegenerators.CategorizedPageGenerator(catlib.Category(site, u"Categorie:1735_în_arhitectură"))
		filteredGen = pagegenerators.NamespaceFilterPageGenerator(transGen,
									[namespace], site)
		if preload:
			pregenerator = pagegenerators.PreloadingGenerator(filteredGen, 125)
		else:
			pregenerator = filteredGen

		count = 0
		if namespace == 0:
			namespaceName = ""
		else:
			namespaceName = "_" + site.namespace(namespace)
		filename = lang + namespaceName + "_pages.json"
		f = open(filename, "r+")
		jsonFile = json.load(f)
		f.close();
		#pre-calculate as much as possible of the information we'll need
		vallist = jsonFile.values() # extract list of values
		valCount = len(vallist)
		#print vallist
		reworkedDict = {}
		for i in xrange(valCount):
			for j in xrange(len(vallist[i])):
				reworkedDict[vallist[i][j]["name"]] = vallist[i][j]
		del vallist
		del jsonFile
		for page in pregenerator:
			#page = pywikibot.Page(site, u"File:Bucuresti punte 1837.jpg")
			content = None
			pageTitle = page.title()
			if pageTitle in reworkedDict:
				content = reworkedDict[pageTitle]
			useCache = False
			if content:
				if 'lastedit' in content:
					lastedit = content['lastedit']
				else:
					lastedit = 0
				if 'code' in content:
					code = content['code']
				else:
					code = 0
				#on quick parse, we just use the previous values, even 
				# if the page has changed 
				#on normal parse, we first check if the page has changed
				if parse_type == PARSE_QUICK:
					useCache = True
				elif parse_type == PARSE_NORMAL:
					# if we preloaded the page, we already have the time
					pageEdit = page.editTime().totimestampformat()
					if int(pageEdit) <= int(lastedit):
						useCache = True
				else:
					useCache = False
			if useCache:
				if code in fullDict:
					fullDict[code].append(content)
				else:
					fullDict[code] = [content]
				pywikibot.output(u'Skipping "%s"' % page.title())
				continue
			elif page.exists() and not page.isRedirectPage():
				processArticle(page.get(), page, langOpt)
				count += 1
		print count
		#print fullDict
		f = open(filename, "w+")
		json.dump(fullDict, f, indent = 2)
		f.close();
		fullDict = {}
	closeLog()

if __name__ == "__main__":
	try:
		#cProfile.run('main()', './parseprofile.txt')
		main()
	finally:
		pywikibot.stopme()
