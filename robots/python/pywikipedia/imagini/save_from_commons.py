#!/usr/bin/python
# -*- coding: utf-8  -*-
"""
This script retrieves images from commons and saves them locally,
allong with the corresponding wikitext.

The pages to be saved are read from a text file given as the "file" parameter.

Usage: python save_from_commons.py -lang:commons -family:commons -cfg:[lmiro|lmicommons|default]
"""
import os
import urllib2
import codecs
from os import listdir
from os.path import isfile, join

import pywikibot
from pywikibot import pagegenerators
from pywikibot import config as user

config = {
	'lmiro': {
		'file': u"rofiles.txt",
		'dir': u"/mnt/jacob/filme/dir_rofiles.txt/",
		'lang': u"ro",
		'family': u"wikipedia",
	},
	'lmicommons': {
		'file': u"monumentfiles.txt",
		'dir': u"/mnt/jacob/filme/monumentimages/",
		'lang': u"commons",
		'family': u"commons",
	},
	'default': {
		'file': u"downloadlist.txt",
		'dir': u"img_download",
		'lang': u"commons",
		'family': u"commons",
	},
}

def deletedFiles(name, destdir):
	lines = codecs.open(name, "r", "utf8").readlines()
	pywikibot.output("Read %d lines from file %s" % (len(lines), name))
	for f in listdir(destdir):
		f1 = f.replace(".wiki.txt", "") + u"\n"
		if f1 not in lines:
			pywikibot.output("%s" % f)

def processFile(name, destdir):
	pywikibot.output("Processing file %s, destdir is %s" % (name, destdir))
	try:
		os.mkdir(destdir)
	except OSError:
		pass
	pywikibot.output("Starting to verify existing files")
	tmp_file = u"/tmp/save_from_commons.txt"
	with codecs.open(name, "r", "utf8") as myfile:
		data=myfile.readlines()
	f = codecs.open(tmp_file, "w", "utf8")
	for filename in data:
		txtfile = destdir + filename.replace('\n','') + u".wiki.txt"
		if os.path.exists(txtfile):
			pywikibot.output(txtfile + u" already exists, skipping")
		else:
			f.write(filename)
	f.close()
	pywikibot.output("Starting to get pages from wiki")
	site = pywikibot.getSite()
	filteredGen = pagegenerators.TextfilePageGenerator(tmp_file, site)
	pregenerator = pagegenerators.PreloadingGenerator(filteredGen, 50)
	for page in pregenerator:
		#print page.title()
		#print page.namespace()
		imagepage = pywikibot.FilePage(page)
		title = imagepage.title()
		jpgfile = destdir + title
		txtfile = jpgfile + u".wiki.txt"
		print txtfile
		if os.path.exists(txtfile):
			pywikibot.output(jpgfile + u" already exists, skipping")
			continue
		pywikibot.output(u"Working on " + title)
		pywikibot.output(u"Getting text...")
		try:
			text = imagepage.get()
			url = imagepage.fileUrl()
			pywikibot.output(u"Saving text...")
			f = codecs.open(txtfile, "w+", 'utf-8')
			f.write(text)
			f.close()
		except:
			pywikibot.output("Failed")
			continue
		pywikibot.output(u"Getting image...")
		response = urllib2.urlopen(url)
		jpg = response.read()
		pywikibot.output(u"Saving image...")
		f = open(jpgfile, "wb+")
		f.write(jpg)
		f.close()

def main():
	OP_DEL = 1
	cfg = "default"
	op = None
	for arg in pywikibot.handleArgs():
		if arg.startswith('-cfg'):
			cfg = arg [len('-cfg:'):]
		if arg.startswith('-deletedFiles'):
			op = OP_DEL
	file_to_work= config[cfg]['file']
	dest_dir 	= config[cfg]['dir']
	user.mylang = config[cfg]['lang']
	user.family = config[cfg]['family']
	
	if op:
		deletedFiles(file_to_work, dest_dir)
	else:
		processFile(file_to_work, dest_dir)

if __name__ == "__main__":
	main()
