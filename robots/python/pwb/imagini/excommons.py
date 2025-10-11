#!/usr/bin/python
# -*- coding: utf-8  -*-
"""
This script retrieves FoP images from commons and saves them on rowiki, 
along with the corresponding wikitext.

Usage: python excommons.py -lang:commons -family:commons
"""
import os
import re

import pywikibot
from pywikibot import pagegenerators
from pywikibot import textlib


def save_fop_category():
    deletion_cat = "Category:Romanian_FOP_cases/pending"
    cat_page =  pywikibot.Category(pywikibot.Site(), deletion_cat)
    generator = pagegenerators.CategorizedPageGenerator(cat_page)
    for proposal_page in generator:
        for file_page in proposal_page.linkedPages(namespaces=6):
            evacuate_file(file_page, 'ro', 'wikipedia')

def save_relevant_templates():
    opt = {
        "templates_yes":"delete",
        "templates_any":"Monument istoric\nSIRUTA\nCodRAN"
    }
    generator = pagegenerators.PetScanPageGenerator("", namespaces=[6], extra_options=opt)
    for file_page in generator:
        evacuate_file(pywikibot.FilePage(file_page), 'ro', 'wikipedia')

def evacuate_file(file_page, lang, family):
    pywikibot.output(file_page.title())
    if not file_page.exists():
        pywikibot.output("File was already deleted, continue...")
        return

    local_site = pywikibot.Site(lang,family)

    local_file_page = pywikibot.FilePage(local_site, file_page.title())
    if not local_file_page.file_is_used:
        pywikibot.output("File unused, continue...")
        return
    if not local_file_page.file_is_shared():
        if local_file_page.latest_file_info.sha1 == file_page.latest_file_info.sha1:
            pywikibot.output("File is already downloaded, continue...")
        else:
            pywikibot.warning("File name conflict: name already exists on local "
                       "wiki, but with different contents")
        return

    local_text = file_page.get() + "\n{{FOP România}}"
    builder = textlib.MultiTemplateMatchBuilder(pywikibot.Site('ro', 'wikipedia'))
    template_regex = builder.pattern("delete")
    local_text = re.sub(template_regex, "", local_text)
    print(local_text)
            
    tmp_file_name = f"{os.getcwd()}/{file_page.title(as_filename=True, with_ns=False)}"
    success = file_page.download(filename=tmp_file_name)
    if not success:
        pywikibot.error(f"Error downloading {file_page.title()}")
        os.remove(tmp_file_name)
        return

    success = local_file_page.upload(source=tmp_file_name, 
               comment=f"Evacuez un fișier ce va fi șters de la Commons", 
               text=local_text, 
               ignore_warnings=True)
    os.remove(tmp_file_name)
    if success:
        pywikibot.output(f"Uploaded {local_file_page.title()}")
    else:
        pywikibot.error(f"Error uploading {local_file_page.title()}")

def main():
    op = None
    for arg in pywikibot.handle_args():
        if arg.startswith('-op'):
            op = arg [len('-op:'):]
    #user.mylang = config[cfg]['lang']
    #user.family = config[cfg]['family']
	
    if op == "fop":
        save_fop_category()
    elif op == "fmt":
        save_relevant_templates()
    else:
        pywikibot.error(f"Unknown operation {op}")

if __name__ == "__main__":
    main()
