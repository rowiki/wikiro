#!/usr/bin/python
# -*- coding: utf-8  -*-
'''
Script that parses the CSV file offered by CIMEC and generates a series 
of {{ElementRAN}} templates
'''
import csv
import sys
import json
import cProfile
import sirutalib
import string
sys.path.append("..")
import wikipedia, re, pagegenerators
import config as user
import strainu_functions as strainu

class Entity:
    _UNKNOWN = u"necunoscut"
    _SITE = u"sit"
    _ENSEMBLE = u"ansamblu"
    _COMPLEX = u"complex"
    _separator = u"_"
    
copyrightMessage = u"© Institutul Național al Patrimoniului/CIMEC. Licență CC-BY-SA-3.0-RO"

siruta_db = sirutalib.SirutaDatabase()

class RanDatabase:
    def __init__(self):
        ran = csv.reader(open("ran_full_mod.csv", "r"))
        
        self.lmi_db = {}
        self.readLmiDb()
        complete_page = u""
        self.full_dict = {}
        self.lmi_regexp = re.compile("(([a-z]{1,2})-(i|ii|iii|iv)-([a-z])-([a-z])-([0-9]{5}(\.[0-9]{2})?))", re.I)
        
        for line in ran:
            tldict = self.parseLine(line)
            if tldict:
                self.full_dict[tldict['com']] = tldict
            else:
                pass
                
        self.getAdditionalInformation()
                   
        counties = siruta_db.get_all_counties(prefix=False)
        counties.append(u"")#it seems we have some empty counties
        pages_txt = {}
        for county in counties:
            county = county.replace(u"-", u" ")
            county = string.capwords(county)
            pages_txt[county] = []
            
        indexes = self.full_dict.keys()
        indexes.sort()
        for com in indexes:
            pages_txt[self.full_dict[com]['county']].append(self.buildTemplate(self.full_dict[com]))
            
        for elem in pages_txt:
            print u"".join(pages_txt[elem]).encode('utf8')
            
        self.writeRanDb()
            
    def readLmiDb(self):
        f = open("db.json", "r+")
        db = json.load(f)
        for monument in db:
            self.lmi_db[monument["Cod"]] = monument
        f.close()
        
    def writeRanDb(self):
        f = open("ran_db.json", "w+")
        json.dump(self.full_dict, f, indent=2)
        f.close()

            
    def getAdditionalInformation(self):
        for com in self.full_dict:
            for elem in ['county','siruta','village','commune','address']:
                if self.full_dict[com][elem] == u"":
                    self.full_dict[com][elem] = self.getElemFromSup(com, elem)
            lmi = self.full_dict[com]['lmi']
            if self.full_dict[com]['name'] == u"" and \
                     lmi in self.lmi_db:
                self.full_dict[com]['name'] = self.lmi_db[lmi]['Denumire']
                self.full_dict[com]['image'] = self.lmi_db[lmi]['Imagine']
            
    def sanitizeLmiCode(self, lmi):
        if lmi == u"":
            return u""
        lmi = lmi.replace(lmi[0:2], lmi[0:2].upper(), 1)
        lmi = lmi.replace(' ', '').replace('--','-').replace('..','.').replace('_','-').replace('i','I')
        lmi = re.sub("([IV])([msa])", "\g<1>-\g<2>", lmi, count=1)
        lmi = re.sub("([AB])([0-9])", "\g<1>-\g<2>", lmi, count=1)
        lmi = re.sub("([msa])\.?([AB])", "\g<1>-\g<2>", lmi, count=1)
        lmi = re.sub("([A-Z][A-Z])\.-?I", "\g<1>-I", lmi, count=1)
        if self.lmi_regexp.search(lmi) == None:
            return u""
        elif lmi in self.lmi_db:
            return lmi
        else:
            return "<!--" + lmi + "-->"#comment it so it doesn't show up
    
    def parseComplexity(self, line):
        com = Entity._UNKNOWN
        if len(line) == 3:
            com = Entity._COMPLEX
        elif len(line) == 2:
            com = Entity._ENSEMBLE
        elif len(line) == 1:
            com = Entity._SITE
            
        return com
        
    def getCustomSirutaType(self, siruta):
        try:
            siruta = int(siruta)
        except Exception:
            return u""
        type = siruta_db.get_type(siruta)
        if type == 1 or type == 4 or type == 9:
            return u"municipiul"
        if type == 2 or type == 5 or type == 17:
            return u"oraș"
        if type == 3:
            return u"comuna"
        if type == 10 or type == 18:
            return u"localitate componentă"
        if type == 11 or type == 19 or type == 22 or type == 23:
            return u"sat"
        else:
            return u""
            
    def getCustomSirutaSupType(self, siruta):
        try:
            siruta = int(siruta)
        except ValueError:
            return u""
        siruta_sup = siruta_db.get_sup_code(siruta)
        return self.getCustomSirutaType(siruta_sup)
        
    def buildTemplate(self, tldict):
        template = u"{{ElementRAN\n"
        template += u"| Cod = %s\n" % tldict['ran']
        template += u"| CodLMI = %s\n" % tldict['lmi']
        template += u"| CodSIRUTA = %s\n" % tldict['siruta']
        template += u"| Index = %s\n" % tldict['com']
        template += u"| Nume = %s\n" % tldict['name']
        template += u"| Imagine = %s\n" % tldict['image']
        template += u"| TipCod = %s\n" % self.parseComplexity(tldict['com'].split(Entity._separator))
        template += u"| NumeAlternative = %s\n" % tldict['altName']
        template += u"| Adresă = %s\n" % tldict['address']
        type_str = self.getCustomSirutaType(tldict['siruta'])
        type_sup_str = self.getCustomSirutaSupType(tldict['siruta'])
        if type_sup_str == u"comuna":
            place_prefix = u"Comuna "
        else:
            place_prefix = u""
        place = u""
        if type_str == u"sat" or type_str == u"localitate componentă":
            place = u"%s [[%s, %s]], %s [[%s%s, %s]]" % (type_str,
                                                        tldict['village'], 
                                                        tldict['county'], 
                                                        type_sup_str,
                                                        place_prefix,
                                                        tldict['commune'], 
                                                        tldict['county'])
        elif type_str <> u"":
            place = u"%s [[%s, %s]]" % (type_str,
                                        tldict['village'], 
                                        tldict['county'])
        template += u"| Localitate = %s\n" % place
        template += u"| Datare = %s\n" % tldict['dates']
        template += u"| Cultura = %s\n" % tldict['culture']
        template += u"| Faza = %s\n" % tldict['phase']
        template += u"| Descoperit = %s\n" % tldict['discovery']
        template += u"| Descoperitor = %s\n" % tldict['discoverer']
        template += u"| Stare = %s\n" % tldict['state']
        template += u"| Categorie = %s\n" % tldict['category']
        template += u"}}\n"
        return template
        
    def parseLine(self, line):
        # 0 - Id_sit,Id_ansamblu,Id_complex,Siruta,cod,
        # 5 - Numar_complex,Tip,Categorie,Nume,Nume_alternative,
        #10 - Nume_alte limbi,Limba_nume,Localitate,Unitatea_superioara,Judet,
        #15 - Adresa,Punct,Punct_alte denumiri,Punct_denumiri_alte limbi,Limba_punct,
        #20 - Reper,Parcela_cadastrala,Localizare_specifica,Suprafata,Longitudine,
        #25 - Latitudine,altitudine,Forma_de_relief,Microrelief,Reper_hidrografic,
        #30 - Tip_reper_hidrografic,Stratigrafie,Datare_inceput,Datare_sfarsit,Datare_relativa,
        #35 - Perioada,Cultura,Faza_culturala,Descriere,Observatii,
        #40 - Atestare_documentara,Data_descoperirii,Descoperitor,Stare_conservare,COD-LMI-2004,
        #45 - Utilizare_teren,Data-actualizarii
        tldict = {}
        tldict['com'] = Entity._separator.join([x for x in line[0:3] if x])
        tldict['com_sup'] = self.getIndexSup(tldict['com'])
        tldict['siruta'] = unicode(line[3], "utf8")
        tldict['ran'] = unicode(line[4], "utf8")
        if tldict['ran'] == u"":
            #print u"Linia %s nu are un cod RAN valid" % str(line)
            return None
        tldict['monumentType'] = unicode(line[6], "utf8")
        tldict['category'] = unicode(line[7], "utf8")
        tldict['name'] = unicode(line[8], "utf8")
        tldict['altName'] = unicode(line[9], "utf8")
        tldict['village'] = unicode(line[12], "utf8")
        tldict['commune'] = unicode(line[13], "utf8")
        tldict['county'] = unicode(line[14], "utf8")
        tldict['address'] = unicode(line[15], "utf8")
        if unicode(line[16], "utf8").strip() <> u"":
            if tldict['address'] <> u"":
                tldict['address'] += u", punct "
            tldict['address'] += unicode(line[16], "utf8")
        if unicode(line[17], "utf8").strip() <> u"" and tldict['address'] <> u"":
            tldict['address'] == u" (%s)" % unicode(line[17], "utf8")
        #datare și perioada sunt întotdeauna la fel
        tldict['dates'] = unicode(line[34], "utf8")
        tldict['culture'] = unicode(line[35], "utf8") 
        tldict['phase'] = unicode(line[36], "utf8") 
        tldict['discovery'] = unicode(line[41], "utf8")
        tldict['discoverer'] = unicode(line[42], "utf8")
        tldict['state'] = unicode(line[43], "utf8")
        lmi = unicode(line[44], "utf8")
        tldict['lmi'] = self.sanitizeLmiCode(lmi)
        tldict['image'] = u""
        return tldict
            
    def getIndexSup(self, index):
        sep = index.rfind(Entity._separator)
        if sep > -1:
            return index[0:sep]
        else:
            return None
            
    def getElemFromSup(self, com, elem):
        com_sup = self.getIndexSup(com)
        if not com_sup:
            return u""
        if not com_sup in self.full_dict:
            return u""
        if self.full_dict[com_sup][elem] == u"":
            self.full_dict[com_sup][elem] = self.getElemFromSup(com_sup, elem)
            
        return self.full_dict[com_sup][elem]
        
def main():
    RanDatabase()
        
    #wikipedia.output(complete_page)

if __name__ == "__main__":
    try:
        cProfile.run('main()', 'profiling/genranprofile.txt')
        #main()
    finally:
        wikipedia.stopme()
