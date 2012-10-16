#!/usr/bin/python
# -*- coding: utf-8  -*-

'''
Bot to extract coordinates from articles containing the {{CutieSate}} template

-always           Don't prompt to make changes, just do them.

-startPage		  Start parsing the Wikipedia CSV from this article

Example: "python wiki2osm_links.py -always"
'''
#
# (C) Strainu 2010
# (C) Pywikipedia bot team, 2007-2010
#
# Distributed under the terms of the GPLv2 license.
#
__version__ = '$Id: wiki2osm_links.py 2010-10-27 00:05:22Z strainu $'
#

import time, sys, re, json
import string
import codecs
import wikipedia as pywikibot
import pagegenerators, urllib2, urllib
import OsmApi
import catlib
import xml.dom.minidom
import csv
import math

geohackRegexp = re.compile("geohack\.php\?pagename=(.*?)&(amp;)?params=(.*?)&(amp;)?language=")

class w2oWikiLinks:
	def __init__(self, acceptall = False):
		self.acceptall = acceptall
		self.site = pywikibot.getSite()
		self.done = False
		self.coord_need_update = False
		self._wikiFilename = "wikilinks2osm.csv"
		self._osmFilename = "osmcoord2wiki.csv"
		self._log = "wikilinks.log"
		self._osmUser = "osmbot@strainu.ro"
		self._comment = "Adding link to wikipedia for village %s"
		self._passfile = "osmpasswd"
		self.api = OsmApi.OsmApi(api="api.openstreetmap.org", passwordfile = self._passfile, debug = True)
		#self.api = OsmApi.OsmApi(api="api06.dev.openstreetmap.org", passwordfile = self._passfile, debug = True)
		
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
		template_regexp  = re.compile(regexp, re.I);
		template_match = template_regexp.search(text)
		
		if template_match <> None and template_match.lastindex <> None:
			#pywikibot.output(str(template_match.groups()))
			return template_match.group(3) #value is the third regexp
		else:
			#pywikibot.output("No match")
			return None
			
	def dms2dec(self, deg, min, sec, sign):
		return sign * (deg + (min / 60) + (sec / 3600))
			
	def getDeg(self, decimal):
		if decimal < 0:
			decimal = -decimal
		return float(math.floor(decimal))
		
	def getMin(self, decimal):
		if decimal < 0:
			decimal = -decimal
		return float(math.floor((decimal - math.floor(decimal)) * 60))
		
	def getSec(self, decimal):
		if decimal < 0:
			decimal = -decimal
		return float(math.floor(((decimal - math.floor(decimal)) * 3600) % 60))
		
	def geosign(self, check, plus, minus):
		if check == plus:
			return 1
		elif check == minus:
			return -1
		else:
			return 0 #this should really never happen
		
	def parseGeohackLinks(self, page):
		#title = page.title()
		#html = page.site().getUrl( "/wiki/" + page.urlname(), True)
		output = page.site().getUrl("/w/api.php?action=parse&format=json&page=" +
				page.urlname() + "&prop=externallinks&uselang=ro")
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
				self.loge(u"[[:%s]] Problemă (1) cu legătura Geohack: nu pot \
					identifica coordonatele în grade zecimale: %s" % (title, link))
				return 0,0
		elif numElem == 9: # D_N_D_E_to_D_N_D_E or D_M_S_N_D_M_S_E_something
			if tokens[4] <> "to":
				wikipedia.output(u"*[[%s]] We should ignore parameter 9: %s (%s)" %
								(title, tokens[8], link))
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
				if sec1 == 0 and sec2 == 0:
					self.logw(u"[[:%s]] ar putea avea nevoie de actualizarea \
						coordonatelor - valoarea secundelor este 0" % title)
			else:
				lat1 = float(tokens[0]) * self.geosign(tokens[1], 'N', 'S')
				long1 = float(tokens[2]) * self.geosign(tokens[3], 'E', 'V')
				lat2 = float(tokens[5]) * self.geosign(tokens[6], 'N', 'S')
				long2 = float(tokens[7]) * self.geosign(tokens[8], 'E', 'V')
				if lat1 == 0 or long1 == 0 or lat2 == 0 or long2 == 0:
					#TODO: one of them is 0; this is also true for equator and GMT
					self.loge(u"[[:%s]] Problemă (2) cu legătura Geohack: - \
						una dintre coordonatele de bounding box e 0: %s" %
						(title, link))
					return 0,0
				lat = (lat1 + lat2) / 2
				long = (long1 + long2) / 2
		elif numElem == 8: # D_M_S_N_D_M_S_E
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
			if sec1 == 0 and sec2 == 0:
				self.logw(u"[[:%s]] ar putea avea nevoie de actualizarea" \
					u" coordonatelor - valoarea secundelor este 0" % title)
		elif numElem == 6: # D_M_N_D_M_E
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
			self.loge(u"[[:%s]] are nevoie de actualizarea coordonatelor" \
				u" nu sunt disponibile secundele" % title)
		elif numElem == 4: # D_N_D_E
			deg1 = float(tokens[0])
			sign1 = self.geosign(tokens[1],'N','S')
			deg2 = float(tokens[2])
			sign2 = self.geosign(tokens[3],'E','V')
			lat = sign1 * deg1
			long = sign2 * deg2
		else:
			self.loge(u"[[:%s]] Problemă (3) cu legătura Geohack: nu pot" \
				u" identifica nicio coordonată: %s" % (title, link))
			return 0,0
		if lat < 43 or lat > 48.25 or long < 20 or long > 29.67:
			self.loge(u"[[:%s]] Coordonate invalide pentru România: %f,%f" \
				u" (extrase din %s)" % (title, lat, long, link))
			return 0,0
		return lat,long

	def parseWiki(self, page):
		if page.isRedirectPage():
			page = page.getRedirectTarget()
		if page.namespace() <> 0:
			return None
		page_t = page.title()
		self.coord_need_update = False
		latd = latm = lats = longd = longm = longs = 0
		
		# Show the title of the page we're working on.
		pywikibot.output(u"\n>>> %s <<<" % page_t)
		
		try:
			text = page.get()
		except:
			self.loge(u"An error occurred while getting the page, skipping...")
			return None
		
		village_templates = "(CutieSate|CutieSate2|CasetăSate|Infocaseta Așezare|Infobox așezare|Casetă așezare|Cutie așezare|CasetăOrașe)"
		code = self.getTemplateParam(village_templates, "codpoștal", "([0-9]{5,6})", text)
		latd = self.getTemplateParam(village_templates, "latd", "([0-9\.]+)", text)
		if latd <> None:
			latd = latd.replace(",", ".") #make sure we're dealing with US-style numbers
			latd = float(latd)
		else:
			latd = 0.0
		latm = self.getTemplateParam(village_templates, "latm", "([0-9\.]+)", text)
		if latm <> None:
			latm = latm.replace(",", ".") #make sure we're dealing with US-style numbers
			latm = float(latm)
		else:
			latm = 0.0
		lats = self.getTemplateParam(village_templates, "lats", "([0-9\.]+)", text)
		if lats <> None:
			lats = lats.replace(",", ".") #make sure we're dealing with US-style numbers
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
			longd = longd.replace(",", ".") #make sure we're dealing with US-style numbers
			longd = float(longd)
		else:
			longd = 0.0
		longm = self.getTemplateParam(village_templates, "longm", "([0-9\.]+)", text)
		if longm <> None:
			longm = longm.replace(",", ".") #make sure we're dealing with US-style numbers
			longm = float(longm)
		else:
			longm = 0.0
		longs = self.getTemplateParam(village_templates, "longs", "([0-9\.]+)", text)
		if longs <> None:
			longs = longs.replace(",", ".") #make sure we're dealing with US-style numbers
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
		#print str(latitude) + "," + str(longitude)
		#check data against Romania limits
		if latitude < 43 or latitude > 48.25 or longitude < 20 or longitude > 29.67:
			self.logi("Invalid coordinates, will try to extract them from wikilinks")
			latitude, longitude = self.parseGeohackLinks(page)
		elif longs == 0.0 or lats == 0.0:
			self.logw("Article %s needs coordinates update" % page.title())
			self.coord_need_update = True
		#if (latitude <> 0 and longitude <> 0) or code <> None:
		return page.title(), page.urlname(), latitude, longitude, self.coord_need_update, code
			
	def pageText(self, server, path):
		""" Function to load HTML text of a URL """
		#url = self.encodeSpaces(url)
		url = server + path
		print url.encode('utf8')
		try:
			request = urllib2.Request(url)
			request.add_header("User-Agent", pywikibot.useragent)
			response = urllib2.urlopen(request)
			text = response.read()
			response.close()
			# When you load to many users, urllib2 can give this error.
		except urllib2.HTTPError as e:
			#self.loge(u"Server error. Pausing for 10 seconds... " + str(e.code) + str(e.msg) )
			#response.close()
			time.sleep(10)
			return self.pageText(server, path)
		return text

	def fetchWikiArticles(self, generator):
		""" Main WIKI function: go through each article from the generator and write a line in the CSV file"""
		f = open(self._wikiFilename, 'a+')
		for page in generator:
			if self.done: break
			if page.exists():
				entry = self.parseWiki(page)
			#TODO: use a CSV writer
			if entry <> None:
				for i in entry:
					f.write(unicode(i).encode( "utf-8" ) + "\t")
				f.write("\n")
		f.close()
		
	def encodeSpaces(self, input):
		input = string.replace(input, " ", "+")
		input = string.replace(input, "_", "+")
		return input
		
	def getTagValue(self, root, key):
		value = None
		try:
			for tag in root.childNodes:
				if tag.nodeType != tag.ELEMENT_NODE:
					continue
				if tag.tagName != "tag" or tag.hasAttribute('k') == False:
					continue
				if tag.getAttribute('k') != key:
					continue
				value = tag.getAttribute('v') # returns "" if attribute does not exist
		except Exception as inst:
			self.loge(u"Unknown error in getTagValue, exiting: %s" % inst);
		finally:
			return value
		
	def extractValueFromOsmData(self, xmlText, key, filterKey, filterValue):
		nodeAttributes = ["id", "lat", "lon", "user", "timestamp", "uid", "version", "changeset"]
		try:
			document = xml.dom.minidom.parseString(xmlText)
			nodes = document.getElementsByTagName("node")
			for node in nodes:
				if filterKey <> None:
					value = self.getTagValue(node, filterKey)
					if value == None: # no such key
						continue
					if filterValue <> None and value <> filterValue:
						continue							
				if key in nodeAttributes:
					return node.getAttribute(key)
				else: #should return the value from a tag
					return self.getTagValue(node, key)
		except Exception as inst:
			self.loge(u"Unknown error in extractValueFromOsmData, exiting: %s" % inst);
		return 0
		
	def extractNameFromTitle(self, title):
		tempTitle = unicode.split(title.decode( "utf-8" ), ',')[0]
		return tempTitle.split('(')[0].strip()
		
	def addWikiLink(self, node_id, title):
		if node_id == 0 or node_id == None:
			self.loge(u"Wrong node id %s!" % str(node_id))
			return
		print node_id
		node = self.api.NodeGet(node_id)
		tags = node["tag"]
		if "wikipedia" in tags:
			self.logi(u"Place %s already has a wikipedia link to %s. Perhaps we've already visited it?" % (tags["name"], tags["wikipedia"]))
			return 
		tags["wikipedia"] = unicode("ro:" + title, "utf8")
		node["tag"] = tags
		self.logi("Ready to add wikipedia link (%s) to %s" % (tags["wikipedia"], tags["name"]))
		#self.logd(str(node))
		update = False
		if self.acceptall == False:
			print "Do you want to update the record? ([y]es/[n]o/[a]llways/[q]uit)"
			line = sys.stdin.readline().strip()
			if line == 'y':
				update = True
			elif line == 'a':
				update = True
				self.acceptall = True
			elif line == 'q':
				quit()
		else:
			update = True
		
		if update == True:
			title = title.decode('utf8')
			self.api.ChangesetCreate({u"comment": self._comment % title})
			self.api.NodeUpdate(node)
			self.api.ChangesetClose()
			
	def searchByCoord(self, lat, long, name, nameSup = "", county = ""):
		"""search a village using the coordinates from Wikipedia or Nominatim"""
		server = "http://osmxapi.hypercube.telascience.org/api/0.6/"
		server = "http://open.mapquestapi.com/xapi/api/0.6/"
		#server = "http://jxapi.openstreetmap.org/xapi/api/0.6/"
		urlHead = "node[name="
		self.logi("Searching: %s@(%s,%s)" % (name, lat, long))
		lat = float(lat)
		long = float(long)
		
		latd = self.getDeg(lat)
		latm = self.getMin(lat)
		#print latd, latm
		if lat <> 0:
			latsign = lat / math.fabs(lat)
		else:
			latsign = 1.0
		#searching [wlong-1,wlat-1,wlon+1,wlat+1]
		lowlat = self.dms2dec(latd, latm - 1, 0, latsign)
		highlat = self.dms2dec(latd, latm + 1, 0, latsign)
		#print lowlat, highlat
		
		longd = self.getDeg(long)
		longm = self.getMin(long)
		if long <> 0:
			longsign = long / math.fabs(long)
		else:
			longsign = 1.0
		#searching [wlong-1,wlat-1,wlon+1,wlat+1]
		lowlong = self.dms2dec(longd, longm - 1, 0, longsign)
		highlong = self.dms2dec(longd, longm + 1, 0, longsign)
		
		bbox = str(lowlong) + "," + str(lowlat) + "," + str(highlong) + "," + str(highlat)
		self.logi(u"Fetching village nodes in area: %s" % bbox)
		url = urlHead + urllib.quote(name.encode('utf8')) + "][bbox=" + bbox + "]"
		self.logi(u"searchByCoord URL: %s" % server + url)
		
		xmlText = self.pageText(server, url)
		#print xmlText
		nodeid   = self.extractValueFromOsmData(xmlText, "id", "place", None)
		truelat  = self.extractValueFromOsmData(xmlText, "lat", "place", None)
		truelong = self.extractValueFromOsmData(xmlText, "lon", "place", None)
		truecode = self.extractValueFromOsmData(xmlText, "postal_code", "place", None)
		xmlNameSup = self.extractValueFromOsmData(xmlText, "siruta:name_sup", "place", None)
		xmlCounty = self.extractValueFromOsmData(xmlText, "is_in:county", "place", None)
		
		if nameSup <> "" and xmlNameSup <> nameSup:
			self.loge("The superior entity does not match: %s vs %s" % (nameSup,xmlNameSup))
			return 0,0,0,0
		if county <> "" and xmlCounty <> county:
			self.loge("The county name does not match: %s vs %s" % (county, xmlCounty))
			return 0,0,0,0
		
		return nodeid, truelat, truelong, truecode
		
	def searchByName(self, title, urlTitle, nameSup = "", county = ""):
		placeTypes = ["village", "hamlet"]
		"""Search a village in OSM by name, using the Nominatim service"""
		server = "http://open.mapquestapi.com/nominatim/v1/search?"
		#server = "http://nominatim.openstreetmap.org/nominatim/v1/search?"
		urlHead = "q="
		urlFormat = "&format=xml&email="
		urlLimit = "&limit=10"
		url = urlHead + urlTitle + urlFormat + self._osmUser + urlLimit
		self.logi(u"searchByName URL: %s" % (server + url.encode('utf8')))
		
		xmlText = self.pageText(server, url)
		#print xmlText
		try:
			document = xml.dom.minidom.parseString(xmlText)
			places = document.getElementsByTagName("place")
			for place in places:
				if place.getAttribute('type') in placeTypes:
					node_id = place.getAttribute('osm_id');
					lat = float(place.getAttribute('lat'));
					long = float(place.getAttribute('lon'));
					if lat < 43 or lat > 48.25 or long < 20 or long > 29.67:
						continue #place not in RO
					nodeid, lat, lon, code = self.searchByCoord(lat, long, self.extractNameFromTitle(title), nameSup, county)
					if nodeid <> 0:
						return nodeid, lat, lon, code
		except Exception as inst:
			self.loge("searchByName unknown error: " + str(inst))
		return 0,0,0,0 #nodeid, lat, lon, code 
		
	def writeToOsm(self, startPage = ""):
		"""Main OSM function, designed to parse each CSV line generated by fetchWikiArticles"""
		reader = csv.reader(open(self._wikiFilename, "r"), delimiter='\t')
		writer = csv.writer(open(self._osmFilename, "ab"), delimiter='\t', quoting=csv.QUOTE_NONE)
		if startPage <> "":
			start = 0
		else:
			start = 1
		try:
			for row in reader:
				#row=[title, urlname, latitude, longitude, coord_need_update, postal_code]
				title	= row[0]
				if start == 0:
					if string.find(row[1].decode('utf-8'), startPage) > -1:
						self.logi("Starting from article %s" % title)
						start = 1
					else:
						self.logi("Skipping article %s" % title)
					continue
				name 	= self.extractNameFromTitle(row[0])
				urlEncodedTitle = self.encodeSpaces(row[1])
				lat  	= float(row[2])
				long 	= float(row[3])
				update	= eval(row[4]) #TODO: really risky, potential security breach
				code 	= row[5]
				if lat <> 0 and long <> 0:
					nodeid, truelat, truelong, truecode = self.searchByCoord(lat, long, name)
					if nodeid == 0: #not found, search with Nominatim
						nodeid, truelat, truelong, truecode = self.searchByName(title, urlEncodedTitle)
						update = True
					if nodeid == 0: #still not found, give up
						self.logw("Village %s not found around [%s, %s], the coordinates are probably wrong" % (title, lat, long))
						continue
				else: #no coordinate is available 
					nodeid, truelat, truelong, truecode = self.searchByName(title, urlEncodedTitle)
					update = True
					tempUrlTitle = None
					nameSup = ""
					county = ""
					if nodeid == 0: #not found, remove ()
						regex = re.search('(.*)%28.*%29(.*)', urlEncodedTitle)
						if regex <> None:
							regex2 = re.search('\((.*)\)', title)
							nameSup = regex2.group(1).decode('utf-8')
							tempUrlTitle = regex.group(1) + regex.group(2)
							nodeid, truelat, truelong, truecode = self.searchByName(title, tempUrlTitle, nameSup, county)
							print nodeid
					if nodeid == 0: #not found, remove comma
						if tempUrlTitle == None:
							tempUrlTitle = urlEncodedTitle
						regex = re.search('(.*)%2C(.*)', tempUrlTitle)
						if regex <> None:
							county = title.split(',')[1].strip().decode('utf-8')
							tempUrlTitle = regex.group(1)
							nodeid, truelat, truelong, truecode = self.searchByName(title, tempUrlTitle, nameSup, county)
							print nodeid
					if nodeid == 0: #not found, give up
						self.logw("Village %s not found with Nominatim, must check manually" % title)
						continue
						
				self.addWikiLink(nodeid, title)
				if code <> truecode:
					self.logw("Postal code mismatch: Wiki has %s, OSM has %s" % (code, truecode))
					update = True
				if update == True:
					row.extend([nodeid, truelat, truelong, truecode])
					writer.writerow(row)
					print row
		except csv.Error, e:
			self.loge('file %s, line %d: %s' % (self._wikiFilename, reader.line_num, e))
			return
		
		
