#!/usr/bin/python3

import wikiro.robots.python.pywikipedia.cimec as cimec
import json
import os
import pywikibot
from pywikibot.data import api
from pywikibot.page import ItemPage
import re
import time
import uploader
from uploader import LicenseLevel


class ClasateUploader(uploader.CimecUploader):
	def __init__(self, site):
		super().__init__(site)

	def find_author_data(self, author, offset = 0, filters={'P31':'Q5'}):
		#["neidentificat", "necunoscut", "N/A"]
		repo = pywikibot.Site('wikidata', 'wikidata')
		params = {'action': 'query',
			'list': 'search',
			'format': 'json',
			'srsearch': author,
			'sroffset': offset}
		request = api.Request(site=repo, **params)
		result = request.submit()
		result = [ItemPage(repo, r['title']) for r in result['query']['search']]
		for p in filters:
			result = [r for r in result if not r.isRedirectPage() and p in r.get()['claims']]
			result = [r for r in result if r.get()['claims'][p][0].target.id == filters[p]]
		print('Query', author)
		print('Result',result)
		if len(result) != 1:
			return None, None
		item = result[0]
		if 'P570' in item.get()['claims']:
			death = result[0].get()['claims']['P570'][0].getTarget()
			if death:
				print('Death', death.year)
				return item, death.year
		return item, None

	def filter_tentative_names(self, names):
		ret = []
		for name in names:
			for sep in ['(', ', zis ', ', numit ', ' zis ', ' numit ']:
				if name.find(sep) > 0:
					name = name[:name.find(sep)]
			if name.find(',') > 0:
				l = name.split(',')
				l.append(l[0])
				l = l[1:]
				name = " ".join(l)
			ret.append(name.strip())
		return ret

	def process_authors(self, data):	
		if not data.get("Autor"):
			return data
		#poor man's version to check if we're handling a person
		if data["Autor"][0].isupper():
			orig_authors = data["Autor"].split(';')
			authors = self.filter_tentative_names(orig_authors)
			i = 0
			final_auth = orig_authors

			for author in authors:
				if author.strip() == "":
					continue
				item, year = self.find_author_data(author)
				if year:
					data["Copyright"] = year
				if item:
					link = ":d:" + item.title()
					try:
						link = ":ro:" + item.getSitelink(pywikibot.Site('ro','wikipedia'))
					except pywikibot.NoPage:
						pass
					final_auth[i] = "[[" + link + "|" + orig_authors[i] + "]]"
				i += 1
			data["Autor"] = "; ".join(final_auth)

		return data


	def get_copyright_status(self, data):
		"""
		Gets the copyright status of the object itself and the image of
		the object. This is separate from the Cimec claims and is
		based on what we can determine from the dating and author.

		@param data The data we have on the current object
		@type data Dictionary

		@returns The copyright status of the object and image. Valid
			combinations include: (FOP, PD), (FOP, CCBYSA), (PD, PD),
			(PD, CCBYSA)
		@return type A pair of statuses (obj, img).
		"""
		obj = LicenseLevel.pd_old
		img = LicenseLevel.ccbysa4
		date = data.get("Datare")
		year = data.get("Copyright")
		if year and year + 70 >= int(time.strftime("%Y", time.localtime())):
			obj = LicenseLevel.fop
		elif date and (date.find("XX") > -1 or re.search("19[0-9][0-9]", date)):
			obj = LicenseLevel.fop

		if data["Domeniu"] in ["Documente", "Carte veche și manuscris"]:
			img = LicenseLevel.pd_no
		elif data["Domeniu"] == "Artă plastică":
			tip = data.get("Tip")
			if tip and (tip.find("pictură") > -1 or \
				tip.find("desen") > -1 or \
				tip.find("foto") > -1):
				img = LicenseLevel.pd_no
		return (obj, img)

	def build_name(self, data):
		title = data.get("Titlu") or data.get("Categorie")
		domain = "(" + data.get("Domeniu") + ")"
		name = "_".join([title, domain, \
			data["Clasare Ordin"], data["Clasare Dată"], 
			data["Clasare categorie"], data["key"]])
		name = name.replace("\r", "").replace("\n", " ")
		return "File:" + name + ".jpg"

	def get_local_path(self, data):
		img_name = data["Foto"].split("/")[-1]
		img_path = os.path.join("AIRM", img_name)
		if os.path.exists(img_path):
			return img_path
		else:
			return None

	def create_redirects(self, target, data):
		img_name = data["Foto"].split("/")[-1]
		img_name = "File:"+img_name
		text = "#redirect [[" + target.title() + "]]"
		page = pywikibot.Page(wsite, img_name)
		page.put(text, "Redirecționare către imagine Cimec")

	def build_info_description(self, data):
		desc = ""
		if "Descriere" in data:
			desc = data["Descriere"].replace("\n","\n\n")
		else:
			for key in data.keys():
				if key.find("Descriere") > -1 or \
				   key.find("Legend") > -1:
					desc += "<br/>'''{}:''' {}\n".format(key, data[key])
		if desc == "" and "Titlu" in data:
			desc = data["Titlu"]
			
		return "{{ro|" + desc + "}}"

	def build_info_source(self, data):
		url = cimec.config[self.site]['item_url'].format(data["key"])
		title = data.get("Titlu") or "Institutul Național al Patrimoniului - Cimec"
		return "[{} {}]" \
			.format(url, title)

	def build_info_date(self, data):
		return data.get("Datare") or ""

	def build_info_author(self, data):
		author = data.get("Autor") or ""
		if author != "":
			author += "<br/>"
		if data.get("Fișă întocmită de"):
			author += "''Fișă:'' " + data.get("Fișă întocmită de")
		if author == "":
			author = "{{Autor necunoscut}}"

		return author

	def build_info_license(self, data):
		return "[[#Licențiere|Vezi mai jos]]"

	def build_license_section(self, data):
		text = ""
		obj, img = self.get_copyright_status(data)
		if obj == LicenseLevel.fop:
			text += "{{{{{}}}}}\n".format(LicenseLevel.get_wiki_tl(obj))
			text += "{{{{{}}}}}\n".format(LicenseLevel.get_wiki_tl(img))
		else: #obj should be PD-old
			text += "{{{{{}}}}}\n".format(LicenseLevel.get_wiki_tl(img))
			text += "{{tlc}}\n"
		return text
			

	def build_info_tl(self, data):
		return """{{{{Informații
|  Descriere = {}
|      Sursa = {}
|       Data = {}
|      Autor = {}
| Permisiune = {}
}}}}""".format(self.build_info_description(data),
	       self.build_info_source(data),
	       self.build_info_date(data),
	       self.build_info_author(data),
	       self.build_info_license(data))

	def build_categories(self, data):
		cats = ""
		if data.get("Judet"):
			county = "[[Categorie:Bunuri mobile clasate în patrimoniul național al României aflate în "
			if data.get("Judet") == "București":
				county += "municipiul București"
			else:
				county += "județul " + data.get("Judet")
			county += "]]\n"
			cats += county
		if data.get("Domeniu"):
			domain = "[[Categorie:Bunuri mobile din domeniul {} clasate în patrimoniul național al României]]\n".format(data.get("Domeniu").lower())
			cats += domain
		if cats == "":
			cats = "[[Categorie:Bunuri mobile clasate în patrimoniul național al României]]"

		return cats

	def build_description_page(self, data):
		text = "== Descriere fișier ==\n"
		text += self.build_info_tl(data) + "\n"
		text += "\n== Licențiere ==\n"
		text += self.build_license_section(data) + "\n"
		text += self.build_categories(data)
		return text

	def build_talk_page(self, page, data):
		url = cimec.config[self.site]['item_url'].format(data["key"])
		text = "{{DateCimec|%s|CC-BY-SA-4.0}}" % url
		if "Copyright" in data:
			text += "{{imagine liberă în viitor|an = %d|autor = %s}}" % (data["Copyright"], data["Autor"] or "")
		page = page.toggleTalkPage()
		page.put(text, "Completez formate pentru pagina de discuții")


	def upload(self, database):
		"""
		Upload images for the whole database
		"""
		for entry in database:
			#if database[entry]["key"] != "638D1E677C3D42BD9ACEA527B4FD2481":
			#	continue
			f = self.get_local_path(database[entry])
			if f == None:
				continue
			database[entry] = self.process_authors(database[entry])
			filename = self.build_name(database[entry])
			body = self.build_description_page(database[entry])
			print(filename)
			print(body)
			#continue
			answer = pywikibot.input_choice("Upload?", [("Yes", "Y"), ("No", "N")])
			if answer != 'y':
				continue
			imagepage = pywikibot.FilePage(pywikibot.Site(), filename)  # normalizes filename
			imagepage.text = body

			pywikibot.output('Uploading file to {0}...'.format(pywikibot.Site()))
			try:
				success = imagepage.upload(f,
						ignore_warnings=False,
                                   		chunk_size=0,
                                       		_file_key=None, _offset=0,
                                       		comment="Imagine Cimec nouă")
			except pywikibot.data.api.APIError as error:
				if error.code == 'uploaddisabled':
					pywikibot.error(
						'Upload error: Local file uploads are disabled on %s.'
						% site)
				else:
					pywikibot.error('Upload error: ', exc_info=True)
					continue
			except Exception:
				pywikibot.error('Upload error: ', exc_info=True)
				pywikibot.output("\n")
			else:
				if success:
					# No warning, upload complete.
					pywikibot.output('Upload of %s successful.' % filename)
					database[entry]["Fișier"] = filename
					self.create_redirects(imagepage, data)
					self.build_talk_page(imagepage, data)
					with open("clasate2.json", 'w') as f:
						json.dump(database, f, indent=2)
				else:
					pywikibot.output('Upload aborted.')

	def upload_images(self, filename):
		with open(filename, 'r') as f:
			db = json.loads(f.read())
			self.upload(db)

