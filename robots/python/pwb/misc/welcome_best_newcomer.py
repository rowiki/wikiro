#!/usr/bin/python
# -*- coding: utf-8 -*-

import pymysql
import os
import time
import requests

import pywikibot

host = os.environ['MYSQL_HOST']
user = os.environ['MYSQL_USERNAME']
password = os.environ['MYSQL_PASSWORD']
romonths = {
1: 'ianuarie',
2: 'februarie',
3: 'martie',
4: 'aprilie',
5: 'mai',
6: 'iunie',
7: 'iulie',
8: 'august',
9: 'septembrie',
10: 'octombrie',
11: 'noiembrie',
12: 'decembrie',
}

conn = pymysql.connect(host, user, password)
site = pywikibot.getSite()
def getList(year = int(time.strftime("%Y")), month = int(time.strftime("%m"))):
	with conn.cursor() as cur:
		cur.execute('use rowiki_p')
		sql = """
		SELECT user_name, user_editcount, user_registration FROM user
		WHERE user_registration >= '%4d%02d01000000' AND user_registration < '%4d%02d01000000'
		AND user_name NOT LIKE '%%Bot' AND user_name NOT LIKE '%%bot'
		GROUP BY user_name
		ORDER BY user_editcount DESC
		LIMIT 10
		""" % (year, month-1, year, month)
		print(sql)
		cur.execute(sql)
		return cur.fetchall()

def postToVillage(params):
	text = """
== {user} este cel mai prolific nou venit din {month} {year}==
[[File:Main Barnstar Hires.png|frameless|150px|right]]
'''{{{{ut|{user}}}}}''' este utilizatorul înregistrat luna trecută care a făcut cele mai multe editări (indiferent de calitatea lor). L-am invitat să se prezinte aici și să ne spună cum îl putem ajuta să contribuie în mod constructiv pe Wikipedia. [[Utilizator:Strainu|]] ~~~~~""".format(**params)
	print(text)
	page = pywikibot.Page(site, params['village'])
	ptext = page.get()
	ptext += text
	page.put(ptext)

	try:
		url = "https://ro.wikipedia.org/w/api.php?action=parse&prop=sections&page={village}&format=json".format(**params)
		print(url)
		r = requests.get(url)
		return r.json()["parse"]["sections"][-1]["index"]
	except Exception as e:
		print(e)
		return -1

def postToUser(params):
	text = """
== Cel mai activ nou venit din {month} {year}==
[[File:Main Barnstar Hires.png|frameless|150px|left]]
Bună ziua. Am observat că sunteți utilizatorul înregistrat luna trecută care realizat cele mai multe editări pe Wikipedia și dorim să vă acordăm acest premiu simbolic pentru activitatea de până acum. Am dori să cunoaștem mai bine noii utilizatori, așa că am pregătit [{{{{fullurl:{village}|section={section}&action=edit}}}} o secțiune specială] la Cafenea unde vă invităm să ne spuneți câte ceva despre dumneavoastră și despre cum vă putem ajuta să contribuiți mai ușor la Wikipedia. Dacă aveți orice fel de întrebări despre Wikipedia și editorii ei, le puteți de asemenea pune în pagina respectivă. [[Utilizator:Strainu|]] ~~~~~""".format(**params)
	print(text)
	page = pywikibot.Page(site, params['userpage'])
	ptext = page.get()
	ptext += text
	page.put(ptext)

if __name__ == "__main__":
	year = int(time.strftime("%Y"))
	month = int(time.strftime("%m"))
	results = getList(year, month)
	if len(results):
		params = {'year': year, 'month': romonths[month-1], 'user': results[0][0].decode('utf-8'),
		'village': "Wikipedia:Cafenea", 'userpage': "Discuție Utilizator:" + results[0][0].decode('utf-8')}
		print(params)
		params['section'] = postToVillage(params)
		postToUser(params)

