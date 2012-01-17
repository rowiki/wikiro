#!/usr/bin/python
# -*- coding: utf-8  -*-
"""
This is a Bot written by Strainu to obtain all elements with a "wikipedia:" tag
from a given area in OSM, then add a {{coord}} template to that Wikipedia article
(if it doesn't already exist)

The following command line parameters are supported:

-start				Define the starting page in URL format

-lang               Replace the language you are working on with another language in searching for wiki links

--- Example ---
1.
# This is a script to add a the coordinates of the 
# Romanian villages from OSM to the infobox on the
# Wikipedia page

python osm2wiki_template.py -summary:"Bot: Adding a coordinate" -lang:ro -start:Albac,_Alba


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
    'en': u'Bot: Updating coordinates: %s',
    'fr': u'Robot : Actualiser des coordonees: %s',
    'ro': u'Robot: Actualizez coordonate: %s',
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
		self._dict = {}
		self._keyList = []
		
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
		
	def logd(self, string):
		self.log("* Debug (%s): " % time.strftime("%Y-%m-%d %H:%M:%S"), repr(string))
		
	#extract the first instance of a template 
	#from the page text
	def extractTemplate(self, text, template):
		pywikibot.output(text)
		match = re.search("\{\{\s*" + template, text)
		if match == None:
			return None
		tl = text[match.start():]
		text = text[match.start() + 2:]
		open = 0
		close = 1
		innerTlCount = 0
		while open < close and open <> -1:
			open = text.find("{")
			close = text.find("}") + 1
			if open > close or open == -1:
				while innerTlCount > 0:
					print str(close) + " "  + str(innerTlCount)
					close = text.find("}", close) + 1
					innerTlCount -= 1
			if open < close:
				innerTlCount += text[open + 1:close].count('{')
			print str(open) + " " + str(close) + " " + str(innerTlCount)
			text = text[close:]
		tl = tl.replace(text[1:], "") #the [1:] is because we want to grab the second '}'
		self.logd("I have extracted template: " + tl)
		return tl
	
	#convert a template to a dictionary
	#since we're using python2, the dictionary
	#class is unordered. We therefore need to 
	#keep the order of the keys in order to be
	#able to reconstruct the tmeplate
	def tl2Dict(self, template):
		template = re.sub(r'(\r|\n)', '', template)
		template = template[0:len(template)-2]#get rid of '}}'
		params = template.split('|')
		key = ""
		value = ""
		for line in params:
			line = line.split('=')
			#pywikibot.output(str(line))
			if (len(line) > 1):
				key = line[0].encode("utf8")
				key = re.sub(r'\s', '', key)
				value = "=".join(line[1:]).encode("utf8")
				self._dict[key] = str(value)
				self.insertKeyInKeyList(key)
			elif line[0].startswith('{{') and not "_name" in self._dict: #name of the template
				#pywikibot.output("Name: " + line[0][2:])
				self._dict["_name"] = line[0][2:]
				self.insertKeyInKeyList("_name")
			elif line[0] != "" and key != "":#the first line might not begin with {{
				self._dict[key] = self._dict[key] + "|" + line[0].encode("utf8")
				self.insertKeyInKeyList(key)
			#print self._dict
		return self._dict
		
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
			
	def insertKeyInKeyList(self, key):
		#don't include already existing keys
		if key in self._keyList:
		  return
		index = len(self._keyList)
		try:
			if key == "latd":
				index = 1 + self._keyList.index("codpoștal")
				 #if it doesn't exist, the default value will be used
			elif key == "latm":
				index = 1 + self._keyList.index("latd")
			elif key == "lats":
				index = 1 + self._keyList.index("latm")
			elif key == "latNS":
				index = 1 + self._keyList.index("lats")
			elif key == "longd":
				index = 1 + self._keyList.index("latNS")
			elif key == "longm":
				index = 1 + self._keyList.index("longd")
			elif key == "longs":
				index = 1 + self._keyList.index("longm")
			elif key == "longEV":
				index = 1 + self._keyList.index("longs")
			elif key == "codpoștal":
				index = 1 + self._keyList.index("tip_cod_poștal")
		except:
			pass #we're ok with the default value
		self._keyList.insert(index, key)

	def putCoordOnWiki(self, lang, firstArticle):
		if firstArticle == None:
			start = 1
		else:
			start = 0
		site = pywikibot.getSite(lang)
		reader = csv.reader(open(self._osmFilename, "r"), delimiter='\t')
		village_templates = "(CutieSate|CasetăSate|Infocaseta Așezare|Infobox așezare|Casetă așezare|Cutie așezare)".decode('utf8')
		for row in reader:
			self._keyList = []
			self._dict = {}
			#row=[title, urlname, latitude, longitude, coord_need_update, postal_code, nodeid, Truelat, Truelong, Truecode]
			title = row[0]				
			wikiTitle = row[1]
			
			if firstArticle == wikiTitle:
				start = 1
			if start == 0:
				continue
				
			osmLatd = self.getDeg(float(row[7]))
			osmLatm = self.getMin(float(row[7]))
			osmLats = self.getSec(float(row[7]))
			osmLongd = self.getDeg(float(row[8]))
			osmLongm = self.getMin(float(row[8]))
			osmLongs = self.getSec(float(row[8]))
			osmCode = row[9]
			
			self.logi("Parsing page: %s" % title)
			self.logi("Coords: %d %d %d lat N %d %d %d long E, code %s" % (osmLatd, osmLatm, osmLats, osmLongd, osmLongm, osmLongs, osmCode))
			generator = pywikibot.Page(site, wikiTitle)
			if generator.isRedirectPage():
				generator = generator.getRedirectTarget()
			try:
				text = generator.get()
			except Exception as inst:
				self.loge(u"Unknown error in getPageValue, exiting: %s" % inst);
				continue
				
			oldTl = self.extractTemplate(text, village_templates)
			if oldTl == None:
				self.loge("No template in page %s" % title)
				continue
			self.tl2Dict(oldTl)#populate self._dict
			print self._dict
				
			try:
				latd = self._dict["latd"]
				latd = latd.replace(",", ".") #make sure we're dealing with US-style numbers
				latd = int(latd)
				if latd <> osmLatd:
					self._dict["latd"] = str(osmLatd)
				else:
					self.logi("The latitude degrees field is already correct");
			except:
				self._dict["latd"] = str(osmLatd)
				self.insertKeyInKeyList("latd")
				
			try:
				latm = self._dict["latm"]
				latm = latm.replace(",", ".") #make sure we're dealing with US-style numbers
				latm = int(latm)
				if latm <> osmLatm:
					self._dict["latm"] = str(osmLatm)
				else:
					self.logi("The latitude minutes field is already correct");
			except:
				self._dict["latm"] = str(osmLatm)
				self.insertKeyInKeyList("latm")
				
			try:
				lats = self._dict["lats"]
				lats = lats.replace(",", ".") #make sure we're dealing with US-style numbers
				lats = int(lats)
				if lats<> osmLats:
					self._dict["lats"] = str(osmLats)
				else:
					self.logi("The latitude seconds field is already correct");
			except:
				self._dict["lats"] = str(osmLats)
				self.insertKeyInKeyList("lats")
				
			try:
				latNS = self._dict["latNS"]
				if latNS == 'N' or latNS == 'S':
				  self.logi("The latNS field already exists")
				else:
				  self._dict["latNS"] = "N"
			except:
				self._dict["latNS"] = "N"
				self.insertKeyInKeyList("latNS")
				
			try:
				longd = self._dict["longd"]
				longd = longd.replace(",", ".") #make sure we're dealing with US-style numbers
				longd = int(longd)
				if longd <> osmLongd:
					self._dict["longd"] = str(osmLongd)
				else:
					self.logi("The longitude degrees field is already correct");
			except:
				self._dict["longd"] = str(osmLongd)
				self.insertKeyInKeyList("longd")
				
			try:
				longm = self._dict["longm"]
				longm = longm.replace(",", ".") #make sure we're dealing with US-style numbers
				longm = int(longm)
				if longm <> osmLongm:
					self._dict["longm"] = str(osmLongm)
				else:
					self.logi("The longitude minutes field is already correct");
			except:
				self._dict["longm"] = str(osmLongm)
				self.insertKeyInKeyList("longm")
				
			try:
				longs = self._dict["longs"]
				longs = longs.replace(",", ".") #make sure we're dealing with US-style numbers
				longs = int(longs)
				if longs <> osmLongs:
					self._dict["longs"] = str(osmLongs)
				else:
					self.logi("The longitude seconds field is already correct");
			except:
				self._dict["longs"] = str(osmLongs)
				self.insertKeyInKeyList("longs")
				
			try:
				longEV = self._dict["longEV"]
				if longEV == 'E' or longEV == 'V':
				  self.logi("The longEV field already exists")
				else:
				  self._dict["longEV"] = "E"
			except:
				  self._dict["longEV"] = "E"
				  self.insertKeyInKeyList("longEV")
				
			try:
				code = self._dict["codpoștal"]
				print code
				print osmCode
				if code <> osmCode:
					self._dict["codpoștal"] = str(osmCode)
				else:
					self.logi("The postal code field is already correct");
			except:
				self._dict["codpoștal"] = str(osmCode)
				self.insertKeyInKeyList("codpoștal")
				
			try:
				#the same key might be present more than
				#once in the list, but not the dictionary
				while "coordonate" in self._keyList:
					self._keyList.remove("coordonate")
				del self._dict["coordonate"]
			except:
				self.logi("Problems deleting coordonate key");
			
			tl = u""
			for key in self._keyList:
				if key == "_name":
					tl += "{{" + self._dict[key] + "\n"
				else:
					tl += "| " + key.decode("utf8") + " = " 
					tl += self._dict[key].decode("utf8") + "\n"
			tl += "}}"
			print self._dict		
			self.logd("New template: " + tl)
			# we compare the 2 templates without whitespace 
			# to determine if we made any replacement
			if cmp(re.sub(r'\s', '', oldTl), re.sub(r'\s', '', tl)):
				text = text.replace(oldTl, tl)
				comment = pywikibot.translate(site, msg) % title.decode("utf8")
				generator.put(text, comment)
			else:
				self.logi("No change for current page")
		
def main():
	lang=pywikibot.getSite().language()
	start = None
	# Loading the arguments
	for arg in pywikibot.handleArgs():
		if arg.startswith('-lang'):
			if len(arg) == 5:
				lang = pywikibot.input(u'What language do you want to use?')
			else:
				lang = arg[6:]
		elif arg.startswith('-start'):
			if len(arg) == 6:
				start = pywikibot.input(u'What article do you want to start with?')
			else:
				start = arg[7:]
	
	bot = o2wVillageData()
	# bot.tl2Dict(bot.extractTemplate(u"""{{Infocaseta Așezare
# | nume = Bogdănești
# | alt_nume = 
# | tip_asezare = Sat
# | imagine = 
# | imagine_dimensiune = 250px
# | imagine_descriere = Bogdănești
# | stemă = 
# | hartă = 
# | pushpin_map = 
# | pushpin_label_position = right
# | tip_subdiviziune = Țară
# | nume_subdiviziune = {{ROU}}
# | tip_subdiviziune1 = [[Județele României|Județ]]
# | nume_subdiviziune1 = [[județul Vaslui|Vaslui]]
# | tip_subdiviziune3 = [[Comunele României|Comună]]
# | nume_subdiviziune3 = [[Comuna Bogdănești, Vaslui|Bogdănești]]
# | titlu_atestare = Prima atestare
# | atestare = 
# | suprafață_totală_km2 = 
# | altitudine = 
# | latd = 46
# | latm = 26
# | lats = 58
# | latNS = N
# | longd = 27
# | longm = 43
# | longs = 36
# | longEV = E
# | recensământ = 2002
# | populație = 
# | populație_note_subsol = 
# | tip_cod_poștal = [[Cod poștal]]
# | codpoștal = 
# | camp_gol_nume =
# | camp_gol_info = 
# }}

# '''Bogdănești''' este o localitate în [[județul Vaslui]], [[Moldova]], [[România]]
# """, u"Infocaseta Așezare"))
	# print bot._dict
	bot.putCoordOnWiki(lang, start)

if __name__ == "__main__":
    try:
        main()
    finally:
        pywikibot.stopme()
