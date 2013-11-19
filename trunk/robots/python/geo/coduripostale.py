#!/usr/bin/python
# -*- coding: utf-8  -*-

import httplib, urllib
import os
import json
import time
import csv
from string import maketrans 

import ro_poly

all_tags = {
	"node": {
		"city": [
				"is_in:city",
				"addr:city",
			],
		"street": [
			"addr:street",
			],
		"number": [
			"addr:housenumber",
		],
	},
	"way": {
		"city": [
				"is_in:city",
				"addr:city",
			],
		"street": [
			"addr:street",
			"name",
			],
		"number": [
			"addr:housenumber",
		],
	},
	"postcode": {
		"postal_code",
		"addr:postcode",
	}
}

#structure is:
#  - external dict is an union
#  - inner dict are parameters for a request
#  - if inner dict is not None, the value is the first elem of the pair and isPositive is the second
filters = {
	u"a": {
	u"addr:postcode": None,
	u"siruta:code": (u".", False),
	},
	u"b": {
	u"postal_code": None,
	u"siruta:code": (u".", False),
	},
	}

log = None

class OverpassRequest:
	def __init__(self, api = "www.overpass-api.de", base="api", poly=None, filters=None, output=None):
		self.base = os.path.join("/", base, "interpreter")
		self._api = api
		self.http = httplib.HTTPConnection(self._api)
		self._poly = poly
		self._filters = filters
		self._output = output
	def buildRequest(self, req_type, poly, filters, output):
		req = unicode("[out:json];", "utf8")
		vars = u"("
		for filts in filters:
			vars += u"." + filts + u";"
			req += u"\narea[name=\"România\"]; " + req_type + u"(area)"
			for filt in filters[filts]:
				req += u"[\"" + filt + u"\""
				f = filters[filts][filt]
				if f != None:
					if f[1] == False:#this is a negation
						req += u"!"
					req += u"~" + u"\"" + f[0] + u"\""
				req += u"]"
			req += u"->." + filts + u";"
		req += u"\n" + vars + u");"
		req += u"\nout body;"
		#print req
		return req.encode("utf8")

	def makeRequest(self, req=None):
		if req == None:
			return None
		
		http = httplib.HTTPConnection(self._api)
		params = urllib.urlencode({'@data': req})
		headers = {"Content-type": "application/x-www-form-urlencoded",
				"Accept": "text/plain"}
		http.request("POST", self.base, params, headers)
		response = http.getresponse()
		if response.status == 200:
			ret = response.read()
			http.close()
			return ret
		else:
			http.close()
			return None
		
	def fetchNode(self, poly=None, filters=None, output=None):
		if poly == None:
			poly = self._poly
		if filters == None:
			filters = self._filters
		if output == None:
			output = self._output
		req = self.buildRequest("node", poly, filters, output)
		return self.makeRequest(req)


	def fetchWay(self, poly=None, filters=None, output=None):
		if poly == None:
			poly = self._poly
		if filters == None:
			filters = self._filters
		if output == None:
			output = self._output
		req = self.buildRequest("way", poly, filters, output)
		return self.makeRequest(req)
		
class PostcodeErrLog:
	def __init__(self, type=u"wiki", file=u"postcodelog"):
		self._type = type
		self._file = file
		self._finished = False
		self._err = {
			"E1": u"Nu pot extrage orașul din OSM",
			"E2": u"Nu pot extrage strada din OSM",
			"W3": u"Nu pot extrage numărul din OSM",
			"W4": u"Codul poștal conține mai puțin de 6 cifre",
			"E4": u"Codul poștal conține altceva decât cifre",
			"E5": u"Nu găsesc codul poștal în datele de la poștă",
			"W6": u"Sunt diferențe în spellingul orașului",
			"E6": u"Orașul nu corespunde între OSM și poștă",
			"W7": u"Sunt diferențe în spellingul străzii",
			"E7": u"Strada nu corespunde între OSM și poștă",
			"E8": u"Numărul nu corespunde între OSM și poștă",
		}
		self._log = u"""
== Erori în codurile poștale de pe OSM ==
{|class="wikitable sortable"
! Tip
! Nod/cale
! Descriere
! Valoare câmp OSM
! Valoare câmp poștă
|-
"""
		
	def log(self, err_type, osm_type, osm_item, osm_code, gov_code):
		if err_type[0] == "E":
			bg = "red"
		else:
			bg = "yellow"
		self._log += u"|style=\"background:" + bg + "\"| " + err_type + \
			u"\n| {{" + osm_type + u"|" + unicode(osm_item) + u"}}" + \
			u"\n| " + self._err[err_type]
		if osm_code:
			self._log += u"\n| " + unicode(osm_code)
		else:
			self._log += u"\n| "
		if gov_code:
			if type(gov_code) == list:
				self._log += u"\n| " 
				for code in gov_code:
					self._log += unicode(code) + u"; "
			else:
				self._log += u"\n| " + unicode(gov_code)
		else:
			self._log += u"\n| "
		self._log += u"\n|-\n"
			
	def __enter__(self):
		return self
		
	def finish(self):
		if not self._finished:
			self._log += "|}\n"
			self._finished = True
		
	def __exit__(self, type, value, traceback):
		self.finish()
		_file = open(".".join([self._file, self._type]), "wb+")
		#TODO convert to HTML if needed
		_file.write(self._log.encode("utf8"))
		_file.close()
		
	def upload(self):
		self.finish()
		print "Uploading page to server"
		import pywikibot
		from pywikibot import config as user
		user.mylang = "en"
		user.family = "osm"
		site = pywikibot.getSite()
		page = pywikibot.Page(site, "Romanian Postal Codes")
		page.put(self._log, "Updating error list")
		

