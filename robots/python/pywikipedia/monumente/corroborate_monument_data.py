#!/usr/bin/python
# -*- coding: utf-8  -*-
'''
Based on the output from parse_monument_article.py and update_database.py 
identify different improvements and further errors in the monument pages and database
* să adauge linkuri către articolul despre un monument acolo unde există - d
* să adauge coordonate în listă dacă ele există în articol - d
* să adauge automat poza în listă dacă există una singură cu monumentul respectiv - d
* să raporteze la [[Proiect:Monumente istorice/Erori/Automate]] următoarele situații:
** în listă nu există codul respectiv, dar codul are formatul corect în articol/imagine
** în listă nu există imagine și există mai multe imagini disponibile la commons - i
** în listă și articol coordonatele sunt diferite (diferențe mai mari de 0,01 grade sau ~35 de secunde de grad) - i
'''

import sys, time, warnings, json, string
sys.path.append("..")
import wikipedia, re, pagegenerators
import config as user
import math

_log = "link.err.log"
_flog = None

def initLog():
	global _flog, _log;
	_flog = open(_log, 'w+')
	
def closeLog():
	global _flog
	_flog.close()

def log(string):
	_flog.write(string.encode("utf8") + "\n")
	
def main():
	f = open("db.json", "r+")
	wikipedia.output("Reading database file...")
	db = json.load(f)
	wikipedia.output("...done")
	f.close();
	
	f = open("ro_pages.json", "r+")
	wikipedia.output("Reading ro.wp pages file...")
	pages_ro = json.load(f)
	wikipedia.output("...done")
	f.close();
	
	f = open("commons_pages.json", "r+")
	wikipedia.output("Reading commons pages file...")
	pages_commons = json.load(f)
	wikipedia.output("...done")
	f.close();
	
	initLog()
	#this is the big loop that should only happen once
	for monument in db:
		code = monument["Cod"]
		regexp = re.compile("(([a-z]{1,2})-(i|ii|iii|iv)-([a-z])-([a-z])-([0-9]{5}(\.[0-9]{2})?))", re.I)
		result = re.findall(regexp, code)
		if len(result) > 0:
			code = result[0][0]
		wikipedia.output(code)
		allPages = list()
		article = None
		picture = None
		#wikipedia.output(str(page))
		try:
			#wikipedia.output("OK: " + str(page[code]))
			if code in pages_ro:
				allPages.extend(pages_ro[code])
				if len(allPages) > 1:
					msg = u"*''E'': Codul ''%s'' este prezent în mai multe articole pe Wikipedia: " % code
					for page in allPages:
						msg += (u"[[:%s]] " % page["name"])
					msg += u"\n"
					log(msg)
				elif len(allPages) == 1:
					article = allPages[0]["name"]
			if code in pages_commons:
				if len(pages_commons[code]) == 1: #exactly one picture
					picture = pages_commons[code][0]["name"]
				elif monument["Imagine"] == "": #no image, multiple available
					msg = u"*''I'': Există mai multe imagini disponibile la commons pentru codul ''%s'': " % code
					for pic in pages_commons[code]:
						msg += u"[[:File:%s]], " % pic["name"]
					msg += "\n"
					log(msg)
				allPages.extend(pages_commons[code])
			#wikipedia.output(str(allPages))
		except Exception as e:
			wikipedia.output("Error: " + str(e))
			pass #ignore errors
			
		if article <> None:
			wikipedia.output(article)
			pass
			#TODO put a link in the list
			
		if picture <> None and monument["Imagine"] <> "":
			wikipedia.output(picture)
			pass
			#TODO put an image in the list
		
		if monument["Lat"] == "":
			lat = 0
		else:
			lat = float(monument["Lat"])
		if monument["Lon"] == "":
			long = 0
		else:
			long = float(monument["Lon"])
		artLat = 0
		artLong = 0
		artCoord = None
		updateCoord = True
		for page in allPages:
			if page["lat"] <> 0 and page["long"] <> 0:
				if artLat <> 0: #this also means artLong <> 0
					if math.fabs(artLat - page["lat"]) > 0.01 or math.fabs(artLong - page["long"]) > 0.01:
						if page["namespace"] == 6:
							namespace = "File:"
						else:
							namespace = ""
						log(u"* ''E:'': Coordonate diferite pentru codul %s între [[:%s%s]] (%f,%f) și [[:%s]] (%f,%f)" % (code, namespace, page["name"], page["lat"], page["long"], artCoord, artLat, artLong))
						updateCoord = False
				else:
					artLat = page["lat"]
					artLong = page["long"]
					if page["namespace"] == 6:
						artCoord = "File:" + page["name"]
					else:
						artCoord = page["name"]
					
		if lat == 0 and artLat <> 0 and updateCoord:
			wikipedia.output(str(artLat) + " " + str(artLong))
			pass
			#TODO: update coords on page
		
		if lat <> 0 and artLat <> 0 and (math.fabs(artLat - lat) > 0.01 or math.fabs(artLong - long) > 0.01):
			log(u"* ''E:'': Coordonate diferite pentru codul %s între [[:%s]] (%f,%f) și listă (%f,%f)" % (code, artCoord, artLat, artLong, lat, long))
		
	closeLog()

if __name__ == "__main__":
	try:
		main()
	finally:
		wikipedia.stopme()