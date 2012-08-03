#!/usr/bin/python
# -*- coding: utf-8  -*-
'''
Add {{Monument istoric}} to all the images that illustrate the list, 
but don't have the template in commons.
'''

import sys, time, warnings, json, string
sys.path.append("..")
import wikipedia, re, pagegenerators
import config as user

_log = "link2.err.log"
_flog = None

def initLog():
	global _flog, _log;
	_flog = open(_log, 'w+')

def closeLog():
	global _flog
	_flog.close()

def log(string):
	wikipedia.output(string.encode("utf8") + "\n")
	_flog.write(string.encode("utf8") + "\n")

def main():
	initLog()
	f = open("db.json", "r+")
	wikipedia.output("Reading database file...")
	db = json.load(f)
	wikipedia.output("...done")
	f.close();
	
	#this is the big loop that should only happen once
	for monument in db:
		rawCode = monument["Cod"].strip()
		regexp = re.compile("(([a-z]{1,2})-(i|ii|iii|iv)-([a-z])-([a-z])-([0-9]{5}(\.[0-9]{2})?))", re.I)
		result = re.findall(regexp, rawCode)
		if len(result) > 0:
			code = result[0][0]
		else:
			code = rawCode
		wikipedia.output(code)
		
		image = monument["Imagine"]
		if image == None or image.strip() == "":
			continue
			
		image = image.replace(u'Fișier', u'File')
		image = image.replace(u'Imagine', u'File')
		site = wikipedia.getSite('commons', 'commons')
		try:
			page = wikipedia.Page(site, image)
			if page.isRedirectPage():
				page = page.getRedirectTarget()
			text = page.get()
		except Exception:
			log("I: Local image %s for code %s" % (image, code))
			continue
			
		templates = page.templatesWithParams(thistxt=text)
		info_params = None
		found = 0
		for (template, params) in templates:
			#print template
			if unicode(template) == u"Monument istoric":
				found = 1
				if code <> str(params[0]):
					log(u"W: Nepotrivire între codul din listă (%s) și unul din codurile din imagine (%s)" % (code, params[0]))
			elif unicode(template) == u"Information":
				info_params = params
				#print params
				
		if not found: # we need to add the template
			addedText = "{{Monument istoric|%s}}" % code
			if info_params == None:
				text = addedText + "\n" + text
				#print text
				page.put(text, comment="Adding {{tl|Monument istoric}}")
			else:
				description = None
				for param in info_params:
					pos = unicode(param.lower()).find(u"description")
					if  pos == 0 or (pos > 0 and param[0:pos].strip() == ""):
						description = unicode(param)
						break
				if description:
					text = text.replace(description, description + addedText + "\n")
					#print text
					page.put(text, comment="Adding {{tl|Monument istoric}}")
				else:
					log(u"E: Imaginea pentru %s are formatul {{f|information}} dar nu și o descriere" % code)
	closeLog()
	

if __name__ == "__main__":
	try:
		main()
	finally:
		wikipedia.stopme()
