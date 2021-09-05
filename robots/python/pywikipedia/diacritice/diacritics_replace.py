#!/usr/bin/python
# -*- coding: utf-8  -*-

'''
Bot to replace incorrect Romanian diacritics with correct ones

Command-line arguments:

&params;

-always           Don't prompt to make changes, just do them.

Example: "python diacritics_replace.py -start:B -always"
'''
#
# (C) Strainu 2010
# (C) Pywikibot bot team, 2007-2010
#
# Distributed under the terms of the GPLv2 license.
#
__version__ = '$Id: diacritics_redirects.py 7918 2010-02-08 11:24:22Z xqt $'
#

import time, sys, re
import string
import pywikibot
from pywikibot import pagegenerators

docuReplacements = {
    '&params;': pagegenerators.parameterHelp
}

msg = {	
	 'en': u'Robot: Replacing diacritics for [[:ro:Wikipedia:corectarea diacriticelor|Romanian diacritics correction]]',
     'ro': u'Robot: Înlocuiesc diacritice pentru [[Wikipedia:corectarea diacriticelor|corectarea diacriticelor]]',
     }

class DiacriticsReplaceBot:
    def __init__(self, generator, acceptall, titlecase):
        self.generator = generator
        self.acceptall = acceptall
        self.titlecase = titlecase
        self.site = pywikibot.getSite()
        self.done = False

    def run(self):
        for page in self.generator:
            if self.done: break
            if page.exists():
                self.treat(page)

    def treat(self, page):
        if page.isRedirectPage():
            page = page.getRedirectTarget()
        page_t = page.title()
        # Show the title of the page we're working on.
        # Highlight the title in purple.
        pywikibot.output(u"\n>>> \03{lightpurple}%s\03{default} <<<"
		 % page_t)
        if page_t.find(".js") > -1:
            # JS file, ignore
            pywikibot.output(u"Javascript file, skipping")
            return
		 
        try:
            text = page.get()
            #print text
        except:
            pywikibot.output(u"An error occurred while getting the page, skipping...")
            return
            
        #turkish_regexp  = re.compile(u"(<(span) lang=[^<>]*>((.|\r|\n)*?)<\/\1>)", re.I);
        turkish_regexp = re.compile(u"((<(span|div) lang=[^<>]*>(.|\r|\n)*?<\/span>)|(<(ref)(.*?)>(.|\r|\n)*?<\/ref>)|(<ref(.*?)\/(\s?)>)|(<(gallery)(.*?)>(.|\r|\n)*?<\/gallery>)|(\| (?=Commons).*\n)|(\|\s*(?=[Ii]magine).*\n)|(\|\s*(?=[hH]art[ăa]).*\n)|(\|\s*(?=[Ff]oto).*\n))", re.I);
        turkish_phrases = turkish_regexp.findall(text)
        interwiki_regexp  = re.compile(u"(\[\[:?([a-z]{2,3}|fișier|imagine|media|simple|roa-rup|be-x-old|zh-(yue|classical|min-nan)|bat-smg|cbk-zam|nds-nl|map-bms|cbk-zam|fişier|file|image):(.*?)\]\])", re.I);
        interwiki_phrases = interwiki_regexp.findall(text)
        template_regexp  = re.compile(u"(\{\{(proiecte surori|sisterlinks|commons|commonscat|wikicitat|wikimanuale|wikisursă|wikitravel|wikiştiri|wikţionar|WikimediaPentruPortale|titlu corect|wikisource|lang|lang-tr|lang-tt|lang-az|lang-ku|tr|tt|az|ku)\|((.|\n|\r)*?)\}\})", re.I);
        template_phrases = template_regexp.findall(text)
        
        go = False
        new_text = text.replace(u'ş', u'ș')
        new_text = new_text.replace(u'Ş', u'Ș')
        if new_text == text:
            go = True
        new_text = new_text.replace(u'ţ', u'ț')
        new_text = new_text.replace(u'Ţ', u'Ț')
        
        #print "turkish"
        mixed_phrases = turkish_regexp.findall(new_text)
        #print mixed_phrases
        #print turkish_phrases
        if len(mixed_phrases) > 0 and len(turkish_phrases) > 0:
            for i in range(len(mixed_phrases)):
                new_text = new_text.replace(mixed_phrases[i][0], turkish_phrases[i][0], 1)
        
        #print "interwiki" 
        mixed_phrases = interwiki_regexp.findall(new_text)
        #print mixed_phrases
	#print "-----------------------------------------------------------------------"
        #print interwiki_phrases
        if len(mixed_phrases) > 0 and len(interwiki_phrases) > 0:
            for i in range(len(mixed_phrases)):
                new_text = new_text.replace(mixed_phrases[i][0], interwiki_phrases[i][0], 1)
        
	#print "template"
        mixed_phrases = template_regexp.findall(new_text)
        #print mixed_phrases
	#print template_phrases
        if len(mixed_phrases) > 0 and len(template_phrases) > 0:
            for i in range(len(mixed_phrases)):
                new_text = new_text.replace(mixed_phrases[i][0], template_phrases[i][0], 1)
                
        if new_text == text:
            #pywikibot.output(u"Weird, no diacritics in the page, skipping...")
            return

        pywikibot.showDiff(text, new_text)
            
        if not self.acceptall:
            if go:
                choice = 'y'
            else:
                choice = pywikibot.inputChoice(
                    u'Do you want to replace the diacritics?',
                    ['Yes', 'No', 'All', 'Quit'], ['y', 'N', 'a', 'q'], 'N')
            if choice == 'a':
                self.acceptall = True
            elif choice == 'q':
                self.done = True
        if self.acceptall or choice == 'y':
            comment = pywikibot.translate(self.site, msg)
            try:
                page.put(new_text, comment)
                pywikibot.output(u"Replaced diacritics...")
            except Exception as e:
                pywikibot.output(u"An error occurred, skipping...%s")
                pywikibot.output(e)

def main():
    genFactory = pagegenerators.GeneratorFactory()
    acceptall = False
    titlecase = False

    for arg in pywikibot.handleArgs():
        if arg == '-always':
            acceptall = True
        elif genFactory.handleArg(arg):
            pass
        else:
            pywikibot.showHelp(u'diacritics_replace')
            return

    gen = genFactory.getCombinedGenerator()
    preloadingGen = pagegenerators.PreloadingGenerator(gen, 500)
    bot = DiacriticsReplaceBot(preloadingGen, acceptall, titlecase)
    bot.run()

if __name__ == "__main__":
    try:
        main()
    finally:
        pywikibot.stopme()
