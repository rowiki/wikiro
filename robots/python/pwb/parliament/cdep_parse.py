#!/usr/bin/python
# -*- coding: utf-8 -*-

from bs4 import BeautifulSoup
import logging
import requests
import re
import codecs
import csv
import sys
import json

sys.path.append("wikiro/robots/python/pwb")
import strainu_functions
import csvUtils as csv_utils
import parliament
from parliament import group, person
import pywikibot


birthdate_regex = r"n\.\s+([0-9]{1,2})\s+([a-z\.]{3,4})\s+([0-9]{4})"

people = {
}

invalid_group = u"Deputați neafiliați"

def extractElectoralDistrict(bf):
	res = bf.find('div', attrs={'class':'boxStiri'})
	txt = res.findAll('div', attrs={'class':'boxDep clearfix'})[0].p.text
	match = re.search(r"circumscripţia electorală nr\.([0-9]+)([A-ZȘȚÂĂÎŞŢ]*,)((.|\s)*?[0-9]+)", txt)
	if match:
		print(match.groups())
		return parliament.allCommas(u" ".join([match.group(1), match.group(2).title(), match.group(3).strip()]))
	else:
		return u""

def extractParties(bf):
	res = bf.findAll('h3', string=u"Formaţiunea politică:")
	if res and len(res):
		res = res[0].parent
	else:
		return u""
	txt = res.findAll('tr', attrs={'valign':'top'})[0].next.next.text
	print("Partid", txt)
	return txt

def extractCommisions(bf):
	pass

def extractBirthdate(bf):
	match = re.search(birthdate_regex, bf.find('div', attrs={'class':'profile-pic-dep'}).text)
	if match:
		return u"-".join([match.group(3), parliament.months[match.group(2)], "%02d" % int(match.group(1))])
	else:
		return u""

def extractLegislatures(bf):
	llist = bf.findAll('div', string=u"Alte legislaturi")[0].nextSibling.next
	ret = {}
	#print(llist)
	if not llist:
		return ret
	for element in str(llist).split("<li>"):
		#print("element", element)
		chamber = element[element.find('>')+1:]
		chamber = chamber[:chamber.find(' ')]
		begin = element[element.find('-')+2:element.find('-')+6]
		#print(chamber, begin)
		if chamber == "Deputat":
			ret[begin] = 2
		elif chamber == "Senator":
			ret[begin] = 1
	print(ret)
	return ret

def ParliamentCsv(csvName):
	with open(csvName, "r") as f:
		reader = csv_utils.unicodeCsvReader(f, delimiter=';', quotechar="'")
		for row in reader:
			name = row[0]
			people[name] = parliament.person.ElectedPerson()
			people[name].name = name
			people[name].birthdate = row[1]
			people[name].chamber = int(row[2])
			people[name].district = row[3]
			people[name].wiki = row[4]
			people[name].index = row[5]
			people[name].party = row[6]
			try:
				js = json.loads(row[7])
			except ValueError:
				js = {}
			people[name].legislatures = js

def ElectionsCsv(csvName):
	with open(csvName, "r") as f:
		reader = csv_utils.unicodeCsvReader(f)
		for row in reader:
			name = row[0]
			if name not in people:
				continue
				#people[name] = ElectedPerson()
				#people[name].name = name
			#if row[2] != invalid_group:
			#	people[name].endLastGroup(row[1])
			if row[3] != invalid_group:
				g = parliament.group.GroupMembership()
				g.name = row[3]
				g.start = row[1]
				people[name].addGroup(g)

def MovesCsv(csvName):
	with open(csvName, "r") as f:
		reader = csv_utils.unicodeCsvReader(f)
		for row in reader:
			name = row[0]
			if name not in people:
				continue
				#people[name] = ElectedPerson()
				#people[name].name = name
			#TODO: check group name
			if row[2] != invalid_group:
				people[name].endLastGroup(row[1])
			if row[3] != invalid_group:
				g = parliament.group.GroupMembership()
				g.name = row[3]
				g.start = row[1]
				people[name].addGroup(g)

def DemisionsCsv(csvName):
	with open(csvName, "r") as f:
		reader = csv_utils.unicodeCsvReader(f)
		for row in reader:
			name = row[0]
			if name not in people:
				continue
			#TODO: check group name
			people[name].endLastGroup(row[3])

