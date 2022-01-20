#!/usr/bin/python
# -*- coding: utf-8  -*-
"""

python city_infobox.py -summary:"Bot: Adding a coordinate" -lang:ro -start:Albac,_Alba


"""

#
# (C) Strainu, 2012
#
# Distributed under the terms of the MIT license.
#
__version__ = '$Id: osm2wiki.py 8448 2010-08-24 08:25:57Z xqt $'
#

import xml.dom.minidom
import csv
import math
import time
import sirutalib
import sys
import re, urllib2, urllib
import string
import locale
sys.path.append("..")
import codecs, config
import pagegenerators
import wikipedia as pywikibot
import strainu_functions as strainu

msg = {
    'ro': u'Robot: Actualizez informațiile din infocasetă: %s',
    }

class o2wVillageData:
    def __init__(self, acceptall = False):
        self.acceptall = acceptall
        self.site = pywikibot.getSite()
        self.done = False
        self.coord_need_update = False
        self._statsName = "ro_statistici.csv"
        self._politicianName = "ro_uat_primari_2012.csv"
        self._log = "populationData.log"
        self._data = {}
        
    def log(self, header, string):
        f = open(self._log, 'a+')
        f.write(header.encode( "utf-8" ) + string.encode( "utf-8" ))
        print header.encode( "utf-8" ) + string.encode( "utf-8" )
        print "\n"
        f.write("\n")
        f.close()

    def logi(self, string):
        self.log("* Info (%s): " % time.strftime("%Y-%m-%d %H:%M:%S"), repr(string))

    def loge(self, string):
        self.log("* Error (%s): " % time.strftime("%Y-%m-%d %H:%M:%S"), repr(string))

    def logw(self, string):
        self.log("* WIKI error (%s): " % time.strftime("%Y-%m-%d %H:%M:%S"), repr(string))
        
    def logd(self, string):
        self.log("* Debug (%s): " % time.strftime("%Y-%m-%d %H:%M:%S"), repr(string))
        
    def reverseName(self, name):
        surname = name[:name.find(u" ")]
        first = name[name.find(u" ")+1:]
        if first.rfind(u". ") > -1:
            middle = first[:first.rfind(u". ") + 1]
            first = first[first.rfind(u". ") + 2:]
            return first + u" " + middle + u" " + surname
        elif re.search(r"^[A-Za-z]{1,2} ", first):
            m = re.search(r"^[A-Za-z]{1,2} ", first)
            middle = first[m.start():m.end() - 1]
            first = first[m.end():]
            return first + u" " + middle + u". " + surname
        else:
            return first + u" " + surname
            pass
        return name
        
    def parseCsvs(self):
        #ro_statistici: siruta,jud_code,jud_name,jud_lbl,jud_id,uat_id,sector,juduat_id,uat_ta,uat_pop,uat_name,uat_lbl,uat_name_n,gospodarii,total_locu,colsen,coldep,nr_angajat,nr_absolve
        #ro_uat_primari_2012: id_primar,primar,jud_id,jud_code,jud_name,jud_lbl,siruta,uat_name,uat_lbl,id_partid,partid,abv_partid,id_partid_,nume_parti,abv_par_01,colsen,coldep
        siruta = sirutalib.SirutaDatabase()
        reader = csv.reader(open(self._statsName, "r"))
        for row in reader:
            name_with_prefix = siruta.get_name(int(row[0]))
            name_without_prefix = siruta.get_name(int(row[0]), prefix=False)
            if name_without_prefix <> unicode(row[11], 'utf8') and\
               name_with_prefix <> unicode(row[11], 'utf8'):
                print row[0]
                print siruta.get_name(int(row[0]), prefix=False)
                print unicode(row[11], 'utf8')
                continue
                
            low = [u" DE ", u" CEL ", u" LA ", u" SUB ", u" DIN ", u" LUI ", u" CU "]
            name = strainu.capitalizeWithSigns(name_without_prefix, keep=low)
            for word in low:
                name = name.replace(word, string.lower(word))
                
            county = strainu.capitalizeWithSigns(unicode(row[3], 'utf8'))
            
            self._data[row[0]] = {
                u'siruta': row[0],
                u'jud': county,
                u'nume': name,
                u'pop': unicode(row[14], 'utf8'),
                u'tip': siruta.get_type(int(row[0])),
                u'codp': siruta.get_postal_code(int(row[0])),
            }
            
        reader = csv.reader(open(self._politicianName, "r"))
        for row in reader:
            if row[6] not in self._data:
                self.logw(u"Pentru SIRUTA %s nu avem statistici" % row[6])
                continue
            self._data[row[6]][u'primar'] = self.reverseName(strainu.capitalizeWithSigns(unicode(row[1], 'utf8'),))
            self._data[row[6]][u'partid'] = unicode(row[14], 'utf8')
            
    def extractFloat(self, text):
        Rref = re.compile(ur'<ref.*?>[^<]+</ref>')
        for m in Rref.finditer(text):
            text = text.replace(m.group(), '')
        Rfnum = re.compile(ur'{{formatnum:(.*)}}')
        for m in Rfnum.finditer(text):
            text = m.group(1)
        text = text.replace(",",".")
        return float(text)
        
        
    def populateCommune(self, cod, _dict, _keyList):
        #primar, partid, ales, populație, recensământ, siruta - Casetă comune România
        if u'primar' in _dict:
            _dict[u"primar"] = self._data[cod][u'primar']
        else:
            _dict[u"primar"] = self._data[cod][u'primar']
            _keyList.append(u"primar")
        if u'partid' in _dict:
            _dict[u"partid"] = self._data[cod][u'partid']
        else:
            _dict[u"partid"] = self._data[cod][u'partid']
            _keyList.append(u"partid")
        if u'ales' in _dict:
        #    _dict[u"ales"] = u"2012"
            pass
        else:
            _dict[u"ales"] = u"2012"
            _keyList.append(u"ales")
        if u'populație' in _dict:
            _dict[u"populație"] = self._data[cod][u'pop']
        else:
            _dict[u"populație"] = self._data[cod][u'pop']
            _keyList.append(u"populație")
        if u'recensământ' in _dict:
            _dict[u"recensământ"] = u"[[Recensământul populației din 2011 (România)|2011]]"
        else:
            _dict[u"recensământ"] = u"[[Recensământul populației din 2011 (România)|2011]]"
            _keyList.append(u"recensământ")
        if u'siruta' in _dict:
            _dict[u"siruta"] = self._data[cod][u'siruta']
        else:
            _dict[u"siruta"] = self._data[cod][u'siruta']
            _keyList.append(u"siruta")
        if u"suprafață" in _dict and _dict[u"suprafață"].strip() <> u"":
            try:
                sup = self.extractFloat(_dict[u"suprafață"])
                dens = float(self._data[cod][u'pop']) / sup
                if u"densitate" not in _dict:
                    _keyList.append(u"densitate")
                _dict[u"densitate"] = locale.format_string("%.2f", dens)
            except:
                print "a" + _dict[u"suprafață"] + "b"
        if u'cod-poștal' in _dict and self._data[cod][u'codp'] == 0:
            #remove unused postal code - it should not appear for communes
            del _dict[u'cod-poștal']
            _keyList.remove(u'cod-poștal')
        return (_dict, _keyList)
        
    def populateSector(self, cod, _dict, _keyList):
        #primar, partid, alegeri, populație, recensământ, siruta - Infobox Sectoare București
        if u'primar' in _dict:
            _dict[u"primar"] = self._data[cod][u'primar']
        else:
            _dict[u"primar"] = self._data[cod][u'primar']
            _keyList.append(u"primar")
        if u'partid' in _dict:
            _dict[u"partid"] = self._data[cod][u'partid']
        else:
            _dict[u"partid"] = self._data[cod][u'partid']
            _keyList.append(u"partid")
        if u'alegeri' in _dict:
        #    _dict[u"alegeri"] = u"2012"
            pass
        else:
            _dict[u"alegeri"] = u"2012"
            _keyList.append(u"alegeri")
        if u'populație' in _dict:
            _dict[u"populație"] = self._data[cod][u'pop']
        else:
            _dict[u"populație"] = self._data[cod][u'pop']
            _keyList.append(u"populație")
        if u'recensământ' in _dict:
            _dict[u"recensământ"] = u"[[Recensământul populației din 2011 (România)|2011]]"
        else:
            _dict[u"recensământ"] = u"[[Recensământul populației din 2011 (România)|2011]]"
            _keyList.append(u"recensământ")
        if u'siruta' in _dict:
            _dict[u"siruta"] = self._data[cod][u'siruta']
        else:
            _dict[u"siruta"] = self._data[cod][u'siruta']
            _keyList.append(u"siruta")
        if u"suprafață" in _dict and _dict[u"suprafață"].strip() <> u"":
            try:
                sup = self.extractFloat(_dict[u"suprafață"])
                dens = float(self._data[cod][u'pop']) / sup
                if u"densitate" not in _dict:
                    _keyList.append(u"densitate")
                _dict[u"densitate"] = locale.format_string("%.2f", dens)
            except:
                print "a" + _dict[u"suprafață"] + "b"
        return (_dict, _keyList)
        
    def populateLocation(self, cod, _dict, _keyList):
        #lider_nume (lider_titlu), lider_partid, lider_ales, populație, recensământ, cod_clasificare (tip_cod_clasificare) - Infocaseta Așezare
        if u'lider_nume' in _dict:
            _dict[u"lider_nume"] = self._data[cod][u'primar']
        else:
            _dict[u"lider_nume"] = self._data[cod][u'primar']
            _keyList.append(u"lider_nume")
        if u'lider_titlu' in _dict:
            _dict[u"lider_titlu"] = u'Primar'
        else:
            _dict[u"lider_titlu"] = u'Primar'
            _keyList.append(u"lider_titlu")
        if u'lider_partid' in _dict:
            _dict[u"lider_partid"] = self._data[cod][u'partid']
        else:
            _dict[u"lider_partid"] = self._data[cod][u'partid']
            _keyList.append(u"lider_partid")
        if u'lider_ales' in _dict:
        #    _dict[u"lider_ales"] = u"2012"
            pass
        else:
            _dict[u"lider_ales"] = u"2012"
            _keyList.append(u"lider_ales")
        if u'populație' in _dict:
            _dict[u"populație"] = self._data[cod][u'pop']
        else:
            _dict[u"populație"] = self._data[cod][u'pop']
            _keyList.append(u"populație")
        rec_note = u"<ref name=\"rec_2011\">{{Citat web|title=Rezultatele preliminare ale recensământului din 2011|url=http://www.recensamantromania.ro/wp-content/uploads/2012/08/TS2.pdf}}</ref>"
        if u'populație_note_subsol' in _dict:
            _dict[u"populație_note_subsol"] = rec_note
        else:
            _dict[u"populație_note_subsol"] = rec_note
            _keyList.append(u"populație_note_subsol")
            
        if u'recensământ' in _dict:
            _dict[u"recensământ"] = u"[[Recensământul populației din 2011 (România)|2011]]"
        else:
            _dict[u"recensământ"] = u"[[Recensământul populației din 2011 (România)|2011]]"
            _keyList.append(u"recensământ")
        if u'cod_clasificare' in _dict:
            _dict[u"cod_clasificare"] = self._data[cod][u'siruta']
        else:
            _dict[u"cod_clasificare"] = self._data[cod][u'siruta']
            _keyList.append(u"cod_clasificare")
        if u'tip_cod_clasificare' in _dict:
            _dict[u"tip_cod_clasificare"] = u'[[SIRUTA]]'
        else:
            _dict[u"tip_cod_clasificare"] = u'[[SIRUTA]]'
            _keyList.append(u"tip_cod_clasificare")
        if u"suprafață_totală_km2" in _dict and _dict[u"suprafață_totală_km2"].strip() <> u"":
            try:
                sup = self.extractFloat(_dict[u"suprafață_totală_km2"])
                dens = float(self._data[cod][u'pop']) / sup
                if u"densitate" not in _dict:
                    _keyList.append(u"densitate")
                _dict[u"densitate"] = locale.format_string("%.2f", dens)
            except Exception as e:
                print e
                print "a" + _dict[u"suprafață_totală_km2"] + "b"
        print _dict
        print self._data[cod]
        if u'population_blank1_title' in _dict and \
        _dict[u'population_blank1_title'] == u'Rezultate provizorii 2011':
            del _dict[u'population_blank1']
            _keyList.remove(u'population_blank1')
            del _dict[u'population_blank1_title']
            _keyList.remove(u'population_blank1_title')
        if u'codpoștal' in _dict and self._data[cod][u'codp'] == 0:
            #remove unused postal code - it should not appear for communes
            del _dict[u'codpoștal']
            _keyList.remove(u'codpoștal')
            if u'tip_cod_poștal' in _dict:
                del _dict[u'tip_cod_poștal']
                _keyList.remove(u'tip_cod_poștal')
        #elif self._data[cod][u'codp'] > 0:
        #    if u'codpoștal' in _dict:
        #        _dict[u"codpoștal"] = str(self._data[cod][u'codp'])
        #    else:
        #        _dict[u"codpoștal"] = str(self._data[cod][u'codp'])
        #        _keyList.append(u"codpoștal")
        #    if u'tip_cod_poștal' in _dict:
        #        _dict[u"tip_cod_poștal"] = u"Cod poștal"
        #    else:
        #        _dict[u"tip_cod_poștal"] = u"Cod poștal"
        #        _keyList.append(u"tip_cod_poștal")
        return (_dict, _keyList)

    def putDataOnWiki(self, lang, firstArticle):
        if firstArticle == None:
            start = 1
        else:
            start = 0
        site = pywikibot.getSite(lang)
        #village_templates = "(CutieSate|CutieSate2|CasetăSate|CasetăSate2|Infocaseta Așezare|Infobox așezare|Casetă așezare|Cutie așezare|CasetăOrașe)".decode('utf8')
        commune_templates = u"(Cutie Comune România|Casetă comune România|Infobox Sectoare București|Cutie așezare|Casetă așezare|Infocaseta Așezare|Infobox așezare)"
        _parties = {}
        for cod in self._data:
            _keyList = []
            _dict = {}
            row = self._data[cod]
            
            if row['tip'] == 3:
                title = u"Comuna " + row['nume'] + u", " + row['jud']
            else:
                title = row['nume']
            self.logi("Parsing page: %s" % title)
            generator = pywikibot.Page(site, title)
            if not generator.exists():
                self.loge(u"Pagina %s nu există" % title)
                print row
                continue
            if generator.isRedirectPage():
                generator = generator.getRedirectTarget()
            try:
                text = generator.get()
            except Exception as inst:
                self.loge(u"Unknown error in getPageValue, exiting: %s" % inst)
                continue
                
            oldTl = strainu.extractTemplate(text, commune_templates)
            if oldTl == None:
                self.loge("No template in page %s" % title)
                continue
            (_dict, _keyList) = strainu.tl2Dict(oldTl)#populate _dict
            #print _dict
            
            if not row[u'partid'] in _parties:
                party = row[u'partid']
                ppage = pywikibot.Page(site, party)
                if not ppage.exists():
                    party = row[u"partid"] + u" (România)"
                    ppage = pywikibot.Page(site, party)
                    if ppage.exists():
                        self._data[cod][u'partid'] = u"[[" + party + u"]]"
                else:
                    self._data[cod][u'partid'] = u"[[" + party + u"]]"
                _parties[row[u'partid']] = self._data[cod][u'partid']
            else:
                self._data[cod][u'partid'] = _parties[row[u'partid']]
            
            if _dict[u'_name'].lower() == u"cutie comune românia" or\
                _dict[u'_name'].lower() == u"casetă comune românia":
                (_dict, _keyList) = self.populateCommune(cod, _dict, _keyList)
            elif _dict[u'_name'] == u"Infobox Sectoare București":
                (_dict, _keyList) = self.populateSector(cod, _dict, _keyList)
            elif _dict[u'_name'].lower() == u"casetă așezare" or\
                _dict[u'_name'].lower() == u"cutie așezare" or\
                _dict[u'_name'].lower() == u"infocaseta așezare" or\
                _dict[u'_name'].lower() == u"infobox așezare":
                (_dict, _keyList) = self.populateLocation(cod, _dict, _keyList)
            else:
                self.loge("Unknown template for %s: %s" % (title, _dict[u'_name']))
            
            tl = u""
            for key in _keyList:
                if key == u"_name":
                    tl += u"{{" + _dict[key] + u"\n"
                else:
                    tl += u"| " + key + u" = " 
                    try:
                        tl += _dict[key].strip() + u"\n"
                    except:
                        print key
            tl += u"}}"
            
            # we compare the 2 templates without whitespace 
            # to determine if we made any replacement
            if cmp(re.sub(r'\s', '', oldTl), re.sub(r'\s', '', tl)):
                newtext = text.replace(oldTl, tl)
                pywikibot.showDiff(text, newtext)
                comment = pywikibot.translate(site, msg) % title
                if self.acceptall:
                    answer = 'y'
                else:
                    answer = pywikibot.input(u"Upload change? ([y]es/[n]o/[a]lways)")
                if answer == 'y':
                    generator.put(newtext, comment)
                    pass
                elif answer == 'a':
                    self.acceptall = True
            else:
                self.logi("No change for current page")
        
