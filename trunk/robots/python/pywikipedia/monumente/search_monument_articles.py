#!/usr/bin/python
# -*- coding: utf-8  -*-
'''
Search for all the pages in a given wiki that contain a LMI code

'''

import sys, time, warnings, json, string, re
import cProfile
sys.path.append("..")
#sys.path.append("..")
import strainu_functions as strainu
import pywikibot
from pywikibot import pagegenerators
from pywikibot import config as user

codeRegexp = re.compile("(([a-z]{1,2})-(i|ii|iii|iv)-([a-z])-([a-z])-([0-9]{5}(\.[0-9]{2,3})?))", re.I)
templateRegexp = re.compile("\{\{([aA]utorCodLMI|[cC]odLMI)")
errorRegexp = re.compile("eroare\s?=\s?([^0])", re.I)
_log = "search.err.log"
_flog = None

def initLog():
	global _flog, _log;
	_flog = open(_log, 'a+')
	
def closeLog():
	global _flog
	_flog.close()

def log(string):
	_flog.write(string.encode("utf8") + "\n")

def processArticle(page):
	text = page.get()
	# pywikibot.output(u'Working on "%s"' % title)
	global codeRegexp
	global templateRegexp
	result  = re.findall(codeRegexp, text)
	template = re.findall(templateRegexp, text)
	if len(result) > 0 and len(template) == 0:
		msg = u"* [[%s]]: " % page.title()
		for res in result:
			msg += res[0] + ", "
		log(msg)
		pywikibot.output(msg)
		
def main():
	lang = u'ro'
	textfile = u''
	start = "!"

	for arg in pywikibot.handleArgs():
		if arg.startswith('-lang:'):
			lang = arg [len('-lang:'):]
			user.mylang = lang
		if arg.startswith('-family'):
			user.family = arg [len('-family:'):]
		if arg.startswith('-start'):
			start = arg [len('-start:'):]
	
	site = pywikibot.getSite()
	lang = site.language()
	
	global _log
	initLog()
	
	transGen = pagegenerators.AllpagesPageGenerator(start, includeredirects=False)
	pregenerator = pagegenerators.PreloadingGenerator(transGen, 100)
		
	#page = pywikibot.Page(site, "File:Biserica_Sf._Maria,_sat_Drumul_Carului,_'La_Cetate'-Gradistea._Moeciu,_jud._BRASOV.jpg")
	count = 0
	for page in pregenerator:
		# Do some checking
		processArticle(page)
		count += 1
		if count % 100 == 0:
			pywikibot.output(page.title())
			#time.sleep(3)
	closeLog()

if __name__ == "__main__":
	try:
		#cProfile.run('main()', './parseprofile.txt')
		main()
	finally:
		pywikibot.stopme()
