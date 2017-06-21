#!/usr/bin/python
# -*- coding: utf-8  -*-

#
# (C) Strainu, 2012-2015
#
# Distributed under the terms of the GPLv2 license.
#
#

import re, urllib.request, urllib.error, urllib.parse, urllib.request, urllib.parse, urllib.error
import pywikibot
from pywikibot import textlib
from pywikibot import pagegenerators
import math
import time
import string
import csv


#extract the first instance of a template from the page text
# @param text: the text of the page
# @param template: the name of the template we are looking for
# @return: the text of the template
def extractTemplate(text, template):
    #pywikibot.output(text)
    template = template.replace("_", " ")
    template = template.replace(" ", "[ _]")
    match = re.search("\{\{\s*(" + template + ")", text, re.I)
    if match == None:
        return None
    tl = text[match.start():]
    text = text[match.start() + 2:]
    open = 0
    close = 1
    innerTlCount = 0
    while open < close and open != -1:
        open = text.find("{")
        close = text.find("}") + 1
        #print "one" + str(open) + " " + str(close)
        if open >= close or open == -1:
            while innerTlCount > 0:
                #print "two" + str(close) + " "  + str(innerTlCount)
                close = text.find("}", close) + 1
                #print "three" + str(close)
                innerTlCount -= 1
        if open < close:
            #print text[open + 1:close]
            innerTlCount += text[open + 1:close].count('{')
        #print "four" + str(open) + " " + str(close) + " " + str(innerTlCount)
        text = text[close:]
        #print text
    tl = tl.replace(text[1:], "") #the [1:] is because we want to grab the second '}'
    return tl

#convert a template to a dictionary
#since we're using python2, the dictionary class is unordered. We therefore
# need to keep the order of the keys in order to be able to reconstruct the
# template
# @param: the template text
# @return: a tuple containing a dictionary containing the params as keys and \
#	   the values as value and a list containing the parameter names \
#          The name of the template has the key "_name"
def tl2Dict(template):
    marker = "@@"
    Rtemplate = re.compile(r'{{(msg:)?(?P<name>[^{\|]+?)(\|(?P<params>[^{]+?))?}}')
    Rmath = re.compile(r'<math>[^<]+</math>')
    Rref = re.compile(r'<ref.*?>[^<]+</ref>')
    Rmarker = re.compile(r'%s(\d+)%s' % (marker, marker))
    count = 0
    intern = {}
    for m in Rref.finditer(template):
        count += 1
        text = m.group()
        #print "ref " + text
        template = template.replace(text, '%s%d%s' % (marker, count, marker))
        intern[count] = text
    for m in Rmath.finditer(template):
        count += 1
        text = m.group()
        #print "math " + text
        template = template.replace(text, '%s%d%s' % (marker, count, marker))
        intern[count] = text
    while Rtemplate.search(template[2:]) is not None:
        for m in Rtemplate.finditer(template):
            count += 1
            text = m.group()
            #print "template " + text
            text2 = text
            for m2 in Rmarker.finditer(text):
                text2 = text2.replace(m2.group(), intern[int(m2.group(1))])
            template = template.replace(text, '%s%d%s' % (marker, count, marker))
            intern[count] = text2
    _dict = {}
    _keyList = []
    #print intern
    template = re.sub(r'(\r|\n)', '', template)
    template = template[0:len(template)-2]#get rid of '}}'
    params = template.split('|')
    key = ""
    value = ""
    anon_params = 1
    for line in params:
        line = line.split('=')
        #pywikibot.output(str(line))
        if (len(line) > 1):
            key = line[0]#.encode("utf8")
            key = key.strip()
            value = "=".join(line[1:])
            _dict[key] = value
            if not key in _keyList:
                _keyList.append(key)
        elif line[0].startswith('{{') and not "_name" in _dict: #name of the template
            #pywikibot.output("Name: " + line[0][2:])
            _dict["_name"] = line[0][2:].strip()
            if not key in _keyList:
                _keyList.append("_name")
        elif line[0] != "" and key != "":#the first line might not begin with {{
            _dict[key] = _dict[key] + "|" + line[0]
            if not key in _keyList:
                _keyList.append(key)
        else:#anonymous param, hopefully
            _dict[anon_params] = line[0]
            if not anon_params in _keyList:
                _keyList.append(anon_params)
            anon_params += 1
	#TODO: add anonymous parameters
    for key, value in list(_dict.items()):
        matches = Rmarker.findall(value)
        for match in matches:
            count = int(match)
            value = value.replace('%s%d%s' % (marker, count, marker), intern[count])
            _dict[key] = value
    #print _dict
    return (_dict, _keyList)
    
