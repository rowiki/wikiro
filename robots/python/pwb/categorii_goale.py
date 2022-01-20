#!/usr/bin/python
# -*- coding: utf-8  -*-

"""This script is used to find empty categories on ro.wp
"""

import pywikibot
from pywikibot import pagegenerators
import json
import re
import sys
import codecs

sys.path.append('.')
import strainu_functions
import csvUtils

def main():
	site = pywikibot.Site()
	"""
	gen = pagegenerators.AllpagesPageGenerator("!", 14, includeredirects=False, site=site)
	empty = 0
	total = 0
	"""
	gen = pagegenerators.AllpagesPageGenerator("Medicină_prenatam", 14, includeredirects=False, site=site)
	empty = 23068
	total = 72000
	for page in gen:
		#print page
		total += 1
		if page.isEmptyCategory():
			#print page
			empty += 1
		if total % 1000 == 0:
			print page
			print "%d/%d (%f%%)" % (empty, total, empty * 100.0 / total)
	print "%d/%d (%f%%)" % (empty, total, empty * 100.0 / total)

if __name__ == "__main__":
	main()
