#!/usr/bin/python
# -*- coding: utf-8  -*-
"""
This file contains misc functions useful when working with Romanian cities

"""

#
# (C) Strainu, 2010
#
# Distributed under the terms of the MIT license.
#

import csv
import sys
import json

import sirutalib


class PostalCodes(object):
	_dict = {}
	_cods = u"Codpostal"
	_countys = u"Judet"
	_citys = u"Localitate"
	_siruta = None

	def __init__(self, bucharest_file, big_file, small_file):
		with open(bucharest_file, 'r') as f:
			key = 19660
			self._dict[key] = [sys.maxint, 0]
			reader = csv.DictReader(f)
			for r in reader:
				if self._cods in r:
					cp = int(r[self._cods])
					if cp < self._dict[key][0]:
						self._dict[key][0] = cp
					if cp > self._dict[key][1]:
						self._dict[key][1] = cp
		with open(big_file, 'r') as f:
			reader = csv.DictReader(f)
			for r in reader:
				for siruta in [u"SIRUTA", u"SIRSUP"]:
					key = int(r[siruta] or u'0')
					if key not in self._dict:
						self._dict[key] = [sys.maxint, 0]
					if self._cods in r:
						cp = int(r[self._cods])
						if cp < self._dict[key][0]:
							self._dict[key][0] = cp
						if cp > self._dict[key][1]:
							self._dict[key][1] = cp

		with open(small_file, 'r') as f:
			reader = csv.DictReader(f)
			for r in reader:
				key = int(r[u"SIRUTA"] or u'0')
				if self._cods in r:
					cp = int(r[self._cods])
					self._dict[key] = [cp, cp]
		#print(json.dumps(self._dict, indent=2))

	def toString(self, list):
		if not list:
			return u""
		if list[0] == list[1]:
			return u"%06d" % list[0]
		return u"%06d–%06d" % (list[0], list[1])

	def getFromSIRUTA(self, sirutaCode):
		"""Get the postal code for a SIRUTA code"""
		return self.toString(self._dict.get(sirutaCode))

	def getFromWikipediaTitle(self, title):
		"""Get the postal code starting from a Wikipedia article title"""
		raise NotImplementedError()

	def getFromWikidataItem(self, q):
		"""Get the postal code starting from a Wikipedia article title"""
		raise NotImplementedError()


if __name__ == "__main__":
	p = PostalCodes("codp_B.csv","codp_50k.csv","codp_1k.csv")
	print p.getFromSIRUTA(19660)
	print p.getFromSIRUTA(86687)
	print p.getFromSIRUTA(86696)
	print p.getFromSIRUTA(160270)
	print p.getFromSIRUTA(154718)
