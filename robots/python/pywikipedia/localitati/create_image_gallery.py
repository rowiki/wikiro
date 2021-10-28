#!/usr/bin/python
#-*- coding:utf-8 -*-

import json
import re
import sys

import pywikibot
from pywikibot import pagegenerators
from pywikibot.data import sparql
from pywikibot import config as user

import fill_wikidata_info as wikidata

sys.path.append("wikiro/robots/python")
import otherconfig as config
from geo import mapillary

gallery = []
table = []
stats = {}
candidates = {}
fix = False
always = False
do_mapillary = False

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


def fix_local_image(img, item):
    global always, fix

    if not fix:
        return
    # check if page exists on commons
    if not img.fileIsShared():
        # local image, nothing to fix
        return
    img = pywikibot.FilePage(pywikibot.Site('commons', 'commons'),img.title(with_ns=False))

    worker = wikidata.ImageProcessing(wikidata.config)
    worker.always = always
    worker.setItem(item)
    worker.addImage(img)
    always = worker.always


def find_mapillary_candidates(dic):
    global candidates, do_mapillary
    if not do_mapillary:
        return
    t = []
    coord = dic.get("coord")
    if not coord:
        return
    match = re.search(r"\(([0-9\.]+)\s([0-9\.]+)\)", coord)
    if not match:
        return
    lon = match.group(1)
    lat = match.group(2)
    res = mapillary.get_images_looking(config.mapillary.get('key'), lat, lon, 50, 10)
    if not res:
        return
    for image_data in res.get("features"):
        t.append(mapillary.get_image_url(image_data["properties"]["key"], 2048))
    if len(t):
        candidates[dic.get('page_title')] = t
        # print(t)


def dump_mapillary_data():
    global candidates
    global do_mapillary

    if not do_mapillary:
        return

    with open("mapillary_cities_pictures.json", "w") as f:
        json.dump(candidates, f)


def treat_sparql(dic):
    global gallery
    global stats
    global table
    try:
        table_template = """|- class="{css}"
| {siruta} 
| class="uat-list link" | [[{name}]] 
| class="uat-list image" | [[File:{file}|faracadru|200px]]<br/>{{{{Plain link|1={upload}|2=Încarcă imagine}}}}"""
        name = dic['itemLabel']
        img = dic['image']
        upperImg = dic['upperImage']
        siruta = dic['siruta']
        linkLabel = name + " (" + siruta + ")"
        county = dic.get('countyLabel')
        commons = dic.get('commons')
        #print(dic)
        if dic.get('mayor') != None:
            css = "uat-list uat"
        else:
            css = "uat-list settlement"
        if commons:
            cats = commons.replace(' ','_')
        else:
            cats = ""
        altLink = u'Județul ' + county
        if county not in stats:
            stats[county] = {
                'total': 0,
                'wikidata': 0,
                'local': 0,
                'missing': 0
            }
        stats[county]['total'] += 1
        stats['total']['total'] += 1
        upload_link = '//commons.wikimedia.org/w/index.php?title=Special:UploadWizard&campaign=wlro&uselang=ro&id=' + siruta + '&categories=' + cats
        if img:
            imgl = pywikibot.Link('File:' + img[img.rfind('/')+1:])
            gallery.append(imgl.title + '|<div style="background:lightgreen">[[' + (dic.get('page_title') or altLink) + '|' + linkLabel + ']]</div>')
            table.append(
	        table_template.format(css=css,
                                      siruta=siruta,
                                      name=(dic.get('page_title') or name),
                                      file=imgl.title,
                                      upload=upload_link))
            stats[county]['wikidata'] += 1
            stats['total']['wikidata'] += 1
            return
        if upperImg:
            imgl = pywikibot.Link('File:' + upperImg[upperImg.rfind('/')+1:])
            gallery.append(imgl.title + '|<div style="background:lightgreen">[[' + (dic.get('page_title') or altLink) + '|' + linkLabel + ']]</div>')
            table.append(
	        table_template.format(css=css,
                                      siruta=siruta,
                                      name=(dic.get('page_title') or name),
                                      file=imgl.title,
                                      upload=upload_link))
            stats[county]['wikidata'] += 1
            stats['total']['wikidata'] += 1
            return

        find_mapillary_candidates(dic)
        if dic.get('page_title'):
            rp = pywikibot.Page(pywikibot.Site('ro', 'wikipedia'), dic.get('page_title'))
            #print(rp)
            pi = rp.page_image()
            if pi and " map" not in pi.title() and \
                      "Map." not in pi.title() and \
                      "harta" not in pi.title().lower() and \
                      "3D" not in pi.title() and \
                      "Josephinische" not in pi.title() and \
                      "svg" not in pi.title() and \
                      " judetul " not in pi.title() and \
                      " distrikto " not in pi.title() and \
                      " jud " not in pi.title():
                gallery.append(pi.title() + '|<div style="background:yellow">[[' + rp.title() + '|' + name + " (" + siruta + ")]]</div>")
                table.append(
	            table_template.format(css=css,
                                          siruta=siruta,
                                          name=(dic.get('page_title') or name),
                                          file=pi.title().replace('Fișier:',''),
                                          upload=upload_link))
                stats[county]['local'] += 1
                stats['total']['local'] += 1
                fix_local_image(pi, rp.data_item())
                return
        gallery.append(u'File:Replace this image - temple.JPG|<div style="background:red">[[' + (dic.get('page_title') or altLink) + '|' + linkLabel + ']]</div>')
        table.append(
	        table_template.format(css=css,
                                      siruta=siruta,
                                      name=(dic.get('page_title') or name),
                                      file="Replace this image - temple.JPG",
                                      upload=upload_link))
        stats[county]['missing'] += 1
        stats['total']['missing'] += 1
    except Exception as e:
        print(dic)
        print(e)
        raise

