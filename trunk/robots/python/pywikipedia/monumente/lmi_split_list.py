#!/usr/bin/python
# -*- coding: utf-8  -*-
import locale
import sys
import json
import re
import urllib

sys.path.append("..")
import strainu_functions as strainu
import pywikibot
from pywikibot import pagegenerators
from pywikibot import config as user

pageText = u"[[Fișier:Monument istoric.svg|thumb|100px|Simbol pentru monumentele istorice]]\n"\
u"'''Lista monumentelor istorice din județul {0}''' cuprinde "\
u"[[monument istoric|monumentele istorice]] din [[județul {0}]] "\
u"înscrise în [[Patrimoniul cultural național al României]].\n\n"\
u"[[Lista monumentelor istorice din România|Lista completă]] este "\
u"menținută și actualizată periodic de către [[Ministerul Culturii, "\
u"Cultelor și Patrimoniului Național]] din [[România]], prin "\
u"intermediul [[Institutul Național al Patrimoniului|Institutului "\
u"Național al Patrimoniului]], ultima versiune datând din [[2010]]"\
u"<ref name=\"lmi\">[http://www.cultura.ro/page/17 Lista monumentelor "\
u"istorice] pe cultura.ro</ref>. Această listă cuprinde și "\
u"actualizările ulterioare, realizate prin ordin al ministrului culturii.\n\n"\
u"'''Datorită numărului mare de monumente, această listă a fost "\
u"împărțită după localitatea în care se află monumentul'''. Dacă știți "\
u"localitatea (satul sau orașul) în care se află monumentul, alegeți "\
u"localitatea din lista din această pagină. Dacă nu știți localitatea, "\
u"puteți căuta un monument din județ folosind formularul de mai jos.\n\n"

letterPageText = u"[[Fișier:Monument istoric.svg|thumb|100px|Simbol pentru monumentele istorice]]\n"\
u"'''Lista monumentelor istorice din județul {0} - {1}''' cuprinde "\
u"[[monument istoric|monumentele istorice]] înscrise în [[Patrimoniul "\
u"cultural național al României]] și aflate în localități ce încep cu "\
u"litera {1} din [[județul {0}]].\n\n"\
u"[[Lista monumentelor istorice din România|Lista completă]] este "\
u"menținută și actualizată periodic de către [[Ministerul Culturii, "\
u"Cultelor și Patrimoniului Național]] din [[România]], prin intermediul "\
u"[[Institutul Național al Patrimoniului|Institutului Național al "\
u"Patrimoniului]], ultima versiune datând din [[2010]]<ref "\
u"name=\"lmi\">[http://www.cultura.ro/page/17 Lista monumentelor "\
u"istorice] pe cultura.ro</ref>. Această listă cuprinde și actualizările "\
u"ulterioare, realizate prin ordin al ministrului culturii.\n\n"
u"Puteți căuta un monument din județul {0} folosind formularul de mai "\
u"jos sau puteți naviga prin toate monumentele din județ la [[lista "\
u"monumentelor istorice din județul {0}]].\n\n"

searchBox = u"<inputbox>\n"\
u"type=fulltext\n"\
u"prefix=Lista monumentelor istorice din județul {0}\n"\
u"break=no\n"\
u"width=30\n"\
u"searchbuttonlabel=Căutare în liste\n"\
u"</inputbox>\n\n"\
u"{{{{CompactTOC6}}}}\n\n"

def buildTemplate(monument):
	template = u"{{ElementLMI\n"
	if monument[u'Cod'] <> u"":
            template += u"| Cod = %s\n" % monument[u'Cod']
	if monument[u'NotăCod'] <> u"":
            template += u"| NotăCod = %s\n" % monument[u'NotăCod']
	if monument[u'FostCod'] <> u"":
            template += u"| FostCod = %s\n" % monument[u'FostCod']
	if monument[u'Cod92'] <> u"":
            template += u"| Cod92 = %s\n" % monument[u'Cod92']
	if monument[u'CodRan'] <> u"":
            template += u"| CodRan = %s\n" % monument[u'CodRan']
	if monument[u'Denumire'] <> u"":
            template += u"| Denumire = %s\n" % monument[u'Denumire']
	if monument[u'Localitate'] <> u"":
            template += u"| Localitate = %s\n" % monument[u'Localitate']
	if monument[u'Adresă'] <> u"":
            template += u"| Adresă = %s\n" % monument[u'Adresă']
	if monument[u'Datare'] <> u"":
            template += u"| Datare = %s\n" % monument[u'Datare']
	if monument[u'Arhitect'] <> u"":
            template += u"| Arhitect = %s\n" % monument[u'Arhitect']
	if monument[u'Imagine'] <> u"":
            template += u"| Imagine = %s\n" % monument[u'Imagine']
	if monument[u'Commons'] <> u"":
            template += u"| Commons = %s\n" % monument[u'Commons']
	if monument[u'Lat'] <> u"":
            template += u"| Lat = %s\n" % monument[u'Lat']
	if monument[u'Lon'] <> u"":
            template += u"| Lon = %s\n" % monument[u'Lon']
	template += u"}}\n"
        return template
        
