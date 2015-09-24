#!/usr/bin/python
# -*- coding: utf-8 -*-

from BeautifulSoup import BeautifulSoup
import requests
import re
import codecs
import csv
import sys

sys.path.append(".")
import strainu_functions
import parliament
from parliament import group, person
import pywikibot


birthdate_regex = r"n\.\s+([0-9]{1,2})\s+([a-z\.]{3,4})\s+([0-9]{4})"

people = {
}

invalid_group = u"Deputaţi neafiliaţi"

def extractElectoralDistrict(bf):
	res = bf.find('div', attrs={'class':'boxStiri'})
	txt = res.findAll('div', attrs={'class':'boxDep clearfix'})[0].p.text
	match = re.search(ur"circumscripţia electorală nr\.([0-9]+)([A-ZȘȚÂĂÎŞŢ]*,)((.|\s)*?[0-9]+)", txt)
	if match:
		print match.groups()
		return parliament.allCommas(u" ".join([match.group(1), match.group(2).title(), match.group(3).strip()]))
	else:
		return u""

def extractParties(bf):
	pass

def extractCommisions(bf):
	pass

def extractBirthdate(bf):
	match = re.search(birthdate_regex, bf.find('div', attrs={'class':'profile-pic-dep'}).text)
	if match:
		return u"-".join([match.group(3), parliament.months[match.group(2)], "%02d" % int(match.group(1))])
	else:
		return u""

def ParliamentCsv(csvName):
	with open(csvName, "r") as f:
		reader = strainu_functions.unicode_csv_reader(f, delimiter=';')
		for row in reader:
			name = row[0]
			people[name] = parliament.person.ElectedPerson()
			people[name].name = name
			people[name].birthdate = row[1]
			people[name].chamber = int(row[2])
			people[name].district = row[3]
			people[name].wiki = row[4]

def ElectionsCsv(csvName):
	with open(csvName, "r") as f:
		reader = strainu_functions.unicode_csv_reader(f)
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
		reader = strainu_functions.unicode_csv_reader(f)
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
		reader = strainu_functions.unicode_csv_reader(f)
		for row in reader:
			name = row[0]
			if name not in people:
				continue
			#TODO: check group name
			people[name].endLastGroup(row[3])

def scrollThroughPages():
	count = [0,0,0]
	f = codecs.open("parliament.csv", "w+", "utf8")
	for camera in [1,2]:
		for om in range(1,500):
			url = 'http://www.cdep.ro/pls/parlam/structura2015.mp?idm=%d&leg=2012&cam=%d&idl=1' % (om, camera)
			print url
			r = requests.get(url)
			#print r.status_code
	
			html = r.text
			parsed_html = BeautifulSoup(html)
			if parsed_html.find('div', attrs={'class':'profile-pic-dep'}).img['src'] == u"/img/judete/judet.jpg":
				print u"Camera %d s-a terminat" % camera
				break
			new_man = len(parsed_html.findAll('div', text=u"Alte legislaturi")) == 0
			if new_man == False:
				#TODO: also try these ones
				print u"Parlamentarul a fost și în alte legislaturi"
				continue
			wiki = u""
			name = parliament.allCommas(parsed_html.find('div', attrs={'class':'boxTitle'}).h1.text.title())
			page = pywikibot.Page(pywikibot.getSite(), name)
			if page.exists():
				print u"ro.wp are deja articol"
				wiki = page.title()
			print name
			print extractBirthdate(parsed_html)
			f.write(name + u";" + extractBirthdate(parsed_html) + u";" + str(camera) + u";\"" + extractElectoralDistrict(parsed_html) + u"\";\"" + wiki + u"\"\n")
			count[camera] += 1
	f.close()

	print u"Senatori: %d" % count[1]
	print u"Deputați: %d" % count[2]

if __name__ == "__main__":
	#scrollThroughPages()
	ParliamentCsv("parliament/parliament.csv")
	ElectionsCsv("parliament/alegeri.csv")
	MovesCsv("parliament/migrari.csv")
	DemisionsCsv("parliament/demisii.csv")
	
	print "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n"
	for person in people:
		if people[person].chamber == 1:
			continue
		print people[person].generateArticle()
		print u"----"
		pass
