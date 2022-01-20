#!/usr/bin/python
# -*- coding: utf-8  -*-
'''
Command line options:

-path   The folder where the files are located. Default is the current folder
-prefix The prefix of the text files containing the new database
-db     The database to work on.
-out    The file to write the output to

'''
import sys, time, warnings, json, re, os, codecs
import urlparse
import md5

import pywikibot
from pywikibot import pagegenerators
from pywikibot import config

import update_database as dbs

sys.path.append(".")
import strainu_functions as sf

countries = {
    ('ro', 'lmi') : {
        'project' : u'wikipedia',
        'lang' : u'ro',
        'rowTemplate' : u'ElementLMI',
        'footerTemplate' : u'SfârșitTabelLMI',
        'namespaces' : [0],
        'fields' : [
            u'Cod',
            u'Denumire',
            u'Localitate',
            u'Adresă',
            u'Datare',
            u'Creatori',
            u'Lat',
            u'Lon',
            u'OsmLat',
            u'OsmLon',
            u'Imagine',
            u'Plan',
            u'Commons',
            u'NotăCod',
            u'FostCod',
            u'Cod92',
            u'CodRan',
            u'Copyright',
            ],
        'pagePrefix': {
            u'Lista monumentelor istorice din județul',
            u'Lista monumentelor istorice din București',
        }
    }
}

def fileGenerator(path, prefix):
    listing = os.listdir(path)
    for infile in listing:
        if infile.find(prefix) == 0:
            yield infile
            
def getMonumentNumber(code):
    if code:
        return code[code.rfind("-"):]
    return None
            
def refactorDb(db):
    newdb = {}
    for monument in db:
        index = getMonumentNumber(monument["Cod"])
        newdb[index] = monument
    return newdb
    
def readDb(filename):
    f = open(filename, "r+")
    pywikibot.output("Reading database file...")
    db = json.load(f, encoding="utf8")
    db = refactorDb(db)
    pywikibot.output("...done")
    f.close()
    return db
    
def declassifiedMonuments(left, right, config):
    print u"Generating list of declassified monuments"
    fd = codecs.open(u"declassified_LMI2015.wiki.txt", "w+", "utf8")
    for cod in left:
        if cod in right:
            continue
        
        params = left[cod]
        my_template = u"{{" + config.get('rowTemplate') + u"Declasat\n"
        for name in config.get('fields'):
            if name in params and params[name] <> u"":
                my_template += u"| " + name + u" = " + params[name] + u"\n"
        my_template += u"| Declasat = 2.828/24-12-2015\n"
        my_template += u"}}\n"
        fd.write(my_template)
    fd.close()
    
def uploadNewEntry(where, when, what, config):
    #try:
    site = pywikibot.Site()
    title = urlparse.parse_qs(urlparse.urlparse(str(where)).query)['title'][0].decode('utf8')
    page = pywikibot.Page(site, title)
    text = page.get()
    rowTemplate = config.get('rowTemplate')
    #except:
    #    print "Could not find %s in the list. Try to upload the monument manually." % when
    #    return
    
    templates = pywikibot.textlib.extract_templates_and_params(text)
    for (template, params) in templates:
        if template == rowTemplate:
            for param in params:
                val = params[param].strip()
                fld = param.strip()

                if fld == u"Cod" and (val == when or val.find(getMonumentNumber(when)) > 0):
                    break
                    
            else:
                continue
            break

    else:
        print "Could not find %s in the list (2). Try to upload the monument manually." % when
        return
        
    (before, code, after) = text.partition(val)
    
    cliva = after.find(u"{{" + rowTemplate)
    # if we cannot find another template after the current, we
    # are most likely the last template on the page
    if cliva < 0:
        cliva = after.find(u"{{" + config.get('footerTemplate'))
    if cliva >= 0:
        prevt = after[:cliva]
        after = after[cliva:]
    else:
        print "Could not find %s in the list (3). Try to upload the monument manually." % when
        return

    newtext = before + code + prevt + what + after
    pywikibot.showDiff(text, newtext, context=10)

    answer = pywikibot.inputChoice(u"Upload change?", ['yes', 'no'], ['y', 'N'], 'n')
    if answer == 'y':
        comment = u"Adaug un monument nou din LMI2015: %s" % code
        page.put(newtext, comment)
    
