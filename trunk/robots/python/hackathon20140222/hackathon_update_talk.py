# -*- coding: utf-8  -*-
"""
Print a list of pages, as defined by page generator parameters

These parameters are supported to specify which pages titles to print:

&params;
"""
#
# (C) Pywikibot team, 2008-2013
#
# Distributed under the terms of the MIT license.
#
__version__ = '$Id: 582cc60d3d1391ff48c5f4a21d7a4969a2814170 $'
#

import pywikibot
from pywikibot.pagegenerators import GeneratorFactory, parameterHelp

docuReplacements = {'&params;': parameterHelp}


def main(*args):
    gen = None
    genFactory = GeneratorFactory()
    for arg in pywikibot.handleArgs(*args):
        genFactory.handleArg(arg)
    gen = genFactory.getCombinedGenerator()
    if gen:
        i = 0
        for page in gen:
            if not page.exists():
                continue
            page = page.toggleTalkPage()
            i += 1
            pywikibot.stdout("%s" % page.title())
            page.put("{{DateCimec|1=http://ghidulmuzeelor.cimec.ro/}}", "Adding license")
    else:
        pywikibot.showHelp()

if __name__ == "__main__":
    try:
        main()
    finally:
        pywikibot.stopme()
