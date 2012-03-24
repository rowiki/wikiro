#!/usr/bin/python
# -*- coding: utf-8  -*-
'''
This program parses the Wikimedia Commons images that include the "codeTemplate"
from the options and extract the following information in a csv file:
- code: the LMI code of the monument in the image
- name: the name of the picture, without extension or namespace
- url: the URL of the page that contains the image description
- fileUrl: the URL of the image itself
- license: the license of the image
    - CCBYSA: any version or flavour of CCBYSA, including re-licensed from GFDL
    - CCBY: any version or flavour of CCBY
    - GFDL
    - DP: public domain
    - "" (empty string): unable to determine the license
- author: the author (or first uploader if unable to determine) of the image
- date: the date the picture was taken (or the date of the upload)
- desc_ro: description in Romanian, if available
- desc_en: description in English, if available
- lat: latitude of the monument, if avalailable (WGS84 decimal)
- long: longitude of the monument, if avalailable (WGS84 decimal)
- quality: 1 if the image was marked as "good" in any way

'''

import sys
import time
import warnings
import json
import csv
import string
import cProfile
import re
sys.path.append("..")
import wikipedia
import pagegenerators
import config as user
from pywikibot import *
import strainu_functions as strainu

options = {
    'namespaces': [6],
    'codeTemplate': "Monument_istoric",
    'infoboxes': [],
    'qualityTemplates':
    [
    u'Valued image',
    u'QualityImage',
    u'Assessments',
    u'Wiki Loves Monuments 2011 Europe nominee'
    ],
}


codeRegexp = re.compile("(([a-z]{1,2})-(i|ii|iii|iv)-([a-z])-([a-z])-([0-9]{5}(\.[0-9]{2,3})?))", re.I)
errorRegexp = re.compile("eroare\s?=\s?([^0])", re.I)
geohackRegexp = re.compile("geohack\.php\?pagename=(.*?)&(amp;)?params=(.*?)&(amp;)?language=")
qualityRegexp = None
fullDict = {}
_log = "europeana_images.csv"
_flog = None

def initLog():
    global _flog, _log;
    _flog = open(_log, 'a+')
    
def closeLog():
    global _flog
    _flog.close()

def log(list):
    #wikipedia.output(str(list))
    line = ""
    for s in list:
        #s = s.replace( '"', '\\"')
        s = s.replace(u'"', u'”')
        line += "\"" + s.encode("utf-8") + "\";"
    line = line[:-1] + "\n"
    _flog.write(line)
    
def dms2dec(deg, min, sec, sign):
    return sign * (deg + (min / 60.0) + (sec / 3600.0))
    
def geosign(check, plus, minus):
    if check == plus:
        return 1
    elif check == minus:
        return -1
    else:
        return 0 #this should really never happen
    
