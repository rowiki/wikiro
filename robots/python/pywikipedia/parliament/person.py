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

category = {
	1: u"[[Categorie:Senatori români 2012-2016]]",
	2: u"[[Categorie:Deputați români 2012-2016]]",
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

	def addGroup(self, group):
		self.groups.append(group)

	def endLastGroup(self, endDate):
		#TODO error checking
		self.groups[-1].end = endDate

	def generateInfobox(self):
		text = u"""{{Infocaseta Om politic
| nume = %s
| functia = %s
| început = %s
| sfârșit = %s
| circumscripția = %s
| partid = %s
| data_nașterii = %s
}}
"""
		function = u""
		if self.chamber in chambers:
			function = chambers[self.chamber]
		begin = parliament.niceDate("2012-12-19")
		if len(self.groups) > 0:
			begin = parliament.niceDate(self.groups[0].start)
		end = u""
		if len(self.groups) > 0 and self.groups[-1].end != None:
			end = parliament.niceDate(self.groups[-1].end)
		district = self.district
		party = u""
		if len(self.groups):
			party = self.groups[-1].party[0]
		
		return text % (self.name, function, begin, end, district, party, parliament.niceDate(self.birthdate))
	
	def generateCategory(self):
		if self.chamber in category:
			return category[self.chamber]
		return u""

	def generateArticle(self):
		firstParagraph = u"""'''%s''' (n. {{Data nașterii|%s}}) este un %s român, ales în %d%s%s. %s

"""
		mandateEnd = u"""Mandatul său a încetat pe %s."""
		otherSections = u"""== Vezi și ==
* [[Legislatura 2012-2016 (Camera Deputaților)]]
* [[Parlamentul României]]

== Legături externe ==
* [http://www.cdep.ro/pls/parlam/structura2015.mp?idm=%s&leg=2012&cam=%s&idl=1 Activitatea parlamentară]

"""
		text = self.generateInfobox()
		function = u""
		if self.chamber in chambers:
			function = chambers[self.chamber]
		begin = 2012
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
			grouptext = u"""Pe %s a trecut în %s, apoi s-a mutat în %s (pe data de %s).

""" % (parliament.niceDate(self.groups[1].start), self.groups[1].name, self.groups[2].name, parliament.niceDate(self.groups[2].start))
		elif len(self.groups) > 1:
			grouptext = u"""În timpului mandatului a trecut la %s.

""" % (self.groups[1].name)
		else:
			grouptext = u""
		text = text + grouptext
		text = text + otherSections % (self.index, self.chamber)
		text = text + u"{{DEFAULTSORT:{{subst:swap2|{{subst:PAGENAME}}}}}}\n"
		text = text + self.generateCategory()
		return text
