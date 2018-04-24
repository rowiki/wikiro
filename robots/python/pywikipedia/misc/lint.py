#!/usr/bin/python

import json
import pywikibot
import re
import requests

def getList(lintError):
	url = 'https://ro.wikipedia.org/w/api.php?action=query&format=json&list=linterrors&lntcategories=%s&lntlimit=max&lntnamespace=0%%7C10%%7C102%%7C100' % lintError
	#url = 'https://ro.wikipedia.org/w/api.php?action=query&format=json&list=linterrors&lntcategories=%s&lntlimit=max' % lintError
	print(url)
	try:
		r = requests.get(url)
	except:
		return
		#print(r.status_code)
	
	js = json.loads(r.text)
	if "query" not in js or "linterrors" not in js["query"]:
		return []
	return js["query"]["linterrors"]

def solve_font_bug(text):
	newtext = re.sub("<font(.*?)>\[\[(.*?)\|(.*?)\]\]</font>", r"[[\2|<font\1>\3</font>]]", text)
	newtext = re.sub("<font(.*?)>\[\[(.*?)\]\]</font>", r"[[\2|<font\1>\2</font>]]", newtext)
	#newtext = re.sub("<font(.*?)>\[\[", "[[<font\1>", text)
	return newtext

def solve_wrongly_closed_tag(text):
	newtext = re.sub("<font id(.*?)\s*/\s*>", r"<span id\1></span>", text)
	newtext = re.sub(r"<\s*(b(?!r).*?|[ac-z][a-z]*)\s*>([^<\n]*)<\s*/?\s*\1\s*/?\s*>", r"<\1>\2</\1>", newtext)
	return newtext

def solve_misnested_tag(text):
	newtext = re.sub(r"\{\{nc\|([^\}]*?)\n\n", r"{{nc|\1}}\n\n{{nc|", text)
	newtext = re.sub(r"\n\s*\{\{nowrap end", r"{{nowrap end", newtext)
	newtext = re.sub(r"\{\{·w\}\}\n", r"{{·w}}", newtext)
	newtext = re.sub(r"\{\{nowrap begin\}\}\n", r"{{nowrap begin}}", newtext)
	return newtext

def solve_image_options(text):
	newtext = text
	newtext = re.sub(r"(\d{2,4})\s*px\.", r"\1px", newtext)
	newtext = re.sub(r"(\d{2,4})\s*pv", r"\1px", newtext)
	newtext = re.sub(r"(\d{2,4})\s*pix\s*\|", r"\1px|", newtext)
	newtext = re.sub(r"(\d{2,4})\s*pt", r"\1px", newtext)
	newtext = re.sub(r"(\d{2,4})\s*x([^\d])", r"\1px\2", newtext)
	newtext = re.sub(r"(\d{2,4})\s*pcx", r"\1px", newtext)
	newtext = re.sub(r"\|upleft", r"", newtext)
	newtext = re.sub(r"\|\{\{\{\d\}\}\}", r"", newtext)
	newtext = re.sub(r"\|links", r"|left", newtext)
	newtext = re.sub(r"\|sinistra", r"|left", newtext)
	newtext = re.sub(r"\|rechts", r"|right", newtext)
	newtext = re.sub(r"\|([^r])igh([^t]?)\|", r"|right|", newtext)
	newtext = re.sub(r"\|vertical(ă?)", r"|upright", newtext)
	newtext = re.sub(r"\| , \|", r"|", newtext)
	#newtext = re.sub(r"\|( +)\|", r"|", newtext)
	#newtext = re.sub(r"\|(\d+)\|", r"|\1px|", newtext)
	newtext = re.sub(r"\|\s*\|\s*\|\s*thumb", r"|thumb", newtext)
	newtext = re.sub(r"\|\s*\|\s*thumb", r"|thumb", newtext)
	return newtext

def solve(lintError, f):
	pages = getList(lintError)
	s = set([])
	for p in pages:
		title = p["title"]
		s.add(title)
		if "name" in p["templateInfo"]:
			s.add(p["templateInfo"]["name"])
	for title in s:
		page = pywikibot.Page(pywikibot.getSite(), title)
		while page.isRedirectPage():
			page = page.getRedirectTarget()
		print("Working on %s" % title)
		text = page.get()
		newtext = f(text)
		if newtext != text:
			pywikibot.showDiff(text, newtext)
			resp = pywikibot.input("Do you agree with ALL the changes above? [y/n]")
			#resp = "y"
			if resp == "y" or resp == "Y":
				page.put(newtext, "Solving lint error")

def main():
	#solve("tidy-font-bug", solve_font_bug)
	#solve("self-closed-tag", solve_wrongly_closed_tag)
	#solve("multiple-unclosed-formatting-tags", solve_wrongly_closed_tag)
	#solve("html5-misnesting", solve_misnested_tag)
	solve("bogus-image-options", solve_image_options)


if __name__ == "__main__":
	main()
