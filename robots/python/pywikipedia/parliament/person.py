#!/usr/bin/python
# -*- coding: utf-8 -*-

from BeautifulSoup import BeautifulSoup
import requests
import re

import group

import parliament
import parliament.group
import pywikibot

chambers = {
	1: u"[[Senatul României|senator]]",
	2: u"[[Camera Deputaților din România|deputat]]",
}

chamber_link = {
	1: u"[[Legislatura 2016-2020 (Senat)]]",
	2: u"[[Legislatura 2016-2020 (Camera Deputaților)]]",
}

category = {
	1: u"Categorie:Senatori români %s-%s",
	2: u"Categorie:Deputați români %s-%s",
}


class ElectedPerson(object):
	def __init__(self):
		self.birthdate = None
		self.groups = []
		self.chamber = 0
		self.district = u""
		self.name = u""
		self.wiki = u""
		self.index = -1
		self.legislatures = {}

	def addGroup(self, group):
		self.groups.append(group)

	def endLastGroup(self, endDate):
		#TODO error checking
		self.groups[-1].end = endDate

	def generateInfobox(self):
		elections = [u"2016", u"2012", u"2008", u"2004", u"2000", u"1996", u"1992", u"1990"]
		text = u"""{{Infocaseta Om politic
| nume = %s
| functia = %s
| început = %s
| sfârșit = %s
| circumscripția = %s
| partid = %s
| data_nașterii = %s
"""
		function = u""
		if self.chamber in chambers:
			function = chambers[self.chamber].replace(u'|d', u'|D').replace(u'|s', u'|S')
		begin = u"2016-12-21"
		if len(self.groups) > 0:
			begin = self.groups[0].start
		for y in range(1, len(elections)):
			year = elections[y]
			if year in self.legislatures and self.chamber == self.legislatures[year]:
				begin = year
			else:
				break
		end = u""
		if len(self.groups) > 0 and self.groups[-1].end != None:
			end = parliament.niceDate(self.groups[-1].end)
		district = self.district
		party = u""
		if len(self.groups):
			party = self.groups[-1].party[0]
		birthdate = self.birthdate.replace(u"-", u"|")
		birthdate = u"{{Data nașterii și vârsta|" + birthdate + u"}}"

		text = text % (self.name, function, parliament.niceDate(begin), end, district, party, birthdate)
		index = 2
		print begin
		t = u""
		last_chamber = 0
		for y in range(1, len(elections)):
			year = elections[y]
			if int(year) >= int(begin[:4]):
				continue
			if year not in self.legislatures:
				last_chamber = 0
				continue
			if self.legislatures[year] == last_chamber:
				t = t.replace(elections[y-1], year)
			else:
				text += t
				t = u"""| functia%d = %s
| început%d = %s
| sfârșit%d = %s
""" % (index, chambers[self.legislatures[year]].replace(u'|d', u'|D').replace(u'|s', u'|S'), index, parliament.niceDate(year), index, parliament.niceDate(elections[y-1]))
				index += 1
			last_chamber = self.legislatures[year]
		text += t
		text += u"}}\n"
		return text
	
	def generateCategoriesList(self):
		elections = [u"2016", u"2012", u"2008", u"2004", u"2000", u"1996", u"1992", u"1990"]
		cat = [u"Categorie:Politicieni români în viață"]
		if self.chamber in category:
			cat.append(category[self.chamber] % (u"2016", u"2020"))
		for y in range (1, len(elections)):
			year = elections[y]
			if year in self.legislatures:	
				cat.append(category[self.legislatures[year]] % (year, elections[y-1]))
		print cat
		return cat

	def generateCategories(self):
		catl = self.generateCategoriesList()
		cats = u""
		for cat in catl:
			cats += u"[[" + cat + u"]]\n"
		return cats

	def generateArticle(self):
		firstParagraph = u"""'''%s''' (n. {{Data nașterii|%s}}) este un %s român, ales în %d%s%s. %s

"""
		mandateEnd = u"""Mandatul său a încetat pe %s."""
		otherSections = u"""== Vezi și ==
* %s
* [[Parlamentul României]]

== Legături externe ==
* [http://www.cdep.ro/pls/parlam/structura2015.mp?idm=%s&leg=2016&cam=%s&idl=1 Activitatea parlamentară]

"""
		text = self.generateInfobox()
		function = u""
		if self.chamber in chambers:
			function = chambers[self.chamber]
		begin = 2016
		group = u""
		groupprefix = u""
		if len(self.groups) > 0:
			begin = int(self.groups[0].start[0:4])
			group = self.groups[0].party[1]
			groupprefix = u" din partea "
		end = u""
		if len(self.groups) > 0 and self.groups[-1].end != None:
			end = mandateEnd % parliament.niceDate(self.groups[-1].end)
		#print end
		if function != u"" and end != u"":
			function = u"fost " + function
		birthdate = self.birthdate.replace(u"-", u"|")
		text = text + firstParagraph % (self.name, birthdate, function, begin, groupprefix, group, end)

		if len(self.groups) > 3:
			grouptext = u"În timpului mandatului, a făcut parte din următoarele grupuri parlamentare: "
			for group in self.groups:
				grouptext += u"%s%s" % (group.name, group.interval())
				if group == self.groups[-1]:
					grouptext += u"."
				elif group == self.groups[-2]:
					grouptext += u" și "
				else:
					grouptext += u", "
			grouptext += "\n\n"
		elif len(self.groups) > 2:
			grouptext = u"""Pe %s a trecut la %s, apoi s-a mutat în %s (pe data de %s).

""" % (parliament.niceDate(self.groups[1].start), self.groups[1].name, self.groups[2].name, parliament.niceDate(self.groups[2].start))
		elif len(self.groups) > 1:
			grouptext = u"""În timpului mandatului a trecut la %s.

""" % (self.groups[1].name)
		else:
			grouptext = u""
		text = text + grouptext
		text = text + otherSections % (chamber_link[self.chamber], self.index, self.chamber)
		text = text + u"{{DEFAULTSORT:{{subst:swap2|{{subst:PAGENAME}}}}}}\n"
		text = text + self.generateCategories()
		return text
