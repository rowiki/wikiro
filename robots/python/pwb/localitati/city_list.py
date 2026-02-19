#!/usr/bin/python
#-*- coding:utf-8 -*-

import pywikibot
import re
import sirutalib

from collections import OrderedDict
from pywikibot import pagegenerators
from pywikibot.data import sparql

import wikiro.robots.python.pwb.wikidata as wikidata

from wikiro.robots.python.pwb.localitati import config

def sparql_generator(query, site):
	repo = site.data_repository()
	dependencies = {'endpoint': None, 'entity_url': None, 'repo': repo}
	query_object = sparql.SparqlQuery(**dependencies)
	for elem in query_object.select(query):
		yield elem

def shortcut(long: str):
	return long[:3]

def url2title(url: str):
	return url[url.rfind('/')+1:].replace("%20","_")

def compute_level(title: str):
	return re.search(r'[^=]', title).start()

class CityListBuilder:
	def __init__(self, county: str=None, uat_only: bool=True, info: dict=None, site=True):
		self.county = county
		self.siruta = sirutalib.SirutaDatabase()
		self.uat_only = uat_only
		self.always = None
		self.level = 2
		self.change_description = "Actualizez lista de localități"
		self.section_prefix = "\n{{{{articol principal|Listă de localități din județul {county}}}}}\n\nJudețul este format din {tot} unități administrativ-teritoriale: {{{{subst:plural|{mun}|municipiu|municipii}}}}, {{{{subst:plural|{cit}|oraș|orașe}}}} și {{{{subst:plural|{com}|comună|comune}}}}."
		self.section_footer = ""
		self.full_page_prefix = "__NOEDITSECTION__\n__NOTOC__\n{{{{dezvoltă|Diviziuni administrative|Județul {county}}}}}"
		self.full_page_footer = "\n\n== Vezi și ==\n*[[Județele României]]\n{{{{Listele localităților din România pe județe}}}}\n[[Categorie:Liste de localități din România|{county}]]\n[[Categorie:Liste legate de județul {county}|Localități]]\n[[Categorie:Localități din județul {county}| Listă, Localități]]"

		self.info = info or OrderedDict({'imagine': 'image', 'drapel': 'flag', 'stemă': 'coa', 'populație': 'population', 'suprafață': 'area'})
		self.counts = [0, 0, 0, 0]

	def build_query(self):
		county_siruta = self.siruta.get_siruta_list(type_list=[40], name=self.county, add_prefix=True)
		if len(county_siruta) != 1:
			self.quit()
		county_siruta = county_siruta[0]
		self.query = "select distinct ?item ?instance_id ?itemLabel ?siruta ?page_title"
		for i in self.info:
			self.query += " (sample(?{shi}) as ?{shi})".format(shi=shortcut(i))
		self.query += "\nWHERE {\n" + " ?item wdt:P31 ?instance.\n" 
		self.query += " VALUES (?instance ?instance_id) { (wd:Q640364 1) (wd:Q16858213 2) (wd:Q659103 3) }\n"
		self.query += " ?item wdt:P131 ?county.\n ?county wdt:P843 '{county_siruta}'.\n".format(county_siruta=county_siruta)
		self.query += " ?item wdt:" + config['properties']['SIRUTA'][0] + " ?siruta.\n"
		for i in self.info:
			prop = config['properties'][i][0]
			self.query += "OPTIONAL {{?item wdt:{prop} ?{shi} }}\n".format(prop=prop, shi=shortcut(i))
		self.query += "OPTIONAL {?article    schema:about ?item ;\n"
		self.query += "     schema:isPartOf <https://ro.wikipedia.org/>;  schema:name ?page_title  . }\n"
		self.query += "SERVICE wikibase:label { bd:serviceParam wikibase:language \"ro\" } }\n" 
		self.query += "GROUP BY ?item ?itemLabel ?instance_id ?siruta ?page_title ORDER BY "
		#if self.uat_only:
		self.query += "?instance_id ?itemLabel"
		#else:
		#	self.query += "?itemLabel"
		print(self.query)

	def get_wiki_type(self, siruta_code: int) -> str:
		settlement_type = self.siruta.get_type(siruta_code)
		settlement_type_str = self.siruta.get_type_string(siruta_code)
		if settlement_type == 1:
			#wiki_type = "[[Municipiile României|" + settlement_type_str + "]]"
			wiki_type = settlement_type_str
		elif settlement_type == 4:
			wiki_type = "municipiu"
		elif settlement_type in [2, 5]:
			wiki_type = "oraș"
		elif settlement_type in [3]:
			wiki_type = "comună"
		elif settlement_type in [6]:
			#wiki_type = "[[Sectoarele Bucureștiului|" + settlement_type_str + "]]"
			wiki_type = settlement_type_str
		elif settlement_type in [9, 10, 17, 18]:
			wiki_type = "localitate componentă"
		elif settlement_type in [11, 19, 22, 23]:
			wiki_type = "sat"
		else:
			wiki_type = settlement_type_str
		return wiki_type

	def get_wiki_type_2(self, siruta_code: int) -> str:
		settlement_type = self.siruta.get_type(siruta_code)
		if settlement_type == 9:
			return "reședință de muncipiu"
		elif settlement_type == 17:
			return "reședință de oraș"
		elif settlement_type == 22:
			return "reședință de comună"
		elif settlement_type == 1:
			return "reședință de județ"
		else:
			return None

	def build_villages(self, qid: str, idx: int):
		repo = pywikibot.Site().data_repository()
		print(qid)
		prop = config['properties']['localități componente'][0]
		item = pywikibot.ItemPage(repo, qid)
		vidx = 0
		#preloading here to save API calls. Not interested in memory comsumption for now
		village_pages = list(pywikibot.pagegenerators.PreloadingEntityGenerator([claim.getTarget() for claim in item.claims.get(prop) if 'P582' not in claim.qualifiers]))
		village_pages.sort(key=lambda x:wikidata.get_labels(x))
		for village in village_pages:
			vidx += 1
			name = wikidata.get_labels(village) or "<Sat Necunoscut>"
			try:
				sitelink = village.getSitelink(pywikibot.Site())
				self.output += "|uat" + str(idx) + "settlement" + str(vidx) + "link = [[" + sitelink + "|" + name + "]]\n"
			except pywikibot.exceptions.NoPageError:
				self.output += "|uat" + str(idx) + "settlement" + str(vidx) + "link = [[:d:" + village.getID() + "|" + name + "]]\n"
			vprop = config['properties']['SIRUTA'][0]
			if vprop in village.claims:
					value = wikidata.find_best_claim(village.claims[vprop])
					siruta_code = int(value.getTarget())
					self.output += "|uat" + str(idx) + "settlement" + str(vidx) + "type1 = " + \
						self.get_wiki_type(siruta_code) + "\n"
					residence = self.get_wiki_type_2(siruta_code)
					if residence:
						self.output += "|uat" + str(idx) + "settlement" + str(vidx) + "type2 = " + \
							residence + "\n"
			for field in self.info:
				vprop = config['properties'][field][0]
				if vprop in village.claims:
					value = wikidata.find_best_claim(village.claims[vprop])
					value = value.getTarget()
					self.output += "|uat" + str(idx) + "settlement" + str(vidx) + self.info[field] + " = " + \
						wikidata.wbType_to_string(value, link=False) + "\n"

	def build(self):
		self.setup()
		idx = 0
		tag = "h{lvl}".format(lvl=1+self.level)
		self.output += "{{{{Header tabel localități|nume=Municipii și orașe|tag={}}}}}\n".format(tag)
		old_instance_id = 0
		fields = list(self.info)
		for data in self.generator:
			idx += 1
			if int(data['instance_id']) == 3 and old_instance_id != 3:
				self.output += "{{{{Header tabel localități|nume=Comune|tag={}}}}}\n".format(tag)
			old_instance_id = int(data['instance_id'])
			self.counts[old_instance_id] += 1
			self.counts[0] += 1 # total number of entries
			self.output += "{{Element listă localități\n"
			qid = url2title(data['item'])
			if data.get('page_title'):
				self.output += "|uat" + str(idx) + "link = [[" + data['page_title'] + "|" + data['itemLabel'] + "]]\n"
			else:
				self.output += "|uat" + str(idx) + "link = [[:d:" + qid + "|" + data['itemLabel'] + "]]\n"

			for field in fields:
				if data.get(shortcut(field)):
					value = data.get(shortcut(field))
					if value.find("http") == 0:
						value = url2title(value)
					self.output += "|uat" + str(idx) + self.info[field] + " = " + value + "\n"

			siruta_code = int(data['siruta'])
			self.output += "|uat" + str(idx) + "type1 = " + self.get_wiki_type(siruta_code) + "\n"

			if self.uat_only is False:
				self.build_villages(qid, idx)

			self.output += "}}\n"
		self.teardown()

	def setup(self):
		self.site = pywikibot.Site('wikidata', 'wikidata')
		self.build_query()
		self.generator = sparql_generator(self.query, self.site)
		if self.uat_only:
			what = "unitățile administrativ-teritoriale"
		else:
			what = "localitățile"
		self.output = "\nLista de mai jos conține " + what + " din județul " + self.county + ".\n\n"
		self.output += "<!--NU EDITAȚI ACEASTĂ LISTĂ. EA ESTE ACTUALIZATĂ AUTOMAT DE UN ROBOT FOLOSIND DATE DE LA WIKIDATA -->\n"
		self.output += "{{Început tabel localități}}\n"

	def teardown(self):
		self.output += "{{Sfârșit tabel localități}}\n"
		self.output += "<!--NU EDITAȚI ACEASTĂ LISTĂ. EA ESTE ACTUALIZATĂ AUTOMAT DE UN ROBOT FOLOSIND DATE DE LA WIKIDATA -->\n"

	def confirm(self, question: str) -> bool:
		return True
		answer = self.always or pywikibot.input_choice(question,  [("Yes", "Y"), ("No", "N"), ("Always", "A")])
		if answer == 'a':
			self.always = 'y'
			answer = 'y'
		if answer == 'y':
			return True
		return False

	def add_header_and_footer(self, full_page:  bool):
		if full_page:
			content = self.full_page_prefix.format(county=self.county)
			content += self.output
			content += self.full_page_footer.format(county=self.county)
		else:
			content = self.section_prefix.format(county=self.county, tot=self.counts[0], mun=self.counts[1], cit=self.counts[2], com=self.counts[3])
			content += self.output
			content += self.section_footer.format(county=self.county)
		return content

	def submit_full_page(self, page):
		bot.build()
		content = self.add_header_and_footer(True)
		#print(content)
		if self.confirm("Replace the whole content of the page {} with the text above?".format(page.title())):
			page.put(content, self.change_description)

	def submit_section(self, page, section):
		content = ""
		if page.exists():
			content = page.get()

		result = pywikibot.textlib.extract_sections(content, pywikibot.Site())
		start_section = None
		end_section = None
		for section_content in result.sections:
			if section_content.title.find(section) > -1:
				if start_section:
					print("Found 2 sections named", section, "in", page.title(), "please check the page content.")
					return
				start_section = section_content.title
				self.level = compute_level(section_content.title)
			elif start_section and self.level >= compute_level(section_content.title):
				end_section = section_content.title
				break
		if start_section is None:
			print("Section", section, "was not found in page", page.title())
			return

		bot.build()
		start_index = content.find(start_section) + len(start_section)
		end_index = -1
		if end_section:
			end_index = content.find(end_section)
			if end_index <= start_index:
				print("Coult not correctly identify section", section, "in page", page.title())
		old_text = content[start_index:end_index]
		new_text = self.add_header_and_footer(False)
		#pywikibot.showDiff(old_text, new_text)
		if self.confirm("Apply the diff above for page {}?".format(page.title())):
			content = content.replace(old_text, new_text)
			page.put(content, self.change_description)

	def publish(self, title=None, section=None):
		if title is None:
			print("No page title provided, can't save")
			return

		page = pywikibot.Page(pywikibot.Site(), title)

		if section == None:
			self.submit_full_page(page)
		else:
			self.submit_section(page, section)

if __name__ == "__main__":
	for county in config['counties']:
		bot = CityListBuilder(county, uat_only=False)
		bot.publish("Listă de localități din județul {county}".format(county=county))
		bot = CityListBuilder(county, uat_only=True)
		bot.publish("Județul {county}".format(county=county), "Diviziuni administrative")
	
