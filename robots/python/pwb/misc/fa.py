#!/usr/bin/python
# -*- coding: utf-8  -*-

'''
'''
#
# (C) Strainu 2016
#
# Distributed under the terms of the MIT license.
#

import re
import sys
from collections import OrderedDict as od

import pywikibot
from pywikibot import pagegenerators
from pywikibot import config as user

sys.path.append("wikiro/robots/python/pwb")
import strainu_functions as sf

def printWikidata(page):
	item = pywikibot.ItemPage.fromPage(page)
	return item.title()

def printCombo(generator):
	ret = u""
	for page in generator:
		item = printWikidata(page)
		text = u"[\"" + page.title() + u"\"] = \"" + item + u"\","
		#skip non-articles until we know what to do with them
		if page.namespace() != 0:
			text = u"--" + text
		print(text)
		ret += text + u"\n"
	return ret

if __name__ == "__main__":
	#page = pywikibot.Page(pywikibot.getSite(), title=u"Metroul din București")
	#printWikidata(page)

	text = u"local featuredPages = {\n"

	text += u"--Categorie:Articole de calitate\n"
	text += u"[AC] = {\n"
	cat = pywikibot.Category(pywikibot.getSite(), u"Categorie:Articole de calitate")
	gen = pagegenerators.PreloadingGenerator(pagegenerators.CategorizedPageGenerator(cat))
	text += printCombo(gen)
	text += u"},\n"

	text += u"--Categorie:Articole bune\n"
	text += u"[AB] = {\n"
	cat = pywikibot.Category(pywikibot.getSite(), u"Categorie:Articole bune")
	gen = pagegenerators.PreloadingGenerator(pagegenerators.CategorizedPageGenerator(cat))
	text += printCombo(gen)
	text += u"},\n"

	text += u"--Categorie:Liste de calitate\n"
	text += u"[LC] = {\n"
	cat = pywikibot.Category(pywikibot.getSite(), u"Categorie:Liste de calitate")
	gen = pagegenerators.PreloadingGenerator(pagegenerators.CategorizedPageGenerator(cat))
	text += printCombo(gen)
	text += u"},\n"

	text += u"""}

local fp, j = {}, 0
for k,_ in pairs(featuredPages) do
	for k1,v1 in pairs(featuredPages[k]) do
	j = j + 1
	fp[k1] = v1
    end
end

return {
	all=fp,
	ac=featuredPages["AC"],
	ab=featuredPages["AB"],
	ab=featuredPages["LC"],
	len=j,
	}"""

	page = pywikibot.Page(pywikibot.getSite(), title=u"Modul:Pagina principală/articole")
	page.put(text, u"Actualizez lista de articole")
