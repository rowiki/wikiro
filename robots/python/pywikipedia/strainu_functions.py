#!/usr/bin/python
# -*- coding: utf-8  -*-

#
# (C) Strainu, 2012
#
# Distributed under the terms of the GPLv2 license.
#
#

import re, urllib2, urllib
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
    match = re.search("\{\{\s*" + template, text, re.I)
    if match == None:
        return None
    tl = text[match.start():]
    text = text[match.start() + 2:]
    open = 0
    close = 1
    innerTlCount = 0
    while open < close and open <> -1:
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
# @return: a dictionary containing the params as keys and the values as value \
#          The name of the template has the key "_name"
def tl2Dict(template):
    marker = "@@"
    Rtemplate = re.compile(ur'{{(msg:)?(?P<name>[^{\|]+?)(\|(?P<params>[^{]+?))?}}')
    Rmath = re.compile(ur'<math>[^<]+</math>')
    Rref = re.compile(ur'<ref.*?>[^<]+</ref>')
    Rmarker = re.compile(ur'%s(\d+)%s' % (marker, marker))
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
    template = re.sub(ur'(\r|\n)', '', template)
    template = template[0:len(template)-2]#get rid of '}}'
    params = template.split('|')
    key = u""
    value = unicode("",'utf8')
    for line in params:
        line = line.split(u'=')
        #pywikibot.output(str(line))
        if (len(line) > 1):
            key = line[0]#.encode("utf8")
            key = key.strip()
            value = u"=".join(line[1:])
            _dict[key] = value
            if not key in _keyList:
                _keyList.append(key)
        elif line[0].startswith(u'{{') and not u"_name" in _dict: #name of the template
            #pywikibot.output("Name: " + line[0][2:])
            _dict[u"_name"] = line[0][2:].strip()
            if not key in _keyList:
                _keyList.append(u"_name")
        elif line[0] != u"" and key != u"":#the first line might not begin with {{
            _dict[key] = _dict[key] + u"|" + line[0]
            if not key in _keyList:
                _keyList.append(key)
    for key, value in _dict.items():
        matches = Rmarker.findall(value)
        for match in matches:
            count = int(match)
            value = value.replace(u'%s%d%s' % (marker, count, marker), intern[count])
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
    if start > end or (start > sep and sep > -1) or sep > end:
        return None
        
    if sep < 0:
        return [text[:start], text[start+2:end], text[end+2:]]
    else:
        return [text[:start], text[start+2:sep], text[end+2:]]
        
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
    
def capitalizeWithSigns(text, keep=[]):
    text = text.replace(u"-", u"@@0@@ ")
    text = text.replace(u".", u"@@1@@ ")
    i = 2
    for term in keep:
        text = text.replace(term, u"@@%d@@ " % i)
        i += 1
    text = string.capwords(text,' ')
    for term in reversed(keep):
        i -= 1
        text = text.replace(u"@@%d@@ " % i, term)
    text = text.replace(u"@@1@@ ", u".")
    text = text.replace(u"@@0@@ ", u"-")
    return text
        
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
    #thistxt = page.site.resolvemagicwords(thistxt)

    for match in Rlink.finditer(thistxt):
	#print match.group(0)
        title = match.group('title')
        title = title.replace("_", " ").strip(" ")
	#print title
	if title == u"":
		# empty link - problem in the page
		continue
        if page.namespace() in page.site.family.namespacesWithSubpage:
            # convert relative link to absolute link
            if title.startswith(".."):
                parts = self.title().split('/')
                parts.pop()
                title = u'/'.join(parts) + title[2:]
            elif title.startswith("/"):
                title = u'%s/%s' % (page.title(), title[1:])
        if title.startswith("#"):
            # this is an internal section link
            continue
        if not page.site.isInterwikiLink(title):
            page2 = pywikibot.Page(page.site, title)
            try:
                hash(str(page2))
            except Exception:
                pywikibot.output(u"Page %s contains invalid link to [[%s]]."
                        % (page.title(), title))
                continue
            if not page2.isImage():
                continue
            if page2.title(withSection=False) and page2 not in result:
                result.append(page2)
    return result
    
def unicode_csv_reader(utf8_data, dialect=csv.excel, **kwargs):
    csv_reader = csv.reader(utf8_data, dialect=dialect, **kwargs)
    for row in csv_reader:
        yield [unicode(cell, 'utf-8') for cell in row]
            
if __name__ == "__main__":
    print extractTemplate("{{Sema_2|a=b<ref>{{citat|}}</ref>|e=f}}", "Sema 2")
    print extractTemplate("{{Sema 2|a=b<ref>{{citat|}}</ref>|e=f}}", "Sema_2")
    print tl2Dict(u"{{Sema|a=bș<ref>{{ciătat|c=d}}</ref>sd|e=f{{citat|c=d|c1=d1}}}}")
    print tl2Dict("{{Sema2|a=b<ref>{{citat|}}</ref>|e=f}}")
    print tl2Dict("{{Sema2|a={{citat|c=d|c1=d1}}|e=f}}")
    print tl2Dict("{{Sema3|a={{#if:citat|{{c}}<ref>{{d}}</ref>|e=f}}}}")
    print tl2Dict("{{Sema4|a={{#if:citat|c|d}}}}")
    #print textlib.extract_templates_and_params("{{Sema2|a={{citat|c=d|c1=d1}}|e=f}}")
