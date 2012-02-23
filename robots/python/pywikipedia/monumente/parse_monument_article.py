#!/usr/bin/python
# -*- coding: utf-8  -*-
'''
Parse the monument pages (articles and images) and put the output in a json file
with the following format:
dict{code, list[dict{name, project, lat, lon, image, author}, ...]}

'''

import sys, time, warnings, json, string
import cProfile
sys.path.append("..")
import wikipedia, re, pagegenerators
import config as user
import strainu_functions as strainu

options = {
	'ro': 
	{
		'namespaces': [0],
		'codeTemplate': "codLMI",
		'infoboxes':
		[
		{
			'name': u'Infocaseta Monument',
			'author': u'artist',
			'image': u'imagine',
		},
		{
			'name': u'Clădire Istorică',
			'author': u'arhitect',
			'image': u'imagine',
		},
		{
			'name': u'Castru|Infocaseta Castru',
			'author': u'artist',#does not really exist, putting something here to prevent error checking
			'image': u'imagine',
		},
		{
			'name': u'Infocaseta Biserică din lemn',
			'author': u'meșteri',
			'image': u'imagine',
		},
		{
			'name': u'Cutie Edificiu Religios|Infocaseta Edificiu religios',
			'author': u'arhitect',
			'image': u'imagine',
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
		'codeTemplate': "Monument_istoric",
		'infoboxes': [],
		'qualityTemplates':
		[
		u'Valued image',
		u'QualityImage',
		u'Assessments',
		],
	}
}


codeRegexp = re.compile("(([a-z]{1,2})-(i|ii|iii|iv)-([a-z])-([a-z])-([0-9]{5}(\.[0-9]{2})?))", re.I)
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
	output = page.site().getUrl("/w/api.php?action=parse&format=json&page=" + page.urlname() + "&prop=externallinks&uselang=ro")
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
		#wikipedia.output("No geohack link found in article")
		return 0,0
	#valid formats (see https://wiki.toolserver.org/view/GeoHack#params for details):
	# D_M_S_N_D_M_S_E
	# D_M_N_D_M_E
	# D_N_D_E
	# D;D
	# D_N_D_E_to_D_N_D_E 
	link = link.replace(",", ".") #make sure we're dealing with US-style numbers
	tokens = link.split('_')
	#print tokens
	#sanitize non-standard strings
	l = tokens[:]
	for token in l:
		if token.strip() == '' or string.find(token, ':') > -1 or string.find(token, '{{{') > -1:
			tokens.remove(token)
	numElem = len(tokens)
	if tokens[0] == link: #no _
		tokens = tokens[0].split(';')
		if float(tokens[0]) and float(tokens[1]): # D;D
			lat = tokens[0]
			long = tokens[1]
		else:
			log(u"*''E'': [[:%s]] Problemă (1) cu legătura Geohack: nu pot identifica coordonatele în grade zecimale: %s" % (title, link))
			return 0,0
	elif numElem == 9: # D_N_D_E_to_D_N_D_E or D_M_S_N_D_M_S_E_something
		if tokens[4] <> "to":
			wikipedia.output(u"*[[%s]] We should ignore parameter 9: %s (%s)" % (title, tokens[8], link))
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
				log(u"*''W'': [[:%s]] ar putea avea nevoie de actualizarea coordonatelor - valoarea secundelor este 0" % title)
		else:
			lat1 = float(tokens[0]) * geosign(tokens[1], 'N', 'S')
			long1 = float(tokens[2]) * geosign(tokens[3], 'E', 'V')
			lat2 = float(tokens[5]) * geosign(tokens[6], 'N', 'S')
			long2 = float(tokens[7]) * geosign(tokens[8], 'E', 'V')
			if lat1 == 0 or long1 == 0 or lat2 == 0 or long2 == 0: #TODO: one of them is 0; this is also true for equator and GMT
				log(u"*''E'': [[:%s]] Problemă (2) cu legătura Geohack: - una dintre coordonatele de bounding box e 0: %s" % (title, link))
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
			log(u"*''W'': [[:%s]] ar putea avea nevoie de actualizarea coordonatelor - valoarea secundelor este 0" % title)
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
		log(u"*''E'': [[:%s]] are nevoie de actualizarea coordonatelor - nu sunt disponibile secundele" % title)
	elif numElem == 4: # D_N_D_E
		deg1 = float(tokens[0])
		sign1 = geosign(tokens[1],'N','S')
		deg2 = float(tokens[2])
		sign2 = geosign(tokens[3],'E','V')
		lat = sign1 * deg1
		long = sign2 * deg2
	else:
		log(u"*''E'': [[:%s]] Problemă (3) cu legătura Geohack: nu pot identifica nicio coordonată: %s" % (title, link))
		return 0,0
	if lat < 43 or lat > 48.25 or long < 20 or long > 29.67:
		log(u"*''E'': [[:%s]] Coordonate invalide pentru România: %f,%f (extrase din %s)" % (title, lat, long, link))
		return 0,0
	return lat,long

# def checkAllCodes(result1, result2, page):
	# if len(result2) == 0:
		# log(u"*''E'': [[:%s]] nu conține niciun cod LMI valid" % page.title())
		# return None
	# elif len(result1) == 0:
		# log(u"*''W'': [[:%s]] conține un cod LMI cu spații" % page.title())
	# elif len(result2) > 1:
		# code = result2[0][0]
		# for res in result2:#TODO:wtf????
			# if code != result2[0][0]:
				# log(u"*''E'': [[[:%s]] conține mai multe coduri LMI distincte" % page.title())
				# return None
	# return result2[0][0]#first regular expression

def checkAllCodes(result, title):
	if len(result) == 0:
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
						log(u"*''W'': [[:%s]] conține mai multe coduri LMI distincte: %s, %s. Dacă nu e vorba de o greșeală, ar putea fi oportună separarea articolelor/decuparea pozelor pentru acele coduri" % (title, code, res[0]))
						return None
	return result[0][0]#first regular expression

def processArticle(text, page, conf):
	title = page.title()
	wikipedia.output(u'Working on "%s"' % title)
	global codeRegexp
	code = checkAllCodes(re.findall(codeRegexp, text), title)
	if code == None:
		return
	if re.search(errorRegexp, text) <> None:
		log(u"*''E'': [[:%s]] a fost marcat de un editor ca având o eroare în codul LMI %s" % (title, code))
		return
		
	if qualityRegexp <> None and re.search(qualityRegexp, text) <> None:
		quality = True
	else:
		quality = False
		
	lat, long = parseGeohackLinks(page)
	
	author = None
	image = None
	for box in conf['infoboxes']:
		tl = strainu.extractTemplate(text, box['name'])
		if tl == None:
			continue
		(_dict, _keys) = strainu.tl2Dict(tl)
		if box['author'] in _dict:
			author = _dict[box['author']]
			#wikipedia.output(author)
		if box['image'] in _dict:
			#TODO:prefix with the namespace
			image = _dict[box['image']]
			#wikipedia.output(image)
		break
	if code in fullDict:
		fullDict[code].append({'name': title, 
								'project': page.site().language(), 
								'lat': lat, 'long': long,
								'author': author, 'image': image,
								'quality': quality})
	else:
		fullDict[code] = [{'name': title, 
							'project': page.site().language(), 
							'lat': lat, 'long': long,
							'author': author, 'image': image,
								'quality': quality}]
	
def processImagePage(text, page, conf):
	pass
	
def main():
	lang = u'ro'
	textfile = u''

	for arg in wikipedia.handleArgs():
		if arg.startswith('-lang:'):
			lang = arg [len('-lang:'):]
			user.mylang = lang
		if arg.startswith('-family'):
			user.family = arg [len('-family:'):]
	
	site = wikipedia.getSite()
	lang = site.language()
	if not options.get(lang):
		wikipedia.output(u'I have no options for language "%s"' % lang)
		return False
	
	langOpt = options.get(lang)
			
	rowTemplate = wikipedia.Page(site, u'%s:%s' % (site.namespace(10), langOpt.get('codeTemplate')))
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
	print qReg
	qualityRegexp = re.compile(qReg, re.I)

	for namespace in langOpt.get('namespaces'):
		transGen = pagegenerators.ReferringPageGenerator(rowTemplate, onlyTemplateInclusion=True)
		filteredGen = pagegenerators.NamespaceFilterPageGenerator(transGen, [namespace], site)
		pregenerator = pagegenerators.PreloadingGenerator(filteredGen, 250)
		
		#page = wikipedia.Page(site, "File:Biserica_Sf._Maria,_sat_Drumul_Carului,_'La_Cetate'-Gradistea._Moeciu,_jud._BRASOV.jpg")
		count = 0
		for page in pregenerator:
			if page.exists() and not page.isRedirectPage():
				# Do some checking
				processArticle(page.get(), page, langOpt)
				count += 1
		print count
		if namespace == 0:
			namespaceName = ""
		else:
			namespaceName = "_" + site.namespace(namespace)
		f = open(lang + namespaceName + "_pages.json", "w+")
		json.dump(fullDict, f)
		f.close();
		fullDict = {}
	closeLog()

if __name__ == "__main__":
	try:
		#cProfile.run('main()', './parseprofile.txt')
		main()
	finally:
		wikipedia.stopme()
