#!/usr/bin/python
# -*- coding: utf-8  -*-
'''
Parse the monument pages (articles, lists and images) and correct frequent
mistakes found in lists

Requires Python 2.7

'''

import sys, time, warnings, json, string, re
from collections import OrderedDict
sys.path.append("..")
import pywikibot
from pywikibot import pagegenerators
from pywikibot import config as user

mistakes = {
	u',\s,': u',',
	u'(\| (?!Imagine|Plan))(.*)\s*\( ': u'\g<1>\g<2> (',
	u'(\| (?!Imagine|Plan))(.*)\s*\)': u'\g<1>\g<2>)',
	#u' {2,}': u' ',
	u'([Cc])rucedepiatră': u'\g<1>ruce de piatră',
	u' și\"(\s+)' : u' și\g<1>\"',
	u'([0-9]+)\s+,\s+' : u'\g<1>, ',
	u'\sk\sm\s': u' km ',
	u'([0-9])\s?[kK]\s?m\s?(SV|NV|SE|NE|nord|sud|est|vest|[NSEV]|de|față)': u'\g<1> km \g<2>',
	u'([0-9])\s?[mM]\s?(SV|NV|SE|NE|nord|sud|est|vest|[NSEV]|de|față)': u'\g<1> m \g<2>',
	u'([0-9]),(\s+)([0-9]+)\skm': u'\g<1>,\g<3> km',
	u'([0-9])\.([0-9]+)\skm': u'\g<1>,\g<2> km',
	u'([Kk])m([0-9])': u'\g<1>m \g<2>',
	u'([Ll])a(SV|NV|SE|NE|nord|sud|est|vest|[NSEV])de': u'\g<1>a \g<2> de ',
	u'(\| (?!Imagine|Plan))(.*)([Ll])a([0-9])': u'\g<1>\g<2>\g<3>a \g<4>',
	u'([șȘ])i([0-9])': u'\g<1>i \g<2>',
	u'([0-9])([Ll])a': u'\g<1> \g<2>a',
	u'([0-9])([Șș])i': u'\g<1> \g<2>i',
	u'(SV|NV|SE|NE|nord|sud|est|vest|[NSEVnsev])\s?de\s?(sat|oraș|intravilan|localitate|comună|șosea|drum|conac|Dunăre)': '\g<1> de \g<2>',
	u'([k\s]m)\s?de\s?(sat|oraș|intravilan|localitate|comună|șosea|drum)': '\g<1> de \g<2>',
	u'<?!n>desat': 'de sat',
	u'PiațaUnirii([0-9]+)' : u'Piața Unirii \g<1>',
	u'PiațaUnirii' : u'Piața Unirii',
	u'S\s?f\s?\.' : u'Sf.',
	u'([Pp])oddepiatră': u'\g<1>od de piatră',
	u'(\| (?!Imagine|Plan|Commons))(.*)([Nn])r\.?([0-9])': u'\g<1>\g<2>\g<3>r. \g<4>',
	u'(\| (?!Imagine|Plan|Commons))(.*)([Nn])r\s?([0-9])': u'\g<1>\g<2>\g<3>r. \g<4>',
	u'(\| (?!Imagine|Plan|Commons))(.*)([^:])([sS])tr\.([^\s_])': u'\g<1>\g<2>\g<3>\g<4>tr. \g<5>',
	u'cca\.?\s?([0-9])': u'cca. \g<1>',
	u'([Ll])a\s?cca\.?(\s?)': u'\g<1>a cca.\g<2>',
	u'(\| (?!Imagine|Plan))(.*),(([^0-9\s_]))': u'\g<1>\g<2>, \g<3>',
	#u'(\| (?!Imagine|Plan))(.*)([^w])\.([a-zăîâșțA-ZĂÂÎȘȚ]{4,})': u'\g<1>\g<2>\g<3>. \g<4>',#high risk
	u'(\| (?!Imagine|Plan|Commons))([a-zăîâșțA-ZĂÂÎȘȚ]*)([^w])\.([a-gi-zăîâșțA-GI-ZĂÂÎȘȚ][a-zăîâșțA-ZĂÂÎȘȚ]{3,})': u'\g<1>\g<2>\g<3>. \g<4>',#high risk
	u'(\| (?!Imagine|Plan))(.*)sf\.sec': u'\g<1>\g<2>sf. sec',
	u'(\| (?!Imagine|Plan))(.*)sec\.X': u'\g<1>\g<2>sec. X',
	u'(\| (?!Imagine|Plan|Commons))(.*)\s*:([a-zăâîșț])': u'\g<1>\g<2>: \g<3>',#high risk
	u' *; *': u'; ',
	u'(\| (?!Localitate))(.*)șila': u'\g<1>\g<2>și la',
	u'șiîn': u'și în',
	u'șiașezarea': u'și așezarea',
	u'([^a])dela': u'\g<1>de la',
	u'și([Ss])tr': u'și \g<1>tr',
	u'([a-zăâîșț])de([jJ])os': u'\g<1> de Jos',
	u'([a-zăâîșț])de([sS])us': u'\g<1> de Sus',
	u'\[\[([a-zăâîșț]+)([Mm])are([^a-zăâîșț])': u'[[\g<1> Mare\g<3>',
	u'\[\[([a-zăâîșț]+)([Mm])ic([^a-zăâîșț])': u'[[\g<1> Mic\g<3>',
	u'D\s?N\s?([0-9]{1,3})\s([ABCDEFGH])([\s,\.])': u'DN\g<1>\g<2>\g<3>',
	u'D\s?N\s?([0-9]{1,3})': u'DN\g<1>',
	u'Ziddeincintă': u'Zid de incintă',
	u'(\| (?!Imagine|Plan|CodRan|FostCod|NotăCod|OsmL|RefCod))(.*)([a-zăîâșț])([A-ZĂÂÎȘȚ])': u'\g<1>\g<2>\g<3> \g<4>',#high risk
	u' alui ': u' a lui ',
	u'(I|V|X)a\.(\s?)Chr\.': u'\g<1> a. Chr.',
	u'a\.Chr\.': u'a. Chr.',
	u'(I|V|X)p\.(\s?)Chr\.': u'\g<1> p. Chr.',
	u'p\.Chr\.': u'p. Chr.',
	u'(\| (?!Imagine|Plan))(.*)sec\.(I|V|X)': u'\g<1>\g<2>sec. \g<3>',
	u'(\| (?!Imagine|Plan|Commons))(.*):([0-9])': u'\g<1>\g<2>: \g<3>',
	#specific to one or few file(s)
	u'Cetate(a?)de([a-zăîâșțA-ZĂÂÎȘȚ])': u'Cetate\g<1> de \g<2>',
	u'Moar([ăa])de([a-zăîâșțA-ZĂÂÎȘȚ])': u'Moar\g<1> de \g<2>',
	u'Sitularheologic([a-zăîâșțA-ZĂÂÎȘȚ])': u'Situl arheologic \g<1>',
	u'șin([ăe])([0-9])': u'șin\g<1> \g<2>',
	u'defostul([a-zA-Z])': u'de fostul \g<1>',
	u'([Ll])a\s?distanț([aă])de([0-9])': u'\g<1>a distanț\g<2> de \g<3>',
	u'înpădure': u'în pădure',
	u'de(DJ|DN)': u'de \g<1>',
	u'([Cc])lopotniț([ăa])de([a-zăîâșț])': u'\g<1>lopotniț\g<2> de \g<3>',
	u'Ansamblu(l?)de([a-zăîâșț])': u'Ansamblu\g<1> de \g<2>',
	u'([Îî])nparteade': u'\g<1>n partea de',
	u'laextremitateade': u'la extremitatea de',
	u'allui': u'al lui',
	u'asatului': u'a satului',
	u'lemnabisericii([a-zăîâșț])': u'lemn a bisericii \g<1>',
	u'afosteimănăstiri': u'a fostei mănăstiri',
	u'([cC])astrulde': u'\g<1>astrul de',
	u'([pP])rimajum': u'\g<1>rima jum',
	u'înc .sec': u"înc. sec",
	u'înfața': u'în fața',
	u' azi([a-z]{2,})': u' azi \g<1>',
	#u'(\| (?!Imagine|Plan|Commons|NotăCod|Cod92))(.*)([a-z])([0-9])': u'\g<1>\g<2>\g<3> \g<4>',
	u'(\| Adresă)(.*)Str(.*?[a-z])([0-9])': u'\g<1>\g<2>Str\g<3> \g<4>',
	u'(\| (?!Imagine|Plan|Commons|NotăCod|Cod92))(.*)([0-9])(ale|[dD]e|[Ii]anuarie|[Ff]ebruarie|[Mm]artie|[Aa]prilie|[Mm]ai|[iI]unie|[iI]ulie|[Aa]ugust|[Ss]eptembrie|[Oo]ctombrie|[Nn]oiembrie)': u'\g<1>\g<2>\g<3> \g<4>',
	u'([Pp])eolungimede': u'\g<1>e o lungime de',
	u'personalitățialeistorieiși': u'personalități ale istoriei și',
	u'(\| (?!Imagine|Plan|Commons|NotăCod))(.*)([NSEV])(de|între|printre)': u'\g<1>\g<2>\g<3> \g<4>',
	u'(\| (?!Imagine|Plan|Commons|NotăCod))(.*)([NSE])al ': u'\g<1>\g<2>\g<3> al ',
	u'deunpâlcde': u'de un pâlc de',
	u'deint': u'de int',
	#u'(\| (?!Imagine|Plan|Commons|NotăCod))(.*)(DJ|DC|DN)([a-zA-Z])': u'\g<1>\g<2>\g<3> \g<4>',
	u'înfost(ul|a)([a-z])': u'în fost\g<1> \g<2>',
	u'deculmeaparalelă': u'de culmea paralelă',
	u'(de?)s a t': u'\g<1> sat',
	u'([bcdfghjklmnpqsștțvwxzăî])și ': u'\g<1> și ',
	u'([bcdfghjklmnpqrsștțvwxzăî])nr\.': u'\g<1> nr.',
	u'și(turn|zid|poartă|râul)': u'și \g<1>',
	u'turn(de|al)\s?([a-z]?)': u'turn \g<1> \g<2>',
	u'confluențapârâului': u'confluența pârâului',
	u' platoul , Dogaru\"': u' platoul \"Dogaru\"',
	u'(a|e|o|u|i)nr\.': u'\g<1> nr.',
	u'(d|t)de': u'\g<1> de',
	u'(\| (?!Imagine|Plan|Commons|NotăCod))(.*)([0-9])([a-zA-Z]{4,})': u'\g<1>\g<2>\g<3> \g<4>',
	u'afost': u'a fost',
	u'([0-9]-) ([0-9])': u'\g<1>\g<2>',
	u'([0-9]) (-[0-9])': u'\g<1>\g<2>',
	#u'([a-z]{4,})a a ': u'\g<1>a',
	#u'([a-z]{4,})a a ([^I])': u'\g<1>ă a \g<2>',
	u' ([înqwtyuipdfghjlzxcvbn]) ': u'\g<1> ', 
	u'(\| (?!Imagine|Plan|Commons|NotăCod|Creatori))(.*?)?([^=])"(.*?)"([^\/\>])': u'\g<1>\g<2>\g<3>„\g<4>”\g<5>',
	u'([^\s])„\s': u'\g<1> „',
	u'([a-zA-ZăîâșțĂÂÎȘȚ])„([a-zA-ZăîâșțĂÂÎȘȚ])': u'\g<1> „\g<2>',
	u'([a-zA-ZăîâșțĂÂÎȘȚ])”([a-zA-ZăîâșțĂÂÎȘȚ])': u'\g<1>” \g<2>',
	u'„ ([A-Za-z])': u'„\g<1>',
	u'([A-Za-zîăâșț]) ”': u'\g<1>”',
	u'([a-zA-ZăîâșțĂÂÎȘȚ])\(([a-zA-ZăîâșțĂÂÎȘȚ])': u'\g<1> (\g<2>',
	u'([a-zA-ZăîâșțĂÂÎȘȚ])\)([a-zA-ZăîâșțĂÂÎȘȚ])': u'\g<1>) \g<2>',
	u'CLUJ\s?-\s?NAPOCA': u'[[Cluj-Napoca]]',
	u'chimbarea a grup': u'chimbarea grup',
	u'(\| (?!Imagine|Plan|Commons|NotăCod|Adresă))(.*)([”„\.]) -([^\s])': u'\g<1>\g<2>\g<3> - \g<4>',
	u'Epoca([a-zA-Z]+)': u'Epoca \g<1>',
	u'\{\{#tag:ref\|(.*)\|name=\"cimec\"\}\}': u'<ref name=\"cimec\">\g<1></ref>'
}
minormistakes = {
	u'([Zz])iddeapărare': u'\g<1>id de apărare',
	u'peun([a-zA-Z])': u'pe un \g<1>',
	u'peo([a-zA-Z])': u'pe o \g<1>',
	u'(\| (?!Imagine|Plan|Commons|NotăCod))(.*) ,(,?)(\s?)': u'\g<1>\g<2>, ',
	u'(\| (?!Imagine|Plan|Commons|NotăCod))(.*) ;(\s?)': u'\g<1>\g<2>; ',
	u';;': u'; ',
	u'(\| (?!Imagine|Plan|Commons|NotăCod))(.*) \.(\.?)(\s?)': u'\g<1>\g<2>. ',
	u'([^a-z])(\s?)l a ': u'\g<1> la ',
	u'(N|S) (E|V)(\s?)d(\s?)e(\s?)': u'\g<1>\g<2> de ',
	u'troițadinfața': u'troița din fața',
	u'\"(și|dar|sau) ': u'\" \g<1> ',
	u' catre ': u' către ',
	u'Casă(ș|ț|ă|â|a)': u'Casă \g<1>',
	u'căzuțiînrăzboi': u'căzuți în război',
	u'(G|g)arade': u'\g<1>ara de',
	u'([;,:/?!])([;,.:?!])': u'\g<1>',
	u'cadastrala': u'cadastrală',
	u' =(\s*)" ': u' = "',
	u'(\| (?!Imagine|Plan|Commons|NotăCod))(.*)(\s+)" ([A-ZȘȚÂĂÎ])': u'\g<1>\g<2>\g<3>"\g<4>',
	u'dealtaa(\s*)': u'de alta a ',
	u'deoparte(\s*)': u'de o parte ',
	u' și(de|a|pe)': u' și \g<1>',
	u' k ': u' km ',
	u'([^aeiourpăâc\(_" ])și': u'\g<1> și',
	u' de([bg])': u' de \g<1>',
	u' de(pe|la|sub|peste|o|lemn|locul) ': u' de \g<1> ',
	u' la(vecini|extremitatea) ': u' de \g<1> ',
	u' pe(malul) ': u' pe \g<1> ',
	u'([Pp])iațade': u'\g<1>iața de',
	u'și(fa|vă|sp|gr)': u'și \g<1>',
	u'([Mm])ăgurade': u'\g<1>ăgura de',
	u'([Mm])irceacel': u'\g<1>ircea cel',
	u'([Mm])ovilacu': u'\g<1>ovila cu',
	u'amănăstir': u'a mănăstir',
	u'dinmina': u'din mina',
	u'\*80': u'\'80',
	u'SEa': u'SE a',
	u'([Cc])urteade': u'\g<1>urtea de',
	u'satpedrum': u'sat pe drum',
	u'([Aa])ripade(.*)': u'\g<1>ripa de \g<2>',
	u'([VE])a ': u'\g<1> a ',
	u'ridin': u'ri din',
	u'\[\[(.*)_(.*)\]\]': u'[[\g<1> \g<2>]]',
	u'(\s?)-(\s?)catolică': u'-catolică',
	u'o(\s?)-(\s?)(dac|roman)': u'o-\g<3>',
	u'(\| (?!Imagine|Plan|Commons|NotăCod|Adresă))(.*) -([^\s\"„\-])': u'\g<1>\g<2>-\g<3>',
	u'(\| (?!Imagine|Plan|Commons|NotăCod|Adresă))(.*)([^\s\"”\-;,\.])- ': u'\g<1>\g<2>\g<3>-',
	u'(\| (?!Imagine|Plan|Commons|NotăCod|Adresă))(.*) -([\"„\-])': u'\g<1>\g<2> - \g<3>',
	u'(\| (?!Imagine|Plan|Commons|NotăCod|Adresă))(.*)([\"”\.])- ': u'\g<1>\g<2>\g<3> - ',
	u'([Cc])as([aă])d': u'\g<1>as\g<2> d',
	u'([a-z])înpartea': u'\g<1> în partea',
	u'([a-z])depământ': u'\g<1> de pământ',
	u'{{CompactTOC6}}': u'{{CuprinsPrefix|prefix={{subst:#invoke:String|sub|{{subst:urlencode:{{subst:PAGENAME}}|WIKI}}|1|-2}}}}\n{{-}}',
	u'peambelemaluriale': u'pe ambele maluri ale',
	u'fața([^d\s\)])': u'fața \g<1>',
	u'părți([^lt\s\),.])': u'părți \g<1>',
	u'învi([ae])': u'în vi\g<1>',
	u'dealta': u'de alta',
	u'([XV]+)l ': u'\g<1>I ',
	u'sec(\.?)(\s?)([XVI]+) ([XVI]+)': u'sec.\g<2>\g<3>\g<4>',
	u'(\| .*)=  ': u'\g<1>= ',
	u'(\| (?!Imagine|Plan|CodRan|FostCod|NotăCod|OsmL|RefCod))(.*)  ': u'\g<1>\g<2> ',
	#u'(.+)\| (.*)\]\]': u'\g<1>|\g<2>]]',
	u'cucota': u'cu cota',
	u'arâului': u'a râului',
	u', ,': u', ',
	u';\s?,': u';',
	u'(\| (?!Imagine|Plan|Commons|NotăCod|Adresă))(.*)\.\.': u'\g<1>\g<2>.',
	u'terenulactualei': u'terenul actualei',
	u'lalimita': u'la limita',
	u' lași': u' Iași',
	u'șisec': u'și sec',
	u'centrulsatului': u'centrul satului',
	u'satdinspre': u'sat dinspre',
	u'terasăjoasă': u'terasă joasă',
	u'localitatedinspre': u'localitate dinspre',
	u'fragmentedin': u'fragmente din',
	u'\[\[\s': u'[[',
	u'(\[\[(?!Categorie).*)\|\s': u'\g<1>|',
	u'dispensaruluinr': u'dispensarului nr',
	u'alpârăului': u'al pârâului',
	u'aldealului': u'al dealului',
	u'înluncă': u'în luncă',
	u'parcelacadastrală': u'parcela cadastrală',
	u'(a|ale)șoselei': u'\g<1> șoselei',
	u'”\"': u'”',
	u'deextremitate': u'de extremitate',
}

