# -*- coding: utf-8  -*-
"""
This bot will rename some pages.

You can run the bot with the following commandline parameters:
    -remove:text         Removes the given string from the title (can be used 
                         more than once)
    -append:text         Adds the specified string to the end of the title
    -prefix:text         Adds the specified string to the begining of the title
    -file:filename       The file with the articles to be renamed (compulsory)

Examples:

    1. Adding the county name to a number of towns:
        python pagerename.py -append:", County" -file:towns.txt
    
    2. Replacing "_ab" with "_ba" at the end of the title:
        python pagerename.py -remove:_ab -append:_ba -file:titles.txt
    
    3. Replacing "ab_" with "ba_" at the beginning of the title:
        python pagerename.py -remove:ab_ -prefix:ba_ -file:titles.txt
    
"""
#
# (C) Andrei Cipu, 2007
#
# Distributed under the terms of the CC-GNU GPL License.
#
__version__='$Id: pagerename.py,v 1.000 2006/12/31 11:26:00 wikipedian Exp $'

from __future__ import generators
import sys, re
import wikipedia, pagegenerators, catlib, config

msg={
    'en': u'Automated renaming of articles',
    'fr': u'Renommer automatiquement les articles',
    'ro': u'Redenumirea automată a articolelor'
    }
    
class RenameRobot:
    """
    A robot that renames pages
    """
    def __init__(self, generator, pre = "", post = "", rem = []):
        """
        Arguments:
            * generator    - A generator that yields Page objects.
            * pre          - The text to prefix the title with
            * post         - The text to append to the title
            * rem          - The text to remove from titles
        """
        self.generator = generator
        self.pre = pre
        self.post = post
        self.rem = rem
    
    def run(self):
        """
        Starts the robot
        """
        # Run the generator which will yield Pages which might need to be
        # renamed.
        for page in self.generator:
            try:
                # Load the page's text from the wiki
                old_title = page.title()
                new_title = old_title
                for old in self.rem:
                    new_title = new_title.replace(old,"")
                    #print "old",old,new_title
                new_title = self.pre + new_title + self.post
                if new_title == old_title:
                    wikipedia.output(u'%s: The old and new names are the same' % page.title())
                    continue
                #print "Vechi:",old_title,"\nNou:",new_title
                commenttext = "Robot: Renamed " + old_title + " to " + new_title
                try:
                    original_text = page.get()
                    redirect_text = "#redirect [["+new_title+"]]"
                    if not page.canBeEdited():
                        wikipedia.output(u'Skipping locked page %s' % page.title())
                        continue
                                
                    #writing the files
                    page2 = wikipedia.Page(wikipedia.getSite(), new_title)
                    wikipedia.output(page2.title())
                    if page2.exists():
                        wikipedia.output(u"Page %s already exists, not adding!" % page2.title())
                        continue;
                    else:
                        #print page2.title(),original_text, commenttext
                        page2.put(original_text, comment = commenttext, minorEdit = False)
                        #print redirect_text, commenttext
                        page.put(redirect_text, comment = commenttext, minorEdit = False)
                except wikipedia.IsRedirectPage:
                    wikipedia.output(u'Page %s is a redirect' % page.title())
                    continue
            except wikipedia.NoPage:
                wikipedia.output(u'Page %s not found' % page.title())
                continue
    
def main():
    gen=None
    #what we will add to the begining of the title
    pre=""
    #what we will add to the end of the title
    post=""
    #what we will remove from the title
    rem=[]
    
    # Read commandline parameters.
    for arg in wikipedia.handleArgs():
        if arg.startswith('-append'):
            if len(arg) >= 8:
                post = arg[8:]
            else: 
                post = wikipedia.input(u'Please enter the text to append:')
        elif arg.startswith('-file'):
            if len(arg) >= 6:
                textfilename = arg[6:]
                gen = pagegenerators.TextfilePageGenerator(textfilename)
        elif arg.startswith('-prefix'):
            if len(arg) >= 8:
                pre = arg[8:]
            else: 
                pre = wikipedia.input(u'Please enter the text to prefix with:')
        elif arg.startswith('-remove'):
            if len(arg) >= 8:
                rem.append(arg[8:])
            else: 
                rem.append(wikipedia.input(u'Please enter the text to remove:'))
    
    if not gen:
        # syntax error, show help text from the top of this file
        wikipedia.output(__doc__, 'utf-8')
        wikipedia.stopme()
        sys.exit()
    preloadingGen = pagegenerators.PreloadingGenerator(gen, pageNumber = 20)
    bot = RenameRobot(preloadingGen, pre, post, rem)
    bot.run()

if __name__ == "__main__":
    try:
        main()
    finally:
        wikipedia.stopme()