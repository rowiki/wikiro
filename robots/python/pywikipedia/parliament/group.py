#!/usr/bin/python
# -*- coding: utf-8 -*-

from BeautifulSoup import BeautifulSoup
import requests
import re

import pywikibot

parties = {
	u"Grupul parlamentar al Partidului Democrat Liberal": u"[[Partidul Democrat Liberal (România)|Partidul Democrat Liberal]]",
	u"Grupul parlamentar al Partidului Social Democrat": u"[[Partidul Social Democrat (România)|Partidul Social Democrat]]",
	u"Grupul parlamentar al minorităţilor naţionale": u"grupul minorităților naționale",
	u"Grupul parlamentar al Partidului Naţional Liberal": u"[[Partidul Național Liberal (România)|Partidul Național Liberal]]",
	u"Grupul parlamentar al Uniunii Democrate Maghiare din România": u"[[Uniunea Democrată Maghiară din România]]",
	u"Grupul parlamentar Liberal Conservator (PC-PLR)": u"[[Partidul Alianța Liberalilor și Democraților]]", 
	u"Grupul parlamentar Democrat şi Popular": u"grupul parlamentar Democrat şi Popular",
	u"Deputaţi neafiliaţi": None,

}

class GroupMembership(object):
	def __init__(self):
		self._name = None
		self.party = None
		self.start = None
		self.end = None

	@property
	def name(self):
		return self._name

	@name.setter
	def name(self, value):
		if value in parties:
			self.party = parties[value]
		else:
			self.party = None
		self._name = value