def parseGeohackLinks(page):
    #title = page.title()
    #html = page.site().getUrl( "/wiki/" + page.urlname(), True)
    output = page.site().getUrl("/w/api.php?action=parse&format=json&page=" +
            page.urlname() + "&prop=externallinks&uselang=ro")
    linksdb = json.loads(output)
    title = linksdb["parse"]["title"]
    links = linksdb["parse"]["externallinks"]
    global geohackRegexp
    geohack_match = None
    for item in links:
        geohack_match = geohackRegexp.search(item)
        if geohack_match <> None:
            link = geohack_match.group(3)
            #wikipedia.output(geohack_match.group(3))
            break
    if geohack_match == None or link == None or link == "":
        #wikipedia.output("No geohack link found in article")
        return 0,0
    #valid formats:
    # D_M_S_N_D_M_S_E
    # D_M_N_D_M_E
    # D_N_D_E
    # D;D
    # D_N_D_E_to_D_N_D_E 
    # (see https://wiki.toolserver.org/view/GeoHack#params for details)
    link = link.replace(",", ".") #make sure we're dealing with US-style numbers
    tokens = link.split('_')
    #wikipedia.output(tokens)
    #sanitize non-standard strings
    l = tokens[:]
    for token in l:
        if token.strip() == '' or string.find(token, ':') > -1 or \
                string.find(token, '{{{') > -1:
            tokens.remove(token)
    numElem = len(tokens)
    if tokens[0] == link: #no _
        tokens = tokens[0].split(';')
        if float(tokens[0]) and float(tokens[1]): # D;D
            lat = tokens[0]
            long = tokens[1]
        else:
            wikipedia.output(u"*''E'': [[:%s]] Problemă (1) cu legătura Geohack: nu pot \
                identifica coordonatele în grade zecimale: %s" % (title, link))
            return 0,0
    elif numElem == 9: # D_N_D_E_to_D_N_D_E or D_M_S_N_D_M_S_E_something
        if tokens[4] <> "to":
            wikipedia.output(u"*[[%s]] We should ignore parameter 9: %s (%s)" % 
                            (title, tokens[8], link))
            deg1 = float(tokens[0])
            min1 = float(tokens[1])
            sec1 = float(tokens[2])
            sign1 = geosign(tokens[3],'N','S')
            deg2 = float(tokens[4])
            min2 = float(tokens[5])
            sec2 = float(tokens[6])
            sign2 = geosign(tokens[7],'E','V')
            lat = dms2dec(deg1, min1, sec1, sign1)
            long = dms2dec(deg2, min2, sec2, sign2)
            if sec1 == 0 and sec2 == 0:
                wikipedia.output(u"*''W'': [[:%s]] ar putea avea nevoie de actualizarea \
                    coordonatelor - valoarea secundelor este 0" % title)
        else:
            lat1 = float(tokens[0]) * geosign(tokens[1], 'N', 'S')
            long1 = float(tokens[2]) * geosign(tokens[3], 'E', 'V')
            lat2 = float(tokens[5]) * geosign(tokens[6], 'N', 'S')
            long2 = float(tokens[7]) * geosign(tokens[8], 'E', 'V')
            if lat1 == 0 or long1 == 0 or lat2 == 0 or long2 == 0: 
                #TODO: one of them is 0; this is also true for equator and GMT
                wikipedia.output(u"*''E'': [[:%s]] Problemă (2) cu legătura Geohack: - \
                    una dintre coordonatele de bounding box e 0: %s" % 
                    (title, link))
                return 0,0
            lat = (lat1 + lat2) / 2
            long = (long1 + long2) / 2
    elif numElem == 8: # D_M_S_N_D_M_S_E
        deg1 = float(tokens[0])
        min1 = float(tokens[1])
        sec1 = float(tokens[2])
        sign1 = geosign(tokens[3],'N','S')
        deg2 = float(tokens[4])
        min2 = float(tokens[5])
        sec2 = float(tokens[6])
        sign2 = geosign(tokens[7],'E','V')
        lat = dms2dec(deg1, min1, sec1, sign1)
        long = dms2dec(deg2, min2, sec2, sign2)
        if sec1 == 0 and sec2 == 0:
            wikipedia.output(u"*''W'': [[:%s]] ar putea avea nevoie de actualizarea \
                coordonatelor - valoarea secundelor este 0" % title)
    elif numElem == 6: # D_M_N_D_M_E
        deg1 = float(tokens[0])
        min1 = float(tokens[1])
        sec1 = 0.0
        sign1 = geosign(tokens[2],'N','S')
        deg2 = float(tokens[3])
        min2 = float(tokens[4])
        sec2 = 0.0
        sign2 = geosign(tokens[5],'E','V')
        lat = dms2dec(deg1, min1, sec1, sign1)
        long = dms2dec(deg2, min2, sec2, sign2)
        wikipedia.output(u"*''E'': [[:%s]] are nevoie de actualizarea coordonatelor \
            nu sunt disponibile secundele" % title)
    elif numElem == 4: # D_N_D_E
        deg1 = float(tokens[0])
        sign1 = geosign(tokens[1],'N','S')
        deg2 = float(tokens[2])
        sign2 = geosign(tokens[3],'E','V')
        lat = sign1 * deg1
        long = sign2 * deg2
    else:
        wikipedia.output(u"*''E'': [[:%s]] Problemă (3) cu legătura Geohack: nu pot \
            identifica nicio coordonată: %s" % (title, link))
        return 0,0
    if lat < 43 or lat > 48.25 or long < 20 or long > 29.67:
        wikipedia.output(u"*''E'': [[:%s]] Coordonate invalide pentru România: %f,%f \
            (extrase din %s)" % (title, lat, long, link))
        return 0,0
    return lat,long
    
def extractParam(haystack, needle):
    needle = needle.lower()
    for param in haystack:
            comp = param.strip().lower()
            if comp.startswith(needle):
                return param[param.find('=') + 1:].strip()
    return ""
    
