#!/usr/bin/python
# -*- coding: utf-8  -*-

"""
{
    "FostCod": "",
    "Cod92": "",
    "Arhitect": "",
    "Denumire": "Situl arheologic de la Alba Iulia",
    "Adres\u0103": "pe \u00eentreg teritoriul",
    "Datare": "",
    "Commons": "commons:Category:Situl arheologic Alba Iulia",
    "source": "http://ro.wikipedia.org/w/index.php?title=Lista_monumentelor_istorice_din_jude\u021bul_Alba&redirect=no&useskin=monobook&oldid=7383805",
    "Not\u0103Cod": "",
    "Cod": "AB-I-s-A-00001",
    "Coordonate": "",
    "Imagine": "File:Ruins of 10th Century Christian Church from Alba Iulia 2011.jpg",
    "Lat": "46.066667",
    "CodRan": "1026.01",
    "Localitate": "municipiul [[Alba Iulia]]",
    "Lon": "23.566667"
  },

"""

import json
import sirutalib
import sys
import threading
import urllib, urllib2
#import MySQLdb as mdb
sys.path.append("..")
import strainu_functions as strainu
import wikipedia
	
cache = {}
sql = unicode("INSERT INTO `monumente`(`siruta`, `cod`, `fostcod`, `cod92`, `denumire`, `adresa`, `arhitect`, `datare`, `codran`, `coordonate`, `imagine`) VALUES (", "utf8")
separator = unicode("', '", "utf8")
_csv = sirutalib.SirutaDatabase()
uag = { 'User-Agent' : u"despresate/1.0 (http://despresate.strainu.ro/; despresate@strainu.ro)" }
index = 1
index_lock = threading.Lock()

def escape_string(text):
	return text.replace("'", "\\'").replace("\"", "\\\"")

def clean_title(title):
	return title.replace("-", " ").replace("  ", " ");


def get_siruta(city):
	global cache
	entities = city.split("[[")
	#print entities
	if len(entities) < 2:
		return 1
	if len(entities) < 3:
		name = entities[1][0:entities[1].find("]")].upper()
		if name.find("|") > -1:
			name = name[0:name.find("|")]
	        if name.find(",") > -1:
			county = name[name.find(",")+1:].strip().upper()
			name = name[0:name.find(",")]
		else:		
			county = None
		name = name.replace("Comuna ", "")
		supname = None
	else:
		name = entities[1][0:entities[1].find("|")]
		county = name[name.find(",")+1:].strip().upper()
		name = name[:name.find(",")].upper()

                supname = entities[2][0:entities[2].find("]")]
	        if supname.find("|") > -1:
                	supname = supname[0:supname.find("|")]
	        if supname.find(",") > -1:
                	supname = supname[0:supname.find(",")]
		supname = supname.replace("Comuna ", "").strip().upper()
	#special cases
	if county == u"ROMÂNIA":
		county = None
	name = name.replace(u"(oraș)", u"")
	name = name.strip()
	#print name
	#print supname
	#print county
	cache_key = unicode(name) + unicode(supname) + unicode(county)
	if cache_key in cache:
		#print "***Cache hit*** " + str(cache[cache_key])
		return cache[cache_key]
	potential = 0
	for elem in _csv._data:
		if _csv.get_type(elem) == 40:
			continue
		if clean_title(_csv.get_name(elem, False)) == clean_title(name):
			if county and county.upper() <> _csv.get_county_string(elem, False):
				continue
			if supname and clean_title(supname.upper()) <> clean_title(_csv.get_sup_name(elem, False)):
				if potential == 0:
					potential = elem
				else:
					potential = -1
				continue
			#print _csv._data[elem]["siruta"]
			cache[cache_key] = elem
			return elem
		else:
			continue
	if potential > 0:
		#print entities
		#print "Potential comuna %s" %  _csv.get_sup_name(potential, False)
		return potential
	return 1
		


