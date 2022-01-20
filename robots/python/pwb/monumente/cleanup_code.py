#!/usr/bin/python
# -*- coding: utf-8  -*-
'''
Parse the monument pages (lists and images) and remove spaces from the 
monument ID
'''

import sys, time, warnings, json, string
sys.path.append("..")
import wikipedia, re, pagegenerators
import config as user

options = {
	'ro': 
	{
	'namespaces': [0],
	'codeTemplate': "ElementLMI",
	},
	'commons':
	{
	'namespaces': [6],
	'codeTemplate': "Monument istoric",
	}
}

def processList(text, page, conf):
	wikipedia.output(u'Working on "%s"' % page.title(True))
	index3 = 0
	changed = False
	while 1:
		space = ""
		index = text.lower().find("{{" + conf['codeTemplate'].lower(), index3)
		if index == -1:
			wikipedia.output(u'No more templates in "%s"' % page.title(True))
			break
		index2 = text.find("| Cod = ", index)
		len = 8
		if index2 == -1:
			index2 = text.find("|Cod = ", index)
			len = 7
		if index2 == -1:
			index2 = text.find("|Cod=", index)
			len = 5
		index3 = text.find("\n", index2)
		index4 = text.find("\r", index2)
		#wikipedia.output(text[index2:index3])
		if index4 + 1 == index3:
			index3 = index4
		index4 = text.find("azi", index2, index3)
		if index4 <> -1:
			#print "azi"
			index3 = index4
			space = " "
		index4 = text.find("fost", index2, index3)
		if index4 <> -1:
			#print "fost"
			index3 = index4
			space = " "
		index4 = text.find("\"", index2, index3)
		if index4 <> -1:
			#print "\""
			index3 = index4
			space = " "
		index4 = text.find("(", index2, index3)
		if index4 <> -1:
			#print "("
			index3 = index4
			space = " "
		code = text[index2 + len:index3]
		#wikipedia.output(code)
		newCode = re.sub(r'\s', '', code)
		newCode += space
		if code == newCode or code.lstrip() == "":
			#wikipedia.output(u"No change for %s, continuing" % code)
			continue
		# wikipedia.output(text[index2:index3])
		# wikipedia.output(str(index))
		# wikipedia.output(str(index2))
		# wikipedia.output(str(index3))
		# wikipedia.output(str(index4))
		# wikipedia.output(text)
		text = text.replace(code, newCode, 1)
		changed = True
		wikipedia.output(u'Code "%s" replaced with "%s"' % (code, newCode))
	comment = u'Se curăță codul LMI din articolul %s' % page.title(True)
	if changed: #something changed
		page.put(text, comment)


def processArticle(text, page, conf):
	wikipedia.output(u'Working on "%s"' % page.title(True))
	index = text.lower().find("{{" + conf['codeTemplate'].lower())
	if index == -1:
		wikipedia.output(u'No template in "%s"' % page.title(True))
		return
	index += 2 + len(conf['codeTemplate']) + 1
	index2 = text.find("}}", index)
	index3 = text.find("|", index)
	if index3 < index2 and index3 > -1:
		code = text[index:index3]
	else:
		code = text[index:index2]
	newCode = re.sub(r'\s', '', code)
	if code == newCode:
		wikipedia.output(u"No change for %s, exiting" % code)
		return
	text = text.replace(code, newCode)
	comment = u'Code "%s" replaced with "%s"' % (code, newCode)
	wikipedia.output(comment)
	import time
	time.sleep(10)
	page.put(text, comment)
	
	
def main():
	lang = u'ro'
	textfile = u''

	for arg in wikipedia.handleArgs():
		if arg.startswith('-lang:'):
			lang = arg [len('-lang:'):]
			user.mylang = lang
		if arg.startswith('-family'):
			user.family = arg [len('-family:'):]
	
	site = wikipedia.getSite()
	lang = site.language()
	if not options.get(lang):
		wikipedia.output(u'I have no options for language "%s"' % lang)
		return False
	
	langOpt = options.get(lang)
			
	rowTemplate = wikipedia.Page(site, u'%s:%s' % (site.namespace(10), langOpt.get('codeTemplate')))

	transGen = pagegenerators.ReferringPageGenerator(rowTemplate, onlyTemplateInclusion=True)
	filteredGen = pagegenerators.NamespaceFilterPageGenerator(transGen, langOpt.get('namespaces'), site)
	pregenerator = pagegenerators.PreloadingGenerator(filteredGen, 25)
	for page in pregenerator:
		if page.exists() and not page.isRedirectPage():
			# Do some checking
			if lang == u'ro':
				processList(page.get(), page, langOpt)
			else:
				processArticle(page.get(), page, langOpt)
	# page = wikipedia.Page(site, u"Lista monumentelor istorice din România/Vaslui")
	# processList(page.get(), page, langOpt)

if __name__ == "__main__":
	try:
		main()
	finally:
		wikipedia.output(u"Main error?")
		wikipedia.stopme()