def findTemplate(list, tl):
    for t in list:
        if t[0] == tl:
            return t
    return None

def checkAllCodes(result, title):
    if len(result) == 0:
        wikipedia.output(u"*''E'': [[:%s]] nu conține niciun cod LMI valid" % title)
        return None
    elif len(result) > 1:
        code = result[0][0]
        if (code.rfind('.') > -1):
            c1 = code[-8:code.rfind('.')] 
        else: 
            c1 = code[-5:]
        for res in result:
                if code != res[0]:
                    point = res[0].rfind('.')
                    if (point > -1):
                        c2 = res[0][-8:point] 
                    else: 
                        c2 = res[0][-5:]
                    if c1 != c2: #they're NOT sub-monuments
                        wikipedia.output(u"*''W'': [[:%s]] conține mai multe coduri LMI \
                            distincte: %s, %s. Dacă nu e vorba de o \
                            greșeală, ar putea fi oportună separarea \
                            articolelor/decuparea pozelor pentru acele \
                            coduri" % (title, code, res[0]))
                        return None
    return result[0][0]#first regular expression

def processArticle(text, page, conf):
    urlPrefix = "http://commons.wikimedia.org/w/index.php?title="
    title = page.title()
    templates = page.templatesWithParams()
    wikipedia.output(str(templates))
    
    if re.search(errorRegexp, text) <> None:
        return
    global codeRegexp
    code = checkAllCodes(re.findall(codeRegexp, text), title)
    if code == None:
        return
        
    if qualityRegexp <> None and re.search(qualityRegexp, text) <> None:
        quality = "1"
    else:
        quality = "0"
        
    lat, long = parseGeohackLinks(page)

    desc_ro = ""
    desc_en = ""
    author = ""
    date = ""
    license = ""
    history = page.getFileVersionHistory()[0]
    information = findTemplate(templates, u'Information')
    dateTl = findTemplate(templates, u'Date')
    
    
    #desc_ro
    t = findTemplate(templates, 'Ro')
    if t <> None:
        desc_ro = t[1][0]
        if desc_ro.startswith(u"1="):
            desc_ro = desc_ro[2:]
        desc_ro = textlib.removeDisabledParts(desc_ro.replace("\n", "<br/>"))
    
    #desc_en
    t = findTemplate(templates, 'En')
    if t <> None:
        desc_en = t[1][0]
        if desc_en.startswith(u"1="):
            desc_en = desc_en[2:]
        desc_en = textlib.removeDisabledParts(desc_en.replace("\n", "<br/>"))
        
    #author
    if information:#search for {{Information}}
        author = extractParam(information[1], u"author")
        author = textlib.removeDisabledParts(author.replace("\n", "<br/>"))
        wikipedia.output(author)
        author = strainu.stripLink(author).strip()
        author = strainu.stripExternalLink(author).strip()
        wikipedia.output(author)
    else:#search for the author of the first upload
        wikipedia.output(str(history))
        author = history[1]
        
    #date
    if dateTl: #search for {{Date}}
        date = dateTl[1][0] + "-" + dateTl[1][1] + "-" + dateTl[1][2]
    elif information:#search for {{Information}}
        date = extractParam(information[1], u"date").strip()
        date = textlib.removeDisabledParts(date.replace("\n", "<br/>"))
        wikipedia.output(date)
    else: #search for the date of the first upload
        date = history[0][:10]
    
    #license
    if findTemplate(templates, u'PD') or findTemplate(templates, u'PD-self'):
        wikipedia.output("1")
        license = "DP"
    elif findTemplate(templates, u'Cc-by-sa-1.0') or \
            findTemplate(templates, u'Cc-by-sa-2.0') or \
            findTemplate(templates, u'Cc-by-sa-3.0') or \
            findTemplate(templates, u'Cc-by-sa-2.5') or \
            findTemplate(templates, u'Cc-by-sa-3.0-ro') or \
            findTemplate(templates, u'Cc-by-sa-3.0-migrated'):
        wikipedia.output("2")
        license = "CCBYSA"
    elif findTemplate(templates, u'Cc-by-1.0') or \
            findTemplate(templates, u'Cc-by-2.0') or \
            findTemplate(templates, u'Cc-by-3.0') or \
            findTemplate(templates, u'Cc-by-2.5') or \
            findTemplate(templates, u'Cc-by-3.0-ro'):
        wikipedia.output("3")
        license = "CCBY"
    elif findTemplate(templates, u'GFDL') or findTemplate(templates, u'GFDL-self') or findTemplate(templates, u'GFDL-user'):
        wikipedia.output("4")
        license = "GFDL"
    elif findTemplate(templates, u'Self'):
        wikipedia.output(str(findTemplate(templates, u'Self')[1]))
        lic = ",".join(findTemplate(templates, u'Self')[1])
        if lic[0:1] == "1=":
            lic = lic[2:]
        lic = lic.lower()
        wikipedia.output(lic)
        if lic.find(u'cc-by-sa-1.0') > -1 or \
                lic.find(u'cc-by-sa-2.0') > -1 or \
                lic.find(u'cc-by-sa-3.0') > -1 or \
                lic.find(u'cc-by-sa-2.5') > -1 or \
                lic.find(u'cc-by-sa-3.0-ro') > -1 or \
                lic.find(u'cc-by-sa-3.0-migrated') > -1:
            wikipedia.output("5.1")
            license = "CCBYSA"
        elif lic.find(u'cc-by-1.0') > -1 or \
                lic.find(u'cc-by-2.0') > -1 or \
                lic.find(u'cc-by-3.0') > -1 or \
                lic.find(u'cc-by-2.5') > -1 or \
                lic.find(u'cc-by-3.0-ro') > -1:
            wikipedia.output("5.2")
            license = "CCBY"
        elif lic.find(u'gfdl') > -1:
            wikipedia.output("5.3")
            license = "GFDL"
        elif lic.find(u'pd') > -1:
            wikipedia.output("5.4")
            license = "DP"
    
    #alternative method, really shaky
    if license == "":
        if text.lower().find('cc-by-sa') or \
                text.lower().find('ccbysa'):
            wikipedia.output("6")
            license = "CCBYSA"
        elif text.lower().find('cc-by') or \
                text.lower().find('ccby'):
            wikipedia.output("7")
            license = "CCBY"
        elif text.find('{{PD-'):
            wikipedia.output("8")
            license = "DP"
        elif information <> None:#search for {{Information}}
            wikipedia.output("9")
            license = extractParam(templates['Information'], "Permission")
            
    #pretty title
    pretty = page.titleWithoutNamespace()
    pretty = pretty[:pretty.rfind('.')]
    
    #code, title, pageURL, fileURL, desc_en, desc_ro, quality, author, date, lat, long, license
    log([code, pretty, urlPrefix + page.urlname(), page.fileUrl(), license, author, date, desc_ro, desc_en, str(lat), str(long), quality])
    
