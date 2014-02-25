#!/usr/bin/python
# -*- coding: utf-8  -*-

'''
Strip diacritics from entries
'''
import csv
import json
import sys

import overpass
import OsmApi
import strainu_functions as fun
import coduripostale

api = OsmApi.OsmApi(api="api.openstreetmap.org", passwordfile = "../osmpasswd", debug = True)

def maketransU(s1, s2, todel=""):
	trans_tab = dict( zip( map(ord, s1), map(ord, s2) ) )
	trans_tab.update( (ord(c),None) for c in todel )
	return trans_tab

def correctDiacritics(name):
	tran = maketransU(u"ŞşŢţ", u"ȘșȚț")
	return name.translate(tran)

def processTags(node_type, node_id, tags):
	city = street = number = postcode = None
	streets = {}
	highway = False
	matching = True
	
	changed = False
	for tag in tags:
		if tag in coduripostale.all_tags[node_type]["city"]:
			t = correctDiacritics(tags[tag])
			if tags[tag] != t:
				tags[tag] = t
				changed = True
		if tag in coduripostale.all_tags[node_type]["street"]:
			t = correctDiacritics(tags[tag])
			if tags[tag] != t:
				tags[tag] = t
				changed = True
			
	if not changed:
		return
	if node_type == u"node":
		node = api.NodeGet(node_id)
	else:
		node = api.WayGet(node_id)
	node["tag"] = tags
	print node["tag"]
	print "Do you want to update the record? ([y]es/[n]o/[a]llways/[q]uit)"
	#line = sys.stdin.readline().strip()
	line = 'y'
	if line == 'y':
		api.ChangesetCreate({u"comment": "Correcting Romanian diacritics"})
		if node_type == u"node":
			api.NodeUpdate(node)
		else:
			api.WayUpdate(node)
		api.ChangesetClose()

def main():
	bot = overpass.OverpassRequest(poly=u"România", filters=coduripostale.filters, output="json")
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
		processTags(u"node", node["id"], node["tags"])
		
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
		processTags(u"way", way["id"], way["tags"])

if __name__ == "__main__":
	main()
