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
		begin = u""
		end = u""
		district = self.district
		party = u""
		if len(self.groups):
			party = self.groups[-1].party
		
		return text % (self.name, function, begin, end, district, party, parliament.niceDate(self.birthdate))
