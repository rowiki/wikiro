#!/usr/bin/python
# -*- coding: utf-8  -*-

import sys, json
import threading
import urllib, urllib2
import time
sys.path.append("..")
import wikipedia
'''
This script creates LMI links in the Cod namespace on ro.wp basted on 
the monument database.

See [[Wikipedia:Coduri]] (ro) for details on the Cod namespace.
'''

MAX_THREADS = 100
active_threads = 0
active_threads_lock = threading.Lock()
pages_ro = None

def split_code(code):
	parts = code.split('-')
	if len(parts) < 3:
		print parts
		return (None, None, None, None)
	return (parts[0], parts[1], parts[2], parts[3])

def main():
	global active_threads, acthive_threads_lock, pages_ro

	f = open("lmi_db.json", "r+")
	wikipedia.output("Reading database file...")
	db = json.load(f)
	wikipedia.output("...done")
	f.close();
	
	f = open("ro_pages.json", "r+")
	wikipedia.output("Reading articles file...")
	pages_ro = json.load(f)
	wikipedia.output("...done")
	f.close();
	
	site = wikipedia.getSite()

	for code in pages_ro:
		active_threads_lock.acquire()
		while active_threads > MAX_THREADS:
			active_threads_lock.release()
			time.sleep(1);
			active_threads_lock.acquire()
		active_threads += 1
		active_threads_lock.release()
		th = ArticleRedirectThread(site, code)
                th.start()
                #th.run()

	
        for monument in db:
                if monument["Cod"] in pages_ro:
                        continue
		active_threads_lock.acquire()
		while active_threads > MAX_THREADS:
			active_threads_lock.release()
			time.sleep(1);
			active_threads_lock.acquire()
		active_threads += 1
		active_threads_lock.release()
		th = ListRedirectThread(site, monument)
                th.start()
                #th.run()
	
class ArticleRedirectThread(threading.Thread):
	def __init__ ( self, _site, _code):
                self.site = _site
		self.code = _code
                threading.Thread.__init__ ( self )

	def run(self):
		global active_threads, acthive_threads_lock, pages_ro
		page = wikipedia.Page(self.site, u"Cod:LMI:" + self.code)
		wikipedia.output(page.title())
		#if page.exists() and not page.isRedirect():
		#		wikipedia.output(u"Page %s is not a redirect" % page.title())
		#else:
		page.put(u"#redirecteaza[[%s]]" % pages_ro[self.code][0]["name"], "Redirecting code to the Wikipedia article")
		active_threads_lock.acquire()
		active_threads -= 1
		active_threads_lock.release()
		


class ListRedirectThread(threading.Thread):
        def __init__ ( self, _site, _monument):
                self.site = _site
		self.monument = _monument
                threading.Thread.__init__ ( self )

        def run(self):
		global active_threads, acthive_threads_lock, pages_ro
		page = wikipedia.Page(self.site, u"Cod:LMI:" + self.monument["Cod"])
                wikipedia.output(page.title())
                
                source_page = wikipedia.url2link(self.monument["source"][self.monument["source"].find(u'=')+1:self.monument["source"].find(u'&')], self.site, self.site)
                #wikipedia.output(source_page)
                source_page = wikipedia.Page(self.site, source_page)
                page_text = u"#redirect [[{0}#{1}]]".format(source_page.title(), self.monument["Cod"])
                if not page.exists() or page.get(False, True) <> page_text:
                        page.put(page_text, "Redirecting code to the Wikipedia article")
		active_threads_lock.acquire()
		active_threads -= 1
		active_threads_lock.release()
				

if __name__ == "__main__":
	try:
		main()
	finally:
		wikipedia.stopme()

