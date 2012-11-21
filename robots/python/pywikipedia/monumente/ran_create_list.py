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
sys.path.append("..")
import wikipedia, re, pagegenerators
import config as user
import strainu_functions as strainu

class Entity:
    _UNKNOWN = u"necunoscut"
    _SITE = u"sit"
    _ENSEMBLE = u"ansamblu"
    _COMPLEX = u"complex"
    
copyrightMessage = u"© Institutul Național al Patrimoniului/CIMEC. Licență CC-BY-SA-3.0-RO"

siruta_db = sirutalib.SirutaDatabase()

class RanDatabase:
    def __init__(self):
        ran = csv.reader(open("ran_full_mod.csv", "r"))
        
        f = open("db.json", "r+")
        self.lmi_db = {}
        wikipedia.output("Reading database file...")
        db = json.load(f)
        for monument in db:
            self.lmi_db[monument["Cod"]] = monument
        wikipedia.output("...done")
        
        f.close();
        complete_page = u""
        self.full_dict = {}
        self.lmi_regexp = re.compile("(([a-z]{1,2})-(i|ii|iii|iv)-([a-z])-([a-z])-([0-9]{5}(\.[0-9]{2})?))", re.I)
        
        for line in ran:
            tldict = self.parseLine(line)
            if tldict:
                self.full_dict[tldict['ran']] = tldict
            else:
                pass
                
        for ran in self.full_dict:
            for elem in ['county','siruta','village','commune','address']:
                if self.full_dict[ran][elem].strip() == u"":
                    self.full_dict[ran][elem] = self.getElemFromSup(ran, elem)
            lmi = self.full_dict[ran]['lmi']
            if self.full_dict[ran]['name'].strip() == u"" and \
                     lmi in self.lmi_db:
                self.full_dict[ran]['name'] = self.lmi_db[lmi]['Denumire']
                self.full_dict[ran]['image'] = self.lmi_db[lmi]['Imagine']
                    
        codes = self.full_dict.keys()
        codes.sort()
        for ran in codes:
            #if self.full_dict[ran]['county'] <> u"Botoșani":
            #    continue
            print self.buildTemplate(self.full_dict[ran]).encode("utf8")
            
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
        if line[2].strip() <> "":
            com = Entity._COMPLEX
        elif line[1].strip() <> "":
            com = Entity._ENSEMBLE
        elif line[0].strip() <> "":
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
        template += u"| Nume = %s\n" % tldict['name']
        template += u"| Imagine = %s\n" % tldict['image']
        template += u"| TipCod = %s\n" % tldict['com']
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
        template += u"}}"
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
        tldict['com'] = self.parseComplexity(line)
        tldict['siruta'] = unicode(line[3], "utf8")
        tldict['ran'] = unicode(line[4], "utf8")
        if tldict['ran'].strip() == u"":
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
                tldict['address'] += u", punct"
            tldict['address'] += unicode(line[16], "utf8")
        if unicode(line[17], "utf8").strip() <> u"" and tldict['address'] <> u"":
            tldict['address'] == u" (%s)" % unicode(line[17], "utf8")
        #datare și perioada sunt întotdeauna la fel
        tldict['dates'] = unicode(line[34], "utf8").strip()
        tldict['culture'] = unicode(line[35], "utf8").strip() 
        tldict['phase'] = unicode(line[36], "utf8").strip() 
        tldict['discovery'] = unicode(line[41], "utf8")
        tldict['discoverer'] = unicode(line[42], "utf8")
        tldict['state'] = unicode(line[43], "utf8")
        lmi = unicode(line[44], "utf8")
        tldict['lmi'] = self.sanitizeLmiCode(lmi)
        tldict['image'] = u""
        return tldict
            
    def getRanSup(self, ran):
        if ran.rfind(".") > -1:
            return ran[0:ran.rfind(".")]
        else:
            return None
            
    def getElemFromSup(self, ran, elem):
        ran_sup = self.getRanSup(ran)
        if not ran_sup:
            return u""
        if not ran_sup in self.full_dict:
            return u""
        if self.full_dict[ran_sup][elem].strip() == u"":
            self.full_dict[ran_sup][elem] = self.getElemFromSup(ran_sup, elem)
            
        return self.full_dict[ran_sup][elem]
        
def main():
    RanDatabase()
        
    #wikipedia.output(complete_page)

if __name__ == "__main__":
    try:
        cProfile.run('main()', 'profiling/genranprofile.txt')
        #main()
    finally:
        wikipedia.stopme()