deprecated = {
	u'\|(\s*)Coordonate\s=.*(\r?)\n': u'',
	u'\|(\s*)Arhitect(\s*)=(\s*)\n': u'',
	u'\|(\s*)Arhitect(\s*)=(\s*)(.*)\n': u'| Creatori = \g<4>\n',
}

improvements = OrderedDict([
	#link km and m to the number
	(u'([0-9])\s*[kK]m', u'\g<1>&nbsp;km'),
	(u'([0-9])\s*[mM]([\s,\.])', u'\g<1>&nbsp;m\g<2>'),
	(u'&nbsp;\s', u'&nbsp;'),
	(u'&nbsp;', u' '),#the replacement is U+00A0
	(u' \s', u' '),#the replacement is U+00A0
])

authors = {
}

def specialProcessingWhitespace():
	pass#u'((?P<letter>[a-zîâășț])( +)){3,}': u'\g<letter>',

def checkAndUpload(page, text, newtext, comment):
	if text != newtext:
                changed = True
                pywikibot.showDiff(text, newtext)
                resp = pywikibot.input("Do you agree with the change above? [y/n]")
                if resp == "y" or resp == "Y":
                         page.put(newtext, comment)
                         return newtext
	return text

def processList(page):
	pywikibot.output(u'Working on "%s"' % page.title(True))
	global mistakes
	global minormistakes
	marker = "@@"
	Rref = re.compile(ur'(<ref.*?>[^<]+</ref>|<ref.*?/>)')
	Rmarker = re.compile(ur'%s(\d+)%s' % (marker, marker))
	#global authors
	origtext = newtext = text = page.get()
	changed = False
	#comment = u'Înlocuiesc spațiul cu non-breaking space (U+00A0) în unitățile de distranță din articolul [[%s]]' % page.title(True)
	#comment = u'Scot câmpul învechite din {{ElementLMI}} în articolul [[%s]]' % page.title(True)
	comment = u'Se corectează anumite erori frecvente din articolul [[%s]]' % page.title(True)
	#for mistake in improvements.keys():
	#	newtext = re.sub(mistake, improvements[mistake], text)
	#	if text <> newtext:
	#		changed = True
	#		text = newtext
	for mistake in mistakes.keys():
		origtext = text
		count = 0
		intern = {}
		for m in Rref.finditer(text):
			count += 1
			t = m.group()
			text = text.replace(t, '%s%d%s' % (marker, count, marker))
			intern[count] = t

		newtext = re.sub(mistake, mistakes[mistake], text)

		matches = Rmarker.findall(newtext)
		for match in matches:

			count = int(match)
			newtext = newtext.replace(u'%s%d%s' % (marker, count, marker), intern[count])
		if newtext != origtext:
			print mistake
			text = checkAndUpload(page, origtext, newtext, comment)
		else:
		    text = origtext
	
	for mistake in minormistakes.keys():
		origtext = text
		count = 0
		intern = {}
		for m in Rref.finditer(text):
			count += 1
			t = m.group()
			text = text.replace(t, '%s%d%s' % (marker, count, marker))
			intern[count] = t

		newtext = re.sub(mistake, minormistakes[mistake], text, count=1000)

		matches = Rmarker.findall(newtext)
		for match in matches:
			count = int(match)
			newtext = newtext.replace(u'%s%d%s' % (marker, count, marker), intern[count])
		if newtext != origtext:
			print mistake
			text = checkAndUpload(page, origtext, newtext, comment)
		else:
		    text = origtext
	#for field in deprecated.keys():
	#	newtext = re.sub(field, deprecated[field], text)
	#	if text <> newtext:
	#		changed = True
	#		text = newtext
	if changed == True:
		pywikibot.showDiff(origtext, text)
		resp = pywikibot.input("Do you agree with ALL the changes above? [y/n]")
		#resp = "y"
		if resp == "y" or resp == "Y":
			page.put(text, comment)




def main():
	lang = u'ro'
	textfile = u''

	#for arg in pywikibot.handleArgs():
	#	if arg.startswith('-lang:'):
	#		lang = arg [len('-lang:'):]
	#		user.mylang = lang
	#	if arg.startswith('-family'):
	#		user.family = arg [len('-family:'):]

	site = pywikibot.getSite()
	lang = site.language()

	rowTemplate = pywikibot.Page(site, u'Format:ElementLMI')

	transGen = pagegenerators.ReferringPageGenerator(rowTemplate, onlyTemplateInclusion=True)
	filteredGen = pagegenerators.NamespaceFilterPageGenerator(transGen, [0], site)
	pregenerator = pagegenerators.PreloadingGenerator(filteredGen, 10)
	for page in pregenerator:
		if page.exists() and not page.isRedirectPage():
			processList(page)
	# page = pywikibot.Page(site, u"Lista monumentelor istorice din România/Dâmbovița")
	# processList(page)

if __name__ == "__main__":
	try:
		main()
	finally:
		pywikibot.output(u"Main error?")
		pywikibot.stopme()
