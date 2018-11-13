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

sys.path.append("wikiro/robots/python/pywikipedia")
import strainu_functions as sf
import csvUtils


def fixDia(text):
	return text.strip().replace(u"Ş", u"Ș").replace(u"ş", u"ș").replace(u"Ţ", u"Ț").replace(u"ţ", u"ț")

categories = {
	u"Filme de documentar": u"Filme documentare românești",
	u"Filme de comedie": u"Filme de comedie românești",
	u"Filme de dramă": u"Filme dramatice românești",
	u"Filme de horror": u"Filme de groază",
	u"Filme de polițist": u"Filme polițiste românești",
	#u"": u"",
}


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
		self._distributionSource = u""
		if self._title:
			self._page = pywikibot.Page(pywikibot.getSite(), self._title)
			self._valid = True
		else:
			print(u"Invalid title")
			self._valid = False

	def fetchAarc(self):
		try:
			r = requests.get(self._aarc)
			text = self.fetchAarcData(r.text)
			self._title = self.fetchAarcTitle(text)
			self._year = self.fetchAarcYear(text)
			self._director = self.fetchAarcEntry(text, "Regie")
			self._scenario = self.fetchAarcEntry(text, "Scenariu")
			self._mainActors = self.fetchAarcEntry(text, "Actori")
			self._producer = self.fetchAarcSection(r.text, u"Producători")
			self._distributor = self.fetchAarcSection(r.text, u"Distribuitori")
			self._distribution = self.fetchAarcDistribution(r.text)
			self._distributionSource = u"<ref name=\"distributie\">[" + self._aarc + u" " + self._title + u"] pe site-ul All About Romanian Cinema</ref>"
		except Exception as e:
			#without AARC data, no article
			#print e
			print(u"AARC not found")
			self._valid = False

	def fetchAarcTitle(self, text):
		text = text[text.find("<h1>")+4:text.find("</h1>")]
		text = text[:text.find("(")].strip()
		return fixDia(text)

	def fetchAarcEntry(self, text, entry):
		text = text[text.find(entry):]
		text = text[text.find("</span>")+len("</span>"):]
		text = text[:text.find("</p>")]
		return fixDia(text)

	def fetchAarcData(self, text):
		index1 = text.find("<div class=\"mainfilminfo\">")
		index2 = text.find("main film info", index1)
		text = text[index1:index2+6]
		return fixDia(text)

	def fetchAarcYear(self, text):
		m = re.search("<h1>(.*)\((\d*)\)</h1>", text)
		if m:
			return int(m.group(2))
		return 0

	def fetchAarcSection(self, text, section):
		search = r"<h3.*>%s" % section
		
		r = re.search(search, text)
		if r:
			text = text[text.find(r.group(0)):]
			m = re.search(r"<p>(.*?)</p>", text)
			if m:
				return m.group(1)
		return ""
		

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
			m = re.search(r"[^>]*(?=<\/a>)", name)
			if m:
				name = m.group(0)
			role = fixDia(role[:role.find("</td")])
			if role == u"":
				role = str(index)
				index += 1
			distribution[role] = fixDia(name)
		return distribution

	def fetchCm(self):
		m = re.search("https\:\/\/www\.cinemagia\.ro\/filme\/.*\-(\d*)\/", self._cinemagia)
		if m:
			self._cmId = m.group(1)
			self._valid = False
			return
		else:
			print("CM not found")
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
			m = re.search(r"[^>]*(?=<\/span>)", text)
			if m:
				return m.group(0)
		return None

	def fetchCmTypes(self, text):
		r = text.find("movieGenreUserChoiceResults")
		types = []
		if r > -1:
			text = text[r:]
			text = text[:text.find("</div>")]
			m = re.finditer(r"<span>(.*?)</span>", text)
			for t in m:
				types.append(fixDia(t.group(1)))
		return types

	def fetchCmRating(self, text):
		r = text.find("ratingGlobalInfo")
		if r > -1:
			text = text[r:]
			text = text[:text.find("</div>")]
			m = re.search(r"[^>]*(?=<\/span>)", text)
			if m:
				return float(m.group(0))
		return 0

	def fetchCmYear(self, text):
		r = text.find("link1")
		if r > -1:
			text = text[r:]
			m = re.search(r"(?<=\()[^>]*(?=\)<\/a>)", text)
			if m:
				return int(m.group(0))
		return 0

	def fetchImdbRating(self, text):
		r = text.find("imdb-rating")
		if r > -1:
			text = text[r:]
			m = re.search(r"[^>]*(?=<\/a>)", text)
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
		if self._valid and self._page.exists():
			print(u"există")
			return False
		self._text = u""
		self.fetchAarc()
		self.fetchCm()
		if not self._valid:
			print("invalid")
			return False
		self.addInfobox()
		self.addMainArticle()
		self.addDistribution()
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
  | producător          = %s
  | scenarist           = %s
  | narator             = 
  | rolurile_principale = %s
  | muzica              = 
  | dir_imag            = 
  | montaj              = 
  | studio              = 
  | distribuitor        = %s
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
""" % (self._title, u", ".join(self._types), self._director, self._producer, self._scenario, actors, self._distributor, sf.none2empty(self._duration), sf.none2empty(self._cmId), sf.none2empty(self._imdbId))
		self._text += text
		
	def addMainArticle(self):
		if len(self._mainActors):
			mainActors = u" Rolurile principale au fost interpretate de actorii " + self._mainActors + u"."
		else:
			mainActors = u""
		self._text += u"""
