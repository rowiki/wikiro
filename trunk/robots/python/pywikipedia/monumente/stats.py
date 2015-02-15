#!/usr/bin/python
# -*- coding: utf-8  -*-

import sys, json
import operator
#sys.path.append("..")
import pywikibot
#from pywikibot import pagegenerators
#from pywikibot import config


def split_code(code):
	parts = code.split('-')
	if len(parts) < 3:
		print parts
		return (None, None, None, None)
	return (parts[0], parts[1], parts[2], parts[3])

def main():
	f = open("ro_lmi_db.json", "r+")
	pywikibot.output("Reading database file...")
	db = json.load(f)
	pywikibot.output("...done")
	f.close();
	
	f = open("commons_lmi_Category_pages.json", "r+")
	pywikibot.output("Reading commons categories file...")
	cat_commons = json.load(f)
	pywikibot.output("...done")
	f.close();
	
	f = open("commons_lmi_File_pages.json", "r+")
	pywikibot.output("Reading commons images file...")
	pages_commons = json.load(f)
	pywikibot.output("...done")
	f.close();
	
	f = open("ro_lmi_pages.json", "r+")
	pywikibot.output("Reading articles file...")
	pages_ro = json.load(f)
	pywikibot.output("...done")
	f.close();
	
	f = open("ro_lmi_Fișier_pages.json", "r+")
	pywikibot.output("Reading ro images file...")
	files_ro = json.load(f)
	pywikibot.output("...done")
	f.close();
	
	images = 0
	plans = 0
	coords = 0
	authors = 0
	articles = 0
	potential_articles = 0
	total = len(db)

	total_county = {}
	total_nature = {}
	total_type = {}
	total_interest = {}
	total_apmnir = 0

	image_county = {}
	image_nature = {}
	image_type = {}
	image_interest = {}
	image_apmnir = 0
	
	for monument in db:
		(county, nature, type, interest) = split_code(monument["Cod"])
		if county == None:
			print monument
			continue
		if county == "B":
			i = monument["source"].find(u"sector")
			sector = monument["source"][i+7:i+8]
			county = county + sector
		
		if monument["Denumire"].find("[[") >= 0:
			articles += 1
		elif interest == "A":
			potential_articles += 1

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
			type = "ansambluri"
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
		if monument["Plan"].strip() <> "":
			#images += 1
			plans += 1
		if monument["Lat"] <> "" or monument["Lon"] <> "":
			coords += 1
		if monument["Creatori"] <> "":
			authors += 1
			
	total_images = 0
	for mon in pages_commons:
		total_images += len(pages_commons[mon])
	for mon in files_ro:
		total_images += len(files_ro[mon])
	
	print "* Total imagini: ''%d''" % total_images
	print "* Monumente cu imagini: ''%d/%d (%f%%)''" % (images, total, images * 100.0 / total)
	print "* Monumente cu imagini sau plan: ''%d/%d (%f%%)''" % (images + plans, total, (images + plans) * 100.0 / total)
	print "* Monumente cu coordonate: ''%d/%d (%f%%)''" % (coords, total, coords * 100.0 / total)
	print "* Monumente cu arhitect: ''%d/%d (%f%%)''" % (authors, total, authors * 100.0 / total)
	print "* Monumente cu articole: ''%d/%d (%f%%)''" % (articles, total, articles * 100.0 / total)
	print "* Categorii la commons: ''%d''" % len(cat_commons)
	print "* Articole distincte: ''%d''" % len(pages_ro)
	print "* Articole lipsă (cat A): ''%d''" % potential_articles

	print "----"
	for nature in image_nature.keys():
		if nature != "arheologie":
			total_apmnir += total_nature[nature]
			image_apmnir += image_nature[nature]
		print "* Imagini pentru monumente de %s: ''%f%%''" % (nature, image_nature[nature] * 100.0 / total_nature[nature])
	print "----"
	for type in image_type.keys():
		print "* Imagini pentru %s: ''%f%%''" % (type, image_type[type] * 100.0 / total_type[type])
	print "* Comparație cu monumenteromania.ro: ''%f%% (%d/%d)''" % (image_apmnir * 100.0 / total_apmnir, image_apmnir, total_apmnir)
	print "----"
	for interest in image_interest.keys():
		print "* Imagini pentru monumente de interes %s: ''%f%%''" % (interest, image_interest[interest] * 100.0 / total_interest[interest])
	print "----"

	#image_keys = image_county.keys()
	#image_keys.sort()
	images_percent = {}
	for county in image_county.keys():
		images_percent[county] = image_county[county] * 100.0 / total_county[county]
	sorted_images = sorted(images_percent.iteritems(), key=operator.itemgetter(1), reverse=True)
	for county,number in sorted_images:
		print "* Imagini pentru judetul %s: ''%f%% (%d/%d)''" % (county, number , image_county[county], total_county[county])

if __name__ == "__main__":
	try:
		import update_database
		update_database.main()
		main()
	finally:
		pywikibot.stopme()

