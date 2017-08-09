#!/usr/bin/python
# -:- coding:utf-8 -:-

import pywikibot
from pywikibot import config as user
from pywikibot import pagegenerators
from pywikibot.data import sparql

gallery = []

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


def treat(item):
    global gallery
    try:
        item = pywikibot.ItemPage( pywikibot.Site("wikidata", "wikidata"), item.title())
        item.get()
        name = item.labels.get('ro')
        print(name)
        siruta = item.claims.get('P843')[0].getTarget()
    except Exception as e:
        print(e)
        return
    rp = getWikiArticle(item)
    if not rp:
        gallery.append('File:Replace this image - temple.JPG|' + name + '/SIRUTA: ' + str(siruta))
        return
    img = item.claims.get('P18')
    if img:
        img = img[0].getTarget()
        #print(img)
        gallery.append(img.title() + '|[[' + rp.title() + '|' + str(siruta) + "]]")
        return

    pi = rp.page_image()
    if not pi:
        gallery.append('File:Replace this image - temple.JPG|[[' + rp.title() + '|' + str(siruta) + "]]")
        return
    gallery.append(pi.title() + '|[[' + rp.title() + '|' + str(siruta) + "]]")
 

def treat_sparql(dic):
    global gallery
    try:
        name = dic['itemLabel']
        img = dic['image']
        siruta = dic['siruta']
        if img:
            imgl = pywikibot.Link('File:' + img[img.rfind('/')+1:])
            gallery.append(imgl.title + '|[[' + (dic.get('page_title') or dic.get('countyLabel')) + '|' + siruta + "]]")
            return
        if dic.get('page_title'):
            rp = pywikibot.Page(pywikibot.Site('ro', 'wikipedia'), dic.get('page_title'))
            #print(rp)
            pi = rp.page_image()
            if pi:
                gallery.append(pi.title() + '|[[' + rp.title() + '|' + siruta + "]]")
                return
        gallery.append('File:Replace this image - temple.JPG|[[' + (dic.get('page_title') or "Județul " + dic.get('countyLabel')) + '|' + siruta + "]]")
    except Exception as e:
        print(dic)
        print(e)
        #raise
    
   
def dump_text(gallery):
    #art = pywikibot.Page(pywikibot.Site('ro', 'wikipedia'), 'Utilizator:Strainu/wlro')
    text = "<gallery>\n"
    for img in gallery:
        text += img + "\n"
    text += "</gallery>"
    #print(text)
    #art.put(text)
    return text

def add_text(where, t, overwrite=False):
    try:
        art = pywikibot.Page(pywikibot.Site('ro', 'wikipedia'), 'Utilizator:Strainu/wlro' + where)
        print(art)
        text = ""
        if art.exists() and not overwrite:
            text = art.get()
        text += t
        print(text)
        art.put(text)
    except Exception as e:
        print(e)

if __name__ == "__main__":
    pywikibot.handle_args()
    user.mylang = 'wikidata'
    user.family = 'wikidata'
    global gallery
    gallery = []
    #page = pywikibot.Page(pywikibot.Site(), "P843", ns=120)
    # page = pywikibot.Page(pywikibot.Site(), "Q193055", ns=0)
    #generator = pagegenerators.ReferringPageGenerator(page)
    repo = pywikibot.Site().data_repository()
    dependencies = {'endpoint': None, 'entity_url': None, 'repo': repo}
    query_object = sparql.SparqlQuery(**dependencies)
    query = """SELECT ?item ?itemLabel ?siruta ?image ?page_title ?countyLabel
WHERE 
{
  ?item wdt:P843 ?siruta.
  ?item wdt:P131* ?county.
  ?county wdt:P31 wd:Q1776764.
  OPTIONAL { ?item wdt:P18 ?image. }
  OPTIONAL {?article 	schema:about ?item ;
			schema:isPartOf <https://ro.wikipedia.org/>;  schema:name ?page_title  . }
  SERVICE wikibase:label { bd:serviceParam wikibase:language 'ro'. }
}
ORDER BY ?countyLabel ?itemLabel"""
    data = query_object.select(query)
    #print (data)
    #count = 0
    last_county = None
    try:
        #for page in generator:
            #treat(page)
            #count += 1
            #if count % 100 == 0:
            #    dump_text(gallery)
        for result in data:
            #print(result)
            if result['countyLabel'] != last_county:
                #add_text("", "\n* [[/" + result['countyLabel'] + "]]")
                if last_county:
                    add_text("/" + last_county, dump_text(gallery), True)
                gallery = []
                last_county = result['countyLabel']
            treat_sparql(result)
        add_text("/" + last_county, dump_text(gallery), True)
            
    except Exception as e:
        print(e)
        raise
    #finally:
        #dump_text(gallery)
