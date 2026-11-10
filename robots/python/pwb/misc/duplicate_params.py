#!/usr/bin/pyhon3
# -*- coding: utf-8  -*-

import pywikibot
from pywikibot import i18n, config, pagegenerators, textlib
from pywikibot.bot import SingleSiteBot

import wikiro.robots.python.pwb.strainu_functions as sf

class DuplicateParamsBot(SingleSiteBot):
	def __init__(self, site=True, generator=None):
		super(SingleSiteBot, self).__init__(
			generator=generator, site=site)
		self._site = site

	def print_multiline(self, name, new_params):
		new_text = "{{" + name + "\n"
		for param, value in new_params.items():
			if type(param) == int:
				new_text += "| " + value
				if value.find("\n") == -1:
					new_text += "\n"
			elif param == "_name":
				continue
			else:
				line = "| " + param + " = " + value
				if value.find("\n") == -1:
					line += "\n"
				line = line.replace("=  ", "= ")
				new_text += line
		new_text += "}}"
		return new_text
	
	def print_singleline(self, name, new_params):
		new_text = "{{" + name
		for param, value in new_params.items():
			if type(param) == int:
				new_text += "|" + value
			elif param == "_name":
				continue
			else:
				new_text += "|" + param + "=" + value
		new_text += "}}"
		return new_text

	def treat(self, page):
		old_full_text = full_text = page.get()
		upload = False
		print(page.title())
		for name,params in textlib.extract_templates_and_params(page.get()):
			#print(name, params)
			text = sf.extractTemplate(page.get(), name)
			if text == None:
				continue
			new_params, _, dp = sf.tl2Dict(text)
			#print(dp)
			if not dp:
				continue
			upload = True
			if text.find("\n") > -1:
				new_text = self.print_multiline(name, new_params)
			else:
				new_text = self.print_singleline(name, new_params)
			full_text = full_text.replace(text, new_text)
		if not upload:
			return
		pywikibot.showDiff(old_full_text, full_text)
		answer = pywikibot.input_choice("Upload?", [('Yes', 'Y'), ('No', 'N')])
		if answer.upper() == 'Y':
			page.put(full_text, "Deduplic parametri duplicați ai formatelor")
			



if __name__ == "__main__":
	site = pywikibot.Site('ro', 'wikipedia')
	cat = u"Categorie:Pagini_care_folosesc_argumente_duplicate_în_apelarea_formatelor"
	#site = pywikibot.getSite('ro','wikipedia')
	#cat = u"Categorie:Imagini încărcate în cadrul Wiki Loves Monuments 2018"
	generator = pagegenerators.CategorizedPageGenerator(pywikibot.Category(site, cat))
	bot = DuplicateParamsBot(site=site, generator=generator)

	bot.run() 
	
