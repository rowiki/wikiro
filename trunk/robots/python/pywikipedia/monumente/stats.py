#!/usr/bin/python
# -*- coding: utf-8  -*-

import sys, json
sys.path.append("..")
import wikipedia

def split_code(code):
	parts = code.split('-')
	if len(parts) < 3:
		print parts
		return (None, None, None, None)
	return (parts[0], parts[1], parts[2], parts[3])

def main():
	f = open("db.json", "r+")
	wikipedia.output("Reading database file...")
	db = json.load(f)
	wikipedia.output("...done")
	f.close();
	
	f = open("commons_File_pages.json", "r+")
	wikipedia.output("Reading commons images file...")
	pages_commons = json.load(f)
	wikipedia.output("...done")
	f.close();
	
	images = 0
	coords = 0
	authors = 0
	total = len(db)

	total_county = {}
	total_nature = {}
	total_type = {}
	total_interest = {}

	image_county = {}
	image_nature = {}
	image_type = {}
	image_interest = {}
	
	for monument in db:
		(county, nature, type, interest) = split_code(monument["Cod"])
		if county == None:
			print monument
			continue

		if nature == "I":
			nature = "arheologie"
		elif nature == "II":
			nature = "arhitectura"
		elif nature == "III":
			nature = "for public"
		elif nature == "IV":
			nature = "memoriale"
		else:
			nature = "eroare natura"
			print nature
			print monument

		if type == "m":
			type = "monumente"
		elif type == "s":
			type = "situri"
		elif type == "a":
			type = "amsambluri"
		else:
			type = "eroare tip"
			print type
			print monument

		if interest == "A":
			interest = "national"
		elif interest == "B":
			interest = "local"
		else:
			interest = "eroare interes"
			print interest
			print monument

		if not county in total_county:
			total_county[county] = 1
			image_county[county] = 0
		else:
			total_county[county] += 1
		if not nature in total_nature:
			total_nature[nature] = 1
			image_nature[nature] = 0
		else:
			total_nature[nature] += 1
		if not type in total_type:
			total_type[type] = 1
			image_type[type] = 0
		else:
			total_type[type] += 1
		if not interest in total_interest:
			total_interest[interest] = 1
			image_interest[interest] = 0
		else:
			total_interest[interest] += 1
		if monument["Imagine"].strip() <> "":
			images += 1
			image_county[county] += 1
			image_nature[nature] += 1
			image_type[type] += 1
			image_interest[interest] += 1
		if monument["Lat"] <> "" or monument["Lon"] <> "":
			coords += 1
		if monument["Arhitect"] <> "":
			authors += 1
	
	print "Imagini: %d/%d (%f%%)" % (images, total, images * 100.0 / total)
	print "Potential imagini: %d" % len(pages_commons)
	print "Coordonate: %d/%d (%f%%)" % (coords, total, coords * 100.0 / total)
	print "Arhitect: %d/%d (%f%%)" % (authors, total, authors * 100.0 / total)

	for county in image_county.keys():
		print "Imagini pentru judetul %s: %f%%" % (county, image_county[county] * 100.0 / total_county[county])
	for nature in image_nature.keys():
		print "Imagini pentru monumente de %s: %f%%" % (nature, image_nature[nature] * 100.0 / total_nature[nature])
	for type in image_type.keys():
		print "Imagini pentru %s: %f%%" % (type, image_type[type] * 100.0 / total_type[type])
	for interest in image_interest.keys():
		print "Imagini pentru monumente de interes %s: %f%%" % (interest, image_interest[interest] * 100.0 / total_interest[interest])

if __name__ == "__main__":
	try:
		main()
	finally:
		wikipedia.stopme()

