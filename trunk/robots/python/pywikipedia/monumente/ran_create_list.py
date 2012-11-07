#!/usr/bin/python
# -*- coding: utf-8  -*-
'''
Script that parses the CSV file offered by CIMEC and generates a series 
of {{ElementRAN}} templates
'''
import csv, sys
import cProfile
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
	
def parseComplexity(line):
	com = Entity._UNKNOWN
	if line[2].strip() <> "":
		com = Entity._COMPLEX
	elif line[1].strip() <> "":
		com = Entity._ENSEMBLE
	elif line[0].strip() <> "":
		com = Entity._SITE
		
	return com
	
def buildTemplate(tldict):
	template = u"{{ElementRAN\n"
	template += u"| Cod = %s\n" % tldict['ran']
	template += u"| CodLMI = %s\n" % tldict['lmi']
	template += u"| TipCod = %s\n" % tldict['com']
	template += u"| Nume = %s\n" % tldict['name']
	if tldict['altName'] <> u"":
		template += u"| NumeAlternative = %s\n" % tldict['altName']
	template += u"| Adresă = %s\n" % tldict['address']
	if tldict['village'] == tldict['commune']:
		place = u"[[%s, %s]]" %(tldict['village'], tldict['county'])
	else:
		place = u"sat [[%s, %s]], comuna [[Comuna %s, %s]]" % (tldict['village'], 
															tldict['county'], 
															tldict['commune'], 
															tldict['county'])
	template += u"| Localitate = %s\n" % place
	template += u"| Datare = %s\n" % tldict['dates']
	template += u"| Perioada = %s\n" % tldict['period']
	template += u"| Cultura = %s\n" % tldict['culture']
	template += u"| Descoperit = %s\n" % tldict['discovery']
	template += u"| Descoperitor = %s\n" % tldict['discoverer']
	template += u"| Stare = %s\n" % tldict['state']
	template += u"}}\n"
	return template
	
def parseLine(line):
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
	county = unicode(line[14], "utf8")
	if county <> u"Botoșani":
		return u""
	tldict = {}
	tldict['county'] = county
	tldict['com'] = parseComplexity(line)
	tldict['siruta'] = unicode(line[3], "utf8")
	tldict['ran'] = unicode(line[4], "utf8")
	if tldict['ran'].strip() == u"":
		print u"Linia %s nu are un cod RAN valid" % str(line)
		return u""
	tldict['monumentType'] = unicode(line[6], "utf8")
	tldict['category'] = unicode(line[7], "utf8")
	tldict['name'] = unicode(line[8], "utf8")
	tldict['altName'] = unicode(line[9], "utf8")
	tldict['village'] = unicode(line[12], "utf8")
	tldict['commune'] = unicode(line[13], "utf8")
	tldict['address'] = unicode(line[15], "utf8")
	if unicode(line[16], "utf8").strip() <> u"":
		if tldict['address'] <> u"":
			tldict['address'] += u", "
		tldict['address'] += unicode(line[16], "utf8")
	if unicode(line[17], "utf8").strip() <> u"" and tldict['address'] <> u"":
		tldict['address'] == u" (%s)" % unicode(line[17], "utf8")
	tldict['dates'] = unicode(line[34], "utf8").strip()
	tldict['period'] = unicode(line[34], "utf8").strip() 
	tldict['culture'] = unicode(line[35], "utf8").strip()  
	if unicode(line[36], "utf8").strip() <> u"":
		tldict['culture'] += u" (faza %s)" % unicode(line[36], "utf8").strip() 
	tldict['discovery'] = unicode(line[41], "utf8")
	tldict['discoverer'] = unicode(line[42], "utf8")
	tldict['state'] = unicode(line[43], "utf8")
	tldict['lmi'] = unicode(line[44], "utf8")
	template = buildTemplate(tldict)
	wikipedia.output(template)
	return template

def main():
	ran = csv.reader(open("ran_full.csv", "r"))
	complete_page = u""
	for line in ran:
		complete_page += parseLine(line)
	#wikipedia.output(complete_page)

if __name__ == "__main__":
	try:
		cProfile.run('main()', 'profiling/genranprofile.txt')
		#main()
	finally:
		wikipedia.stopme()
