#!/usr/bin/python
# -*- coding: utf-8  -*-

import sys, json
sys.path.append("..")
import wikipedia

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
	
	for monument in db:
		if monument["Imagine"].strip() <> "":
			images += 1
		if monument["Lat"] <> "" or monument["Lon"] <> "":
			coords += 1
		if monument["Arhitect"] <> "":
			authors += 1
			
	print "Imagini: %d/%d (%f%%)" % (images, total, images * 100.0 / total)
	print "Potential imagini: %d" % len(pages_commons)
	print "Coordonate: %d/%d (%f%%)" % (coords, total, coords * 100.0 / total)
	print "Arhitect: %d/%d (%f%%)" % (authors, total, authors * 100.0 / total)

if __name__ == "__main__":
	try:
		main()
	finally:
		wikipedia.stopme()