def main():
    lang=pywikibot.getSite().language()
    start = None
    acceptall = False
    # Loading the arguments
    for arg in pywikibot.handleArgs():
        if arg.startswith('-lang'):
            if len(arg) == 5:
                lang = pywikibot.input(u'What language do you want to use?')
            else:
                lang = arg[6:]
        elif arg.startswith('-start'):
            if len(arg) == 6:
                start = pywikibot.input(u'What article do you want to start with?')
            else:
                start = arg[7:]
        elif arg.startswith('-all'):
            acceptall = True
    
    bot = o2wVillageData(acceptall)
    # bot.tl2Dict(bot.extractTemplate(u"""{{Infocaseta Așezare
# | nume = Bogdănești
# | alt_nume = 
# | tip_asezare = Sat
# | imagine = 
# | imagine_dimensiune = 250px
# | imagine_descriere = Bogdănești
# | stemă = 
# | hartă = 
# | pushpin_map = 
# | pushpin_label_position = right
# | tip_subdiviziune = Țară
# | nume_subdiviziune = {{ROU}}
# | tip_subdiviziune1 = [[Județele României|Județ]]
# | nume_subdiviziune1 = [[județul Vaslui|Vaslui]]
# | tip_subdiviziune3 = [[Comunele României|Comună]]
# | nume_subdiviziune3 = [[Comuna Bogdănești, Vaslui|Bogdănești]]
# | titlu_atestare = Prima atestare
# | atestare = 
# | suprafață_totală_km2 = 
# | altitudine = 
# | latd = 46
# | latm = 26
# | lats = 58
# | latNS = N
# | longd = 27
# | longm = 43
# | longs = 36
# | longEV = E
# | recensământ = 2002
# | populație = 
# | populație_note_subsol = 
# | tip_cod_poștal = [[Cod poștal]]
# | codpoștal = 
# | camp_gol_nume =
# | camp_gol_info = 
# }}

# '''Bogdănești''' este o localitate în [[județul Vaslui]], [[Moldova]], [[România]]
# """, u"Infocaseta Așezare"))
    # print bot._dict
    bot.parseCsvs()
    bot.putDataOnWiki(lang, start)

if __name__ == "__main__":
    try:
        locale.setlocale(locale.LC_ALL, "ro_RO")
        main()
    finally:
        pywikibot.stopme()