def generate_categories():
	counties = {
"B": u"municipiul București",
"AB": u"județul Alba",
"AR": u"județul Arad",
"AG": u"județul Argeș",
"BC": u"județul Bacău",
"BH": u"județul Bihor",
"BN": u"județul Bistrița-Năsăud",
"BT": u"județul Botoșani",
"BV": u"județul Brașov",
"BR": u"județul Brăila",
"BZ": u"județul Buzău",
"CS": u"județul Caraș-Severin",
"CL": u"județul Călărași",
"CJ": u"județul Cluj",
"CT": u"județul Constanța",
"CV": u"județul Covasna",
"DB": u"județul Dâmbovița",
"DJ": u"județul Dolj",
"GL": u"județul Galați",
"GR": u"județul Giurgiu",
"GJ": u"județul Gorj",
"HR": u"județul Harghita",
"HD": u"județul Hunedoara",
"IL": u"județul Ialomița",
"IS": u"județul Iași",
"IF": u"județul Ilfov",
"MM": u"județul Maramureș",
"MH": u"județul Mehedinți",
"MS": u"județul Mureș",
"NT": u"județul Neamț",
"OT": u"județul Olt",
"PH": u"județul Prahova",
"SM": u"județul Satu Mare",
"SJ": u"județul Sălaj",
"SB": u"județul Sibiu",
"SV": u"județul Suceava",
"TR": u"județul Teleorman",
"TM": u"județul Timiș",
"TL": u"județul Tulcea",
"VS": u"județul Vaslui",
"VL": u"județul Vâlcea",
"VN": u"județul Vrancea",
}

	domains = [
	"arheologie",
	"artă decorativă",
	"artă plastică",
	"carte veche şi manuscris",
	"documente",
	"etnografie",
	"istorie",
	"medalistică",
	"numismatică",
	"știinţă şi tehnică",
	"știinţele naturii"
	]

	for short, county in counties.items():
		cat = "Categorie:Bunuri mobile clasate în patrimoniul național al României aflate în " + county
		contents = "[[Categorie:Bunuri mobile clasate în patrimoniul național al României|{}]]".format(short)
		page = pywikibot.Page(wsite, cat)
		page.put(contents, "Categorie pentru bunuri clasate")

	for domain in domains:
		cat = "Categorie:Bunuri mobile din domeniul {} clasate în patrimoniul național al României".format(domain)
		contents = "[[Categorie:Bunuri mobile clasate în patrimoniul național al României|{}]]".format(domain)
		page = pywikibot.Page(wsite, cat)
		page.put(contents, "Categorie pentru bunuri clasate")


if __name__ == '__main__':
	#generate_categories()
	uploader = ClasateUploader('clasate')
	uploader.upload_images('clasate.json')