def newMonuments(left, right, config):
    print u"Generating list of new monuments"
    fd = codecs.open(u"new_LMI2015.wiki.txt", "w+", "utf8")
    for cod in sorted(right.keys()):
        if cod in left:
            continue
        
        params = right[cod]
        my_template = u"{{" + config.get('rowTemplate') + u"\n"
        for name in config.get('fields'):
            if name in params and params[name] <> u"":
                my_template += u"| " + name + u" = " + params[name] + u"\n"
        my_template += u"| NotăCod = Clasat în LMI 2015, adoptată prin ordinul [http://lege5.ro/Gratuit/he2dknjvga/ordinul-nr-2828-2015-pentru-modificarea-anexei-nr-1-la-ordinul-ministrului-culturii-si-cultelor-nr-2314-2004-privind-aprobarea-listei-monumentelor-istorice-actualizata-si-a-listei-monumentelor-istoric 2.828/24-12-2015]\n"
        my_template += u"| RefCod = lmi2015\n"
        my_template += u"}}\n"
        fd.write(my_template)
        
        previous = getMonumentNumber(params[u"previous"])
        if previous == None:
            print "No previous. Will handle this case later"
            pass #TODO
        elif previous in left:
            uploadNewEntry(left[previous][u"source"], params[u"previous"], my_template, config)
            params[u"source"] = left[previous][u"source"]
            left[cod] = params
        else:
            print "ERROR: %s is not yet in the database. This should not happen." % params[u"previous"]
            
    fd.close()
    
def rebuildTemplate(config, params):
    my_template = u"{{" + config.get('rowTemplate') + u"\n"
    for name in config.get('fields'):
        if name in params and params[name] <> u"":
            my_template += u"| " + name + u" = " + params[name] + u"\n"
    my_template += u"}}\n"
    return my_template
    
def updateTableData(config, url, code, field, newvalue, upload = True, text = None, ask = True):
    """
    :param url: The wiki page that will be updated
    :type url: string
    :param code: The LMI code that will be updated
    :type code: string
    :param field: The field that will be updated
    :type field: string
    :param newvalue: The new value that will be used for the field
    :type newvalue: string
    :param upload: How to handle the upload of the modified page. False means do not upload; 
        True means upload if we have a change; 
        None means upload even if the text has not changed;
        This option is for empty or previous changes; it will probalby be used with ask=False
    :type upload: Three-state boolean (True, False, None)
    :param text: The text that will be updated (None means get the page from the server)
    :type text: string
    :param ask: Whether to ask the user before uploading
    :type ask: boolean
    :return: The user's answer
    :rtype: string
    """
    pywikibot.output("Uploading %s for %s; value \"%s\"" % (field, code, newvalue))
    site = pywikibot.Site()
    title = urlparse.parse_qs(urlparse.urlparse(str(url)).query)['title'][0].decode('utf8')
    page = pywikibot.Page(site, title)
    
    if text == None:
        pywikibot.output("Getting page contents")
	try:
        	text = page.get()
	except:
		return u''
        
    oldtext = text
    codeFound = False
    last = None
    rawCode = None
    my_params = {}
    rowTemplate = config.get('rowTemplate')
    answer = u""
    
    templates = pywikibot.textlib.extract_templates_and_params(text)
    for (template, params) in templates:
        if template == rowTemplate:
            for param in params:
                val = params[param]
                val = val.split("<ref")[0].strip()
                val2 = re.sub(r'\s', '', val)
                
                fld = param.strip()
                
                if fld == "Cod" and val2 == code:
                    codeFound = True
                    rawCode = val
                    my_params = params
                    break
                    
            if codeFound:
                break
    else: #for .. else
        pywikibot.output(u"Code not found: %s" % code)
        return None
    
    orig = rebuildTemplate(config, my_params)
    oldvalue = my_params.get(field)
    my_params[field] = newvalue
    new = rebuildTemplate(config, my_params)

    try:
	try:
            pywikibot.showDiff(orig, new, context = 3)
	except:
            print oldvalue
            print newvalue
            return None
    
        if ask:
            answer = pywikibot.input(u"Upload change? ([y]es/[n]o)")
        else:
            answer = 'y'
        if answer == 'y':
            (before, code, after) = text.partition(rawCode)
        
            # we need to clean the whole template from both before and after
            clivb = before.rfind(u"{{" + rowTemplate)
            cliva = after.find(u"{{" + rowTemplate)
            # if we cannot find another template after the current, we
            # are most likely the last template on the page
            if cliva < 0:
                cliva = after.find(u"{{" + config.get('footerTemplate'))
            if cliva >= 0 and clivb >= 0:
                after = after[cliva:]
                before = before[:clivb]
            else:
                pywikibot.output("Could not find the current template, aborting!")
                return text
        
            # rebuild the page with the new text
            after = new + after
            text = "".join((before, after))
        
            if upload == True:
                comment = u"Actualizez câmpul %s în lista de monumente conform LMI2015" % field
                try:
                    page.put(text, comment)
                    return answer
                except pywikibot.exceptions.Error:
                    pywikibot.output("Some error occured at upload, let's move on and hope for the best!")
    except Exception as e:
        pywikibot.output("%s Some error occured before upload, let's move on and hope for the best!" % e)
	import traceback
	traceback.print_exc()
    return answer
    