def loadBucharestCsv():
	ret = {}
	with open('codp_B.csv', 'rb') as csvfile:
		reader = csv.reader(csvfile)
		for row in reader:
			if not row[3] in ret:
				ret[row[3]] = [] 
			ret[row[3]] = ret[row[3]] + [{
					'city': u'București',
					'type': unicode(row[0], "utf8"),
					'street': unicode(row[1], "utf8"),
					'number': splitNumberList(row[2][4:]),#skipping "nr. "
					'rawnumber': row[2][4:],
					'code': unicode(row[3], "utf8"),
			}]
	return ret

def load50kCsv():
	ret = {}
	with open('codp_50k.csv', 'rb') as csvfile:
		reader = csv.reader(csvfile)
		for row in reader:
			if row[4][:2] == u"bl":
				continue
			if not row[5] in ret:
				ret[row[5]] = []
			ret[row[5]] = ret[row[5]] + [{
				'city': unicode(row[1], "utf8"),
				'type': unicode(row[2], "utf8"),
				'street': unicode(row[3], "utf8"),
				'number': splitNumberList(row[4][4:]),#skipping "nr. "
				'rawnumber': row[4][4:],
				'code': unicode(row[5], "utf8"),
			}]
	return ret

def load1kCsv():
	ret = {}
	with open('codp_1k.csv', 'rb') as csvfile:
		reader = csv.reader(csvfile)
		for row in reader:
			ret[row[2]] = {
				'city': unicode(row[1], "utf8"),
				'code': unicode(row[2], "utf8"),
			}
	return ret
	
def atoi(s):
	s,_,_ = s.partition(' ') # eg. this helps by trimming off at the first space
	while s:
		try:
			return int(s)
		except:
			s=s[:-1]
	return 0
	
def splitNumberList(nrlist):
	#print u"*" + unicode(nrlist, "utf8")
	if not nrlist:
		return range(1,201)
	elif nrlist.find(";") > -1 or nrlist.find(",") > -1:
		if nrlist.find(";") > -1:
			sep = ";"
		else:
			sep = ","
		n1 = nrlist[:nrlist.find(sep)].strip()
		n2 = nrlist[nrlist.find(sep)+1:].strip()
		l1 = splitNumberList(n1)
		l2 = splitNumberList(n2)
		#print l1
		#print l2
		number = l1 + l2 
	elif nrlist.find("-") > -1:
		n1 = atoi(nrlist[:nrlist.find("-")])
		n2 = nrlist[nrlist.find("-")+1:]
		if n2 == 'T':
			n2 = n1 + 200 #let's hope we don't have more that 100 numbers for a code
		else:
			n2 = atoi(n2)
		#print n1
		#print n2
		number = []
		for n in xrange(n1, n2+1, 2):#numbers on a side should be either odd or even
			number.append(n)
	else:
		n = atoi(nrlist)
		if n:
			number = [n]
		else:
			number = []
	#print u"^" + str(number)
	return number
	
def convertName(oldName, str_type, keepPrefix=True):
	#print oldName
	if str_type:
		str_type += u" "
	prefix = u""
	if oldName.find(",") > -1:
		if keepPrefix:
			prefix = oldName[oldName.find(",") + 2:] + u" "
		newName = oldName[:oldName.find(",")]
	else:
		newName = oldName
	if newName.find(" ") > -1:
		surname = newName[:newName.find(" ")]
		firstname = newName[newName.find(" ")+1:]
		newName = str_type + prefix + firstname + u" " + surname
	else:
		newName = str_type + prefix + newName
	#print newName
	return newName
	

def maketransU(s1, s2, todel=""):
	trans_tab = dict( zip( map(ord, s1), map(ord, s2) ) )
	trans_tab.update( (ord(c),None) for c in todel )
	return trans_tab
	
def removeDiacritics(name, lowerCase=True):
	tran = maketransU(u"ȘșȚțŞşŢţÎîÂâĂăáöőé", u"SsTtSsTtIiAaAaaooe")
	if lowerCase:
		name = name.lower()
	return name.translate(tran)
	
