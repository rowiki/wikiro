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
import locale
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
    
license = u"Conținutul a fost eliberat sub licența CC-BY-SA-3.0-RO"
owner = u"© Institutul Național al Patrimoniului/CIMEC"
copyrightMessage = u"%s. %s." % (owner, license)

pageText = u"'''{2}''' conține toate siturile arheologice {0} [[{1}]] "\
u"înscrise în [[Repertoriul Arheologic Național]] (RAN){3}. RAN este "\
u"administrat de [[Ministerul Culturii și Patrimoniului Național "\
u"(România)|Ministerul Culturii și Patrimoniului Național]] și cuprinde "\
u"date științifice, cartografice, topografice, imagini și planuri, "\
u"precum și orice alte informații privitoare la zonele cu potențial "\
u"arheologic, studiate sau nu, încă existente sau dispărute.\n\n"

mainPageText = u"'''Datorită numărului mare de situri, această listă "\
u"a fost împărțită după localitatea în care se află situl'''. "\
u"Dacă știți localitatea (satul sau orașul) în care se află situl, "\
u"alegeți localitatea din lista din această pagină. Dacă nu știți "\
u"localitatea, puteți căuta un sit din județ folosind formularul de mai jos.\n\n"

letterPageText = u"Puteți căuta un sit din localitățile care încep cu "\
u"litera {0} folosind formularul de mai jos sau puteți naviga prin "\
u"toate siturile din județ la [[{1}]].\n\n"

searchBox = u"<inputbox>\n"\
u"type=fulltext\n"\
u"prefix={0}\n"\
u"break=no\n"\
u"width=30\n"\
u"searchbuttonlabel=Căutare în liste\n"\
u"</inputbox>\n\n"\
u"{{{{CompactTOC6}}}}\n\n"

discText = u"S-a primit permisiunea de reutilizare a informațiilor din RAN "\
u"din partea deținătorului drepturilor (%s). %s prin tichetul OTRS "\
"#2012031110010252.--~~~~\n"

siruta_db = sirutalib.SirutaDatabase()

class RanDatabase:
    def __init__(self):
        locale.setlocale(locale.LC_ALL, "")
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
        del self.lmi_db
        
        counties = siruta_db.get_all_counties(prefix=False)
        counties.append(u"")#it seems we have some empty counties
        pages_txt = {}
        villages = {}
        for county in counties:
            county = county.replace(u"-", u"* ")
            county = string.capwords(county,)
            county = county.replace(u"* ", u"-")
            pages_txt[county] = {
            u"1": [], u"2": [], u"3": [], u"4": [], u"5": [], 
            u"6": [], u"7": [], u"8": [], u"9": [], u"": [],
            u"A": [], u"B": [], u"C": [], u"D": [], u"E": [], 
            u"F": [], u"G": [], u"H": [], u"I": [], u"Î": [],
            u"J": [], u"L": [], u"M": [], u"N": [], u"O": [], 
            u"P": [], u"R": [], u"S": [], u"Ș": [], u"T": [], 
            u"Ț": [], u"U": [], u"V": [], u"Z": [],
            }
            villages[county] = {
            u"1": [], u"2": [], u"3": [], u"4": [], u"5": [], 
            u"6": [], u"7": [], u"8": [], u"9": [], u"": [],
            u"A": [], u"B": [], u"C": [], u"D": [], u"E": [], 
            u"F": [], u"G": [], u"H": [], u"I": [], u"Î": [],
            u"J": [], u"L": [], u"M": [], u"N": [], u"O": [], 
            u"P": [], u"R": [], u"S": [], u"Ș": [], u"T": [], 
            u"Ț": [], u"U": [], u"V": [], u"Z": [],
            }
        del counties
            
        indexes = self.full_dict.keys()
        indexes.sort()
        for com in indexes:
            county = self.full_dict[com]['county']
            village = self.full_dict[com]['village']
            if len(village) > 0:
                first = village[0]
            else:
                first = u""
            pages_txt[county][first].append(self.buildTemplate(self.full_dict[com]))
            villages[county][first].append(village)
        del indexes
            
        site = wikipedia.getSite()
            
        for elem in pages_txt:
            #if elem == u"Neamț" or elem == u"Timiș":
            #    continue
            if elem == "":
                title = u"Lista siturilor arheologice cu județ necunoscut"
                prefix = pageText.format(u"cu", u"județ necunoscut", title, "")
                search = u""
                continue
            
            county = u"județul {0}".format(elem)
            title = u"Lista siturilor arheologice din %s" % county
            prefix = pageText.format(u"din", county, title, "")
            search = searchBox.format(title)
            
            page = wikipedia.Page(site, title)
            page2 = wikipedia.Page(site, u"Discuție:" + title)
        
            letters = villages[elem].keys()
            letters.sort(cmp=locale.strcoll)
            
            text2 = discText % (owner, license)
            
            if True:#not page.exists():
                text = self.generateMainPageText(villages, elem, letters, 
                        prefix, search)
                #print text.encode("utf8")
                page.put(text, copyrightMessage)
            else:
                print u"Skipping %s" % title
            if not page2.exists():
                page2.put(text2, copyrightMessage)
            else:
                print u"Skipping Discuție:%s" % title
                
            
            for letter in letters:
                if len(pages_txt[elem][letter]) == 0:
                    continue
                title_l = title + u" - %s" % letter
                
                page2_l = wikipedia.Page(site, u"Discuție:" + title_l)
                page_l = wikipedia.Page(site, title_l)
                if True:#not page_l.exists():
                    prefix = pageText.format(u"din", county, title_l, 
                            u" și aflate în localități al căror nume începe cu litera %s" % letter)
                    details = letterPageText.format(letter, title)
                    text_l = prefix + details + search + \
                        u"{{ÎnceputTabelRAN}}\n" + \
                        u"".join(pages_txt[elem][letter]) + \
                        u"\n{{SfârșitTabelRAN}}\n" + \
                        u"==Note==\n" + \
                        u"<references>\n\n" + \
                        u"[[Categorie:Repertoriul Arheologic Național|%s]]" % elem
                    page_l.put(text_l, copyrightMessage)
                else:
                    print u"Skipping %s" % title_l
                if not page2_l.exists():
                    page2_l.put(text2, copyrightMessage)
                else:
                    print u"Skipping Discuție:%s" % title_l
                #print text_l.encode("utf8")
            
        self.writeRanDb()
        
    def generateMainPageText(self, villages, elem, letters, prefix, search):
        text = prefix + mainPageText + search
        text += u"{| border=\"0\" cellpadding=\"2\" cellspacing=\"1\" "\
