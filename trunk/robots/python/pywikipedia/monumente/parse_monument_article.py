#!/usr/bin/python
# -*- coding: utf-8  -*-
'''
Parse the monument pages (articles and images) and put the output in a json file
with the following format:
dict{code, list[dict{name, namespace, project, lat, lon}, ...]}

'''

import sys, time, warnings, json, string
sys.path.append("..")
import wikipedia, re, pagegenerators
import config as user

options = {
	'ro': 
	{
	'namespaces': [0],
	'codeTemplate': "codLMI",
	},
	'commons':
	{
	'namespaces': [6],
	'codeTemplate': "Monument_istoric",
	}
}

fullDict = {}
_log = "pages.err.log"
_flog = None
count = 0

def initLog():
	global _flog, _log;
	_flog = open(_log, 'w+')
	
def closeLog():
	global _flog
	_flog.close()

def log(string):
	_flog.write(string.encode("utf8") + "\n")
	
def dms2dec(deg, min, sec, sign):
	return sign * (deg + (min / 60) + (sec / 3600))
	
def geosign(check, plus, minus):
	if check == plus:
		return 1
	elif check == minus:
		return -1
	else:
		return 0 #this should really never happen
	
def parseGeohackLinks(page):
		html = page.site().getUrl( "/wiki/" + page.urlname(), True)
		geohack_regexp = re.compile("geohack\.php\?pagename=(.*?)&(amp;)?params=(.*?)&(amp;)?language");
		geohack_match = geohack_regexp.search(html)
		if geohack_match <> None:
			link = geohack_match.group(3)
			print geohack_match.group(3)
		else:
			wikipedia.output("No geohack link found in article")
			return 0,0
		#valid formats (see https://wiki.toolserver.org/view/GeoHack#params for details):
		# D_M_S_N_D_M_S_E
		# D_M_N_D_M_E
		# D_N_D_E
		# D;D
		# D_N_D_E_to_D_N_D_E 
		tokens = link.split('_')
		#print tokens
		#sanitize non-standard strings
		l = tokens[:]
		for token in l:
			if token == '' or string.find(token, ':') > -1:
				tokens.remove(token)
			token = token.replace(",", ".") #make sure we're dealing with US-style numbers
		if tokens[0] == link: #no _
			tokens = tokens[0].split(';')
			if float(tokens[0]) and float(tokens[1]): # D;D
				lat = tokens[0]
				long = tokens[1]
			else:
				log(u"*''E'': [[%s]] Problemă (1) cu legătura Geohack: nu pot identifica coordonatele în grade zecimale: %s" % (page.title(), link))
				return 0,0
		elif len(tokens) == 9: # D_N_D_E_to_D_N_D_E 
			if tokens[4] <> "to":
				wikipedia.output(u"*[[%s]] We should ignore parameter 9: %s (%s)" % (page.title(), tokens[8], link))
				tokens.remove(tokens[8])
				return 0,0
			lat1 = float(tokens[0]) * geosign(tokens[1], 'N', 'S')
			long1 = float(tokens[2]) * geosign(tokens[3], 'E', 'V')
			lat2 = float(tokens[5]) * geosign(tokens[6], 'N', 'S')
			long2 = float(tokens[7]) * geosign(tokens[8], 'E', 'V')
			if lat1 * long1 * lat2 * long2 == 0: #TODO: one of them is 0; this is also true for equator and GMT
				log(u"*''E'': [[%s]] Problemă (2) cu legătura Geohack: - una dintre coordonatele de bounding box e 0: %s" % (page.title(), link))
				return 0,0
			lat = (lat1 + lat2) / 2
			long = (long1 + long2) / 2
		if len(tokens) == 8: # D_M_S_N_D_M_S_E
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
				log(u"*''W'': [[%s]] ar putea avea nevoie de actualizarea coordonatelor - valoarea secundelor este 0" % page.title())
		elif len(tokens) == 6: # D_M_N_D_M_E
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
			log(u"*''E'': [[%s]] are nevoie de actualizarea coordonatelor - nu sunt disponibile secundele" % page.title())
		elif len(tokens) == 4: # D_N_D_E
			deg1 = float(tokens[0])
			min1 = 0.0
			sec1 = 0.0
			sign1 = geosign(tokens[1],'N','S')
			deg2 = float(tokens[2])
			min2 = 0.0
			sec2 = 0.0
			sign2 = geosign(tokens[3],'E','V')
			lat = dms2dec(deg1, min1, sec1, sign1)
			long = dms2dec(deg2, min2, sec2, sign2)
		else:
			log(u"*''E'': [[%s]] Problemă (3) cu legătura Geohack: nu pot identifica nicio coordonată: %s" % (page.title(), link))
			return 0,0
		if lat < 43 or lat > 48.25 or long < 20 or long > 29.66:
			log(u"*''E'': [[%s]] Coordonate invalide pentru România: %f,%f (extrase din %s)" % (page.title(), lat, long, link))
			return 0,0
		return lat,long

def checkAllCodes(result1, result2, page):
	if len(result2) == 0:
		log(u"*''E'': [[%s]] nu conține niciun cod LMI valid" % page.title())
		return None
	elif len(result1) == 0:
		log(u"*''W'': [[%s]] conține un cod LMI cu spații" % page.title())
	elif len(result2) > 1:
		code = result2[0][0]
		for res in result2:
			if code != result2[0][0]:
				log(u"*''E'': [[[%s]] conține mai multe coduri LMI distincte" % page.title())
				return None
	return result2[0][0]#first regular expression

def processArticle(text, page, conf):
	wikipedia.output(u'Working on "%s"' % page.title(True))
	regexp = re.compile("(([a-z]{1,2})-(i|ii|iii|iv)-([a-z])-([a-z])-([0-9]{5}(\.[0-9]{2})?))", re.I)
	result1 = re.findall(regexp, text)
	text = re.sub(r'\s', '', text)
	result2 = re.findall(regexp, text)
	code = checkAllCodes(result1, result2, page)
	if code == None:
		return	
	else:
		code = re.sub(r'\s', '', code)
		
	lat, long = parseGeohackLinks(page)
	if code in fullDict:
		fullDict[code].append({'name': page.titleWithoutNamespace(), 
								'namespace': page.namespace(), 
								'project': page.site().language(), 
								'lat': lat, 'long': long})
	else:
		fullDict[code] = [{'name': page.titleWithoutNamespace(), 
							'namespace': page.namespace(), 
							'project': page.site().language(), 
							'lat': lat, 'long': long}]
	global count
	count += 1
	
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

	transGen = pagegenerators.ReferringPageGenerator(rowTemplate, onlyTemplateInclusion=True)
	filteredGen = pagegenerators.NamespaceFilterPageGenerator(transGen, langOpt.get('namespaces'), site)
	pregenerator = pagegenerators.PreloadingGenerator(filteredGen, 125)
	global _log
	_log = lang + "_" + _log;
	initLog()
	#page = wikipedia.Page(site, "File:Icoanei 56 (2).jpg")
	for page in pregenerator:
		if page.exists() and not page.isRedirectPage():
			# Do some checking
			processArticle(page.get(), page, langOpt)
	closeLog()
	global count
	print count
	f = open(lang + "_pages.json", "w+")
	json.dump(fullDict, f)
	f.close();

if __name__ == "__main__":
	try:
		main()
	finally:
		wikipedia.stopme()
