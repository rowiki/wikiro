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
	u'(\| (?!Imagine))(.*)\( ': u'(',
	u'(\| (?!Imagine))(.*)	 \)': u')',
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
	u'(\| (?!Imagine))(.*)([Ll])a([0-9])': u'\g<3>a \g<4>',
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
	u'(\| (?!Imagine|Commons))(.*)([Nn])r\.?([0-9])': u'\g<1>\g<2>\g<3>r. \g<4>',
	u'(\| (?!Imagine|Commons))(.*)([Nn])r\s?([0-9])': u'\g<1>\g<2>\g<3>r. \g<4>',
	u'(\| (?!Imagine|Commons))(.*)([^:])([sS])tr\.([^\s_])': u'\g<1>\g<2>\g<3>\g<4>tr. \g<5>',
	u'cca\.?\s?([0-9])': u'cca. \g<1>',
	u'([Ll])a\s?cca\.?(\s?)': u'\g<1>a cca.\g<2>',
	u'(\| (?!Imagine))(.*),(([^0-9\s_]))': u'\g<1>\g<2>, \g<3>',
	#u'(\| (?!Imagine))(.*)([^w])\.([a-zăîâșțA-ZĂÂÎȘȚ]{4,})': u'\g<1>\g<2>\g<3>. \g<4>',#high risk
	u'(\| (?!Imagine|Commons))(.*)([^w])\.([a-gi-zăîâșțA-GI-ZĂÂÎȘȚ][a-zăîâșțA-ZĂÂÎȘȚ]{3,})': u'\g<1>\g<2>\g<3>. \g<4>',#high risk
	u'(\| (?!Imagine))(.*)sf\.sec': u'\g<1>\g<2>sf. sec',
	u'(\| (?!Imagine))(.*)sec\.X': u'\g<1>\g<2>sec. X',
	u'(\| (?!Imagine|Commons))(.*)\s*:([a-zăâîșț])': u'\g<1>\g<2>: \g<3>',#high risk
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
	u'(\| (?!Imagine|CodRan|FostCod|NotăCod))(.*)([a-zăîâșț])([A-ZĂÂÎȘȚ])': u'\g<1>\g<2>\g<3> \g<4>',#high risk
	u' alui ': u' a lui ',
	u'(I|V|X)a\.(\s?)Chr\.': u'\g<1> a. Chr.',
	u'a\.Chr\.': u'a. Chr.',
	u'(I|V|X)p\.(\s?)Chr\.': u'\g<1> p. Chr.',
	u'p\.Chr\.': u'p. Chr.',
	u'(\| (?!Imagine))(.*)sec\.(I|V|X)': u'\g<1>\g<2>sec. \g<3>',
	u'(\| (?!Imagine|Commons))(.*):([0-9])': u'\g<1>\g<2>: \g<3>',
}
minormistakes = {
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
	u'(\| (?!Imagine|Commons|NotăCod|Cod92))(.*)([a-z])([0-9])': u'\g<1>\g<2>\g<3> \g<4>',
	u'(\| (?!Imagine|Commons|NotăCod|Cod92))(.*)([0-9])(ale|[dD]e|[Ii]anuarie|[Ff]ebruarie|[Mm]artie|[Aa]prilie|[Mm]ai|[iI]unie|[iI]ulie|[Aa]ugust|[Ss]eptembrie|[Oo]ctombrie|[Nn]oiembrie)': u'\g<1>\g<2>\g<3> \g<4>',
	u'([Pp])eolungimede': u'\g<1>e o lungime de',
	u'personalitățialeistorieiși': u'personalități ale istoriei și',
	u'(\| (?!Imagine|Commons|NotăCod))(.*)([NSEV])(de|între|printre)': u'\g<1>\g<2>\g<3> \g<4>',
	u'deunpâlcde': u'de un pâlc de',
	u'deint': u'de int',
	u'(\| (?!Imagine|Commons|NotăCod))(.*)(DJ|DC|DN)([a-zA-Z])': u'\g<1>\g<2>\g<3> \g<4>',
	u'înfost(ul|a)([a-z])': u'în fost\g<1> \g<2>',
	u'deculmeaparalelă': u'de culmea paralelă',
	u'(de?)s a t': u'\g<1> sat',
	u'([bcdfghjklmnpqsștțvwxzăî])și ': u'\g<1> și ',
	u'([bcdfghjklmnpqrsștțvwxzăî])nr\.': u'\g<1> nr.',
	u'și(turn|zid|poartă)': u'și \g<1>',
	u'turn(de|al)\s?([a-z]?)': u'turn \g<1> \g<2>',
	
}

deprecated = {
	u'\|\s?Coordonate\s=.*(\r?)\n': u'',
	u'\| Arhitect =\s{0,2}\n': u'',
	u'\| Arhitect = (.*)\n': u'| Creatori = \g<1>\n',
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

def checkAndUpload(page, text, newtext, comment):
	if text != newtext:
                changed = True
                #print mistake
                pywikibot.showDiff(text, newtext)
                resp = pywikibot.input("Do you agree with the change above? [y/n]")
                if resp == "y" or resp == "Y":
                         page.put(newtext, comment)
                         return newtext
	return text

def processList(page):
	pywikibot.output(u'Working on "%s"' % page.title(True))
	#global mistakes
	global minormistakes
	#global authors
	origtext = text = page.get()
	changed = False
	#comment = u'Înlocuiesc spațiul cu non-breaking space (U+00A0) în unitățile de distranță din articolul [[%s]]' % page.title(True)
	#comment = u'Scot câmpul învechite din {{ElementLMI}} în articolul [[%s]]' % page.title(True)
	comment = u'Se corectează anumite erori frecvente din articolul [[%s]]' % page.title(True)
	#for mistake in improvements.keys():
	#	newtext = re.sub(mistake, improvements[mistake], text)
	#	if text <> newtext:
	#		changed = True
	#		text = newtext
	#for mistake in mistakes.keys():
	#	newtext = re.sub(mistake, mistakes[mistake], text)
	#	if newtext != text:
	#		text = checkAndUpload(page, text, newtext, comment)
	
	for mistake in minormistakes.keys():
		newtext = re.sub(mistake, minormistakes[mistake], text)
		if newtext != text:
			text = checkAndUpload(page, text, newtext, comment)
	#for field in deprecated.keys():
	#	newtext = re.sub(field, deprecated[field], text)
	#	if text <> newtext:
	#		changed = True
	#		print author
	#		text = newtext
	return
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
