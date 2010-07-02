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
# (C) Pywikipedia bot team, 2007-2010
#
# Distributed under the terms of the GPLv2 license.
#
__version__ = '$Id: diacritics_redirects.py 7918 2010-02-08 11:24:22Z xqt $'
#

import time, sys, re
import string
import wikipedia as pywikibot
import pagegenerators

docuReplacements = {
    '&params;': pagegenerators.parameterHelp
}

msg = {	
	 'en': u'Robot: Replacing diacritics for [[:ro:Wikipedia:corectarea diacriticelor|Romanian diacritics correction]]',
     'ro': u'Robot: Înlocuite diacritice pentru [[Wikipedia:corectarea diacriticelor|corectarea diacriticelor]]',
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
        except:
            pywikibot.output(u"An error occurred while getting the page, skipping...")
            return
            
        turkish_regexp  = re.compile("(<(span) lang=[^<>]*>((.|\r|\n)*?)<\/\1>)", re.I);
        turkish_phrases = turkish_regexp.findall(text)
        interwiki_regexp  = re.compile("((:?)\[\[([a-z]{2,3}|simple|roa-(rup|tara)|be-x-old|zh-(yue|classical|min-nan)|bat-smg|fiu-vro|nds-nl|map-bms|cbk-zam|fi(ș|ş)ier|imagine|media):(.*?)\]\])", re.I);
        interwiki_phrases = interwiki_regexp.findall(text)
        template_regexp  = re.compile("(\{\{(sisterlinks|commons|commonscat|incubator|interwiki|species|wikicitat|wikimanuale|wikisursă|wikitravel|wikiştiri|wikţionar)\|(.*?)\}\})", re.I);
        template_phrases = template_regexp.findall(text)
        
        new_text = string.replace(text, u'ş', u'ș')
        new_text = string.replace(new_text, u'ţ', u'ț')
        new_text = string.replace(new_text, u'Ş', u'Ș')
        new_text = string.replace(new_text, u'Ţ', u'Ț')
        
        mixed_phrases = turkish_regexp.findall(new_text)
        print "turkish"
        print mixed_phrases
        print turkish_phrases
        if len(mixed_phrases) > 0 and len(turkish_phrases) > 0:
            for i in range(len(mixed_phrases)):
                new_text = string.replace(new_text, mixed_phrases[i][0], turkish_phrases[i][0])
        
        print "interwiki" 
        print mixed_phrases        
        print interwiki_phrases
        mixed_phrases = interwiki_regexp.findall(new_text)
        if len(mixed_phrases) > 0 and len(interwiki_phrases) > 0:
            for i in range(len(mixed_phrases)):
                new_text = string.replace(new_text, mixed_phrases[i][0], interwiki_phrases[i][0])
        
        print mixed_phrases
        mixed_phrases = template_regexp.findall(new_text)
        if len(mixed_phrases) > 0 and len(template_phrases) > 0:
            for i in range(len(mixed_phrases)):
                new_text = string.replace(new_text, mixed_phrases[i][0], template_phrases[i][0])
                
        if new_text == text:
            pywikibot.output(u"Weird, no diacritics in the page, skipping...")
            return
            
        if not self.acceptall:
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
                page.close()
                pywikibot.output(u"Replaced diacritics...")
            except:
                pywikibot.output(u"An error occurred, skipping...")

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
    preloadingGen = pagegenerators.PreloadingGenerator(gen)
    bot = DiacriticsReplaceBot(preloadingGen, acceptall, titlecase)
    bot.run()

if __name__ == "__main__":
    try:
        main()
    finally:
        pywikibot.stopme()
