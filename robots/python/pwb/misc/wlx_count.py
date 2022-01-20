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
import pywikibot

from pywikibot import i18n, config, pagegenerators, textlib, weblib
from pywikibot.bot import SingleSiteBot

organizers = ["Strainu", "Andrei Stroe", "CEllen", "Nicubunu"]
i = 0

class WlxBot(SingleSiteBot):
	def __init__(self, site=True, generator=None):
		super(SingleSiteBot, self).__init__(
			generator=generator, site=site)
		self._site = site
		self.users = {}
		
	def treat(self, page):
		global i
		i+=1
		print(i)
		code = None
		for name,params in textlib.extract_templates_and_params(page.get()):
			if name != u"Monument istoric":
				continue
			code = params[u"1"]
			#print(code)
			break
		if code == None:
			print(page.title())
			return
		author = page.oldest_revision.user
		if author in organizers:
		#if "Țetcu" not in author:
			return
		if author not in self.users:
			self.users[author] = set()
		self.users[author].add(code)

	def compare(self, x, y):
		#print(x)
		#print(y)
		return cmp(len(self.users[x]), len(self.users[x]))
		
	def getResult(self):
		return sorted(self.users, self.compare)

if __name__ == "__main__":
	site = pywikibot.getSite('commons', 'commons')
	cat = u"Category:Images from Wiki Loves Monuments 2018 in Romania"
	#site = pywikibot.getSite('ro','wikipedia')
	#cat = u"Categorie:Imagini încărcate în cadrul Wiki Loves Monuments 2018"
	generator = pagegenerators.CategorizedPageGenerator(pywikibot.Category(site, cat))
	bot = WlxBot(site=site, generator=generator)

	bot.run() 
	r = bot.getResult()
	for i in range(len(r)-1,-1,-1):
		print(r[i] + ": " + str(len(bot.users[r[i]])))