#from [[a|b]] get b
def stripLink(text):
    start = text.find("[[")
    end = text.find("]]")
    sep = text.find("|")
    
    if start < 0 and end < 0:
        return text
    if start < 0 or end < 0:
        return None
    if start > end or (start > sep and sep > -1) or sep > end:
        return None
        
    if sep < 0:
        return text[start+2:end]
    else:
        return text[sep+1:end]

#from [[a|b]] get a
def extractLink(text):
    start = text.find("[[")
    end = text.find("]]")
    sep = text.find("|")
    
    if start < 0 or end < 0:
        return None
    if start > end or (start > sep and sep > -1) or sep > end:
        return None
        
    if sep < 0:
        return text[start+2:end]
    else:
        return text[start+2:sep]

#from "pre [[a|b]] post" get [pre, a, post]
def extractLinkAndSurroundingText(text):
    start = text.find("[[")
    end = text.find("]]")
    sep = text.find("|")
    
    if start < 0 or end < 0:
        return None
    if start > end:
        return None
    if (start > sep and sep > -1) or sep > end:
        sep = -1 #pipe can come from some other item
        
    if sep < 0:
        return [text[:start], text[start+2:end], text[end+2:]]
    else:
        return [text[:start], text[start+2:sep], text[end+2:]]

#from "pre [[a|b]] post" get [pre, b, post]
def stripLinkWithSurroundingText(text):
    start = text.find("[[")
    end = text.find("]]")
    sep = text.find("|")
    
    if start < 0 or end < 0:
        return None
    if start > end:
        return None
    if (start > sep and sep > -1) or sep > end:
        sep = -1 #pipe can come from some other item
        
    if sep < 0:
        return [text[:start], text[start+2:end], text[end+2:]]
    else:
        return [text[:start], text[sep+1:end], text[end+2:]]

def extractImageLink(text):
    start = text.find("[[")
    end = text.rfind("]]")
    sep = text.find("|")
    
    if start < 0 and end < 0:
        return text
    if start < 0 or end < 0:
        return None
    if start > end or (start > sep and sep > -1) or sep > end:
        return None
        
    if sep < 0:
        return text[start+2:end]
    else:
        return text[start+2:sep]

def stripExternalLink(text):
    start = text.find("[")
    end = text.rfind("]")
    sep = text.find(" ")
    
    if start < 0 and end < 0:
        return text
    if start < 0 or end < 0:
        return None
    if start > end or (start > sep and sep > -1) or sep > end:
        return None
        
    if sep < 0:
        return text[start+1:end]
    else:
        return text[start+1:sep]

def stripNamespace(link):
    return link[link.find(':')+1:]

def convertUrlToWikilink(url):
    """
    Convert a wikipedia URL to an internal page.
    Handles both /wiki/Title and /w/index.php?title= version
    TODO: error checking; return a Page object
    """
    from urllib.parse import urlparse, parse_qs
    import urllib.request, urllib.parse, urllib.error
    pr = urlparse(url)
    if (pr.query != ""):
        q = parse_qs(pr.query.encode("utf8"))
        title = q["title"][0]
    else:
        title = urllib.parse.unquote(pr.path.split('/')[-1].encode("utf8"))
    return title.decode("utf8")


# --------- String functions ----------
def capitalizeWithSigns(text, keep=[]):
    text = text.replace("-", "@@0@@ ")
    text = text.replace(".", "@@1@@ ")
    i = 2
    for term in keep:
        text = text.replace(term, "@@%d@@ " % i)
        i += 1
    text = string.capwords(text,' ')
    for term in reversed(keep):
        i -= 1
        text = text.replace("@@%d@@ " % i, term)
    text = text.replace("@@1@@ ", ".")
    text = text.replace("@@0@@ ", "-")
    return text

def findDigit(s):
    for i, c in enumerate(s):
        if c.isdigit():
            return i
    return -1;

