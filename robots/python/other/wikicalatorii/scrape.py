#!/usr/bin/python
# -*- coding: utf-8  -*-

'''
Robot that scrapes content of Romanian wikitravel pages. 

Since Wikitravel disabled the api and does not allow export, we need to scrape the wikitext from the edit form:
<textarea tabindex="1" class="mw-textarea-sprotected" accesskey="," id="wpTextbox1" cols="80" rows="25" style="" name="wpTextbox1"></textarea>
'''
#
# (C) Strainu 2012
#
# Distributed as Public Domain
#

import time, sys, re
import string
import urllib2
import xml.dom.minidom

pages_file = "../../../wikitravel_pages.txt"
url_prefix = "http://wikitravel.org/wiki/ro/index.php?action=edit&title="
regexp = "<textarea (.*)>((.|\r|\n)*)</textarea>"

def get_url(url):
	'''get_url accepts a URL string and return the server response code, response headers, and contents of the file'''
	req_headers = {
		'User-Agent': 'Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:14.0) Gecko/20100101 Firefox/14.0.1',
		'Host': 'wikitravel.org',
		'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
		'Referer': 'http://wikitravel.org/wiki/ro/index.php?action=edit&title=2004',
		'Cookie': 'ki_u=7e51a380-b01d-0f4e-a26d-23b87ed3fb9e; ki_t=1347046989383%3B1347091225931%3B1347091225931%3B2%3B141; wikitravel_org_roUserID=354; wikitravel_org_roUserName=Strainu; BIGipServerwikitravel-varnish1_POOL=2199916554.52514.0000; wikitravel_ip=188.26.141.229; wikitravel_org_ro_session=2d53a6ea1db78f93c94f9a94e1e66b65; BIGipServerwikitravel_POOL=2250248202.20480.0000',}
	
	request = urllib2.Request(url, headers=req_headers) # create a request object for the URL
	opener = urllib2.build_opener() # create an opener object
	response = opener.open(request) # open a connection and receive the http response headers + contents
	     
	code = response.code
	headers = response.headers # headers object
	contents = response.read() # contents of the URL (HTML, javascript, css, img, etc.)
	return contents

def parsePages(filename):
	namespace = ""
	pages = []
	f = open(filename, "r")
	print filename
	for line in f:
		if line[0] == "-":
			namespace = line[4:-1]
			print "Now working in namespace " + namespace
		else:
			page = (namespace + line[:-1])
			print page
			pages += [page]
	f.close()
	return pages

def main():
	compiled = re.compile(regexp)
	pages = parsePages(pages_file)
	for page in pages:
		url = url_prefix + page
		print url
		wikitext = get_url(url)
		#print wikitext
		matchlist = compiled.findall(wikitext)
		content = ""
		if len(matchlist):
			content = matchlist[0][1]
			print content
		open(page.replace("/","__") + ".wiki.txt", "w+").write(content)

if __name__ == "__main__":
    try:
        main()
    except Exception as a:
    	print a
