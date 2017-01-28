#!/usr/bin/python
# -*- coding: utf-8  -*-

'''
'''
#
# (C) Strainu 2017
#
# Distributed under the terms of the MIT license.
#

import re
import sys
from collections import OrderedDict as od
import requests

import pywikibot
from pywikibot import pagegenerators
from pywikibot import config as user
from pywikibot import catlib

sys.path.append("wikiro/robots/python/pywikipedia")
import strainu_functions as sf



class Article:
	def __init__(self, cinemagia_url, aarc_url):
		self._text  = u""
		self._cinemagia = cinemagia_url
		self._aarc = aarc_url
		self._year = 0
		self._cmId = None
		self._cmRating = 0
		self._imdbId = None
		self._imdbRating = 0

	def fetchAarc(self):
		r = requests.get(self._aarc)
		text = self.fetchAarcData(r.text)
		self._year = self.fetchAarcYear(text)
		self._director = self.fetchAarcEntry(text, "Regie")
		self._scenario = self.fetchAarcEntry(text, "Scenariu")
		self._mainActors = self.fetchAarcEntry(text, "Actori")
		self._distribution = self.fetchAarcDistribution(r.text)

	def fetchAarcEntry(self, text, entry):
		text = text[text.find(entry):]
		text = text[text.find("</span>")+len("</span>"):]
		text = text[:text.find("</p>")]
		return text.strip()

	def fetchAarcData(self, text):
		index1 = text.find("<div class=\"mainfilminfo\">")
		index2 = text.find("</div>", index1)
		text = text[index1:index2+6]
		return text

	def fetchAarcYear(self, text):
		m = re.search("<h1>(.*)\((\d*)\)</h1>", text)
		if m:
			return int(m.group(2))
		return 0

	def fetchAarcDistribution(self, text):
		distribution = {}
		text = text[text.find("http://aarc.ro/filme/distributie/"):]
		text = text[:text.find("\"")]
		r = requests.get(text)
		text = r.text
		text = text[text.find(u"<h3>Distribuție</h3>"):]
		text = text[text.find("<tbody"):]
		text = text[:text.find("</tbody>")]
		entries = text.split("</tr><tr>")
		if len(entries) == 1:
			return distribution
		for entry in entries:
			(pic, name, role) = entry.split("<td>")
			name = name[:name.find("</td")].strip()
			m = re.search(ur"[^>]*(?=<\/a>)", name)
			if m:
				name = m.group(0)
			role = role[:role.find("</td")].strip()
			distribution[role] = name
		return distribution

	def fetchCm(self):
		m = re.search("https\:\/\/www\.cinemagia\.ro\/filme\/.*\-(\d*)\/", self._cinemagia)
		if m:
			self._cmId = m.group(1)
		else:
			return # no need to parse if we cannot match the URL
		r = requests.get(self._cinemagia)
		text = r.text
		self._cmRating = self.fetchCmRating(r.text)
		self._imdbId = self.fetchImdbId(r.text)
		self._imdbRating = self.fetchImdbRating(r.text)
		self._year= self.fetchCmYear(r.text)

	def fetchCmRating(self, text):
		r = text.find("ratingGlobalInfo")
		if r > -1:
			text = text[r:]
			text = text[:text.find("</div>")]
			m = re.search(ur"[^>]*(?=<\/span>)", text)
                        if m:
                                return float(m.group(0))
		return 0

	def fetchCmYear(self, text):
                r = text.find("link1")
                if r > -1:
                        text = text[r:]
                        m = re.search(ur"(?<=\()[^>]*(?=\)<\/a>)", text)
                        if m:
                                return int(m.group(0))
                return 0

	def fetchImdbRating(self, text):
		r = text.find("imdb-rating")
		if r > -1:
			text = text[r:]
			m = re.search(ur"[^>]*(?=<\/a>)", text)
			if m:
				score = m.group(0)
				score = score[score.find(':')+1:].strip()
				return float(score)
		return 0

	def fetchImdbId(self, text):
		r = text.find("imdb-rating")
                if r > -1:
                        text = text[r:]
                        m = re.search("http.*\/tt(.*?)(?=\/\")", text)
                        if m:
                                return m.group(1)
                return None

	def buildArticle(self):
		self._text = u""
		self.addInfobox()
		self.addMainArticle()
		self.addCommonSections()
		self.addCats()
		print self._text

	def addInfobox(self):
		actors = self._mainActors.replace(u",", u"<br/>")
		text = u"""{{Infocaseta Film
  | nume_film           =  {{PAGENAME}}
  | alte_nume           = 
  | imagine             = 
  | descriere_imagine   = Afișul filmului  
  | rating              =  
  | gen                 = 
  | regizor             = %s
  | producător          = 
  | scenarist           = %s
  | narator             = 
  | rolurile_principale = %s
  | muzica              = 
  | dir_imag            = 
  | montaj              = 
  | studio              = 
  | distribuitor        = 
  | premiera            = 
  | premiera_ro         = 
  | premiera_md         = 
  | durată              = 
  | țara                = [[România]]
  | limba_originală     = [[limba română|română]]
  | dispromână          = original
  | buget               =  
  | încasări            =  
  | operator            = 
  | sunet               = 
  | scenografie         = 
  | costume             = 
  | machiaj             = 
  | casting             = 
  | premii              = 
  | precedat_de         = 
  | urmat_de            = 
  | website             = 
  | id_cinemagia        = %s
  | id_cinemarx         = 
  | imdb_id             = %s
  | id_rotten-tomatoes  =
  | id_allrovi          =
}}
""" % (self._director, self._scenario, actors, sf.none2empty(self._cmId), sf.none2empty(self._imdbId))
		self._text += text
		
	def addMainArticle(self):
		actors_list = u""
		for actor in self._distribution:
			actors_list += u"* [[%s]] ca %s\n" % (self._distribution[actor], actor)
		text = u"""
'''''{{PAGENAME}}''''' este un film românesc din [[%d]] regizat de %s. Rolurile principale au fost interpretate de actorii %s.

==Prezentare==
{{sinopsis}}
==Distribuție==
{{coloane-listă|2|
%s
 }}
""" % (self._year, self._director, self._mainActors, actors_list)
		self._text += text

	def imdbWikiText(self):
		if self._imdbId:
			return u"* {{Titlu IMDb|%s}} {{rating 10|%.1f}}\n" % (self._imdbId, self._imdbRating)
		return u""

	def cmWikiText(self):
		if self._cmId:
			return u"* {{cinemagia|id=%s}} {{rating 10|%.1f}}\n" % (self._cmId, self._cmRating)
		return u""


	def addCommonSections(self):
		text = u"""
==Note==
{{Listănote|2}}

==Legături externe==
%s%s

==Vezi și==
* [[%d în film]]

{{Ciot-film-România}}
""" % (self.imdbWikiText(), self.cmWikiText(), self._year)
		self._text += text

	def addCats(self):
		text = u"""
[[Categorie:Filme românești]]
[[Categorie:Filme în limba română]]
"""
		if self._year:
			text += u"[[Categorie:Filme din %d]]\n" % self._year
		if self._director:
			directors = self._director.split(",")
			for director in directors:
				text += u"[[Categorie:Filme regizate de %s]]\n" % director.strip()
		self._text += text

if __name__ == "__main__":
	a = Article("https://www.cinemagia.ro/filme/aripi-de-zapada-27066/", "http://aarc.ro/filme/film/aripi-de-zapada-1985")
	a = Article("N/A", "http://aarc.ro/filme/film/abecedarul-1984")
	a.fetchAarc()
	a.fetchCm()
	a.buildArticle()
