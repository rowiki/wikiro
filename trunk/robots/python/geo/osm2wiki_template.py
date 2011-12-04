#!/usr/bin/python
# -*- coding: utf-8  -*-
"""
This is a Bot written by Strainu to obtain all elements with a "wikipedia:" tag
from a given area in OSM, then add a {{coord}} template to that Wikipedia article
(if it doesn't already exist)

The following command line parameters are supported:

-bbox               The are in which to search for elementsin the format longmin,latmin,longmax,latmax

-summary            Define the summary to use

-lang               Replace the language you are working on with another language in searching for wiki links

--- Example ---
1.
# This is a script to add a {{coord}} template to the top of the 
# pages with a wikipedia= or wikipedia:ro= entry in OSM in Romania
# Warning! Put it in one line, otherwise it won't work correctly.

python osm2wiki_template.py -bbox:20.2148438,43.7710938,29.7729492,48.1147666 -summary:"Bot: Adding a coordinate" -lang:ro


"""

#
# (C) Strainu, 2010
#
# Distributed under the terms of the MIT license.
#
__version__ = '$Id: osm2wiki.py 8448 2010-08-24 08:25:57Z xqt $'
#

import re, pagegenerators, urllib2, urllib
import wikipedia as pywikibot
import codecs, config
import xml.dom.minidom
import csv
import math
import time

msg = {
    'ar': u'بوت: إضافة %s',
    'cs': u'Robot přidal %s',
    'de': u'Bot: "%s" hinzugefügt',
    'en': u'Bot: Adding coordinates: %s',
    'fr': u'Robot : Ajoute des coordonees: %s',
    'he': u'בוט: מוסיף %s',
    'fa': u'ربات: افزودن %s',
    'it': u'Bot: Aggiungo %s',
    'ja': u'ロボットによる: 追加 %s',
    'ksh': u'Bot: dobeijedonn: %s',
    'nds': u'Bot: tofoiegt: %s',
    'nn': u'Robot: La til %s',
    'pdc': u'Waddefresser: %s dezu geduh',
    'pl': u'Robot dodaje: %s',
    'pt': u'Bot: Adicionando %s',
    'ro': u'Robot: Adaug coordonate: %s',
    'ru': u'Бот: добавление %s',
    'sv': u'Bot: Lägger till %s',
    'szl': u'Bot dodowo: %s',
    'vo': u'Bot: Läükon vödemi: %s',
    'zh': u'機器人: 正在新增 %s',
    }

