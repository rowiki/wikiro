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
import csvUtils


def fixDia(text):
	return text.replace(u"Ş", u"Ș").replace(u"ş", u"ș").replace(u"Ţ", u"Ț").replace(u"ţ", u"ț")


class Article:
	def __init__(self, title, cinemagia_url, aarc_url):
		self._text  = u""
		self._title = fixDia(title)
		self._cinemagia = cinemagia_url
		self._aarc = aarc_url
		self._year = 0
		self._cmId = None
		self._cmRating = 0
		self._imdbId = None
		self._imdbRating = 0
		self._types = []
		self._duration = None
		self._page = pywikibot.Page(pywikibot.getSite(), self._title)

	def fetchAarc(self):
		r = requests.get(self._aarc)
		text = self.fetchAarcData(r.text)
		self._title = self.fetchAarcTitle(text)
		self._year = self.fetchAarcYear(text)
		self._director = self.fetchAarcEntry(text, "Regie")
		self._scenario = self.fetchAarcEntry(text, "Scenariu")
		self._mainActors = self.fetchAarcEntry(text, "Actori")
		self._distribution = self.fetchAarcDistribution(r.text)

	def fetchAarcTitle(self, text):
		text = text[text.find("<h1>")+4:text.find("</h1>")]
		text = text[:text.find("(")].strip()
		return fixDia(text)

	def fetchAarcEntry(self, text, entry):
		text = text[text.find(entry):]
		text = text[text.find("</span>")+len("</span>"):]
		text = text[:text.find("</p>")]
		return fixDia(text.strip())

	def fetchAarcData(self, text):
		index1 = text.find("<div class=\"mainfilminfo\">")
		index2 = text.find("main film info", index1)
		text = text[index1:index2+6]
		return text

	def fetchAarcYear(self, text):
		m = re.search("<h1>(.*)\((\d*)\)</h1>", text)
		if m:
			return int(m.group(2))
		return 0

	def fetchAarcDistribution(self, text):
		distribution = {}
		index = 0
		r = text.find("http://aarc.ro/filme/distributie/")
		if r > -1:
			text = text[r:]
			text = text[:text.find("\"")]
			r = requests.get(text)
			text = r.text
		text = text[text.find(u"<h3>Distribuție"):]
		text = text[text.find("<tbody"):].strip()
		text = text[text.find(">"):].strip()
		text = text[:text.find("</tbody>")]
		entries = text.split("</tr>")
		if len(entries) == 1:
			return distribution
		for entry in entries:
			entry = entry.strip()
			if not entry:
				break
			(pic, name, role) = entry.split("<td>")
			name = name[:name.find("</td")].strip()
			m = re.search(ur"[^>]*(?=<\/a>)", name)
			if m:
				name = m.group(0)
			role = role[:role.find("</td")].strip()
			if role == u"":
				role = str(index)
				index += 1
			distribution[role] = fixDia(name)
		return distribution

	def fetchCm(self):
		m = re.search("https\:\/\/www\.cinemagia\.ro\/filme\/.*\-(\d*)\/", self._cinemagia)
		if m:
			self._cmId = m.group(1)
		else:
			print "CM not found"
			return # no need to parse if we cannot match the URL
		r = requests.get(self._cinemagia)
		text = r.text
		self._cmRating = self.fetchCmRating(r.text)
		self._imdbId = self.fetchImdbId(r.text)
		self._imdbRating = self.fetchImdbRating(r.text)
		self._year = self.fetchCmYear(r.text)
		self._types = self.fetchCmTypes(r.text)
		self._duration = self.fetchCmDuration(r.text)

	def fetchCmDuration(self, text):
		r = text.find(u"<h3>Durata")
		if r > -1:
			text = text[r:]
			m = re.search(ur"[^>]*(?=<\/span>)", text)
                        if m:
                                return m.group(0)
		return None

	def fetchCmTypes(self, text):
		r = text.find("movieGenreUserChoiceResults")
		types = []
		if r > -1:
			text = text[r:]
			text = text[:text.find("</div>")]
			m = re.finditer(ur"<span>(.*?)</span>", text)
                        for t in m:
                                types.append(t.group(1))
		return types

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
				try:
					return float(score)
				except:
					return 0
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
		if self._page.exists():
			print u"există"
			return False
		self._text = u""
		self.fetchAarc()
		self.fetchCm()
		self.addInfobox()
		self.addMainArticle()
		self.addReception()
		self.addCommonSections()
		self.addCats()
		return True

	def addInfobox(self):
		actors = self._mainActors.replace(u",", u"<br/>")
		text = u"""{{Infocaseta Film
  | nume_film           =  %s
  | alte_nume           = 
  | imagine             = 
  | descriere_imagine   = Afișul filmului  
  | rating              =  
  | gen                 = %s
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
  | durată              = %s
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
""" % (self._title, u", ".join(self._types), self._director, self._scenario, actors, sf.none2empty(self._duration), sf.none2empty(self._cmId), sf.none2empty(self._imdbId))
		self._text += text
		
	def addMainArticle(self):
		actors_list = u""
		for actor in self._distribution:
			if len(actor) > 2:
				actors_list += u"* [[%s]] &mdash; %s\n" % (self._distribution[actor], actor)
			else:
				actors_list += u"* [[%s]]\n" % self._distribution[actor]
		if len(self._mainActors):
			mainActors = u" Rolurile principale au fost interpretate de actorii " + self._mainActors + u"."
		else:
			mainActors = u""
		self._text += u"""
'''''%s''''' este un film românesc din [[%d]] regizat de %s.%s

==Prezentare==
{{sinopsis}}

==Distribuție==
{{coloane-listă|2|
%s
 }}
""" % (self._title, self._year, self._director, mainActors, actors_list)

	def addReception(self):
		spectators = 0
		out = csvUtils.csvToJson("filme.csv", field=u"film")
		title = self._title.upper().replace(u"Â", u"A").replace(u"Ă", u"A").replace(u"Ș", u"S").replace(u"Ț", u"T").replace(u"Î", u"I")
		if title in out:
			spectators += int(out[title][u"spectatori"])
		if spectators == 0:
			return
		self._text += u"""

==Primire==
Filmul a fost vizionat de {{subst:plural|%d|spectator}} de spectatori în cinematografele din România, după cum atestă o situație a numărului de spectatori înregistrat de filmele românești de la data premierei și până la data de 31 decembrie 2014 alcătuită de [[Centrul Național al Cinematografiei]].<ref>{{cite web|url=http://cnc.gov.ro/wp-content/uploads/2015/10/6_Spectatori-film-romanesc-la-31.12.2014.pdf |title=Situația numărului de spectatori înregistrat de filmele românești ieșite în premieră până la 31.12.2014|date=2014-12-31|format=PDF|publisher=Centrul Național al Cinematografiei|accessdate=2017-01-29}}</ref>
""" % spectators

	def imdbWikiText(self):
		if self._imdbId:
			return u"* {{Titlu IMDb|%s}} {{rating 10|%.1f}}\n" % (self._imdbId, self._imdbRating)
		return u""

	def cmWikiText(self):
		if self._cmId:
			return u"* {{cinemagia|id=%s}} {{rating 10|%.1f}}\n" % (self._cmId, self._cmRating)
		return u""


	def addCommonSections(self):
		_directorTl = u""
		if self._director:
			directors = self._director.split(",")
			for director in directors:
				f = pywikibot.Page(pywikibot.getSite(), u"Format:" + director.strip())
				if f.exists():
					_directorTl += u"{{" + director.strip() + u"}}\n"
		text = u"""
==Note==
{{Listănote|2}}

==Legături externe==
%s%s

==Vezi și==
* [[%d în film]]
%s
{{Ciot-film-România}}
""" % (self.imdbWikiText(), self.cmWikiText(), self._year, _directorTl)
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
		for t in self._types:
			cat = u"Filme de %s" % t.lower()
			cat = pywikibot.Category(pywikibot.getSite(), cat)
			if cat.exists():
				for p in cat.templatesWithParams():
					if p[0].title() == "Format:Redirect categorie":
						break
				else:
					text += u"[[%s]]\n" % cat.title()
		self._text += text

if __name__ == "__main__":
	#a = Article("https://www.cinemagia.ro/filme/aripi-de-zapada-27066/", "http://aarc.ro/filme/film/aripi-de-zapada-1985")
	#a = Article("N/A", "http://aarc.ro/filme/film/abecedarul-1984")
	#a = Article(u"Această Lehamite", "https://www.cinemagia.ro/filme/aceasta-lehamite-229/", "http://aarc.ro/filme/film/aceasta-lehamite-1993")
	
	flist = csvUtils.csvToJson("filme_url.csv", field=u"titlu articol")
	count = 0
	for f in flist:
		print flist[f]
		a = Article(f.decode('utf8'), flist[f][u"url cinemagia"], flist[f]["url aarc"])
		if a.buildArticle():
			count += 1
			a._page.put(a._text)
		if count and count % 10 == 0:
			break
