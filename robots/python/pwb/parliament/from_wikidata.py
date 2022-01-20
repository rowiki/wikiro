#!/usr/bin/python
#-*- coding:utf-8 -*-

import json
import re
import sys

import pywikibot
from pywikibot import pagegenerators
from pywikibot.data import sparql
from pywikibot import config as user

sys.path.append("wikiro/robots/python/pwb")
import parliament
from parliament import person

def fixup_party(party):
	party = party.replace("Partidul", "Partidului")
	party = party.replace("Uniunea", "Uniunii")
	return party

always = None
def process_item(repo, result):
	global always
	item = result.get("item")

	if not item:
		return

	item = item[item.rfind('/')+1:]
	item = repo.get_entity_for_entity_id(item)

	result["item"] = item
	#result["partyLabel"] = fixup_party(result["partyLabel"])

	item.get()
	if "rowiki" in item.sitelinks:
		return

	page = pywikibot.Page(pywikibot.getSite('ro','wikipedia'), result["itemLabel"])
	if not page.exists():
		return
	while page.isRedirectPage():
		page = page.getRedirectTarget()

	answer = always or pywikibot.inputChoice(u"Add rowiki link for %s" % result["itemLabel"], ['Yes', 'No', 'Always'], ['y', 'n', 'a'], 'n')
	if answer == 'a':
		always = 'y'
		answer = 'y'
	if answer == 'y':
		try:
			item.setSitelink(page)
			import time
			time.sleep(1)
		except pywikibot.exceptions.PageSaveRelatedError as e:
			print("Eroare la salvarea paginii" + str(e))
			pass

def bla():
	person = parliament.person.ElectedPerson()
	person.name = result["itemLabel"]
	person.birthdate = item.claims.get('P569')[0].getTarget().toTimestr()
	person.chamber = 2#int(row[2])
	person.district = ""
	person.wiki = ""
	
	art = person.generateArticle()
	print(art)
	page = pywikibot.Page(pywikibot.getSite("ro", "wikipedia"), person.name)
	answer = pywikibot.inputChoice(u"Upload page %s" % person.name, ['Yes', 'No', 'Rename'], ['y', 'n', 'r'], 'n')
	if answer == 'y':
		try:
			page.put(art, "Completez articolul despre un parlamentar")
		except pywikibot.exceptions.PageSaveRelatedError as e:
			print("Eroare la salvarea paginii" + str(e))
			pass

	print(result)

def main():
	user.mylang = 'wikidata'
	user.family = 'wikidata'

	repo = pywikibot.Site().data_repository()
	dependencies = {'endpoint': None, 'entity_url': None, 'repo': repo}
	query_object = sparql.SparqlQuery(**dependencies)
	sen_query = """select ?item ?itemLabel ?partyLabel WHERE {
	    ?item wdt:P31 wd:Q5.
	    ?item p:P39 ?position.
	    ?position ps:P39 wd:Q19938957;
		      pq:P2937 wd:Q104535655;
		      pq:P768 ?district.

            OPTIONAL {
	        ?item p:P102 ?party2.
		?party2 ps:P102 ?party.
		FILTER( NOT EXISTS { ?party2 pq:P582 [] } )
	    }
						    
	    SERVICE wikibase:label { bd:serviceParam wikibase:language "ro,en". }
	} order by ?districtLabel ?itemLabel"""

	cdep_query = """select ?item ?itemLabel ?partyLabel WHERE {
	    ?item wdt:P31 wd:Q5.
	    ?item p:P39 ?position.
	    ?position ps:P39 wd:Q17556530;
		      pq:P2937 wd:Q104535655;
		      pq:P768 ?district.

            OPTIONAL {
	        ?item p:P102 ?party2.
		?party2 ps:P102 ?party.
		FILTER( NOT EXISTS { ?party2 pq:P582 [] } )
	    }
						    
	    SERVICE wikibase:label { bd:serviceParam wikibase:language "ro,en". }
	} order by ?districtLabel ?itemLabel"""
	data = query_object.select(sen_query)
	if not data:
		return

	for result in data:
		process_item(repo, result)


if __name__ == "__main__":
	main()
