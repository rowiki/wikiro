#!/usr/bin/python
# -*- coding: utf-8  -*-

'''
Bot to add postal codes to nodes and ways that have complete addresses
'''

import csv
import json
import sys

import overpass
import osmapi as OsmApi
sys.path.append('wikiro/robots/python/pywikipedia')
import strainu_functions as fun
import coduripostale

#structure is:
#  - external dict is an union, keys are variables
#  - inner dict are parameters for a request
#  - if inner dict is not None, the value is the first elem of the pair and isPositive is the second
filters_b = {
	u"a": {
		u"addr:city": (u"București", True),
		u"addr:street": None,
		u"addr:housenumber": None,
		u"addr:postcode": (u".", False),
		u"siruta:code": (u".", False),
		u"amenity": (u"post_office", False),
	},
	u"b": {
		u"addr:city": (u"Bucureşti", True),
		u"addr:street": None,
		u"addr:housenumber": None,
		u"addr:postcode": (u".", False),
		u"siruta:code": (u".", False),
		u"amenity": (u"post_office", False),
	},
}

filters_ro = {
	u"a": {
		u"addr:city": None,
		u"addr:street": None,
		u"addr:housenumber": None,
		u"addr:postcode": (u".", False),
		u"siruta:code": (u".", False),
		u"amenity": (u"post_office", False),
	},
}

api = OsmApi.OsmApi(api="api.openstreetmap.org", passwordfile = "osmpasswd", debug = True)

def readVillageFile():
	villages = {}
	reader = fun.unicode_csv_reader(open("codp_1k.csv", "r"))
	for row in reader:
		#county, village (commune), postal_code
		county, village, code = row
		if village in villages:
			villages[village] = None
		else:
			villages[village] = code
	return villages

def uploadNode(node_id, code):
	if not node_id:
		return
	node = api.NodeGet(node_id)
	tags = node["tag"]
	if tags["addr:city"] == u"Bucureşti":
		tags["addr:city"] = u"București"
	if "postal_code" in tags or "addr:postcode" in tags:
		print (u"Place %s %s, %s already has a postal code. Check for errors in the request?" % (tags["addr:street"], tags["addr:housenumber"], tags["addr:city"]))
		return 0 
	tags["addr:postcode"] = code
	tags["addr:postcode:source"] = u"Date de la Poșta Română, publicate sub Licența pentru Guvernare Deschisă v1.0 la http://date.gov.ro/organization/posta-romana"
	node["tag"] = tags
	print ("Ready to add postal_code (%s) to %s %s, %s" % (tags["addr:postcode"], tags["addr:street"], tags["addr:housenumber"], tags["addr:city"]))
	print "Do you want to update the record? ([y]es/[n]o/[a]llways/[q]uit)"
	#line = sys.stdin.readline().strip()
	line = 'y'
	if line == 'y':
		api.ChangesetCreate({u"comment": "adding postal code to %s %s, %s" % (tags["addr:street"], tags["addr:housenumber"], tags["addr:city"])})
		api.NodeUpdate(node)
		api.ChangesetClose()
	return node_id

def uploadWay(way_id, code):
	if not way_id:
		return
	node = api.WayGet(way_id)
	tags = node["tag"]
	if tags["addr:city"] == u"Bucureşti":
		tags["addr:city"] = u"București"
	if "postal_code" in tags or "addr:postcode" in tags:
		print (u"Place %s %s, %s already has a postal code. Check for errors in the request?" % (tags["addr:street"], tags["addr:housenumber"], tags["addr:city"]))
		return 0 
	tags["addr:postcode"] = code
	tags["addr:postcode:source"] = u"Date de la Poșta Română, publicate sub Licența pentru Guvernare Deschisă v1.0 la http://date.gov.ro/organization/posta-romana"
	node["tag"] = tags
	print ("Ready to add postal_code (%s) to %s %s, %s" % (tags["addr:postcode"], tags["addr:street"], tags["addr:housenumber"], tags["addr:city"]))
	print "Do you want to update the record? ([y]es/[n]o/[a]llways/[q]uit)"
	#line = sys.stdin.readline().strip()
	line = 'y'
	if line == 'y':
		api.ChangesetCreate({u"comment": "adding postal code to %s %s" % (tags["addr:street"], tags["addr:housenumber"])})
		api.WayUpdate(node)
		api.ChangesetClose()
	return way_id
	
