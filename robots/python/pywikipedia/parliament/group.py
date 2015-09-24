#!/usr/bin/python
# -*- coding: utf-8 -*-

from BeautifulSoup import BeautifulSoup
import requests
import re

import pywikibot
import parliament

parties = {
	u"Grupul parlamentar al Partidului Democrat Liberal": (u"[[Partidul Democrat Liberal (România)|Partidul Democrat Liberal]]", u"[[Partidul Democrat Liberal (România)|Partidului Democrat Liberal]]"),
	u"Grupul parlamentar al Partidului Social Democrat": (u"[[Partidul Social Democrat (România)|Partidul Social Democrat]]", u"[[Partidul Social Democrat (România)|Partidului Social Democrat]]"),
	u"Grupul parlamentar al minorităților naționale": (u"minorități", u"minorităților naționale"),
	u"Grupul parlamentar al Partidului Național Liberal": (u"[[Partidul Național Liberal (România)|Partidul Național Liberal]]", u"[[Partidul Național Liberal (România)|Partidului Național Liberal]]"),
	u"Grupul parlamentar al Uniunii Democrate Maghiare din România": (u"[[Uniunea Democrată Maghiară din România|Uniunea Democrată Maghiară din România]]", u"[[Uniunea Democrată Maghiară din România|Uniunii Democrate Maghiare din România]]"),
	u"Grupul parlamentar Liberal Conservator (PC-PLR)": (u"[[Partidul Alianța Liberalilor și Democraților|Partidul Alianța Liberalilor și Democraților]]", u"[[Partidul Alianța Liberalilor și Democraților|Partidului Alianța Liberalilor și Democraților]]"), 
	u"Grupul parlamentar Democrat şi Popular": (u"grupul parlamentar Democrat şi Popular", u"grupului parlamentar Democrat şi Popular"),
	u"Deputați neafiliați": (u"independent", u"independent"),

}

class GroupMembership(object):
	def __init__(self):
		self._name = None
		self.party = None
		self.start = None
		self.end = None

	@property
	def name(self):
		return self._name[0].lower() + self._name[1:]

	@name.setter
	def name(self, value):
		if value in parties:
			self.party = parties[value]
		else:
			print "Could not find %s" % value
			self.party = None
		self._name = value

	def interval(self):
		if self.start == None:
			return u""
		if self.end == None:
			return u" (din %s)" % parliament.niceDate(self.start)
		return u" (între %s și %s)" %(parliament.niceDate(self.start), parliament.niceDate(self.end))