'''''%s''''' este un film românesc din [[%d]] regizat de %s.%s

==Prezentare==
{{sinopsis}}
""" % (self._title, self._year, self._director, mainActors)

	def addDistribution(self):
		if len(self._distribution) == 0:
			return
		actors_list = u""
		for actor in self._distribution:
			if len(actor) > 2:
				actors_list += u"* [[%s]] &mdash; %s\n" % (self._distribution[actor], actor)
			else:
				actors_list += u"* [[%s]]\n" % self._distribution[actor]
		self._text +=u"""

==Distribuție==
Distribuția filmului este alcătuită din:%s
{{coloane-listă|2|
%s
 }}
""" % (self._distributionSource, actors_list)

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
Filmul a fost vizionat de {{subst:plural|%d|spectator}} în cinematografele din România, după cum atestă o situație a numărului de spectatori înregistrat de filmele românești de la data premierei și până la data de 31 decembrie 2014 alcătuită de [[Centrul Național al Cinematografiei]].<ref>{{cite web|url=http://cnc.gov.ro/wp-content/uploads/2015/10/6_Spectatori-film-romanesc-la-31.12.2014.pdf |title=Situația numărului de spectatori înregistrat de filmele românești ieșite în premieră până la 31.12.2014|date=2014-12-31|format=PDF|publisher=Centrul Național al Cinematografiei|accessdate=2017-01-29}}</ref>
""" % spectators

	def imdbWikiText(self):
		if self._imdbId:
			if self._imdbRating:
				return u"* {{Titlu IMDb|%s}} {{rating 10|%.1f}}\n" % (self._imdbId, self._imdbRating)
			else:
				return u"* {{Titlu IMDb|%s}}\n" % self._imdbId
		return u""

	def cmWikiText(self):
		if self._cmId:
			if self._cmRating:
				return u"* {{cinemagia|id=%s}} {{rating 10|%.1f}}\n" % (self._cmId, self._cmRating)
			else:
				return u"* {{cinemagia|id=%s}}\n" % self._cmId
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
<references />

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
				cat = u"Categorie:Filme regizate de %s" % director.strip()
				cat = pywikibot.Category(pywikibot.getSite(), cat)
				if cat.exists():
					text += u"[[Categorie:Filme regizate de %s]]\n" % director.strip()
		for t in self._types:
			cat = u"Filme de %s" % t.lower()
			catp = None
			if cat in categories:
				catp = pywikibot.Category(pywikibot.getSite(), categories[cat])
			if not catp or not catp.exists():
				catp = pywikibot.Category(pywikibot.getSite(), cat)

			if catp.exists():
				for p in catp.templatesWithParams():
					if p[0].title() == "Format:Redirect categorie":
						break
				else:
					text += u"[[%s]]\n" % catp.title()
		self._text += text

if __name__ == "__main__":
	flist = csvUtils.csvToJson("filme_url.csv", field=u"titlu articol")
	count = 0
	for f in flist:
		print(flist[f])
		a = Article(f, flist[f][u"url cinemagia"], flist[f]["url aarc"])
		if a.buildArticle():
			count += 1
			print(a._text)
			answer = pywikibot.inputChoice("Upload?", ['Yes', 'No'], ["y", "n"])
			if answer == 'y':
				a._page.put(a._text, u"Creez un articol nou despre un film")
		#if count % 10 == 9:
		#	break
