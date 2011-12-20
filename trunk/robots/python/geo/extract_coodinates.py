#!/usr/bin/python
# -*- coding: utf-8  -*-

'''
Bot to extract coordinates from articles containing the {{CutieSate}} template

-always           Don't prompt to make changes, just do them.

Example: "python wiki2osm.py -always"
'''
#
# (C) Strainu 2010
# (C) Pywikipedia bot team, 2007-2010
#
# Distributed under the terms of the GPLv2 license.
#
__version__ = '$Id: extract_coordinates.py 2010-10-27 00:05:22Z strainu $'
#

import time, sys, re
import string
import codecs
import wikipedia as pywikibot
import pagegenerators
import OsmApi

class extractWikiLinks:
	def __init__(self, generator, acceptall = False):
		self.generator = generator
		self.acceptall = acceptall
		self.site = pywikibot.getSite()
		self.done = False
		
	def writeToCsv(self, file):
		pass
		
	def getTemplateParam(self, template, param, value, text):
		if template == None:
			return None
		# dirty hack: the first regexp blocks when not finding a match, so we simply strip the whitespace
		# from the template and do the matching with a simpler regexp
		#regexp = "(\{\{" + template + "(.|\r|\n)*?" + param + "([ ]*?)=([ ]*?)" + value + ")" #(.|\r|\n)*?\}\})"
		regexp = "(\{\{" + template + ".*?" + param + "=" + value + ")"
		regexp = regexp.decode("utf8")
		#pywikibot.output(regexp)
		text = re.sub(r'\s', '', text)
		template_regexp  = re.compile(regexp, re.I);
		template_match = template_regexp.search(text)
		
		if template_match <> None and template_match.lastindex <> None:
			pywikibot.output(str(template_match.groups()))
			return template_match.group(3) #value is the third regexp
		else:
			return None
			
	def dms2dec(self, deg, min, sec, sign):
		return sign * (deg + (min / 60) + (sec / 3600))
		
	def geosign(self, check, plus, minus):
		if check == plus:
			return 1
		elif check == minus:
			return -1
		else:
			return 0 #this should really never happen
		
	def parseGeohackLinks(self, page):
		html = self.site.getUrl( "/wiki/" + page.urlname(), True)
		geohack_regexp = re.compile("geohack\.php\?pagename=(.*?)&(amp;)?params=(.*?)&(amp;)?language");
		geohack_match = geohack_regexp.search(html)
		if geohack_match <> None:
			link = geohack_match.group(3)
			print geohack_match.group(3)
		else:
			pywikibot.output(u"No match for geohack link: %s" % page.title())
			return 0,0
		#valid formats (see https://wiki.toolserver.org/view/GeoHack#params for details):
		# D_M_S_N_D_M_S_E
		# D_M_N_D_M_E
		# D_N_D_E
		# D;D
		# D_N_D_E_to_D_N_D_E 
		tokens = link.split('_')
		print tokens
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
				pywikibot.output(u"Geohack link parsing error 1: %s" % link)
				return 0,0
		elif len(tokens) == 9: # D_N_D_E_to_D_N_D_E or D_M_S_N_D_M_S_E_something
			if tokens[4] <> "to":
				pywikibot.output(u"Geohack link parsing error 2: %s" % link)
				tokens.remove(tokens[8])
			else:
				lat1 = float(tokens[0]) * self.geosign(tokens[1], 'N', 'S')
				long1 = float(tokens[2]) * self.geosign(tokens[3], 'E', 'V')
				lat2 = float(tokens[5]) * self.geosign(tokens[6], 'N', 'S')
				long2 = float(tokens[7]) * self.geosign(tokens[8], 'E', 'V')
				if lat1 * long1 * lat2 * long2 == 0: #TODO: one of them is 0; this is also true for equator and GMT
					pywikibot.output(u"Geohack link parsing error 3: %s" % link)
					return 0,0
				lat = (lat1 + lat2) / 2
				long = (long1 + long2) / 2
		if len(tokens) == 8: # D_M_S_N_D_M_S_E
			deg1 = float(tokens[0])
			min1 = float(tokens[1])
			sec1 = float(tokens[2])
			sign1 = self.geosign(tokens[3],'N','S')
			deg2 = float(tokens[4])
			min2 = float(tokens[5])
			sec2 = float(tokens[6])
			sign2 = self.geosign(tokens[7],'E','V')
			lat = self.dms2dec(deg1, min1, sec1, sign1)
			long = self.dms2dec(deg2, min2, sec2, sign2)
		elif len(tokens) == 6: # D_M_N_D_M_E
			deg1 = float(tokens[0])
			min1 = float(tokens[1])
			sec1 = 0.0
			sign1 = self.geosign(tokens[2],'N','S')
			deg2 = float(tokens[3])
			min2 = float(tokens[4])
			sec2 = 0.0
			sign2 = self.geosign(tokens[5],'E','V')
			lat = self.dms2dec(deg1, min1, sec1, sign1)
			long = self.dms2dec(deg2, min2, sec2, sign2)
		elif len(tokens) == 4: # D_N_D_E
			deg1 = float(tokens[0])
			min1 = 0.0
			sec1 = 0.0
			sign1 = self.geosign(tokens[1],'N','S')
			deg2 = float(tokens[2])
			min2 = 0.0
			sec2 = 0.0
			sign2 = self.geosign(tokens[3],'E','V')
			lat = self.dms2dec(deg1, min1, sec1, sign1)
			long = self.dms2dec(deg2, min2, sec2, sign2)		
		else:
			pywikibot.output(u"Geohack link parsing error 3: %s" % link)
			return 0,0
		if lat < 43 or lat > 48 or long < 20 or long > 29:
			pywikibot.output("Invalid coordinates %f,%f" % (lat,long))
			return 0,0
		return lat,long

	def fetchArticles(self):
		f = open("wikilinks2osm.csv", 'a+')
		for page in self.generator:
			if self.done: break
			if page.exists():
				entry = self.parseWiki(page)
			if entry <> None:
				for i in entry:
					f.write(unicode(i).encode( "utf-8" ) + "\t")
				f.write("\n")
		f.close()

	def parseWiki(self, page):
		if page.isRedirectPage():
			page = page.getRedirectTarget()
		if page.namespace() <> 0:
			return None
		page_t = page.title()
		# Show the title of the page we're working on.
		# Highlight the title in purple.
		pywikibot.output(u"\n>>> %s <<<" % page_t)
		
		try:
			text = page.get()
		except:
			pywikibot.output(u"An error occurred while getting the page, skipping...")
			return None
		
		village_templates = "(CutieSate|CasetăSate)"
		code = self.getTemplateParam(village_templates, "codpoștal", "([0-9]{5,6})", text)
		latd = self.getTemplateParam(village_templates, "latd", "([0-9\.]+)", text)
		if latd <> None:
			latd = float(latd)
		else:
			latd = 0.0
		latm = self.getTemplateParam(village_templates, "latm", "([0-9\.]+)", text)
		if latm <> None:
			latm = float(latm)
		else:
			latm = 0.0
		lats = self.getTemplateParam(village_templates, "lats", "([0-9\.]+)", text)
		if lats <> None:
			lats = float(lats)
		else:
			lats = 0.0
		latNS = self.getTemplateParam(village_templates, "latNS", "(N|S)", text)
		if latNS <> None:
			lat_sign = self.geosign(latNS, 'N', 'S')
		else:
			lat_sign = 1
		longd = self.getTemplateParam(village_templates, "longd", "([0-9\.]+)", text)
		if longd <> None:
			longd = float(longd)
		else:
			longd = 0.0
		longm = self.getTemplateParam(village_templates, "longm", "([0-9\.]+)", text)
		if longm <> None:
			longm = float(longm)
		else:
			longm = 0.0
		longs = self.getTemplateParam(village_templates, "longs", "([0-9\.]+)", text)
		if longs <> None:
			longs = float(longs)
		else:
			longs = 0.0
		longEV = self.getTemplateParam(village_templates, "longEV", "(E|V)", text)
		if longEV <> None:
			long_sign = self.geosign(longEV, 'E', 'V')
		else:
			long_sign = 1
			
		latitude = self.dms2dec(latd, latm, lats, lat_sign)
		longitude = self.dms2dec(longd, longm, longs, long_sign)
		print str(latitude) + "," + str(longitude)
		#check data against Romania limits
		if latitude < 43 or latitude > 48 or longitude < 20 or longitude > 29:
			pywikibot.output("Invalid coordinates, will try to extract them from wikilinks")
			latitude, longitude = self.parseGeohackLinks(page)
		if latitude <> 0 and longitude <> 0:
			return page.title(), page.urlname(), latitude, longitude
		
def main():
	acceptall = False
	titlecase = False

	for arg in pywikibot.handleArgs():
		if arg == "-always":
			acceptall = True
		else:
			pywikibot.showHelp(u'diacritics_redirects')
			return

	title = "Format:CasetăSate"
	gen = pagegenerators.ReferringPageGenerator(pywikibot.Page(pywikibot.getSite(), title.decode("utf8")))
	preloadingGen = pagegenerators.PreloadingGenerator(gen)
	bot = extractWikiLinks(preloadingGen, acceptall)
	bot.fetchArticles()

if __name__ == "__main__":
	try:
		main()
	finally:
		pywikibot.stopme()