class MonumentThread ( threading.Thread ):
	# Override Thread's __init__ method to accept the parameters needed:
	def __init__ ( self, monument, _file, _file_lock ):
		self._file = _file
		self._file_lock = _file_lock
		self.monument = monument
		threading.Thread.__init__ ( self )

	def get_image(self, filename, siruta):
		global index, index_lock
		filename = filename.replace(u"Fișier", u"File").strip().replace(u" ", u"_")
		if filename == u"":
			return u"NULL"
		sql = "INSERT INTO `imagini`(`index`, `siruta`, `county`, `imgurl`, `thumburl`, `thumbw`, `thumbh`, `descurl`, `attribution`) VALUES("
		print u"***" + filename + u"***"
		newurl = u"http://commons.wikimedia.org/w/api.php?format=json&action=query&titles=" + urllib.quote_plus(filename.encode('utf8')) + "&prop=imageinfo&iiprop=url&iiurlwidth=250px&redirects"
		#print newurl
		req = urllib2.Request(newurl, None, uag)
		resp = urllib2.urlopen(req)
		jsontxt = resp.read()
		#print jsontxt
		pg = json.loads(jsontxt)
		pg = pg[u"query"][u"pages"]
		ret = u"NULL"
		for page in pg:
			if u"missing" in pg[page]:
				return ret
			if not u"imageinfo" in pg[page]:
				return ret
			#print pg[page]
			thumb = pg[page]["imageinfo"][0]["thumburl"]
			tw = pg[page]["imageinfo"][0]["thumbwidth"]
			th = pg[page]["imageinfo"][0]["thumbheight"] 
		        imgurl = pg[page]["imageinfo"][0]["url"]
        		descurl = pg[page]["imageinfo"][0]["descriptionurl"]
			index_lock.acquire()
			ret = str(index)
			index += 1
			index_lock.release()
			sql += ret + u", " + str(siruta) + u", " 
			if _csv.get_county(siruta) <> None:
				sql += str(_csv.get_county(siruta))
			else:
				sql += u"NULL" 
			sql += u", '" + imgurl + separator + thumb +  u"', " + str(tw) + u", " + str(th) + u", '" + descurl + u"', 'Wikimedia Commons');\n"
			self._file_lock.acquire()
			self._file.write(sql.encode("utf8"))
			self._file_lock.release()
		return ret

	def run ( self ):
		ran = escape_string(self.monument[u"CodRan"])
		siruta = get_siruta(self.monument[u"Localitate"])
		query = sql 
		query += unicode(str(siruta), "utf8") 
		query += u", '"
		query += escape_string(self.monument[u"Cod"])
		query += separator
		query += escape_string(self.monument[u"FostCod"]) + separator + escape_string(self.monument[u"Cod92"]) + separator
		query += escape_string(strainu.stripLink(monument[u"Denumire"])) + separator
		query += escape_string(self.monument[u"Adresă"]) + separator + escape_string(self.monument[u"Arhitect"]) + separator
		query += escape_string(self.monument[u"Datare"])
		query += separator + ran + u"', "

		if self.monument[u"Lat"]:
			query += u"GeomFromText('POINT(" + self.monument[u"Lat"] + u" " + self.monument[u"Lon"] + u")'), "
		else:
			query += u"NULL, "
		query += self.get_image(self.monument[u"Imagine"], siruta) + u");\n"
		#print query
		self._file_lock.acquire()
		self._file.write(query.encode("utf8"))
		self._file_lock.release()

if __name__ == "__main__":
	f = open("lmi_db.json", "r+")
	print "Reading database file..."
	db = json.load(f, "utf8")
	print "...done"
	f.close();


	_file = open("insert.sql", "w+");
	_file_lock = threading.Lock()

	for monument in db:
		th = MonumentThread(monument, _file, _file_lock)
		th.start()
		#th.run()
	
	_file_lock.acquire()
	_file.close()
	_file_lock.release()


