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

class CeeSpringBot(SingleSiteBot):
	def __init__(self, site=True, generator=None):
		super(SingleSiteBot, self).__init__(
			generator=generator, site=site)
		self._site = pywikibot.getSite()
		self.points = {}
		self.poland = {}
		self.czech = {}
		self.countries = {}

	def getWordCount(self, text):
		text = textlib.removeDisabledParts(text)
		text = textlib.removeHTMLParts(text)
		text = textlib.removeLanguageLinks(text)
		text = textlib.removeCategoryLinks(text)
		word_list = re.findall(r"[\w']+", text)

		return len(word_list)
		
	def treat(self, page):
		for name,params in textlib.extract_templates_and_params(page.get()):
			if name != u"Wikimedia CEE Spring 2016":
				continue
			country = params[u"țară"]
			break
		page = page.toggleTalkPage()
		words = self.getWordCount(page.get())
		author = page.oldest_revision.user
		if author not in self.points:
			self.points[author] = 0
			self.poland[author] = 0
			self.czech[author] = 0
			self.countries[author] = {}
		if words < 200: #not acceptable
			print u"Articol descalificat: " + page.title()
			return
		#TODO creation date
		self.points[author] += 1
		if country not in [u"Albania", u"Estonia", u"Bașchiria", u"Bașkiria", u"Austria", u"Rusia", u"Republika Srpska", u"Republica Srpska", u"Slovacia", u"Ucraina", u"Lituania", u"Macedonia", u"Bulgaria", u"Croația", u"Azerbaidjan", u"Polonia", u"Kazahstan", u"Grecia", u"Belarus", u"Iacuția", u"Iacutia", u"Serbia", u"Letonia", u"Kosovo", u"Armenia", u"Cehia", u"Republica Cehă", u"Bosnia și Herțegovina", u"Georgia", u"Esperanto", u"Turcia", u"Ungaria"]:
			print u"Țară greșită: " + country
		else:
			self.points[author] += 0.2
			self.countries[author][country] = 0
			if country in [u"Polonia"]:
				self.poland[author] += 1.2
			if country in [u"Cehia", u"Republica Cehă"]:
				self.czech[author] += 1.2
		print page.title()
		print author
		print words
		print country
		
	def getResult(self):
		print self.points
		print self.poland
		print self.czech
		print self.countries

if __name__ == "__main__":
	generator = pagegenerators.CategorizedPageGenerator(pywikibot.Category(pywikibot.getSite(), u"Categorie:Articole participante la Wikimedia CEE Spring 2016"))
	bot = CeeSpringBot(site=True, generator = generator)

	bot.run() 
	bot.getResult()