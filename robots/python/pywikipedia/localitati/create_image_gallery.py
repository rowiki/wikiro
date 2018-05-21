#!/usr/bin/python
# -:- coding:utf-8 -:-

import pywikibot
from pywikibot import config as user
from pywikibot import pagegenerators
from pywikibot.data import sparql

gallery = []
stats = {}

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
        #print(name)
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
    global stats
    try:
        name = dic['itemLabel']
        img = dic['image']
        upperImg = dic['upperImage']
        siruta = dic['siruta']
        linkLabel = name + " (" + siruta + ")"
        county = dic.get('countyLabel')
        altLink = u'Județul ' + county
        if county not in stats:
            stats[county] = {
                'total': 0,
                'wikidata': 0,
                'local': 0,
                'missing': 0
            }
        stats[county]['total'] += 1
        if img:
            imgl = pywikibot.Link('File:' + img[img.rfind('/')+1:])
            gallery.append(imgl.title + '|[[' + (dic.get('page_title') or altLink) + '|' + linkLabel + ']]')
            stats[county]['wikidata'] += 1
            return
        if upperImg:
            imgl = pywikibot.Link('File:' + upperImg[upperImg.rfind('/')+1:])
            gallery.append(imgl.title + '|[[' + (dic.get('page_title') or altLink) + '|' + linkLabel + ']]')
            stats[county]['wikidata'] += 1
            return

        if dic.get('page_title'):
            rp = pywikibot.Page(pywikibot.Site('ro', 'wikipedia'), dic.get('page_title'))
            #print(rp)
            pi = rp.page_image()
            if pi and " map" not in pi.title() and \
                      "harta" not in pi.title() and \
                      "3D" not in pi.title() and \
                      "Josephinische" not in pi.title() and \
                      "svg" not in pi.title():
                gallery.append(pi.title() + '|[[' + rp.title() + '|' + name + " (" + siruta + ")]]")
                stats[county]['local'] += 1
                return
        gallery.append(u'File:Replace this image - temple.JPG|[[' + (dic.get('page_title') or altLink) + '|' + linkLabel + ']]')
        stats[county]['missing'] += 1
    except Exception as e:
        print(dic)
        print(e)
        raise
    
   
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
        art = pywikibot.Page(pywikibot.Site('ro', 'wikipedia'), 'Utilizator:Strainu/wlro/' + where)
        print(art)
        text = ""
        if art.exists() and not overwrite:
            text = art.get()
        text += t
        #print(text)
        art.put(text, "Actualizare galerie")
    except Exception as e:
        print(e)

def generate_stats(county):
    table_line = """
|-
| %s || %d || %d (%.2f%%) || %d (%.2f%%) || %d (%.2f%%) || %s"""
    wikidata_percentage = float(stats[county]['wikidata']) * 100.0 / stats[county]['total']
    local_percentage = float(stats[county]['local']) * 100.0 / stats[county]['total']
    missing_percentage = float(stats[county]['missing']) * 100.0 / stats[county]['total']
    bar = "<span style=\"background-color:green\">"
    for i in range(round(wikidata_percentage)):
        bar += "&nbsp;"
    bar += "</span>"
    bar += "<span style=\"background-color:yellow\">"
    for i in range(round(local_percentage)):
        bar += "&nbsp;"
    bar += "</span>"
    bar += "<span style=\"background-color:red\">"
    for i in range(round(missing_percentage)):
        bar += "&nbsp;"
    bar += "</span>"
    text = table_line % ( "[[/" + county + "|" + county + "]]",
            stats[county]['total'],
            stats[county]['wikidata'],
            wikidata_percentage,
            stats[county]['local'],
            local_percentage,
            stats[county]['missing'],
            missing_percentage,
            bar
	)
    return text

if __name__ == "__main__":
    pywikibot.handle_args()
    user.mylang = 'wikidata'
    user.family = 'wikidata'
    global gallery
    gallery = []
    main_text = """{| class=\"wikitable sortable\"
! Județ
! Total
! Wikidata
! Local
! Fără imagine
! Acoperire"""
    #page = pywikibot.Page(pywikibot.Site(), "P843", ns=120)
    # page = pywikibot.Page(pywikibot.Site(), "Q193055", ns=0)
    #generator = pagegenerators.ReferringPageGenerator(page)
    repo = pywikibot.Site().data_repository()
    dependencies = {'endpoint': None, 'entity_url': None, 'repo': repo}
    query_object = sparql.SparqlQuery(**dependencies)
    query = """SELECT ?item ?itemLabel ?siruta ?image ?upperImage ?page_title ?countyLabel
WHERE 
{
  ?item wdt:P843 ?siruta.
  ?item wdt:P131* ?county.
  ?county wdt:P31 wd:Q1776764.
  OPTIONAL { ?item wdt:P18 ?image. }
  OPTIONAL { 
    { ?item wdt:P31 wd:Q34842263. } UNION {?item wdt:P31 wd:Q34842776.}
    ?item wdt:P1376 ?upper.          
    ?upper wdt:P18 ?upperImage.
  }
  OPTIONAL {?article    schema:about ?item ;
                        schema:isPartOf <https://ro.wikipedia.org/>;  schema:name ?page_title  . }
  SERVICE wikibase:label { bd:serviceParam wikibase:language 'ro'. }
}
ORDER BY ?countyLabel ?itemLabel ?siruta"""
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
                    add_text(last_county, dump_text(gallery), True)
                    main_text += generate_stats(last_county)
                gallery = []
                last_county = result['countyLabel']
            treat_sparql(result)
        add_text(last_county, dump_text(gallery), True)
        main_text += generate_stats(last_county) + "\n|}"
        art = pywikibot.Page(pywikibot.Site('ro', 'wikipedia'), 'Utilizator:Strainu/wlro')
        art.put(main_text, "Actualizare statistici")        
    except Exception as e:
        print(e)
        raise
    #finally:
        #dump_text(gallery)
