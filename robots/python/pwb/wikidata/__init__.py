#!/usr/bin/python
#-*- coding:utf-8 -*-

from typing import Any, Generator
import pywikibot
from pywikibot.data import sparql

def find_best_claim(claims):
    """Find the first best ranked claim."""
    index = None
    for i, claim in enumerate(claims):
        if claim.rank == 'preferred':
            return claim
        if index is None and claim.rank == 'normal':
            index = i
    if index is None:
        index = 0
    return claims[index]

def get_labels(item):
        item.get()
        return item.labels.get('ro') or item.labels.get('mul') or item.labels.get('en') or item.labels.get('fr') or None

def sparql_generator(query, site) -> Generator[Any, Any, None]:
	repo = site.data_repository()
	dependencies = {'endpoint': 'https://query-main.wikidata.org/sparql', 'entity_url': 'https://www.wikidata.org/entity', 'repo': None}#repo}
	query_object = sparql.SparqlQuery(**dependencies)
	for elem in query_object.select(query):
		yield elem

def wbType_to_string(target, link: bool=True) -> str:
        if type(target) == pywikibot.ItemPage:
            site = pywikibot.Site().data_repository()
            item = pywikibot.ItemPage(site, target.id)
            text = get_labels(item)
            if link == False:
                    return text
            if pywikibot.Site().dbName() in item.sitelinks:
                    localpage = item.sitelinks[pywikibot.getSite().dbName()]
                    return "[[%s|%s]]" % (localpage, text)
            return "{{ill-wd|%s}}" % target.id
        elif type(target) == pywikibot.FilePage:
            if link:
                return str(target)
            else:
                return target.title()
        elif type(target) == pywikibot.WbQuantity:
            r = str(target.amount)
            if r.find('.') > -1:
                    r = r.replace('.',',')
            return r
        elif type(target) == pywikibot.WbTime:
            return "-".join(str(target.year), str(target.month), str(target.day))
        elif type(target) == pywikibot.WbMonolingualText:
            if target.language not in ['en', 'fr', 'ro']:
                    return None
            return str(target.text)
        return str(target)
