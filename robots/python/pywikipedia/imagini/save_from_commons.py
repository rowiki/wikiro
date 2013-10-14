#!/usr/bin/python
# -*- coding: utf-8  -*-
"""
This script retrieves images from commons and saves them locally,
allong with the corresponding wikitext.

The pages to be saved are read from a text file given as the "file" parameter.

Usage: python save_from_commons.py -lang:commons -family:commons -file:a.txt
"""
import os
import urllib2
import codecs

import pywikibot
from pywikibot import pagegenerators

def processFile(name):
	site = pywikibot.getSite()
	filteredGen = pagegenerators.TextfilePageGenerator(name, site)
	pregenerator = pagegenerators.PreloadingGenerator(filteredGen, 125)
	for page in pregenerator:
		imagepage = pywikibot.ImagePage(page)
		title = imagepage.title()
		pywikibot.output(u"Working on " + title)
		pywikibot.output(u"Getting text...")
		text = imagepage.get()
		url = imagepage.fileUrl()
		pywikibot.output(u"Saving text...")
		f = codecs.open(u"dir_" + name + "/" + title + u".wiki.txt", "w+", 'utf-8')
		f.write(text)
		f.close()
		pywikibot.output(u"Getting image...")
		response = urllib2.urlopen(url)
		jpg = response.read()
		pywikibot.output(u"Saving image...")
		f = open(u"dir_" + name + "/" + title, "wb+")
		f.write(jpg)
		f.close()

def main():
	file_to_work = u""
	for arg in pywikibot.handleArgs():
		if arg.startswith('-file'):
			file_to_work = arg [len('-file:'):]
			
	os.mkdir(u"dir_" + file_to_work)
	processFile(file_to_work)

if __name__ == "__main__":
	main()
