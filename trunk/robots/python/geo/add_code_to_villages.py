#!/usr/bin/python
# -*- coding: utf-8  -*-

'''
Bot to add postal codes to villages which don't yet have that data.
'''

import csv
import json
import sys

import overpass
import OsmApi
import strainu_functions as fun

#structure is:
#  - external dict is an union, keys are variables
#  - inner dict are parameters for a request
#  - if inner dict is not None, the value is the first elem of the pair and isPositive is the second
filters = {
	u"a": {
		u"addr:postcode": (u".", False),
		u"postal_code": (u".", False),
		u"siruta:code": None,
		u"place": (u"village", True),
	},
	u"b": {
		u"addr:postcode": (u".", False),
		u"postal_code": (u".", False),
		u"siruta:code": None,
		u"place": (u"hamlet", True),
	},
	}

api = OsmApi.OsmApi(api="api.openstreetmap.org", passwordfile = "../osmpasswd", debug = True)
bot = overpass.OverpassRequest(poly=u"România", filters=filters, output="json")

def upload(node_id, code):
	if not node_id:
		return
	node = api.NodeGet(node_id)
	tags = node["tag"]
	if "postal_code" in tags or "addr:postcode" in tags:
		print (u"Place %s already has a postal code. Check for errors in the request?" % tags["name"])
		return 
	tags["postal_code"] = code
	tags["postal_code:source"] = u"Date de la Poșta Română, publicate sub Licența pentru Guvernare Deschisă v1.0 la http://date.gov.ro/organization/posta-romana"
	node["tag"] = tags
	print ("Ready to add postal_code (%s) to %s" % (tags["postal_code"], tags["name"]))
	print "Do you want to update the record? ([y]es/[n]o/[a]llways/[q]uit)"
	line = sys.stdin.readline().strip()
	if line == 'y':
		api.ChangesetCreate({u"comment": "adding postal code to %s" % tags["name"]})
		api.NodeUpdate(node)
		api.ChangesetClose()

def checkStatus(village, county):
	commune = u""
	if village.find(u"(") > -1:
		commune = village[village.find(u"(")+1:village.find(u")")]
		#print u"Comuna " + commune
	
	key = village.split(u"(")[0].strip() + county
	if key in obj:
		if obj[key]["commune"].find(commune) == -1:
			#print obj
			return 0
		return obj[key]["id"]
	else:
		return 0
	
def refactor(objects):
	ret = {}
	for obj in objects["elements"]:
		if "name" not in obj["tags"] or "is_in:county" not in obj["tags"] or "is_in" not in obj["tags"]:
			print obj
			continue
		key = obj["tags"]["name"] + obj["tags"]["is_in:county"]
		ret[key] = {
			"name": obj["tags"]["name"],
			"county": obj["tags"]["is_in:county"],
			"commune": obj["tags"]["is_in"],
			"id": obj["id"],
		}
	return ret
	
if __name__ == "__main__":
	global obj
	js = bot.fetchNode()
	obj = json.loads(js, "utf8")
	obj = refactor(obj)
	print json.dumps(obj, indent=2)
	#print obj
	reader = fun.unicode_csv_reader(open("codp_1k.csv", "r"))
	for row in reader:
		#county, village (commune), postal_code
		county, village, code = row
		upload(checkStatus(village, county), row[2])
