#!/usr/bin/python
# -*- coding: utf-8  -*-

#
# (C) Strainu, 2012
#
# Distributed under the terms of the GPLv2 license.
#
#

import re, pagegenerators, urllib2, urllib
import wikipedia as pywikibot
import pywikibot.textlib as textlib
import math
import time


#extract the first instance of a template from the page text
# @param text: the text of the page
# @param tempalte: the name of the template we are looking for
# @return: the text of the template
def extractTemplate(text, template):
    #pywikibot.output(text)
    match = re.search("\{\{\s*" + template, text)
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
            key = re.sub(ur'\s', '', key)
            value = u"=".join(line[1:])
            _dict[key] = value
            if not key in _keyList:
                _keyList.append(key)
        elif line[0].startswith(u'{{') and not u"_name" in _dict: #name of the template
            #pywikibot.output("Name: " + line[0][2:])
            _dict[u"_name"] = line[0][2:]
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
    
        
    # def getDeg(self, decimal):
        # if decimal < 0:
            # decimal = -decimal
        # return int(math.floor(decimal))
        
    # def getMin(self, decimal):
        # if decimal < 0:
            # decimal = -decimal
        # return int(math.floor((decimal - math.floor(decimal)) * 60))
        
    # def getSec(self, decimal):
        # if decimal < 0:
            # decimal = -decimal
        # return int(math.floor(((decimal - math.floor(decimal)) * 3600) % 60))
        
    # def geosign(self, check, plus, minus):
        # if check == plus:
            # return 1
        # elif check == minus:
            # return -1
        # else:
            # return 0 #this should really never happen
            
if __name__ == "__main__":
    print tl2Dict(u"{{Sema|a=bș<ref>{{ciătat|c=d}}</ref>sd|e=f{{citat|c=d|c1=d1}}}}")
    print tl2Dict("{{Sema2|a=b<ref>{{citat|}}</ref>|e=f}}")
    print tl2Dict("{{Sema2|a={{citat|c=d|c1=d1}}|e=f}}")
    print tl2Dict("{{Sema3|a={{#if:citat|{{c}}<ref>{{d}}</ref>|e=f}}}}")
    print tl2Dict("{{Sema4|a={{#if:citat|c|d}}}}")
    #print textlib.extract_templates_and_params("{{Sema2|a={{citat|c=d|c1=d1}}|e=f}}")