def custom_compare(item1, item2):
	first_dash = item1[u"Cod"].find(u"-")
	second_dash = item1[u"Cod"].find(u"-", first_dash+1)
	p1 = item1[u"Cod"][first_dash+1:second_dash]
	first_dash = item2[u"Cod"].find(u"-")
	second_dash = item2[u"Cod"].find(u"-", first_dash+1)
	p2 = item2[u"Cod"][first_dash+1:second_dash]
	roman_numerals = {'I': 1, 'II': 2, 'III': 3, 'IV': 4}
	if roman_numerals[p1] <> roman_numerals[p2]:
		return (roman_numerals[p1] < roman_numerals[p2])
	if item1[u"Localitate"].find(u"muncipiul [[Sibiu") > -1 and \
		item2[u"Localitate"].find(u"muncipiul [[Sibiu") == -1:
			return -1
	
	if item1[u"Localitate"].find(u"muncipiul [[Sibiu") == -1 and \
		item2[u"Localitate"].find(u"muncipiul [[Sibiu") > -1:
			return 1
			
	cmp_village = locale.strcoll(item1[u"Localitate"], item2[u"Localitate"])
	if cmp_village:
		return cmp_village
		
	last_dash = item1[u"Cod"].rfind(u"-")
	l1 = item1[u"Cod"][last_dash+1:]
	last_dash = item2[u"Cod"].rfind(u"-")
	l2 = item2[u"Cod"][last_dash+1:]
	
	return l1 < l2

def main():
	locale.setlocale(locale.LC_ALL, "")
	f = open("lmi_db.json", "r+")
	pywikibot.output("Reading database file...")
	db = json.load(f)
	pywikibot.output("...done")
	f.close();
	
	county = u"Argeș"
	
	pages_txt = {
            u"1": [], u"2": [], u"3": [], u"4": [], u"5": [], 
            u"6": [], u"7": [], u"8": [], u"9": [], u"": [],
            u"A": [], u"B": [], u"C": [], u"D": [], u"E": [], 
            u"F": [], u"G": [], u"H": [], u"I": [], u"Î": [],
            u"J": [], u"L": [], u"M": [], u"N": [], u"O": [], 
            u"P": [], u"R": [], u"S": [], u"Ș": [], u"T": [], 
            u"Ț": [], u"U": [], u"V": [], u"Z": [], u"*": [],
	}
	villages = {
            u"1": [], u"2": [], u"3": [], u"4": [], u"5": [], 
            u"6": [], u"7": [], u"8": [], u"9": [], u"": [],
            u"A": [], u"B": [], u"C": [], u"D": [], u"E": [], 
            u"F": [], u"G": [], u"H": [], u"I": [], u"Î": [],
            u"J": [], u"L": [], u"M": [], u"N": [], u"O": [], 
            u"P": [], u"R": [], u"S": [], u"Ș": [], u"T": [], 
            u"Ț": [], u"U": [], u"V": [], u"Z": [], u"*": [],
	}
	
	cnt = urllib.quote(county.encode("utf8"))
	for monument in db:
		if monument["source"].find(cnt) == -1:
			continue
		first_link_end = monument['Localitate'].find(u']')+2
		if first_link_end > 1:
			village = monument['Localitate'][0:first_link_end]
			if strainu.stripLink(village) <> None:
				village = strainu.stripLink(village).title()
		village = village.strip()
		print village
		if len(village) > 0:
			first = village[0]
		else:
			first = u""
		pages_txt[first].append(monument)
		villages[first].append(village)
		
	site = pywikibot.getSite()
	
	title = u"Lista monumentelor istorice din județul %s" % county
	prefix = pageText.format(county)
	search = searchBox.format(county)
	description = u"Împart lista de monumente istorice din județul %s" % county
	letters = villages.keys()
	letters.sort(cmp=locale.strcoll)
	
	page = pywikibot.Page(site, title)
	text = prefix + search
	text += u"{| border=\"0\" cellpadding=\"2\" cellspacing=\"1\" "\
u"align=\"center\" style=\"margin-left:1em; background:#ffffff;\" "\
u"width=\"100%\"\n|- align=\"center\""

	for letter in letters:
		if len(villages[letter]) == 0:
			continue
		text += u"\n{{{{TOC ro tabele|{0}}}}}".format(letter)
		count = 0
		village_list = list(set(villages[letter]))
		village_list.sort(cmp=locale.strcoll)
		for village in village_list:
			if count % 5 == 0:
				text +="\n|-"
			count += 1
			text += u"\n| [[Lista monumentelor istorice din județul {2} - {0}|{1}]]".format(letter, village, county)
                    
	text += u"\n|}\n\n" + \
                u"==Note==\n" + \
                u"<references/>\n\n" + \
                u"[[Categorie:Liste de monumente istorice din România]]\n" + \
                u"[[Categorie:Monumente istorice din județul %s]]\n" % county
        
	page.put(text, description)
	#print text.encode("utf8")
    
	for letter in letters:
		if len(pages_txt[letter]) == 0:
			continue
		title_l = title + u" - %s" % letter
		
		page_l = pywikibot.Page(site, title_l)
		text_l = letterPageText.format(county, letter)
		pages_txt[letter].sort(cmp=custom_compare)
		text_l += search + \
			u"{{ÎnceputTabelLMI|Județul-iso=RO-SB}}\n"
		for elem in pages_txt[letter]:
			text_l += buildTemplate(elem)
		text_l += u"\n{{SfârșitTabelLMI}}\n" + \
			u"==Note==\n" + \
			u"<references>\n\n" + \
			u"{{Monumente istorice din România}}\n\n" + \
			u"[[Categorie:Liste de monumente istorice din România]]\n" + \
			u"[[Categorie:Monumente istorice din județul %s]]\n" % county
		page_l.put(text_l, description)
		#if letter == "S":
		#	print text_l.encode("utf8")

if __name__ == "__main__":
	main()
