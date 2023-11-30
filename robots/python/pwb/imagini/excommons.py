#!/usr/bin/python
# -*- coding: utf-8  -*-
"""
This script retrieves FoP images from commons and saves them on rowiki, 
along with the corresponding wikitext.

Usage: python excommons.py -lang:commons -family:commons
"""
import codecs
import os
from os import listdir
from os.path import isfile, join
import re
import urllib.request

import pywikibot
from pywikibot import config as user
from pywikibot import pagegenerators
from pywikibot import textlib


def save_fop_category():
    delition_cat = "Category:Romanian_FOP_cases/pending"
    cat_page =  pywikibot.Category(pywikibot.Site(), delition_cat)
    generator = pagegenerators.CategorizedPageGenerator(cat_page)
    for proposal_page in generator:
        for file_page in proposal_page.linkedPages(namespaces=6):
            pywikibot.output(file_page.title())
            rowiki = pywikibot.Site('ro','wikipedia')

            local_file_page = pywikibot.FilePage(rowiki, file_page.title())
            if local_file_page.file_is_used == False:
                pywikibot.output("File unused, continue...")
                continue
            if local_file_page.file_is_shared() == False:
                if local_file_page.latest_file_info.sha1 == file_page.latest_file_info.sha1:
                    pywikibot.output("File is already downloaded, continue...")
                else:
                    pywikibot.warn("File name conflict: name already exists on local wiki, but with different contents")
                continue

            local_text = file_page.get() + "\n{{FOP România}}"
            builder = textlib.MultiTemplateMatchBuilder(rowiki)
            template_regex = builder.pattern("delete")
            local_text = re.sub(template_regex, "", local_text)
            print(local_text)
            return
            
            tmp_file_name = f"{os.getcwd()}/{file_page.title(as_filename=True, with_ns=False)}"
            success = file_page.download(filename=tmp_file_name)
            if success == False:
                pywikibot.error(f"Error downloading {file_page.title()}")
                os.remove(tmp_file_name)
                return
            success = local_file_page.upload(source=tmp_file_name, 
                        comment=f"File is about to be deleted on Commons, evacuating.", 
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
    else:
        pywikibot.error(f"Unknown operation {op}")

if __name__ == "__main__":
    main()
