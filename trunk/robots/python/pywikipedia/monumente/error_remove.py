#!/usr/bin/python
# -*- coding: utf-8  -*-
'''
Parse the monument pages (articles, lists and images) and correct frequent
mistakes found in lists

'''

import sys, time, warnings, json, string
sys.path.append("..")
import wikipedia, re, pagegenerators
import config as user

mistakes = {
	u',\s,': u',',
	u'\( ': u'(',
	u' \)': u')',
	#u' {2,}': u' ',
	u'([Cc])rucedepiatră': u'\g<1>ruce de piatră',
	u' și\"(\s+)' : u' și\g<1>\"',
	u'([0-9]+)\s+,\s+' : u'\g<1>, ',
	u'\sk\sm\s': u' km ',
	u'([0-9])\s?[kK]\s?m\s?(SV|NV|SE|NE|nord|sud|est|vest|[NSEV]|de|față)': u'\g<1> km \g<2>',
	u'([0-9])\s?[mM]\s?(SV|NV|SE|NE|nord|sud|est|vest|[NSEV]|de|față)': u'\g<1> m \g<2>',
	u'([0-9]),(\s+)([0-9]+)\skm': u'\g<1>,\g<3> km',
	u'([0-9])\.([0-9]+)\skm': u'\g<1>,\g<3> km',
	u'([Kk])m([0-9])': u'\g<1>m \g<2>',
	u'([Ll])a(SV|NV|SE|NE|nord|sud|est|vest|[NSEV])de': u'\g<1>a \g<2> de ',
	u'([Ll])a([0-9])': u'\g<1>a \g<2>',
	u'([șȘ])i([0-9])': u'\g<1>i \g<2>',
	u'([0-9])([Ll])a': u'\g<1> \g<2>a',
	u'([0-9])([Șș])i': u'\g<1> \g<2>i',
	u'(SV|NV|SE|NE|nord|sud|est|vest|[NSEV])\s?de\s?(sat|oraș|intravilan|localitate|comună|șosea|drum|conac|Dunăre)': '\g<1> de \g<2>',
	u'([k\s]m)\s?de\s?(sat|oraș|intravilan|localitate|comună|șosea|drum)': '\g<1> de \g<2>',
	u'<?!n>desat': 'de sat',
	u'PiațaUnirii([0-9]+)' : u'Piața Unirii \g<1>',
	u'PiațaUnirii' : u'Piața Unirii',
	u'S\s?f\s?\.' : u'Sf.',
	u'([Pp])oddepiatră': u'\g<1>od de piatră',
	u'(\| (?!Imagine))(.*)([Nn])r\.?([0-9])': u'\g<1>\g<2>\g<3>r. \g<2>',
	u'(\| (?!Imagine))(.*)([Nn])r\s?([0-9])': u'\g<1>\g<2>\g<3>r. \g<2>',
	u'([^:])([sS])tr\.([^\s_])': u'\g<1>\g<2>tr. \g<2>',
	u'cca\.?\s?([0-9])': u'cca. \g<1>',
	u'([Ll])a\s?cca\.?(\s?)': u'\g<1>a cca.\g<2>',
	u',(([^0-9\s_]))': u', \g<2>',
	u'(\| (?!Imagine))(.*)([^w])\.([a-zăîâșțA-ZĂÂÎȘȚ]{4,})': u'\g<1>\g<2>\g<3>. \g<4>',#high risk
	u'(\| (?!Imagine))(.*)sf\.sec': u'\g<1>\g<2>sf. sec',
	u'(\| (?!Imagine))(.*)sec\.X': u'\g<1>\g<2>sec. X',
	u'\s*:([a-zăâîșț])': u': \g<1>',#high risk
	u' *; *': u'; ',
	u'șila': u'și la',
	u'șiîn': u'și în',
	u'([^a])dela': u'\g<1>de la',
	u'și([Ss])tr': u'și \g<1>tr',
	u'([a-zăâîșț])de([jJ])os': u'\g<1> de Jos',
	u'([a-zăâîșț])de([sS])us': u'\g<1> de Sus',
	u'\[\[([a-zăâîșț]+)([Mm])are([^a-zăâîșț])': u'[[\g<1> Mare\g<3>',
	u'\[\[([a-zăâîșț]+)([Mm])ic([^a-zăâîșț])': u'[[\g<1> Mic\g<3>',
	u'D\s?N\s?([0-9]{1,3})\s([ABCDEFGH])([\s,\.])': u'DN\g<1>\g<2>\g<3>',
	u'D\s?N\s?([0-9]{1,3})': u'DN\g<1>',
	u'Ziddeincintă': u'Zid de incintă',
	u'(\| (?!Imagine))(.*)([a-zăîâșț])([A-ZĂÂÎȘȚ])': u'\g<1>\g<2>\g<3> \g<4>',#high risk
	#specific to one or few file(s)
	u'Cetate(a?)de([a-zăîâșțA-ZĂÂÎȘȚ])': u'Cetate\g<1> de \g<2>',
	u'Moar([ăa])de([a-zăîâșțA-ZĂÂÎȘȚ])': u'Moar\g<1> de \g<2>',
	u'Sitularheologic([a-zăîâșțA-ZĂÂÎȘȚ])': u'Situl arheologic \g<1>',
	u'șin([ăe])([0-9])': u'șin\g<1> \g<2>',
	u'defostul([a-zA-Z])': u'de fostul \g<1>',
	u'([Ll])a\s?distanț([aă])de([0-9])': u'\g<1>a distanț\g<2> de \g<3>',
	u'înpădure': u'în pădure',
	u'de(DJ|DN)': u'de \g<1>',
	u'Clopotnițăde([a-zăîâșț])': u'Clopotniță de \g<1>',
	u'Ansamblu(l?)de([a-zăîâșț])': u'Ansamblu\g<1> de \g<2>',
	u'([Îî])nparteade': u'\g<1>n partea de',
	u'laextremitateade': u'la extremitatea de',
	u'allui': u'al lui',
	u'asatului': u'a satului',
}

