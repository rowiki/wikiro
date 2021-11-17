#!/usr/bin/python
# -*- coding: utf-8  -*-

import sys, json
sys.path.append("..")
import pywikibot
from pywikibot import pagegenerators
from pywikibot.data import sparql
from pywikibot import config as user
'''
This script creates redirects in the Cod namespace on ro.wp based on the SIRUTA
database. We create a link to the article if it exists.

See [[ro:Wikipedia:Coduri]] (ro) for details on the usage of the Cod namespace.
'''

options = {
    'shortcutPrefix': u'Cod:SIRUTA:',
}

def getWikiArticle(item):
    try:
        rp = item.getSitelink("rowiki")
        rp = pywikibot.Page(pywikibot.Site('ro', 'wikipedia'), rp)
        if rp.isRedirectPage():
            rp = rp.getRedirectTarget()
        return rp
    except Exception as e:
        pywikibot.error(e)
        return None


def main():
    user.mylang = 'wikidata'
    user.family = 'wikidata'
    repo = pywikibot.Site().data_repository()
    dependencies = {'endpoint': None, 'entity_url': None, 'repo': repo}
    query_object = sparql.SparqlQuery(**dependencies)
    query = """SELECT ?siruta ?page_title
    WHERE
    {
      ?item wdt:P843 ?siruta.
      OPTIONAL {?article    schema:about ?item ;
                            schema:isPartOf <https://ro.wikipedia.org/>;  schema:name ?page_title  . }
      SERVICE wikibase:label { bd:serviceParam wikibase:language 'ro'. }
   }"""
    data = query_object.select(query)
    if not data:
        pywikibot.output("No data")
        return

    user.mylang = 'ro'
    user.family = 'wikipedia'
    site = pywikibot.Site()

    for result in data:
        pywikibot.output(result)
        if result.get('page_title') == None:
            continue

        source_page = options['shortcutPrefix'] + result.get('siruta')
        pywikibot.output(source_page)
        source_page = pywikibot.Page(site, source_page)
        if source_page.exists():
            continue

        page = pywikibot.Page(site, result['page_title'])
        pywikibot.output(page.title())
        #page.get()
        if page.exists() and page.isRedirectPage():
            page = page.getRedirectTarget()
        elif not page.exists():
            continue
	
        page_text = u"#redirect [[{0}]]".format(page.title())
        source_page.put(page_text, "Redirecting code to the Wikipedia article")

if __name__ == "__main__":
	try:
		main()
	finally:
		pywikibot.stopme()

