#!/usr/bin/python
# -*- coding: utf-8  -*-

'''
Bot to create redirects from correct to incorrect Romanian diacritics

Command-line arguments:

&params;

-always           Don't prompt to make changes, just do them.

Example: "python diacritics_redirects.py -start:B -always"
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
import pywikibot
from pywikibot import pagegenerators
from pywikibot import config as user

docuReplacements = {
    '&params;': pagegenerators.parameterHelp
}

msg = {	
     'ro': u'Robot: Crează redirectare la [[%s]] pentru [[Wikipedia:Corectarea diacriticelor|corectarea diacriticelor]]',
    }

class DiacriticsBot:
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
                         
        #try to see if the title contains turkish diacritics
        try:
            text = page.get()
        except:
            pywikibot.output(u"An error occurred while getting the page, skipping...")
            return
        if text.find(u"{{titlu corect") > -1: 
            # turkish text, we should ignore it, maybe log it
            pywikibot.output(u"Found title marked as turkish, skipping...")
            return
        
        #transrule = string.maketrans("şţŞŢ", "șțȘȚ")
        #page_cap = pywikibot.Page(self.site, page_t.translate(transrule))
        new_page_t = string.replace(page_t, u'ș', u'ş')
        new_page_t = string.replace(new_page_t, u'ț', u'ţ')
        new_page_t = string.replace(new_page_t, u'Ș', u'Ş')
        new_page_t = string.replace(new_page_t, u'Ț', u'Ţ')
        page_mod = pywikibot.Page(self.site, new_page_t)
        
        if new_page_t == page_t:
            pywikibot.output(u'%s does not contain diacritics, skipping...\n'
                             % page_mod.aslink())
        elif page_mod.exists() and page_mod.isRedirectPage() and page_mod.getRedirectTarget() == page:
            pywikibot.output(u'%s already exists, skipping...\n'
                             % page_mod.aslink())
        else:            
            pywikibot.output(u'[[%s]] doesn\'t exist' % page_mod.title())
            if not self.acceptall:
                choice = pywikibot.inputChoice(
                        u'Do you want to create a redirect?',
                        ['Yes', 'No', 'All', 'Quit'], ['y', 'N', 'a', 'q'], 'N')
                if choice == 'a':
                    self.acceptall = True
                elif choice == 'q':
                    self.done = True
            if self.acceptall or choice == 'y':
                comment = pywikibot.translate(self.site, msg) % page_t
                try:
                	page_mod.put(u"#%s [[%s]]" % (self.site.redirect(True), page_t.title()), comment)
                except Exception as e:
                    pywikibot.output(u"An error occurred, skipping...")
                    print type(e)

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
            pywikibot.showHelp(u'diacritics_redirects')
            return

    #gen = genFactory.getCombinedGenerator()
    #preloadingGen = pagegenerators.PreloadingGenerator(gen)
    transGen = pagegenerators.AllpagesPageGenerator('Ț', includeredirects=False)
    pregenerator = pagegenerators.PreloadingGenerator(transGen, 120)
    bot = DiacriticsBot(pregenerator, acceptall, titlecase)
    bot.run()

if __name__ == "__main__":
    try:
        main()
    finally:
        pywikibot.stopme()