class o2wVillageData:
	def __init__(self, acceptall = False):
		self.acceptall = acceptall
		self.site = pywikibot.getSite()
		self.done = False
		self.coord_need_update = False
		self._osmFilename = "osmcoord2wiki.csv"
		self._log = "villageData.log"
		self._comment = "Adding link to wikipedia for village %s"
		
	def log(self, header, string):
		f = open(self._log, 'a+')
		f.write(header.encode( "utf-8" ) + string.encode( "utf-8" ))
		print header.encode( "utf-8" ) + string.encode( "utf-8" )
		print "\n"
		f.write("\n")
		f.close()

	def logi(self, string):
		self.log("* Info (%s): " % time.strftime("%Y-%m-%d %H:%M:%S"), repr(string))

	def loge(self, string):
		self.log("* Error (%s): " % time.strftime("%Y-%m-%d %H:%M:%S"), repr(string))

	def logw(self, string):
		self.log("* WIKI error (%s): " % time.strftime("%Y-%m-%d %H:%M:%S"), repr(string))
		
	def tl2Dict(self, template):
		template = re.sub(r'\s', '', template)
		dic = {}
		params = template.split('|')
		for line in params:
			line = line.split('=')
        if len(line) is 2:
            dic[line[0]] = line[1]
			#TODO:this is bullshit
        else:
            continue
		print dic
    return dic
		
	def getTemplateParam(self, template, param, value, text):
		if template == None:
			return None
		# dirty hack: the first regexp blocks when not finding a match, so we simply strip the whitespace
		# from the template and do the matching with a simpler regexp
		#regexp = "(\{\{" + template + "(.|\r|\n)*?" + param + "([ ]*?)=([ ]*?)" + value + ")" #(.|\r|\n)*?\}\})"
		template = re.sub(r'\s', '', template)
		regexp = "(\{\{" + template + ".*?" + param + "=" + value + ")"
		regexp = regexp.decode("utf8")
		#pywikibot.output(regexp)
		text = re.sub(r'\s', '', text)
		#pywikibot.output(text)
		template_regexp  = re.compile(regexp, re.I | re.S);
		template_match = template_regexp.search(text)
		
		if template_match <> None and template_match.lastindex <> None:
			pywikibot.output(template_match.group(3))
			return template_match.group(3) #value is the third regexp
		else:
			return None
	
	def setValueToTemplate(self, text, template, param, value, newValue, exists):
		if exists:
			regexp = "(\{\{" + template.decode("utf8") + ".*?(" + param.decode("utf8") + "\s*=\s*" + str(value) + "))"
			pywikibot.output(regexp)
			template_regexp  = re.compile(regexp, re.I | re.S);
			template_match = template_regexp.search(text)
			if template_match <> None and template_match.lastindex <> None:
				#pywikibot.output("ala" + str(template_match.groups()))
				#key=value is the third regexp
				newKeyValue = param.decode("utf8") + "=" + str(newValue)
				text = text.replace(template_match.group(3), newKeyValue)
				self.logi("Changed " + template_match.group(3) + " with " + newKeyValue); 
		else:
			print "Field " + param + " does not exits"
			template_regexp  = re.compile(template.decode("utf8"), re.I | re.S);
			template_match = template_regexp.search(text)
			newKeyValue = template_match.group(0) + "\n|" + param.decode("utf8") + "=" + str(newValue)
			text = text.replace(template_match.group(0), newKeyValue)
			self.logi("Added " + newKeyValue + " to the template")
			#TODO: put the parameter in the right place
			pass
		return text
		
	def getDeg(self, decimal):
		if decimal < 0:
			decimal = -decimal
		return int(math.floor(decimal))
		
	def getMin(self, decimal):
		if decimal < 0:
			decimal = -decimal
		return int(math.floor((decimal - math.floor(decimal)) * 60))
		
	def getSec(self, decimal):
		if decimal < 0:
			decimal = -decimal
		return int(math.floor(((decimal - math.floor(decimal)) * 3600) % 60))
		
	def geosign(self, check, plus, minus):
		if check == plus:
			return 1
		elif check == minus:
			return -1
		else:
			return 0 #this should really never happen

	def putCoordOnWiki(self, lang, firstArticle):
		if firstArticle == None:
			start = 1
		else:
			start = 0
		site = pywikibot.getSite(lang)
		reader = csv.reader(open(self._osmFilename, "r"), delimiter='\t')
		village_templates = "(CutieSate|CasetăSate|Infocaseta Așezare|Infobox aşezare|Casetă așezare)"
		for row in reader:
			#row=[title, urlname, latitude, longitude, coord_need_update, postal_code, nodeid, Truelat, Truelong, Truecode]
			title = row[1]				
			
			if firstArticle == title:
				start = 1
			if start == 0:
				continue
				
			osmCode = row[5]
			osmLatd = self.getDeg(float(row[7]))
			osmLatm = self.getMin(float(row[7]))
			osmLats = self.getSec(float(row[7]))
			osmLongd = self.getDeg(float(row[8]))
			osmLongm = self.getMin(float(row[8]))
			osmLongs = self.getSec(float(row[8]))
			pywikibot.output("\n")
			self.logi("Parsing page: %s" % row[0])
			self.logi("Coords: %d %d %d lat N %d %d %d long E" % (osmLatd, osmLatm, osmLats, osmLongd, osmLongm, osmLongs))
			generator = pywikibot.Page(site, title)
			if generator.isRedirectPage():
				generator = generator.getRedirectTarget()
			try:
				text = generator.get()
			except Exception as inst:
				self.loge(u"Unknown error in getTagValue, exiting: %s" % inst);
				
			code = self.getTemplateParam(village_templates, "codpoștal", "([0-9]{5,6})", text)
			if code <> None:
				if code <> osmCode:
					text = self.setValueToTemplate(text, village_templates, "codpoștal", code, osmCode, True)
				else:
					self.logi("The postal code field is already correct");
			else:
				text = self.setValueToTemplate(text, village_templates, "codpoștal", code, osmCode, False)
				
			latd = self.getTemplateParam(village_templates, "latd", "([0-9\.]+)", text)
			if latd <> None:
				latd = latd.replace(",", ".") #make sure we're dealing with US-style numbers
				latd = int(latd)
				if latd <> osmLatd:
					text = self.setValueToTemplate(text, village_templates, "latd", latd, osmLatd, True)
				else:
					self.logi("The latitude degrees field is already correct");
			else:
				text = self.setValueToTemplate(text, village_templates, "latd", latd, osmLatd, False)
				
			latm = self.getTemplateParam(village_templates, "latm", "([0-9\.]+)", text)
			if latm <> None:
				latm = latm.replace(",", ".") #make sure we're dealing with US-style numbers
				latm = int(latm)
				if latm <> osmLatm:
					text = self.setValueToTemplate(text, village_templates, "latm", latm, osmLatm, True)
				else:
					self.logi("The latitude minutes field is already correct");
			else:
				text = self.setValueToTemplate(text, village_templates, "latm", latm, osmLatm, False)
				
			lats = self.getTemplateParam(village_templates, "lats", "([0-9\.]+)", text)
			if lats <> None:
				lats = lats.replace(",", ".") #make sure we're dealing with US-style numbers
				lats = int(lats)
				if lats<> osmLats:
					text = self.setValueToTemplate(text, village_templates, "lats", lats, osmLats, True)
				else:
					self.logi("The latitude seconds field is already correct");
			else:
				text = self.setValueToTemplate(text, village_templates, "lats", lats, osmLats, False)
				
			latNS = self.getTemplateParam(village_templates, "latNS", "(N|S)", text)
			if latNS == None:
				text = self.setValueToTemplate(text, village_templates, "latNS", latNS, "N", False)
			else:
				self.logi("The latNS field already exists")
				
			longd = self.getTemplateParam(village_templates, "longd", "([0-9\.]+)", text)
			if longd <> None:
				longd = longd.replace(",", ".") #make sure we're dealing with US-style numbers
				longd = int(longd)
				if longd <> osmLongd:
					text = self.setValueToTemplate(text, village_templates, "longd", longd, osmLongd, True)
				else:
					self.logi("The longitude degrees field is already correct");
			else:
				text = self.setValueToTemplate(text, village_templates, "longd", longd, osmLongd, False)
				
			longm = self.getTemplateParam(village_templates, "longm", "([0-9\.]+)", text)
			if longm <> None:
				longm = longm.replace(",", ".") #make sure we're dealing with US-style numbers
				longm = int(longm)
				if longm <> osmLongm:
					text = self.setValueToTemplate(text, village_templates, "longm", longm, osmLongm, True)
				else:
					self.logi("The longitude minutes field is already correct");
			else:
				text = self.setValueToTemplate(text, village_templates, "longm", longm, osmLongm, False)
				
			longs = self.getTemplateParam(village_templates, "longs", "([0-9\.]+)", text)
			if longs <> None:
				longs = longs.replace(",", ".") #make sure we're dealing with US-style numbers
				longs = int(longs)
				if longs <> osmLongs:
					text = self.setValueToTemplate(text, village_templates, "longs", longs, osmLongs, True)
				else:
					self.logi("The longitude seconds field is already correct");
			else:
				text = self.setValueToTemplate(text, village_templates, "longs", longs, osmLongs, False)
				
			longEV = self.getTemplateParam(village_templates, "longEV", "(E|V)", text)
			if longEV == None:
				text = self.setValueToTemplate(text, village_templates, "latEV", longEV, "E", False)
			else:
				self.logi("The longEV field already exists")
						
			#TODO: remove "coordonate" field
			#generator.put(text)
		
def main():
	bbox="20.2148438,43.7710938,29.7729492,48.1147666" #default to Romania
	summary=None
	lang=pywikibot.getSite().language()
	start = None
	# Loading the arguments
	for arg in pywikibot.handleArgs():
		if arg.startswith('-bbox'):
			if len(arg) == 5:
				bbox = pywikibot.input(
					u'Please input the area to search for tagged nodes:')
			else:
				bbox = arg[6:]
		elif arg.startswith('-summary'):
			if len(arg) == 8:
				summary = pywikibot.input(u'What summary do you want to use?')
			else:
				summary = arg[9:]
		elif arg.startswith('-lang'):
			if len(arg) == 5:
				lang = pywikibot.input(u'What language do you want to use?')
			else:
				lang = arg[6:]
		elif arg.startswith('-start'):
			if len(arg) == 6:
				start = pywikibot.input(u'What article do you want to start with?')
			else:
				start = arg[7:]
		
	#pages = getPageList(bbox, lang)
	
	bot = o2wVillageData()
	bot.putCoordOnWiki(lang, start)

if __name__ == "__main__":
    try:
        main()
    finally:
        pywikibot.stopme()
