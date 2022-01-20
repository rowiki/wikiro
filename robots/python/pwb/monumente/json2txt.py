#!/usr/bin/python
# -*- coding: utf-8  -*-

import json, codecs

import pywikibot
from pywikibot import config as user

def split_code(code):
        parts = code.split('-')
        if len(parts) < 3:
                print(parts)
                return (None, None, None, None)
        return (parts[0], parts[1], parts[2], parts[3])

config = {
	'lmiro': {
		'output': u"rofiles.txt",
		'input': u"ro_lmi_Fișier_pages.json",
		'lang': u"ro",
		'family': u"wikipedia",
	},
	'lmicommons': {
		'output': u"monumentfiles.txt",
		'input': u"commons_lmi_File_pages.json",
		'lang': u"commons",
		'family': u"commons",
	},
	'default': {
		'output': u"downloadlist.txt",
		'input': u"img_download",
		'lang': u"commons",
		'family': u"commons",
	},
}

if __name__ == "__main__":
	for arg in pywikibot.handleArgs():
		if arg.startswith('-cfg'):
			cfg = arg [len('-cfg:'):]

	output      = config[cfg]['output']
	input    = config[cfg]['input']
	user.mylang = config[cfg]['lang']
	user.family = config[cfg]['family']

	f = codecs.open(input, "r+", "utf8")
	#f = codecs.open("ro_Fișier_pages.json", "r+", "utf8")
	print ("Reading %s images file..." % cfg)
	pages_commons = json.load(f)
	print ("...done")
	f.close();

	f = codecs.open(output, "w+", "utf8")
	#f = codecs.open("export_monumenteuitate_20140207.txt", "w+", "utf8")
	#f = codecs.open("rofiles.txt", "w+", "utf8")
	for code in pages_commons:
		#(county, nature, type, interest) = split_code(code)
		#if nature != u"II":
		#	continue
		#if county not in [u'AG', u'BR', u'BZ', u'CL', u'CT', u'DB', u'DJ', u'GR', u'GJ', u'IL', u'IF', u'MH', u'OT', u'PH', u'TL', u'TR', u'VL']:
		#	continue
		for img in pages_commons[code]:
			f.write(img["name"] + u"\n")

	f.close()
