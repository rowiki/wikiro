#!/usr/bin/python
# -*- coding: utf-8 -*-
"""Implementation of the bot that is proposing pages for deletion

Run using:

    python pwb.py pșnbot.py
"""
import os
import datetime

import pywikibot
from pywikibot import pagegenerators
from pywikibot.bot import SingleSiteBot

months = ['ianuarie', 'februarie', 'martie', 'aprilie', 'mai', 'iunie', 'iulie',
	'august', 'septembrie', 'octombrie', 'noiembrie', 'decembrie']

def month_name(month):
	return months[month-1]

def next_month(month):
	return (month % 12) + 1

class PSNBot(SingleSiteBot):
	def __init__(self, month, year, generator):
		super(PSNBot, self).__init__(
	            generator=generator, site=pywikibot.Site())
		self.month = month
		self.year = year
		self.generator = generator
		self.psp = "Wikipedia:Pagini de șters"

	"""Returns the username of the initial author of the page"""
	def get_initial_author(self, page):
		user, date = page.getCreator()
		return user

	def get_tagger(self, page):
		starttime = pywikibot.Timestamp.fromtimestampformat("%04d%02d%02d" % (self.year, next_month(self.month), 1))
		endtime   = pywikibot.Timestamp.fromtimestampformat("%04d%02d%02d" % (self.year, self.month, 1))
		lastrev   = None
		for rev in page.revisions(content=True, starttime=starttime, endtime=endtime):
			#print(rev)
			if rev.text and rev.text.find("{{notabilitate") == -1 and \
				rev.text.find("notabilitate=") == -1 and \
				rev.text.find("{{Notabilitate") == -1:
				break
			lastrev = rev

		if not lastrev:
			raise Exception("Could not find tagger")

		return lastrev.user

	def get_proposal_page(self, page):
		ps_title = ps_title_base = self.psp + "/" + page.title()
		index = 1
		p = pywikibot.Page(self.site, ps_title)
		while p.exists():
			index += 1
			ps_title = ps_title_base + " (" + str(index) + ")"
			p = pywikibot.Page(self._site, ps_title)
		return p, ps_title[ps_title.rfind("/")+1:]

	def deletion_proposed(self, page):
		text = page.get()
		if text.find("{{șterge") > -1:
			return True
		return False

	def prepare_psn(self, tagger):
		return "{{subst:pșn|user=%s|lună=%s|an=%d}}" % (tagger, month_name(self.month), self.year)

	def update_article(self, page, ps_title, tagger):
		text = "{{șterge|1=%s|discuție=%s}}" % (self.prepare_psn(tagger), ps_title)
		page.text = text + page.text
		page.save("Nominalizez pentru ștergere din cauza posibilei lipse de notabilitate")
		
	def create_nomination(self, page, ps_page, ps_title, tagger):
		text = "{{subst:formular ștergere|%s|%s|3=%s}}--~~~~" % (page.title(), ps_title, self.prepare_psn(tagger))
		ps_page.text = text
		ps_page.save("Creez o propunere nouă de ștergere")

	def update_list(self, ps_page):
		proposal_section = "== Discuții curente =="
		list_page = pywikibot.Page(self._site, self.psp)
		list_page.text = list_page.text.replace(proposal_section, proposal_section + "\n{{" + ps_page.title() + "}}")
		list_page.save("Adaug o propunere nouă de ștergere")

	def notify_creator(self, page, ps_title, tagger, author):
		author_page = pywikibot.Page(self._site, "Discuție Utilizator:" + author)
		if author_page.exists():
			text = author_page.text
		else:
			text = ""

		this_month = datetime.datetime.now().month
		this_year = datetime.datetime.now().year
		diff = (this_year - self.year) * 12 + (this_month - self.month)
		if diff <= 0:
			raise ValueError("Invalid month provided")
		author_page.text = text + "\n{{subst:au-pșn|%s|%s|luni=%d|nominator=%s}}--~~~~" % (page.title(), ps_title, diff, tagger)
		author_page.save("Pagină nominalizată pentru ștergere")

	def treat(self, page):
		print(page.title())
		if self.deletion_proposed(page):
			return False

		ps_page, ps_title = self.get_proposal_page(page)
		tagger = self.get_tagger(page)
		author = self.get_initial_author(page)

		answer = True
		print(ps_page.title(), tagger, author)
		#answer = pywikibot.input_yn("Nominate for deletion %s?" % page.title())
		if answer:
			self.create_nomination(page, ps_page, ps_title, tagger)
			self.update_list(ps_page)
			self.update_article(page, ps_title, tagger)
			self.notify_creator(page, ps_title, tagger, author)

if __name__ == "__main__":
	month_diff = 4
	import datetime
	year = datetime.datetime.now().year
	month = datetime.datetime.now().month
	if month <= month_diff:
		year -= 1
	month = (month - 1 + 12 - month_diff) % 12

	cat = pywikibot.Category(pywikibot.getSite(), u"Categorie:Articole despre subiecte cu notabilitate incertă din %s 2019" % months[month])
	generator = pagegenerators.CategorizedPageGenerator(cat)
	bot = PSNBot(month+1, year, generator)
	bot.run()
	