u"align=\"center\" style=\"margin-left:1em; background:#ffffff;\" "\
u"width=\"100%\"\n|- align=\"center\""

        for letter in letters:
            if len(villages[elem][letter]) == 0:
                continue
            text += u"\n{{{{TOC ro tabele|{0}}}}}".format(letter)
            count = 0
            village_list = list(set(villages[elem][letter]))
            village_list.sort(cmp=locale.strcoll)
            for village in village_list:
                if count % 5 == 0:
                    text +="\n|-"
                count += 1
                text += u"\n| [[Lista siturilor arheologice din județul {2} - {0}|{1}]]".format(letter, village, elem)
                    
        text += u"\n|}\n\n" + \
                u"==Note==\n" + \
                u"<references/>\n\n" + \
                u"[[Categorie:Repertoriul Arheologic Național|%s]]" % elem
        return text

    def readLmiDb(self):
        f = open("lmi_db.json", "r+")
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
                self.full_dict[com]['lat'] = self.lmi_db[lmi]['Lat']
                self.full_dict[com]['lon'] = self.lmi_db[lmi]['Lon']
            
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
        if tldict['lmi'] <> u"":
            template += u"| CodLMI = %s\n" % tldict['lmi']
        if tldict['siruta'] <> u"":
            template += u"| CodSIRUTA = %s\n" % tldict['siruta']
        template += u"| Index = %s\n" % tldict['com']
        if tldict['name'] <> u"":
            template += u"| Nume = %s\n" % tldict['name']
        if tldict['image'] <> u"":
            template += u"| Imagine = %s\n" % tldict['image']
        if tldict['com'] <> u"":
            template += u"| TipCod = %s\n" % self.parseComplexity(tldict['com'].split(Entity._separator))
        if tldict['altName'] <> u"":
            template += u"| NumeAlternative = %s\n" % tldict['altName']
        if tldict['address'] <> u"":
            template += u"| Adresă = %s\n" % tldict['address']
        type_str = self.getCustomSirutaType(tldict['siruta'])
        type_sup_str = self.getCustomSirutaSupType(tldict['siruta'])
        if type_sup_str == u"comuna":
            place_prefix = u"Comuna "
        else:
            place_prefix = u""
        place = u""
        if type_str == u"sat" or type_str == u"localitate componentă":
            place = u"{0} [[{1}, {2}|{1}]]".format(
                    type_str,
                    tldict['village'], 
                    tldict['county'])
            if type_sup_str == u"comuna":
                place += u", {1} [[{2}{3}, {0}|{3}]]".format(
                            tldict['county'], 
                            type_sup_str,
                            place_prefix,
                            tldict['commune'])
            elif type_sup_str <> u"":
                place += u", %s [[%s]]" % (type_sup_str,
                                        tldict['village'])
        elif type_str <> u"":
            place = u"%s [[%s]]" % (type_str,
                                        tldict['village'])
                                        
        if place <> u"":
            template += u"| Localitate = %s\n" % place
        if tldict['monumentType'] <> u"":
            template += u"| TipMonument = %s\n" % tldict['monumentType']
        if tldict['dates'] <> u"":
            template += u"| Datare = %s\n" % tldict['dates']
        if tldict['culture'] <> u"":
            template += u"| Cultura = %s\n" % tldict['culture']
        if tldict['phase'] <> u"":
            template += u"| Faza = %s\n" % tldict['phase']
        if tldict['discovery'] <> u"":
            template += u"| Descoperit = %s\n" % tldict['discovery']
        if tldict['discoverer'] <> u"":
            template += u"| Descoperitor = %s\n" % tldict['discoverer']
        if tldict['state'] <> u"":
            template += u"| Stare = %s\n" % tldict['state']
        if tldict['category'] <> u"":
            template += u"| Categorie = %s\n" % tldict['category']
        if tldict['lat'] <> u"":
            template += u"| Lat = %s\n" % tldict['lat']
        if tldict['lon'] <> u"":
            template += u"| Lon = %s\n" % tldict['lon']
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
        tldict['lat'] = u""
        tldict['lon'] = u""
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
