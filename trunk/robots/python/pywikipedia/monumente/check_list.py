#!/usr/bin/python
# -*- coding: utf-8  -*-
'''
Count the number of monuments in each page

'''

import sys, codecs, json
sys.path.append("..")
import wikipedia, config, re, pagegenerators

template = u'ElementLMI'
rowTemplate = u'Template:' + template
templateRegexp = re.compile("\{\{\s*" + template)

def processPage(page):
    '''
    Process a page containing one or multiple instances of the monument row template
    '''
    global rowTemplate
    global templateRegexp
    title = page.title(True)
    #templates = page.templatesWithParams(thistxt=page.get())
    wikipedia.output(u'Working on page "%s"' % title)

    count = len(re.findall(templateRegexp, page.get()))
    wikipedia.output(u'Count: %d' % count)
    if count:
        f = codecs.open("counts.log", 'a+', 'utf8')
        f.write(title)
        f.write(": %d\n" % count)
        f.close()

def processPageList():
    '''
    Process all the monuments of one country
    '''

    site = wikipedia.getSite()
    rowTemplatePage = wikipedia.Page(site, rowTemplate)
    codecs.open("counts.log", 'w+', 'utf8').close()
    transGen = pagegenerators.ReferringPageGenerator(rowTemplatePage, onlyTemplateInclusion=True)
    #filteredGen = pagegenerators.NamespaceFilterPageGenerator(transGen, countryconfig.get('namespaces'))
    pregenerator = pagegenerators.PreloadingGenerator(transGen, 100)
    for page in pregenerator:
        if page.exists() and not page.isRedirectPage():
	        # Do some checking
	        processPage(page)
	        
def compareLists(file1, file2):    
    f1 = open(file1, "r+")
    wikipedia.output("Reading database file 1...")
    db1 = json.load(f1)
    wikipedia.output("...done")
    f1.close();    
    f2 = open(file2, "r+")
    wikipedia.output("Reading database file 2...")
    db2 = json.load(f2)
    wikipedia.output("...done")
    f2.close();
    for monument in db1:
        code1 = monument["Cod"]
        #print code1
        found = 0
        count = 0
        for monument2 in db2:
            count = count + 1
            code2 = monument2["Cod"]
            if code1 == code2:
                found = 1
                break
            #elif count % 1000 == 0:
            #    wikipedia.output(u"Searched %d/%d" % (count, len(db2)))    
        if not found:
            wikipedia.output(u"Code %s not found" % code1)
        else:
            wikipedia.output(u"Code %s was found after %d codes" % (code1, count))

if __name__ == "__main__":
    try:
        #processPageList()
        compareLists("lmi_db.json", "db_true.json")
    finally:
        wikipedia.stopme()