def main():
    user.mylang = "commons"
    user.family = "commons"
    start = None
    for arg in wikipedia.handleArgs():
        if arg.startswith('-start:'):
            start = arg [len('-start:'):]
        
    site = wikipedia.getSite(code = user.mylang, fam=user.family)
    rowTemplate = wikipedia.Page(site, u'%s:%s' % (site.namespace(10), \
                                options.get('codeTemplate')))
    global _log
    global fullDict
    global qualityRegexp
    initLog()
    
    qReg = u"\{\{("
    for t in options.get('qualityTemplates'):
        qReg = qReg + t + "|"
    qReg = qReg[:-1]
    qReg += ")(.*)\}\}"
    #wikipedia.output(qReg)
    qualityRegexp = re.compile(qReg, re.I)

    for namespace in options.get('namespaces'):
        transGen = pagegenerators.ReferringPageGenerator(rowTemplate, 
                                    onlyTemplateInclusion=True)
        filteredGen = pagegenerators.NamespaceFilterPageGenerator(transGen, 
                                    [namespace], site)
        pregenerator = pagegenerators.PreloadingGenerator(filteredGen, 250)
        
        count = 0
        for page in pregenerator:
            wikipedia.output(u'Working on "%s"' % page.title())
            if start and page.urlname() <> start:
                continue
            elif start:
                start = None
                continue
            if page.exists() and not page.isRedirectPage():
                image = wikipedia.ImagePage(site, page.title())
                # Do some checking
                processArticle(image.get(), image, options)
                count += 1
        print count

if __name__ == "__main__":
    try:
        #cProfile.run('main()', './parseprofile.txt')
        main()
    finally:
        wikipedia.stopme()