def dump_table(table):
    text = """<templatestyles src="Wikipedia:Wiki Loves România/styles.css" />
Pentru a încărca o imagine, faceți clic pe legătura de sub imagine  și veți fi dus la asistentul de încărcare de unde puteți încărca noi imagini cu localitatea respectivă. Puteți încărca imagini chiar dacă există deja o imagine pentru localitatea respectivă.
    
{| class="wikitable sortable center"
! Cod SIRUTA
! Nume
! class="unsortable" | Imagine
"""
    for line in table:
        text += line + "\n"
    text += "|}"
    return text

def dump_gallery(gallery):
    #art = pywikibot.Page(pywikibot.Site('ro', 'wikipedia'), 'Utilizator:Strainu/wlro')
    text = "<gallery>\n"
    for img in gallery:
        text += img + "\n"
    text += "</gallery>"
    # print(text)
    # art.put(text)
    return text


def add_text(where, t, overwrite=False):
    try:
        art = pywikibot.Page(pywikibot.Site('ro', 'wikipedia'), 'Wikipedia:Wiki Loves România/' + where)
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
    if county == 'total':
        name = "'''Total'''"
    else:
        name = "[[/" + county + "|" + county + "]]"
    table_line = """
|-
| %s || %d || data-sort-value="%f" | %d (%.2f%%) || data-sort-value="%f" | %d (%.2f%%) || data-sort-value="%f" | %d (%.2f%%) || %s"""
    wikidata_percentage = float(stats[county]['wikidata']) * 100.0 / stats[county]['total']
    local_percentage = float(stats[county]['local']) * 100.0 / stats[county]['total']
    missing_percentage = float(stats[county]['missing']) * 100.0 / stats[county]['total']
    bar = "<span style=\"background-color:green\">"
    for i in range(int(round(wikidata_percentage))):
        bar += "&nbsp;"
    bar += "</span>"
    bar += "<span style=\"background-color:yellow\">"
    for i in range(int(round(local_percentage))):
        bar += "&nbsp;"
    bar += "</span>"
    bar += "<span style=\"background-color:red\">"
    for i in range(int(round(missing_percentage))):
        bar += "&nbsp;"
    bar += "</span>"
    text = table_line % (name,
            stats[county]['total'],
            wikidata_percentage,
            stats[county]['wikidata'],
            wikidata_percentage,
            local_percentage,
            stats[county]['local'],
            local_percentage,
            missing_percentage,
            stats[county]['missing'],
            missing_percentage,
            bar
           )
    return text


def main():
    global do_mapillary, always, fix
    for arg in pywikibot.handle_args():
        if arg.startswith("-fix"):
            fix = True
        if arg.startswith("-always"):
            always = True
        if arg.startswith("-mapillary"):
            do_mapillary = True
    user.mylang = 'wikidata'
    user.family = 'wikidata'
    global gallery
    global table
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
    query = """SELECT ?item ?itemLabel ?mayor ?siruta ?image ?upperImage ?page_title ?countyLabel ?coord ?commons
    WHERE
    {
      ?item wdt:P843 ?siruta.
      ?item wdt:P131* ?county.
      ?county wdt:P31 wd:Q1776764.
      #OPTIONAL { ?item wdt:P625 ?coord. }
      OPTIONAL { ?item wdt:P18 ?image. }
      OPTIONAL { ?item wdt:P373 ?commons. }
      OPTIONAL { ?item wdt:P6 ?mayor. }
      #OPTIONAL {
      #  { ?item wdt:P31 wd:Q34842263. } UNION {?item wdt:P31 wd:Q34842776.}
      #  ?item wdt:P1376 ?upper.
      #  ?upper wdt:P18 ?upperImage.
      #}
      OPTIONAL {?article    schema:about ?item ;
                            schema:isPartOf <https://ro.wikipedia.org/>;  schema:name ?page_title  . }
      SERVICE wikibase:label { bd:serviceParam wikibase:language 'ro'. }
   }
   ORDER BY ?countyLabel ?siruta ?itemLabel"""
    data = query_object.select(query)
    if not data:
        return
    #print (data)
    #count = 0
    last_county = None
    try:
        #for page in generator:
            #treat(page)
            #count += 1
            #if count % 100 == 0:
            #    dump_text(gallery)
        stats['total'] = {
            'total': 0,
            'wikidata': 0,
            'local': 0,
            'missing': 0
        }
        print("Working on %d results" % len(data))
        for result in data:
            #print(result)
            if result['countyLabel'] != last_county:
                if last_county:
                    dump_mapillary_data()
                    #add_text(last_county, dump_gallery(gallery), True)
                    add_text(last_county, dump_table(table), True)
                    main_text += generate_stats(last_county)
                gallery = []
                table = []
                last_county = result['countyLabel']
            treat_sparql(result)
        #add_text(last_county, dump_gallery(gallery), True)
        add_text(last_county, dump_table(table), True)
        main_text += generate_stats(last_county)
        main_text += generate_stats('total') + "\n|}"
        art = pywikibot.Page(pywikibot.Site('ro', 'wikipedia'), 'Wikipedia:Wiki Loves România#Situația_la_zi')
        #art.put(main_text, "Actualizare statistici")
        dump_mapillary_data()
    except Exception as e:
        print(e)
        raise
    #finally:
        #dump_text(gallery)

if __name__ == "__main__":
    main()