def mergeMonuments(left, right, config):
    tempfile = u"merge_skip.json"
    f = open(tempfile, "r+")
    pywikibot.output("Reading monuments to skip...")
    db = json.load(f, encoding="utf8")
    pywikibot.output("...done")
    f.close()
    pattern = re.compile(u'(.*?)?"(.*?)"([^\/\>]?)')
    uploaded = {}
    refused = {}
    for cod in sorted(left.keys()):
        if cod not in right:
            continue
            
        oldCode = left[cod][u"Cod"]
        oldSrc = left[cod][u"source"]
        
        if oldCode in db:
            continue
        
        for param in left[cod]:
            if param in [u"Lat", u"Lon", u"OsmLat", u"OsmLon", \
                        u"Commons", u"Creatori", u"source", u"previous", \
                        u"Imagine", u"CodRan", u"RefCod", u"NotăCod", \
                        u"Copyright", u"FostCod", u"Localitate"]:
                continue
            leftText = left[cod][param].strip()
            rightText = right[cod][param].replace(u'„', u'"').replace(u'”', u'"').replace(u'“', u'"').replace(u'\'\'', u'"').replace(u',,', u'"').strip()
            rightText = pattern.sub(u'\\1„\\2”\\3', rightText)
            rightText = rightText.replace(u"cca ", u"cca. ").replace(u"(*)", u"<ref name=\"notaplan\" />")
            if leftText == rightText:
                continue
            if leftText.find(u'č') > -1:
                continue
            if rightText == u"":
                continue
            leftTextNoSpaces = leftText.replace(u" ", u"").replace(u" ", u"").replace(u"&nbsp;", u"")
            while leftTextNoSpaces.find(u'[') > -1:
                leftTextNoSpaces = "".join(sf.stripLinkWithSurroundingText(leftTextNoSpaces))
            print leftTextNoSpaces
            rightTextNoSpaces = rightText.replace(u" ", u"").replace(u" ", u"").replace(u"&nbsp;", u"")
            while rightTextNoSpaces.find(u'[') > -1:
                rightTextNoSpaces = "".join(sf.stripLinkWithSurroundingText(rightTextNoSpaces))
            print rightTextNoSpaces
            if leftTextNoSpaces.lower() == rightTextNoSpaces.lower():
                continue
            ask = True
            if leftTextNoSpaces.lower().replace(u",", u"") == rightTextNoSpaces.lower().replace(u",", u"").replace(u"–", u"-"):
                ask = False
            m = md5.new()
            m.update(leftText.encode('utf8'))
            m.update(rightText.encode('utf8'))
            d = m.digest()
            if d in refused:
                continue
            if d in uploaded:
                ask = False
            print "\t=== " + param + " @ " + oldCode + " ===";
            response = updateTableData(config, oldSrc, oldCode, param, rightText, ask=ask)
            print response
            if response == 'n':
                db[oldCode] = 'y'
                f = open(tempfile, "w+")
                json.dump(db, f, indent = 2)
                f.close();
                refused[d] = 1
            elif response == 'y':
                uploaded[d] = 1
            elif response != None:
                refused[d] = 1
            
    
def buildNewDb(path, prefix, config):
    print u"Generating the new database"
    dbs.monuments_db = []
    for infile in fileGenerator(path, prefix):
        text = u""
        with codecs.open(infile, "r", "utf8") as fd:
            text = fd.read()
        dbs.processText(infile, config, text, infile)
    dbs.writeOutput(u"ro_lmi2015_db.json")
            
def theBigMergeFunction():
    path = u"."
    prefix = u"LMI2015_"
    lang = pywikibot.Site().language()
    database = u"lmi"
    dbs.update(database)
    #buildNewDb(path, prefix, countries.get((lang, database)))
    
    left = readDb("ro_lmi_db.json")
    right = readDb("ro_lmi2015_db.json")
    #declassifiedMonuments(left, right, countries.get((lang, database)))
    #newMonuments(left, right, countries.get((lang, database)))
    mergeMonuments(left, right, countries.get((lang, database)))

if __name__ == "__main__":
    theBigMergeFunction()
    
#cucota
#arâului
#, ,
#;,
#..
#terenulactualei
#lalimita
#lași/Iași
#șisec
#centrulsatului
#satdinspre
#terasăjoasă
#localitatedinspre
#fragmentedin
#[[\s
#dispensaruluinr
#alpârăului
#aldealului
#înluncă
