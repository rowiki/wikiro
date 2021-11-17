#!/usr/bin/python
# -*- coding: utf-8  -*-

import sys, os
import time, datetime
import warnings
import json
import string
import cProfile
import re

import pywikibot
from pywikibot import pagegenerators
from pywikibot import config as user
from pywikibot.data import sparql
from pywikibot.tools import filter_unique

import wikiro.robots.python.pywikipedia.strainu_functions as strainu

def getWikidataProperty(page, prop):
	default_returns = {
		"P625": (0,0),
		"P1770": None,
		"P18": None,
		"P843": None
	}
	#print prop
	if page.namespace() != 0:
		return default_returns.get(prop)
	if page.title()[0] == 'Q':
		item = page
	else:
		try:
			item = page.data_item()
		except:
			print(("Could not obtain wikidata item for " + page.title()))
			return default_returns.get(prop)
	#print item.claims
	if prop in item.claims:
		claim = item.claims[prop][0]
		try:
			target = claim.getTarget()
			#print(target)
			if isinstance(target, pywikibot.Coordinate):
				#Bug: https://www.mail-archive.com/wikidata-tech@lists.wikimedia.org/msg00714.html
				if (target.precision or 1.0 / 3600) <= _coordVariance:
					return target.lat, target.lon
				else:
					return 0,0
			else:
				return target
		except Exception as e:
			print(("Wikidata Exception " + repr(e)))
	return default_returns.get(prop)


def process_page(page, template):
	#trace = Trace(sys._getframe().f_code.co_name)
	title = page.title()
	text = page.get()
	pywikibot.output('Working on "%s"' % title)
	rx = r'\{\{' + template + '|([0-9]+)\}\}'
	code = None
	codes = re.findall(rx, text)
	if codes and len(codes) > 1:
		return codes[1]
	else:
		pywikibot.error("Could not find SIRUTA code in " + page.title())
		return ""
	
def parse_images():
	user.mylang = 'commons'
	user.family = 'commons'
	site = pywikibot.Site()

	namespaces = [6]
	preload = True
	template = "SIRUTA"
	images_db = {}

	for namespace in namespaces:
		transGen = []
		for template in [template]:
			rowTemplate = pywikibot.Page(site, '%s:%s' % (site.namespace(10), \
								template))
			transGen.append(rowTemplate.getReferences(follow_redirects=False, content=False))
		combinedGen = pagegenerators.CombinedPageGenerator(transGen)
		combinedGen = filter_unique(combinedGen, key=hash)
		#combinedGen = pagegenerators.CategorizedPageGenerator(pywikibot.Category(site, u"Categorie:Imagini încărcate în cadrul Wiki Loves Monuments 2020"))
		filteredGen = pagegenerators.NamespaceFilterPageGenerator(combinedGen,
									[namespace], site)
		if preload:
			pregenerator = pagegenerators.PreloadingGenerator(filteredGen, 500)
		else:
			pregenerator = filteredGen

		count = 0
		if namespace == 0:
			namespaceName = ""
		else:
			namespaceName = site.namespace(namespace)

		for page in pregenerator:
			try:
				pageTitle = page.title()
				if page.exists() and not page.isRedirectPage():
					print((page.title()))
					code = process_page(page, template)
					count += 1
					if not code:
						continue
					if code not in images_db:
						images_db[code] = []
					images_db[code].append(page.title(with_ns=False))
			except Exception as e:
				pywikibot.output("Exception: " + repr(e))
				import traceback
				traceback.print_exc()
		print("Processed %d pages" % count)

	return images_db

def parse_wikidata():
	site = pywikibot.Site('wikidata', 'wikidata')
	repo = site.data_repository()
	dependencies = {'endpoint': None, 'entity_url': None, 'repo': repo}
	query_object = sparql.SparqlQuery(**dependencies)
	query = """SELECT ?item ?siruta
    WHERE
    {
      ?item wdt:P843 ?siruta .
      MINUS {?item wdt:P18 ?image . }
      MINUS {?item wdt:P2716 ?colaj . }
   }"""
	data = query_object.select(query)
	if not data:
		pywikibot.error("No data")
		return {}
	ret = {}
	for d in data:
		ret[d["siruta"]] = d["item"][d["item"].rfind("/")+1:]

	#print(ret)
	return ret

def add_wikidata_image(q, images):
	site = pywikibot.Site('wikidata', 'wikidata')
	repo = site.data_repository()
	item = pywikibot.ItemPage(repo, q)
	item.get()
	siruta = getWikidataProperty(item, "P843")
	pywikibot.output("Adding image to " + item.labels.get("ro"))
	pywikibot.output("https://commons.wikimedia.org/w/index.php?search=insource%3A{{SIRUTA%7C" + siruta + "}}&title=Special%3AMediaSearch")
	pywikibot.output(images)
	idx = pywikibot.input("Choose a value between 0 and " + str(len(images)-1))
	if idx == 'q' or idx == 'n':
		return
	idx = int(idx)
	if idx < 0 or idx >= len(images):
		pywikibot.error("Invalid index")
		add_wikidata_image(q, images)
		return
	filename = images[idx]
	commons = pywikibot.Site('commons', 'commons')
	val = pywikibot.FilePage(commons, u"File:" + filename)

	if not val.exists() or not val.fileIsShared():
		raise ValueError("Local image given: %s", val.title())
	while val.isRedirectPage():
		val = pywikibot.FilePage(val.getRedirectTarget())
	claim = pywikibot.page.Claim(repo, "P18", datatype='commonsMedia')
	claim.setTarget(val)
	item.addClaim(claim)
	pass

def main():

	images_db = parse_images()
	wikidata_db = parse_wikidata()

	for siruta in images_db:
		if siruta in wikidata_db:
			add_wikidata_image(wikidata_db[siruta], images_db[siruta])

if __name__ == "__main__":
	try:
		main()
	finally:
		pywikibot.stopme()
