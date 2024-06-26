#!/usr/bin/python
# -*- coding: utf-8  -*-

'''
Bot to create redirects from correct to incorrect Romanian diacritics

Command-line arguments:

&params;

-always           Don't prompt to make changes, just do them.

Example: "python duplicate_category.py -start:B -always"
'''
#
# (C) Strainu 2010
# (C) Pywikipedia bot team, 2007-2010
#
# Distributed under the terms of the GPLv2 license.
#
__version__ = '$Id: duplicate_category.py 7918 2010-02-08 11:24:22Z xqt $'
#

import time, sys, re
import string
import wikipedia as pywikibot
import pagegenerators

docuReplacements = {
    '&params;': pagegenerators.parameterHelp
}

msg = {	
     'ar': u'روبوت: إنشاء تحويلة إلى [[%s]]',
     'cs': u'Robot vytvořil přesměrování na [[%s]]',
     'de': u'Bot: Weiterleitung angelegt auf [[%s]]',
     'en': u'Robot: Create redirect to [[%s]]',
     'fr': u'robot: Créer redirection à [[%s]]',
     'he': u'בוט: יוצר הפניה לדף [[%s]]',
     'ja': u'ロボットによる: リダイレクト作成 [[%s]]',
     'ksh': u'Bot: oemleidung aanjelaat op [[%s]]',
     'nl': u'Bot: doorverwijzing gemaakt naar [[%s]]',
     'pt': u'Bot: Criando redirecionamento para [[%s]]',
     'ro': u'Robot: Crează redirectare la [[%s]]. Vezi [[Wikipedia:Corectarea diacriticelor]]',
     'sv': u'Bot: Omdirigerar till [[%s]]',
     'zh': u'機器人: 建立重定向至[[%s]]',
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
        
        new_page_t = string.replace(page_t, u'ş', u'ș')
        new_page_t = string.replace(new_page_t, u'ţ', u'ț')
        new_page_t = string.replace(new_page_t, u'Ş', u'Ș')
        new_page_t = string.replace(new_page_t, u'Ţ', u'Ț')
        page_mod = pywikibot.Page(self.site, new_page_t)
        
        if new_page_t == page_t:
            pywikibot.output(u'%s does not contain diacritics, skipping...\n'
                             % page_mod.aslink())
        elif page_mod.exists():
            pywikibot.output(u'%s already exists, skipping...\n'
                             % page_mod.aslink())
        else:            
            pywikibot.output(u'[[%s]] doesn\'t exist' % page_mod.title())
            if not self.acceptall:
                choice = pywikibot.inputChoice(
                        u'Do you want to create a new category?',
                        ['Yes', 'No', 'All', 'Quit'], ['y', 'N', 'a', 'q'], 'N')
                if choice == 'a':
                    self.acceptall = True
                elif choice == 'q':
                    self.done = True
            if self.acceptall or choice == 'y':
                comment = pywikibot.translate(self.site, msg) % new_page_t
                try:
                    page_mod.put(text, comment)
                    page.put(u'{{Redirect categorie|%s}}' % page_mod.titleWithoutNamespace(), comment)
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
            pywikibot.showHelp(u'diacritics_redirects')
            return

    gen = genFactory.getCombinedGenerator()
    preloadingGen = pagegenerators.PreloadingGenerator(gen)
    bot = DiacriticsBot(preloadingGen, acceptall, titlecase)
    bot.run()

if __name__ == "__main__":
    try:
        main()
    finally:
        pywikibot.stopme()
