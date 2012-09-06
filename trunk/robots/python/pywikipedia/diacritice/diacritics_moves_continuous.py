#!/usr/bin/python
# -*- coding: utf-8  -*-

'''
Bot to move files from incorrect to correct Romanian diacritics titles.

This is an addition to diacritics_moves.py, that takes a list of pages 
from a Wikipedia page and moves the pages, reporting every step in the talk page.

Command-line arguments:

&params;

-always           Don't prompt to make changes, just do them. (ignored)

Example: "python diacritics_moves_continuous.py"
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
import datetime
sys.path.append("..")
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
     'ro': u'Robot: Mută [[%s]] pentru [[Wikipedia:corectarea diacriticelor|]]',
     'sv': u'Bot: Omdirigerar till [[%s]]',
     'zh': u'機器人: 建立重定向至[[%s]]',
    }

class DiacriticsBot:
    def __init__(self, generator, acceptall, titlecase, outpage):
        self.generator = generator
        self.acceptall = acceptall
        self.titlecase = titlecase
        self.site = pywikibot.getSite()
        self.done = False
	self.outpage = outpage
	self.output = "\n==Rulare din %s==\n<span lang=\"ro\">\n" % datetime.datetime.now()

    def run(self):
        for page in self.generator:
            if self.done: break
            if page.exists():
                self.treat(page)
        pywikibot.output(self.output)
	self.output += "</span>\n--~~~~\n"
	comment = "Raport de rulare"
	if self.outpage.exists():
	    self.outpage.put(self.outpage.get() + self.output, comment)
        else:
	    self.outpage.put(self.output, comment)

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
        new_page_t = string.replace(page_t, u'ş', u'ș')
        new_page_t = string.replace(new_page_t, u'ţ', u'ț')
        new_page_t = string.replace(new_page_t, u'Ş', u'Ș')
        new_page_t = string.replace(new_page_t, u'Ţ', u'Ț')
        page_mod = pywikibot.Page(self.site, new_page_t)
        
        if new_page_t == page_t:
            pywikibot.output(u'%s does not contain diacritics, skipping...\n'
                             % page_mod.aslink())
        else:
	    if page_mod.exists() and not page_mod.isRedirectPage():
                self.output += u"* [[%s]] conține diacritice vechi, iar [[%s]] nu este o pagină de redirecționare\n" % (page_t, new_page_t)
		return
	    elif page_mod.exists() and not page == page_mod.getRedirectTarget():
	       self.output += u"* [[%s]] conține diacritice vechi, iar [[%s]] redirecționează spre o alta pagină ([[%s]])\n" % (page_t, new_page_t, page_mod.getRedirectTarget().title())
	       return
            pywikibot.output(u'[[%s]] will be created' % page_mod.title())
            comment = pywikibot.translate(self.site, msg) % page_t
            try:
                page.move(new_page_t, comment, throttle=True, deleteAndMove=True, leaveRedirect=True, )
            except Exception as e:
	        self.output += u"* Nu am putut muta [[%s]] la [[%s]]. Eroarea a fost: %s\n" % (page_t, new_page_t, str(e))
		pywikibot.output(u"An error occurred, skipping...: %s" % str(e))

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
    #import pdb; pdb.set_trace()
    pagina = u'Wikipedia:Corectarea_diacriticelor/titluri_cu_sedil%c4%83'
    root = pywikibot.Page(pywikibot.getSite(), pagina)
    gen = pagegenerators.LinkedPageGenerator(root)
    preloadingGen = pagegenerators.PreloadingGenerator(gen)
    bot = DiacriticsBot(preloadingGen, acceptall, titlecase, root.toggleTalkPage())
    bot.run()

if __name__ == "__main__":
    try:
        main()
    finally:
        pywikibot.stopme()