def scrollThroughPages():
	count = [0,0,0]
	f = codecs.open("wikiro/robots/python/pwb/parliament/parliament_2024.csv", "w+", "utf8")
	for camera in [1,2]:
		for om in range(1,340):
			url = 'https://www.cdep.ro/pls/parlam/structura2015.mp?idm=%d&leg=2024&cam=%d' % (om, camera)
			print(url)
			try:
				#logging.basicConfig()
				#logging.getLogger().setLevel(logging.DEBUG)
				#requests_log = logging.getLogger("requests.packages.urllib3")
				#requests_log.setLevel(logging.DEBUG)
				#requests_log.propagate = True
				HEADERS = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.99 Safari/537.36'}
				r = requests.get(url, headers=HEADERS, timeout=5)
			except Exception as e:
				print(e)
				continue
			print(r.status_code)
	
			html = r.text
			parsed_html = BeautifulSoup(html)
			if parsed_html.find('div', attrs={'class':'profile-pic-dep'}).img['src'] == u"/img/judete/judet.jpg":
				continue
			#	print(u"Camera %d s-a terminat" % camera)
			#	break
			new_man = len(parsed_html.findAll('div', string=u"Alte legislaturi")) == 0
			prev_legislatures = {}
			if new_man == False:
				#TODO: also try these ones
				print(u"Parlamentarul a fost și în alte legislaturi")
				prev_legislatures = extractLegislatures(parsed_html)
			parties = extractParties(parsed_html)
			wiki = u""
			name = parsed_html.find('div', attrs={'class':'boxTitle'}).h1.next.title()
			name = parliament.allCommas(name)
			page = pywikibot.Page(pywikibot.Site(), name)
			try:
				if page.exists():
					print(u"ro.wp are deja articol")
					wiki = page.title()
				print(name)
				print(extractBirthdate(parsed_html))
				text = name + u";" + extractBirthdate(parsed_html) + u";" + str(camera) + u";\'" + extractElectoralDistrict(parsed_html) + u"\';\'" + wiki + \
				u"\';\'" + str(om) + "\';\'" + str(parties) + "\';\'" + json.dumps(prev_legislatures) + "\'\n"
				print(text)
				f.write(text)
			except:
				continue
			count[camera] += 1
	f.close()

	print(u"Senatori: %d" % count[1])
	print(u"Deputați: %d" % count[2])

if __name__ == "__main__":
	#scrollThroughPages()

	ParliamentCsv("wikiro/robots/python/pwb/parliament/parliament_2024.csv")
	ElectionsCsv("wikiro/robots/python/pwb/parliament/alegeri.csv")
	MovesCsv("wikiro/robots/python/pwb/parliament/migrari.csv")
	DemisionsCsv("wikiro/robots/python/pwb/parliament/demisii.csv")
	
	print("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n")
	for person in people:
		print(person, flush=True)
		if people[person].wiki != u"":
			page = pywikibot.Page(pywikibot.Site(), people[person].wiki)
			while page.isRedirectPage():
				page = page.getRedirectTarget()
			orig = art = page.get()
			catl = people[person].generateCategoriesList()
			for cat in page.categories():
				if cat.title() in catl:
					catl.remove(cat.title())
			print(catl)
			if art.find(u"{{Infocaseta") > -1 or art.find(u"{{Infobox") > -1 or art.find(u"Cutie") > -1:
				print(u"Există deja o infocasetă în articol")
				#if len(catl) == 0:
				#	continue
			else:
				art = people[person].generateInfobox() + art
			index = art.rfind(u"Categorie:")
			index = art.find(u"]]", index) + 2
			if index == 1:
				index = len(art)
			for cat in catl:
				art += u"\n[[%s]]" % cat
		else:
			orig = ""
			art = people[person].generateArticle()
			page = pywikibot.Page(pywikibot.Site(), people[person].name)
			if page.exists():
				print(u"Există deja articolul %s" % page.title())
				art=orig # force skip

		if orig != art:
			pywikibot.showDiff(orig, art)
		
			answer = 'y'#pywikibot.input_choice(u"Upload page %s" % people[person].name, [('Yes','y'), ('No','n'), ('Rename','r')], 'n')
			if answer == 'y':
				try:
					page.put(art, "Completez articolul despre un parlamentar")
				except pywikibot.exceptions.PageSaveRelatedError as e:
					print("Eroare la salvarea paginii" + str(e))
					pass
			elif answer == 'r':
				newname = pywikibot.input(u"Insert new article name")
				page = pywikibot.Page(pywikibot.Site(), newname)
				try:
					if page.exists():
						print(u"Există deja articolul %s" % page.title())
						continue
					page.put(art, "Completez articolul despre un parlamentar")
				except pywikibot.exceptions.PageSaveRelatedError as e:
					print("Eroare la salvarea paginii" + str(e))
					pass

		repo = pywikibot.Site().data_repository()
		try:
			new_item = page.data_item()
			#continue
		except:
			print("Creare item Wikidata")
			new_item = pywikibot.ItemPage(repo)
			pass

		
		if "mul" not in new_item.labels:
			new_item.editLabels(labels={"mul": page.title()}, summary="Setting labels")
		if "ro" not in new_item.descriptions:
			description = "politician român"
			new_item.editDescriptions({'ro': description}, summary="Setting description")
		new_item.setSitelink(page, summary="Setting sitelink")

		if 'P39' not in new_item.claims:
			claim = pywikibot.Claim(repo, 'P39')
			if people[person].chamber == 1:
				claim.setTarget(pywikibot.ItemPage(repo, "Q17556530"))
			if people[person].chamber == 2:
				claim.setTarget(pywikibot.ItemPage(repo, "Q19938957"))
			new_item.addClaim(claim, summary="Add claim P39")
		if 'P31' not in new_item.claims:
			claim = pywikibot.Claim(repo, 'P31')
			claim.setTarget(pywikibot.ItemPage(repo, "Q5"))
			new_item.addClaim(claim, summary="Add claim P31")
		if 'P569' not in new_item.claims:
			try:
				print(people[person].birthdate)
				year, month, day = people[person].birthdate.split("-")
				claim = pywikibot.Claim(repo, 'P569')
				claim.setTarget(pywikibot.WbTime(year=int(year), month=int(month), day=int(day)))
				new_item.addClaim(claim, summary="Add claim P569")
			except Exception as e:
				print(f"Eroare la data nașterii {e}")
				pass

		print(new_item.getID())
