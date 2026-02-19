#!/usr/bin/python
# -*- coding: utf-8  -*-

#
# (C) Strainu, 2021
#
# Distributed under the terms of the MIT license.
#
#

import re, urllib.request, urllib.error, urllib.parse, urllib.request, urllib.parse, urllib.error
from collections import OrderedDict
import pywikibot
from pywikibot import textlib
from pywikibot import pagegenerators
import math
import time
import string
import sys
import csv

sys.path.append("wikiro/robots/python/pwb")
import strainu_functions as sf

always=False

def get_template_as_dict(text, template):
    template_text = sf.extractTemplate(text, template)
    if template_text == None:
        return (None, None, None)
    try:
        d,k = sf.tl2Dict(template_text)
    except:
        pywikibot.error("Could not convert template from text to dict")
        return (None, None, None)
    return (d, k, template_text)

def maybe_newline(inline):
    t = ""
    if not inline:
        t = "\n"

    return t

def rebuild_template_from_dict(data, keys, skip_empty=False, inline=False):
    my_template = "{{" + data["_name"] + maybe_newline(inline)
    for key in keys:
        if not key in data:
            print("ERROR: key {key} is not in data {data}".format(key=key, data=data))
            data[key] = ""

        if skip_empty and sf.is_null_or_empty(data[key].strip()):
            continue

        if key == "_name":
            continue

        my_template += u"| " + key + u" = " + data[key].strip() + maybe_newline(inline)

    my_template += "}}" + maybe_newline(inline)
    return my_template

def rework_template(page,
                    text,
                    template,
		    add_list,
		    remove_list,
		    skip_empty=False,
		    inline=False):
		    #expand_value=True):
    global always
    pywikibot.output("Working on {title}".format(title=page.title()))
    description = "Modific parametri: "
    data, keys, template_text = get_template_as_dict(text, template)

    if data == None:
        pywikibot.error("Template {tl} not found in {title}".format(tl=template, title=page.title()))
        return

    for add_key in add_list.keys():
        description += "+{key} ".format(key=add_key)
        add_other, add_value, add_before = add_list[add_key]
        #if expand_value:
        #    site = pywikibot.getSite()
        #    add_value = site.expand_text(add_value, page.title())
        if add_other not in data:
            add_index = -1
        else:
            add_index = keys.index(add_other)
        if not add_before:
            add_index += 1

        if add_key not in data:
            keys.insert(add_index, add_key)
        else:
            pywikibot.warning("Trying to add an existing parameter, replacing instead")
        data[add_key] = add_value

    for remove_key in remove_list:
        description += "-{key} ".format(key=remove_key)
        keys.remove(remove_key)
        del data[remove_key]

    new_template = rebuild_template_from_dict(data, keys, skip_empty, inline)
    if template_text == new_template:
        return

    new_text = text.replace(template_text, new_template)
    pywikibot.showDiff(text, new_text)
    if always:
        answer = 'y'
    else:
        answer = pywikibot.inputChoice("Do you want to save this change?", ["Yes", "No", "Always"], ['y', 'n', 'a'])
        if answer == 'a':
            always = True
            answer = 'y'

    if answer == 'y':
        page.put(new_text, description)


def main():
    add_list = OrderedDict({})
    remove_list = []
    skip_empty = False
    inline = False
    template = None
    gen_factory = pagegenerators.GeneratorFactory()
    
    for arg in pywikibot.handle_args():
        #-add:(before|after)old_keynew_key:value
        if arg.startswith('-add:'):
            s = arg.split(':')
            add_list[s[3]] = (s[2], s[4], s[1] == "before")

        #-rm:old_key
        elif arg.startswith('-rm:'):
            remove_list.append(arg[len('-rm:')])

        elif arg.startswith('-short'):
            skip_empty = True

        elif arg.startswith('-inline'):
            inline = True

        elif arg.startswith('-template:'):
            template = arg[len('-template:')]

        else:
            gen_factory.handleArg(arg)

    if template == None:
        pywikibot.error("You must give a template")
        return

    if len(remove_list) == 0 and len(add_list) == 0:
        while True:
            answer = pywikibot.inputChoice("Do you want to add or remove another parameter?", ["add after", "add before", "remove", "start"], ["a", "b", "r", "s"])
            if answer == 's':
                break
            name = pywikibot.input('Parameter name')
            if answer == 'r':
                remove_list.append(name)
            elif answer == 'a' or answer == 'b':
                value = pywikibot.input('Parameter value')
                after = pywikibot.input('The other param')
                add_list[name] = (after, value, answer == 'b')
    
    gen = gen_factory.getCombinedGenerator(preload=True)

    for page in gen:
        text = page.get()
        rework_template(page, text, template, add_list, remove_list, skip_empty, inline)


if __name__ == "__main__":
    main()