def matchStreets(osm_street, postal_code_data):
	#print osm_street
	osm_street = osm_street.lower()
	if osm_street == postal_code_data['type'].lower() + u" " + postal_code_data['street'].lower():
		return True
	if osm_street == convertName(postal_code_data['street'], postal_code_data['type']).lower():
		return True
	if osm_street == convertName(postal_code_data['street'], postal_code_data['type'], False).lower():
		return True
	if osm_street == postal_code_data['street'].lower():
		return True
	if osm_street == convertName(postal_code_data['street'], u"").lower():
		return True
	if osm_street == convertName(postal_code_data['street'], u"", False).lower():
		return True
	#print osm_street
	#print postal_code_data['type'].lower() + u" " + postal_code_data['street'].lower()
	#print convertName(postal_code_data['street'], postal_code_data['type']).lower()
	#print convertName(postal_code_data['street'], postal_code_data['type'], False).lower()
	#print postal_code_data['street'].lower()
	#print convertName(postal_code_data['street'], u"").lower()
	#print convertName(postal_code_data['street'], u"", False).lower()
	#print "False"
	return False

def processTags(node_type, node, tags, postal_data, other_data):
	city = street = number = postcode = None
	streets = {}
	highway = False
	matching = True
	for tag in tags:
		if tag in all_tags[node_type]["city"]:
			city = tags[tag]
		if tag in all_tags[node_type]["street"]:
			streets[tag] = tags[tag]
		if tag in all_tags[node_type]["number"]:
			number = tags[tag]
			number = splitNumberList(number)
			#print str(number)
		if tag in all_tags["postcode"]:
			postcode = tags[tag]
		if tag == u"highway":
			highway = True
	
	if highway and "name" in streets:
		street = streets["name"]
	elif "addr:street" in streets:
		street = streets["addr:street"]
	
	try:
		num_postcode = int(postcode)
		if len(postcode) < 6:
			log.log(err_type="W4", osm_type=node_type, osm_item=node, osm_code=postcode, gov_code=None)
			matching = False
			postcode = "%06d" % num_postcode
	except:
		log.log(err_type="E4", osm_type=node_type, osm_item=node, osm_code=postcode, gov_code=None)
		matching = False
		return #can't compare with anything
	if city == None:
		log.log(err_type="E1", osm_type=node_type, osm_item=node, osm_code=city, gov_code=None)
		matching = False
	if street == None:
		log.log(err_type="E2", osm_type=node_type, osm_item=node, osm_code=street, gov_code=None)
		matching = False
	if number == None:
		log.log(err_type="W3", osm_type=node_type, osm_item=node, osm_code=number, gov_code=None)
		matching = False
	if postcode not in postal_data: 
		if postcode not in other_data:
			log.log(err_type="E5", osm_type=node_type, osm_item=node, osm_code=postcode, gov_code=None)
			matching = False
		return matching
	
	streets_match = False
	gov_streets = []
	for address in postal_data[postcode]:
		if city and address['city'] != city:
			if removeDiacritics(address['city']) == removeDiacritics(city):
				log.log(err_type="W6", osm_type=node_type, osm_item=node, osm_code=city, 
														gov_code=address['city'])
			else:
				log.log(err_type="E6", osm_type=node_type, osm_item=node, osm_code=city, 
														gov_code=address['city'])
			return False
		if street and not matchStreets(street, address):
			#try to match without diacritics
			pc = dict(address)
			pc['street'] = removeDiacritics(pc['street'])
			if matchStreets(removeDiacritics(street), pc):
				log.log(err_type="W7", osm_type=node_type, osm_item=node, osm_code=street, 
														gov_code=address['street'])
			else:
				gov_streets.append(address['type'] + u" " + address['street'])
				continue
		streets_match = True
		if number and address['number']:
			for nr in number:
				if nr not in address['number']:
					log.log(err_type="E8", osm_type=node_type, osm_item=node, osm_code=number, 
														gov_code=address['rawnumber'])
					return False
	if streets_match == False:
		log.log(err_type="E7", osm_type=node_type, osm_item=node, osm_code=street, 
						gov_code=gov_streets)
		return False
	return matching

def main():
	global log
	bot = OverpassRequest(poly=ro_poly.ro_poly, filters=filters, output="json")
	postal_data = loadBucharestCsv()
	postal_data.update(load50kCsv())
	other_data = load1kCsv()
	with PostcodeErrLog() as log2:
		log = log2
		print "Fetching nodes..."
		js = bot.fetchNode()
		#print js
		obj = json.loads(js, "utf8")
		if not "elements" in obj:
			print "No elements in the response"
			return
		
		for node in obj["elements"]:
			if not "tags" in node:
				print "No tags in the response"
				continue
			processTags(u"node", node["id"], node["tags"], postal_data, other_data)
			
		print "Fetching ways..."
		js = bot.fetchWay()
		obj = json.loads(js, "utf8")
		if not "elements" in obj:
			print "No elements in the response"
			return
		
		for way in obj["elements"]:
			if not "tags" in node:
				print "No tags in the response"
				continue
			processTags(u"way", way["id"], way["tags"], postal_data, other_data)
			
		log.upload()
		log = None

if __name__ == "__main__":
	main()
