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
from pywikibot import catlib

sys.path.append(".")
import strainu_functions as sf

def templateInSubpage(sourcePrefix, destPrefix):
	generator = pagegenerators.PrefixingPageGenerator(prefix=sourcePrefix)
	for page in generator:
		if page.isRedirectPage():
			continue

		suffix = page.title().split('/')[1]
		dest = pywikibot.Page(pywikibot.getSite(), title=destPrefix + suffix)
		print dest
		if dest.exists():
			pywikibot.output(u"Page %s already exists" % dest.title())
			#page.put(u"#redirect [[%s]]" % dest.title(), comment="Redirectez către noua locatie a graficelor cu date demografice")
			continue

		try:
			text = page.get()
		except:
			pywikibot.output(u"Could not read %s" % page.title())
			continue

		output = od({})
		tpl = sf.tl2Dict(sf.extractTemplate(text, u"Demografia"))[0]
		for i in range(1,20):
			p1 = u"a" + str(i)
			p2 = u"p" + str(i)
			if p1 in tpl and p2 in tpl:
				output[i] = {"year": tpl[p1], "pop": tpl[p2]}


		text = u"an,populatie\n"
		for elem in output:
			text += "%s,%s\n" % (output[elem]["year"], output[elem]["pop"])
		print text
		dest.put(text,comment="Creez o nouă pagină cu date demografice")
		#page.put(u"#redirect [[%s]]" % dest.title(), comment="Redirectez către noua locatie a graficelor cu date demografice")


def graphInSubpage(sourcePrefix, destPrefix):
	popRegex = ur"bar:([0-9]+) from:[0-9]+ till:(\s*)([0-9]+)"
	popPattern = re.compile(popRegex)
	yearRegex = ur"bar:([0-9]+) text:(\s*)([0-9]+)"
	yearPattern = re.compile(yearRegex)
	generator = pagegenerators.PrefixingPageGenerator(prefix=sourcePrefix)
	for page in generator:
		suffix = page.title().split('/')[1]
		dest = pywikibot.Page(pywikibot.getSite(), title=destPrefix + suffix)
		call = destPrefix.replace('/', '|') + suffix
		#print dest
		if  page.isRedirectPage() and page.getRedirectTarget() != dest:
			continue
		if dest.exists():
			pywikibot.output(u"Page %s already exists" % dest.title())
			page.put(u"{{%s}}" % call, comment="Redirectez către noua locatie a graficelor cu date demografice")
			continue

		try:
			text = page.get()
		except:
			pywikibot.output(u"Could not read %s" % page.title())
			continue

		output = od({})
		for line in text.splitlines():
			match = popPattern.search(line)
			if match:
				if match.group(1) not in output:
					output[match.group(1)] = {"year": None, "pop": None}
				output[match.group(1)]["pop"] = match.group(3)
			match = yearPattern.search(line)
			if match:
				if match.group(1) not in output:
					output[match.group(1)] = {"year": None, "pop": None}
				output[match.group(1)]["year"] = match.group(3)

		text = u"an,populatie\n"
		for elem in output:
			text += "%s,%s\n" % (output[elem]["year"], output[elem]["pop"])
		#print text
		dest.put(text,comment="Creez o nouă pagină cu date demografice")
		page.put(u"{{%s}}" % call, comment="Redirectez către noua locatie a graficelor cu date demografice")

def replaceSubpage(sourcePrefix, destPrefix):
	generator = pagegenerators.PrefixingPageGenerator(prefix=sourcePrefix)
	for page in generator:
		title = page.title()
		suffix = title[title.find('/') + 1:]
		repl = u"{{" + title
		repl2 = u"{{" + title.replace(u"Format:", u"")
		repl3 = u"{{" + title.replace(u"Format:", u"").lower().replace(suffix.lower(), suffix)
		call = u"{{" + destPrefix + u'|' + suffix
		generator2 = pagegenerators.ReferringPageGenerator(page)
		for page2 in generator2:
			try:
				text = page2.get()
			except:
				continue
			newtext = text.replace(repl, call)
			newtext = newtext.replace(repl2, call)
			newtext = newtext.replace(repl3, call)
			pywikibot.showDiff(text, newtext)
			page2.put(newtext, comment="Folosesc noul format pentru grafice de demografie")