def refactor_b(objects):
	ret = {}
	for obj in objects["elements"]:
		if obj["tags"]["addr:city"] == u"Bucureşti":
			obj["tags"]["addr:city"] = u"București"
		key = obj["tags"]["addr:street"].lower() + obj["tags"]["addr:city"].lower()
		if key not in ret:
			ret[key] = []
		ret[key].append({
			"street": obj["tags"]["addr:street"],
			"nr": obj["tags"]["addr:housenumber"],
			"city": obj["tags"]["addr:city"],
			"id": obj["id"],
		})
	return ret
	
def refactor_ro(objects):
	ret = {}
	for obj in objects["elements"]:
		if obj["tags"]["addr:city"] == u"București" or obj["tags"]["addr:city"] == u"Bucureşti":
			continue
		key = obj["tags"]["addr:street"].lower() + obj["tags"]["addr:city"].lower()
		if key not in ret:
			ret[key] = []
		ret[key].append({
			"street": obj["tags"]["addr:street"],
			"nr": obj["tags"]["addr:housenumber"],
			"city": obj["tags"]["addr:city"],
			"id": obj["id"],
		})
	return ret
	
def upload(_id, code, isNode=True):
	if isNode:
		uploadNode(_id, code)
	else:
		uploadWay(_id, code)
	
def findId(tip, street, post_nr, city):
	global obj
	key = tip.lower() + u" " + street.lower() + city.lower()
	_post_nr = coduripostale.splitNumberList(post_nr[4:])
	if key in obj:
		_ret = []
		for addr in obj[key]:
			obj_nr = coduripostale.splitNumberList(addr["nr"])
			for nr in obj_nr:
				if nr not in _post_nr:
					break
			else:
				#print post_nr
				#print _post_nr
				#print obj_nr
				_ret.append(addr["id"])
		if len(_ret):
			return _ret
	return 0
	
def check_Bucharest(isNode=True):
	global obj
	bot = overpass.OverpassRequest(poly=u"București", filters=filters_b, output="json")
	if isNode:
		js = bot.fetchNode()
	else:
		js = bot.fetchWay()
	try:
		obj = json.loads(js, "utf8")
		obj = refactor_b(obj)
	except:
		print "No response received from the overpass API"
		return
	print json.dumps(obj, indent=2)
	reader = fun.unicode_csv_reader(open("codp_B.csv", "r"))
	for row in reader:
		tip, street, nr, code, sector, unused = row
		_ids = findId(tip, street, nr, u"București")
		if _ids == 0:
			_ids = findId(tip, coduripostale.convertName(street, u""), nr, u"București")
		if _ids == 0:
			_ids = findId(tip, coduripostale.convertName(street, u"", False), nr, u"București")
		if _ids:
			#print row
			for _id in _ids:
				upload(_id, code, isNode)
		else:
			#print row
			pass
	
def check_cities(isNode=True):
	global obj
	bot = overpass.OverpassRequest(poly=u"România", filters=filters_ro, output="json")
	if isNode:
		js = bot.fetchNode()
	else:
		js = bot.fetchWay()
	try:
		obj = json.loads(js, "utf8")
		obj = refactor_ro(obj)
	except:
		print "No response received from the overpass API"
		return
	#print json.dumps(obj, indent=2)
	reader = fun.unicode_csv_reader(open("codp_50k.csv", "r"))
	villages = readVillageFile()
	for row in reader:
		county, city, tip, street, nr, code = row
		if nr[:2] == u"bl":
			continue
		_ids = findId(tip, street, nr, city)
		if _ids == 0:
			_ids = findId(tip, coduripostale.convertName(street, u""), nr, city)
		if _ids == 0:
			_ids = findId(tip, coduripostale.convertName(street, u"", False), nr, city)
		if _ids:
			#print row
			for _id in _ids:
				upload(_id, code, isNode)
		else:
			#print row
			pass
			
	for o in obj:
		for a in obj[o]:
			c = a["city"]
			if c in villages and villages[c]:
				upload(a["id"], villages[c], isNode)

if __name__ == "__main__":
	#print obj
	for isNode in [True,False]:
		check_Bucharest(isNode)
		check_cities(isNode)
