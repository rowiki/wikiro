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
	1: u"[[Senatul României|Senator]]",
	2: u"[[Camera Deputaților din România|Deputat]]",
}


class ElectedPerson(object):
	def __init__(self):
		self.birthdate = None
		self.groups = []
		self.chamber = 0
		self.district = u""
		self.name = u""

	def addGroup(self, group):
		self.groups.append(group)

	def endLastGroup(self, endDate):
		#TODO error checking
		self.groups[-1].end = endDate

	def generateInfobox(self):
		text = u"""{{Infocaseta Om politic
| nume = %s
| functia = %s
| inceput = %s
| sfarsit = %s
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
			party = self.groups[-1].party
		
		return text % (self.name, function, begin, end, district, party, parliament.niceDate(self.birthdate))

	def generateArticle(self):
		firstParagraph = """'''{{subst:PAGENAME}}''' este un %s român, ales în %d din partea %s. %s

"""
		mandateEnd = """Mandatul său a încetat pe %s.

"""
		if len(self.groups) > 3:
			groups = """În timpului mandatului, a făcut parte din următoarele grupuri parlamentare: %s, %s, %s și %s

"""
		elif len(self.groups) > 2:
			groups = """Pe %s a trecut în %s, apoi s-a mutat în %s (pe data de %s).

"""
		elif len(self.groups) > 1:
			groups = """În timpului mandatului, a făcut parte din următoarele grupuri parlamentare: %s, %s, %s, %s

"""
