#!/usr/bin/python
# -*- coding: utf-8  -*-

'''
Bot to create redirects from no diacritics to pages containing non-Romanian diacritics

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
     'ro': u'Robot: Crează redirectare la [[%s]] pentru a putea introduce titlul manual',
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
        if text.find(u"{{titlu corect") > -1: 
            # turkish text, we should ignore it, maybe log it
            pywikibot.output(u"Found title marked as turkish, skipping...")
            return
        #TODO: the list below is incomplete and might lead to unexpected results
        new_page_t = page_t
        new_page_t = string.replace(new_page_t, u'á', u'a')
        new_page_t = string.replace(new_page_t, u'é', u'e')
        new_page_t = string.replace(new_page_t, u'ü', u'u')
        new_page_t = string.replace(new_page_t, u'ö', u'o')
        new_page_t = string.replace(new_page_t, u'ó', u'o')
        new_page_t = string.replace(new_page_t, u'ã', u'a')
        new_page_t = string.replace(new_page_t, u'í', u'i')
        new_page_t = string.replace(new_page_t, u'ä', u'a')
        new_page_t = string.replace(new_page_t, u'ß', u'ss') 	
        new_page_t = string.replace(new_page_t, u'ç', u'c')
        new_page_t = string.replace(new_page_t, u'Á', u'A')
        new_page_t = string.replace(new_page_t, u'ú', u'u') 	
        new_page_t = string.replace(new_page_t, u'ł', u'l') 	
        new_page_t = string.replace(new_page_t, u'è', u'e')
        new_page_t = string.replace(new_page_t, u'š', u's')
        new_page_t = string.replace(new_page_t, u'ø', u'o')
        new_page_t = string.replace(new_page_t, u'ő', u'o') 	
        new_page_t = string.replace(new_page_t, u'à', u'a') 	
        new_page_t = string.replace(new_page_t, u'õ', u'o') 	
        new_page_t = string.replace(new_page_t, u'ô', u'o') 	
        new_page_t = string.replace(new_page_t, u'č', u'c') 	
        new_page_t = string.replace(new_page_t, u'ë', u'e') 	
        new_page_t = string.replace(new_page_t, u'ñ', u'n') 	
        new_page_t = string.replace(new_page_t, u'É', u'E') 	
        new_page_t = string.replace(new_page_t, u'ć', u'c') 	
        new_page_t = string.replace(new_page_t, u'ê', u'e') 	
        new_page_t = string.replace(new_page_t, u'ō', u'o') 	
        new_page_t = string.replace(new_page_t, u'ń', u'n') 	
        new_page_t = string.replace(new_page_t, u'ï', u'i')
        new_page_t = string.replace(new_page_t, u'ę', u'e') 	
        new_page_t = string.replace(new_page_t, u'ž', u'z') 	
        new_page_t = string.replace(new_page_t, u'Š', u'S')
        new_page_t = string.replace(new_page_t, u'ą', u'a') 	
        new_page_t = string.replace(new_page_t, u'Ö', u'O') 	
        new_page_t = string.replace(new_page_t, u'ð', u'd')
        new_page_t = string.replace(new_page_t, u'ý', u'y') 	
        new_page_t = string.replace(new_page_t, u'ř', u'r') 	
        new_page_t = string.replace(new_page_t, u'Ś', u'S') 	
        new_page_t = string.replace(new_page_t, u'å', u'a') 	
        new_page_t = string.replace(new_page_t, u'ś', u's') 	
        new_page_t = string.replace(new_page_t, u'ż', u'z') 	
        new_page_t = string.replace(new_page_t, u'æ', u'ae')
        new_page_t = string.replace(new_page_t, u'ğ', u'g') 	
        new_page_t = string.replace(new_page_t, u'ǎ', u'a') 	
        new_page_t = string.replace(new_page_t, u'Ž', u'Z') 	
        new_page_t = string.replace(new_page_t, u'ò', u'o')
        new_page_t = string.replace(new_page_t, u'Č', u'C') 	
        new_page_t = string.replace(new_page_t, u'Ç', u'C') 	
        new_page_t = string.replace(new_page_t, u'Å', u'A') 	
        new_page_t = string.replace(new_page_t, u'ě', u'e') 	
        new_page_t = string.replace(new_page_t, u'ū', u'u') 	
        new_page_t = string.replace(new_page_t, u'Ō', u'O')
        new_page_t = string.replace(new_page_t, u'ā', u'a') 	
        new_page_t = string.replace(new_page_t, u'Ż', u'Z')
        new_page_t = string.replace(new_page_t, u'ė', u'e') 	
        new_page_t = string.replace(new_page_t, u'Ü', u'U')
        new_page_t = string.replace(new_page_t, u'ň', u'n') 	
        new_page_t = string.replace(new_page_t, u'Ó', u'O')
        new_page_t = string.replace(new_page_t, u'ź', u'z') 	
        new_page_t = string.replace(new_page_t, u'Ú', u'U') 	
        new_page_t = string.replace(new_page_t, u'Í', u'I') 	
        new_page_t = string.replace(new_page_t, u'İ', u'I')
        new_page_t = string.replace(new_page_t, u'Đ', u'D') 	
        new_page_t = string.replace(new_page_t, u'œ', u'oe')
        new_page_t = string.replace(new_page_t, u'û', u'u')
        new_page_t = string.replace(new_page_t, u'À', u'A') 	
        new_page_t = string.replace(new_page_t, u'ű', u'u') 	
        new_page_t = string.replace(new_page_t, u'Ä', u'A') 	
        new_page_t = string.replace(new_page_t, u'ĺ', u'I') 	
        new_page_t = string.replace(new_page_t, u'ì', u'i')
        new_page_t = string.replace(new_page_t, u'ē', u'e')
        new_page_t = string.replace(new_page_t, u'ů', u'u') 	
        new_page_t = string.replace(new_page_t, u'ù', u'u') 	
        new_page_t = string.replace(new_page_t, u'Æ', u'Ae')
        new_page_t = string.replace(new_page_t, u'Ć', u'C')
        new_page_t = string.replace(new_page_t, u'Ò', u'O') 	
        new_page_t = string.replace(new_page_t, u'Õ', u'O')
        new_page_t = string.replace(new_page_t, u'ċ', u'c') 	
        new_page_t = string.replace(new_page_t, u'ệ', u'e')
        #new_page_t = string.replace(page_t, u'ş', u'ș')
        #new_page_t = string.replace(new_page_t, u'ţ', u'ț')
        #new_page_t = string.replace(new_page_t, u'Ş', u'Ș')
        #new_page_t = string.replace(new_page_t, u'Ţ', u'Ț')
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
                        u'Do you want to create a redirect?',
                        ['Yes', 'No', 'All', 'Quit'], ['y', 'N', 'a', 'q'], 'N')
                if choice == 'a':
                    self.acceptall = True
                elif choice == 'q':
                    self.done = True
            if self.acceptall or choice == 'y':
                comment = pywikibot.translate(self.site, msg) % page_t
                try:
                    page_mod.put(u"#%s [[%s]]" % (self.site.redirect(True), page_t), comment)
                except IOError as (errno, strerror):
                    pywikibot.output(u"An error occurred, skipping... %s" % strerror)

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