def rfindOrLen(s, f):
    r = s.rfind(f)
    if r >= 0:
        return r
    else:
        return len(s)

def none2empty(text):
	if text:
		return text
	else:
		return ""

# --------- Geo functions ------------
def getDeg(decimal):
    if decimal < 0:
        decimal = -decimal
    return int(math.floor(decimal))
   
def getMin(decimal):
    if decimal < 0:
        decimal = -decimal
    return int(math.floor((decimal - math.floor(decimal)) * 60))

def getSec(decimal):
    if decimal < 0:
        decimal = -decimal
    return int(math.floor(((decimal - math.floor(decimal)) * 3600) % 60))

def geosign(check, plus, minus):
    if check == plus:
        return 1
    elif check == minus:
        return -1
    else:
        return 0 #this should really never happen

def linkedImages(page):
    """Return a list of Pages that this Page links to.

    Only returns pages from "normal" internal links. Category links are
    omitted unless prefixed with ":". Image links are omitted when parameter
    withImageLinks is False. Embedded templates are omitted (but links
    within them are returned). All interwiki and external links are omitted.

    @param thistxt: the wikitext of the page
    @return: a list of Page objects.
    """

    Rlink = re.compile(r'\[\[(?P<title>[^\]\|\[]*)(\|[^\]]*)?\]\]')
    result = []
    try:
        thistxt = textlib.removeLanguageLinks(page.get(get_redirect=True),
                                              page.site)
    except pywikibot.NoPage:
        raise
    except pywikibot.IsRedirectPage:
        raise
    except pywikibot.SectionError:
        return []
    thistxt = textlib.removeCategoryLinks(thistxt, page.site)

    # remove HTML comments, pre, nowiki, and includeonly sections
    # from text before processing
    thistxt = textlib.removeDisabledParts(thistxt)

    # resolve {{ns:-1}} or {{ns:Help}}
    # thistxt = page.site.resolvemagicwords(thistxt)

    for match in Rlink.finditer(thistxt):
        # print match.group(0)
        title = match.group('title')
        title = title.replace("_", " ").strip(" ")
        # print title
        if title == "":
            # empty link - problem in the page
            continue
        # convert relative link to absolute link
        if title.startswith(".."):
            parts = self.title().split('/')
            parts.pop()
            title = '/'.join(parts) + title[2:]
        elif title.startswith("/"):
            title = '%s/%s' % (page.title(), title[1:])
        if title.startswith("#"):
            # this is an internal section link
            continue
        if not page.site.isInterwikiLink(title):
            page2 = pywikibot.Page(page.site, title)
            try:
                hash(str(page2))
            except Exception:
                pywikibot.output("Page %s contains invalid link to [[%s]]."
                                 % (page.title(), title))
                continue
            if not page2.isImage():
                continue
            if page2.title(withSection=False) and page2 not in result:
                result.append(page2)
    return result

# --------------- CSV functions -------------------

if __name__ == "__main__":
    print(extractTemplate("{{Sema_2|a=b<ref>{{citat|}}</ref>|e=f}}", "Sema 2"))
    print(extractTemplate("{{Sema 2|a=b<ref>{{citat|}}</ref>|e=f}}", "Sema_2"))
    print(tl2Dict("{{Sema|a=bș<ref>{{ciătat|c=d}}</ref>sd|e=f{{citat|c=d|c1=d1}}}}"))
    print(tl2Dict("{{Sema2|a=b<ref>{{citat|}}</ref>|e=f}}"))
    print(tl2Dict("{{Sema2|a={{citat|c=d|c1=d1}}|e=f}}"))
    print(tl2Dict("{{Sema3|a={{#if:citat|{{c}}<ref>{{d}}</ref>|e=f}}}}"))
    print(tl2Dict("{{Sema4|a={{#if:citat|c|d}}}}"))
    pywikibot.output(convertUrlToWikilink("//ro.wikipedia.org/w/index.php?title=Lista_monumentelor_istorice_din_jude%C8%9Bul_Alba&oldid=10256783"))
    pywikibot.output(convertUrlToWikilink("//ro.wikipedia.org/wiki/Lista_monumentelor_istorice_din_jude%C8%9Bul_Alba"))

    # print textlib.extract_templates_and_params("{{Sema2|a={{citat|c=d|c1=d1}}|e=f}}")
    pass