coords = {
	u'\|\s?Coordonate\s=.*(\r?)\n': u'',
}

improvements = {
	#link km and m to the number
	u'([0-9])\s*[kK]m': u'\g<1>&nbsp;km',
	u'([0-9])\s*[mM]([\s,\.])': u'\g<1>&nbsp;m\g<2>',
	u'&nbsp;\s': u'&nbsp;',
	u'&nbsp;': u' ',#the replacement is U+00A0
	u' \s': u' ',#the replacement is U+00A0
}

def processList(page):
	wikipedia.output(u'Working on "%s"' % page.title(True))
	global mistakes
	origtext = text = page.get()
	changed = False
	for mistake in mistakes.keys():
		newtext = re.sub(mistake, mistakes[mistake], text)
		if text <> newtext:
			changed = True
			text = newtext
	#comment = u'Înlocuiesc spațiul cu non-breaking space (U+00A0) în unitățile de distranță din articolul [[%s]]' % page.title(True)
	#comment = u'Scot câmpul de coordonate din articolul [[%s]]' % page.title(True)
	comment = u'Se corectează anumite erori frecvente din articolul [[%s]]' % page.title(True)
	if changed == True:
		wikipedia.showDiff(origtext, newtext)
		resp = wikipedia.input("Do you agree with ALL the changes above? [y/n]")
		if resp == "y" or resp == "Y":
			page.put(newtext, comment)
		

	
	
def main():
	lang = u'ro'
	textfile = u''

	for arg in wikipedia.handleArgs():
		if arg.startswith('-lang:'):
			lang = arg [len('-lang:'):]
			user.mylang = lang
		if arg.startswith('-family'):
			user.family = arg [len('-family:'):]
	
	site = wikipedia.getSite()
	lang = site.language()
			
	rowTemplate = wikipedia.Page(site, u'Format:ElementLMI')

	transGen = pagegenerators.ReferringPageGenerator(rowTemplate, onlyTemplateInclusion=True)
	filteredGen = pagegenerators.NamespaceFilterPageGenerator(transGen, [0], site)
	pregenerator = pagegenerators.PreloadingGenerator(filteredGen, 10)
	for page in pregenerator:
		if page.exists() and not page.isRedirectPage():
			processList(page)
	# page = wikipedia.Page(site, u"Lista monumentelor istorice din România/Dâmbovița")
	# processList(page)

if __name__ == "__main__":
	try:
		main()
	finally:
		wikipedia.output(u"Main error?")
		wikipedia.stopme()