def main():
	acceptall = False
	titlecase = False
	startPage = ""

	for arg in pywikibot.handleArgs():
		if arg == "-always":
			acceptall = True
		elif arg.startswith("-startPage"):
			if len(arg) == 10:
				startPage = pywikibot.input(u'Please input the article to start with:')
			else:
				startPage = arg[11:].encode('utf-8')
		else:
			pywikibot.showHelp(u'diacritics_redirects')
			return

	categs = [#'Categorie:Localități în județul Alba','Categorie:Localități în județul Arad','Categorie:Localități în județul Argeș',
#				'Categorie:Localități în județul Bacău','Categorie:Localități în județul Bihor','Categorie:Localități în județul Bistrița-Năsăud',
#				'Categorie:Localități în județul Botoșani','Categorie:Localități în județul Brașov','Categorie:Localități în județul Brăila',
#				'Categorie:Localități în județul Buzău','Categorie:Localități în județul Caraș-Severin','Categorie:Localități în județul Cluj',
#				'Categorie:Localități în județul Constanța','Categorie:Localități în județul Covasna','Categorie:Localități în județul Călărași',
#				'Categorie:Localități în județul Dolj','Categorie:Localități în județul Dâmbovița','Categorie:Localități în județul Galați',
#				'Categorie:Localități în județul Giurgiu','Categorie:Localități în județul Gorj','Categorie:Localități în județul Harghita',
#				'Categorie:Localități în județul Hunedoara','Categorie:Localități în județul Ialomița','Categorie:Localități în județul Iași',
#				'Categorie:Localități în județul Ilfov','Categorie:Localități în județul Maramureș','Categorie:Localități în județul Mehedinți',
#				'Categorie:Localități în județul Mureș','Categorie:Localități în județul Neamț','Categorie:Localități în județul Olt',
#				'Categorie:Localități în județul Prahova','Categorie:Localități în județul Satu Mare','Categorie:Localități în județul Sibiu',
#				'Categorie:Localități în județul Suceava','Categorie:Localități în județul Sălaj','Categorie:Localități în județul Teleorman',
#				'Categorie:Localități în județul Timiș','Categorie:Localități în județul Tulcea','Categorie:Localități în județul Vaslui',
#				'Categorie:Localități în județul Vrancea','Categorie:Localități în județul Vâlcea',
#				'Categorie:Comune în județul Alba','Categorie:Comune în județul Arad','Categorie:Comune în județul Argeș',
#				'Categorie:Comune în județul Bacău','Categorie:Comune în județul Bihor','Categorie:Comune în județul Bistrița-Năsăud',
#				'Categorie:Comune în județul Botoșani','Categorie:Comune în județul Brașov','Categorie:Comune în județul Brăila',
#				'Categorie:Comune în județul Buzău','Categorie:Comune în județul Caraș-Severin','Categorie:Comune în județul Cluj',
#				'Categorie:Comune în județul Constanța','Categorie:Comune în județul Covasna','Categorie:Comune în județul Călărași',
#				'Categorie:Comune în județul Dolj','Categorie:Comune în județul Dâmbovița','Categorie:Comune în județul Galați',
#				'Categorie:Comune în județul Giurgiu','Categorie:Comune în județul Gorj','Categorie:Comune în județul Harghita',
#				'Categorie:Comune în județul Hunedoara','Categorie:Comune în județul Ialomița','Categorie:Comune în județul Iași',
#				'Categorie:Comune în județul Ilfov','Categorie:Comune în județul Maramureș','Categorie:Comune în județul Mehedinți',
#				'Categorie:Comune în județul Mureș','Categorie:Comune în județul Neamț','Categorie:Comune în județul Olt',
#				'Categorie:Comune în județul Prahova','Categorie:Comune în județul Satu Mare','Categorie:Comune în județul Sibiu',
#				'Categorie:Comune în județul Suceava','Categorie:Comune în județul Sălaj','Categorie:Comune în județul Teleorman',
#				'Categorie:Comune în județul Timiș','Categorie:Comune în județul Tulcea','Categorie:Comune în județul Vaslui',
#				'Categorie:Comune în județul Vrancea','Categorie:Comune în județul Vâlcea',
				'Categorie:Municipii în România', 'Categorie:Orașe în România']
	
	bot = w2oWikiLinks(acceptall)
	
	#for categ in categs:
	#	pywikibot.output(categ.decode("utf8"))
	#	gen = pagegenerators.CategorizedPageGenerator(catlib.Category(pywikibot.getSite(), categ.decode("utf8")))
	#	preloadingGen = pagegenerators.PreloadingGenerator(gen, 125)
	#	bot.fetchWikiArticles(preloadingGen)
	
	bot.writeToOsm(startPage)

if __name__ == "__main__":
	try:
		main()
	finally:
		pywikibot.stopme()