def replaceSubpageWithDimensions(sourcePrefix, destPrefix):
	generator = pagegenerators.PrefixingPageGenerator(prefix=sourcePrefix)
	for page in generator:
		title = page.title()
		suffix = title[title.find('/') + 1:]
		repl = u"{{" + title
		repl2 = u"{{" + title.replace(u"Format:", u"")
		repl3 = u"{{" + title.replace(u"Format:", u"").replace(u"'", u"&#39;")
		repl4 = u"{{" + title.replace(u"Format:", u"").lower().replace(suffix.lower(), suffix)
		try:
			tpl = sf.tl2Dict(sf.extractTemplate(page.get(), u"Demografia"))[0]
			x = tpl["dimx"].strip()
			y = tpl["dimy"].strip()
		except:
			continue
		call = u"{{" + destPrefix + u'|' + suffix + u'|lățime=' + x + u'|înălțime=' + y
		generator2 = pagegenerators.ReferringPageGenerator(page)
		for page2 in generator2:
			if page2.namespace() > 0:
				continue
			try:
				text = page2.get()
				newtext = text.replace(repl, call)
				newtext = newtext.replace(repl2, call)
				newtext = newtext.replace(repl3, call)
				newtext = newtext.replace(repl4, call)
				if newtext != text:
					pywikibot.showDiff(text, newtext)
					page2.put(newtext, comment="Folosesc noul format pentru grafice de demografie")
			except:
				continue

def wikidataItaly():
	site = pywikibot.Site("test", "wikidata")
	repo = site.data_repository()
	item = pywikibot.ItemPage(repo, u"Q2210")
	item.get()
	generator = pagegenerators.PrefixingPageGenerator(prefix="Format:Demografia/")
	for page in generator:
		title = page.title()
		print title
		suffix = title[title.find('/') + 1:]
		tpl = sf.tl2Dict(sf.extractTemplate(page.get(), u"Demografia"))[0]
		for x in range(1,15):
			print x
			import time
			time.sleep(30)
			try:
				a = "a" + str(x)
				p = "p" + str(x)
				if a in tpl and p in tpl:
					year = tpl[a].strip()
					pop = int(tpl[p].strip())
					if pop == 0 or pop == None:
						continue	
				else:
					continue
				yearclaim = pywikibot.Claim(repo, u'P63') #Population
				target = pywikibot.WbQuantity(amount=pop)
				yearclaim.setTarget(target)

				url = pywikibot.Claim(repo, u'P93')#URL
				target = u"http://dati.istat.it/Index.aspx"
				url.setTarget(target)

				date = pywikibot.Claim(repo, u'P151')#Date of publication
				target = pywikibot.WbTime(year=year)
				date.setTarget(target)
			except:
				continue

			item.addClaim(yearclaim)
			yearclaim.addSources([url])
			yearclaim.addQualifier(date)

			
		break
		#TODO: get proper item
		for claim in item.claims["P63"]:
			url = pywikibot.Claim(repo, u'P93')
			target = u"http://dati.istat.it/Index.aspx"
			url.setTarget(target)

			claim.addSources([url])

if __name__ == "__main__":
	#graphInSubpage(u"Format:Demografie/", u"Format:Grafic demografie/")
	#templateInSubpage(u"Format:Demografia/", u"Format:Grafic demografie/")
	#replaceSubpage(u"Format:Demografie/", u"Grafic demografie")
	#replaceSubpageWithDimensions(u"Format:Demografia/", u"Grafic demografie")
	wikidataItaly()
