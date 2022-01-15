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

author_cache = {}

class ClasateUploader(uploader.CimecUploader):
	def __init__(self, site):
		super().__init__(site)
		self.always = False

	def find_author_data(self, author, offset = 0, filters={'P31':'Q5'}):
		#["neidentificat", "necunoscut", "N/A"]
		if author in author_cache:
			item = author_cache[author]
			if not item:
				return None, None
		else:
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
				author_cache[author] = None
				return None, None
			item = result[0]
			author_cache[author] = item

		claims = item.get()['claims']
		if 'P570' in claims:
			death = claims['P570'][0].getTarget()
			if death:
				print('Death', death.year)
				return item, death.year
		elif 'P7763' in claims:
			copystatus = claims['P570'][0].getTarget()
			if copystatus.id == '71887839':
				return item, 'no'
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
		if not data.get("Autor") or data["Autor"].find("[[") > -1:
			#print(data)
			return data
		#poor man's version to check if we're handling a person
		if data["Autor"][0].isupper():
			orig_authors = data["Autor"].split(';')
			authors = self.filter_tentative_names(orig_authors)
			i = 0
			final_auth = [x.strip() for x in orig_authors]

			print(authors)
			for author in authors:
				author = author.strip()
				if author == "":
					continue
				item, year = self.find_author_data(author)
				if year:
					data["Copyright"] = year
				if item:
					link = ":d:" + item.title()
					try:
						link = ":ro:" + item.getSitelink(pywikibot.Site('ro','wikipedia'))
					except pywikibot.exceptions.NoPageError:
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
		if type(year) == 'string' and year.lower() in ['no', 'nu']:
			year = 0
		if (year and year + 70 >= int(time.strftime("%Y", time.localtime()))):
			obj = LicenseLevel.fop
		elif (not year and date and (date.find("XX") > -1 or re.search("19[0-9][0-9]", date))):
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
		title = title.replace("\r", "").replace("\n", " ").replace("\t", " ")
		domain = "(" + data.get("Domeniu") + ")"
		name = "_".join([title, domain, \
			data["Clasare Ordin"], data["Clasare Dată"], 
			data["Clasare categorie"], data["key"]])
		if len(name) > 210:
			diff = len(name) - 210
			new_title = title[:-diff] + "(...)"
			name = name.replace(title, new_title)
		name = name.replace("1/2", "jumătate de")
		name = name.replace("/", "-")
		name = name.replace("[", "(")
		name = name.replace("]", ")")
		name = name.replace(":", "")
		extension = ".jpg"
		if data.get("Foto"):
			extension = data["Foto"][data["Foto"].rfind("."):]
		name = "File:" + name + extension
		return name

	def get_local_path(self, data):
		img_name = data["Foto"].split("/")[-1]
		#img_path = os.path.join("..", "Desktop", "clasate.cimec", img_name)
		img_path = os.path.join(".", "clasate_cimec", img_name)
		if os.path.exists(img_path):
			return img_path
		else:
			return None

	def create_redirects(self, target, data):
		img_name = data["Foto"].split("/")[-1]
		img_name = "File:"+img_name
		text = "#redirect [[" + target.title() + "]]"
		page = pywikibot.Page(target.site, img_name)
		if page.exists():
			return
		page.put(text, "Redirecționare către imagine Cimec")

	def build_info_description(self, data):
		desc = ""
		if "Descriere" in data:
			desc = data["Descriere"].replace("\n","\n\n")
		else:
			for key in sorted(data.keys()):
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
			if img & LicenseLevel.ccx > 0:
				text += "{{{{{}}}}}\n".format(LicenseLevel.get_wiki_tl(img))
			else:
				text += "{{{{{}}}}}\n".format(LicenseLevel.get_wiki_tl(obj))
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
		return text.strip().replace("\r\n","\n")

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
			for key in database[entry]:
				if type(database[entry][key]) != str:
					continue
				database[entry][key] = self.replace_diacritics(database[entry][key])

			database[entry] = self.process_authors(database[entry])
			# already uploaded, continue
			if "Fișier" in database[entry]:
				if database[entry]["Domeniu"] in ["Numismatică", "Medalistică"]:
					continue
				print(database[entry]["Fișier"])
				try:
					body = self.build_description_page(database[entry])
					page = pywikibot.Page(pywikibot.Site(), database[entry]["Fișier"])
					if body == page.get():
						continue
					pywikibot.showDiff(page.get(), body)
					if page.get().find("{{Cc-by-4.0}}") > -1:
						answer = 'y'
					elif page.get().find("{{DP-Ro}}") > -1 and body.find("{{DP-70}}") > -1:
						answer = 'y'
					else:
						answer = self.always or \
							pywikibot.input_choice("Upload?", [("Yes", "Y"), ("No", "N"), ("Always", "A")])
					if answer == 'a':
						self.always = 'y'
						answer = 'y'
					if answer != 'y':
						continue
					page.put(body, "Actualizez descrierea imaginii ")
				except pywikibot.exceptions.NoPageError:
					del database[entry]["Fișier"]
				except pywikibot.exceptions.IsRedirectPageError:
					continue # Likely a duplicate, don't bother
				continue
			# no picture, continue
			f = self.get_local_path(database[entry])
			if f == None:
				continue
			filename = self.build_name(database[entry])
			body = self.build_description_page(database[entry])
			pywikibot.output(filename)
			pywikibot.output(body)
			#continue

			if filename[5:9].lower() in ["foto"]:
				ignore = ['bad-prefix']
				pywikibot.output("Ignoring invalid prefix for " + filename[:4])
			else:
				ignore = False

			answer = self.always or pywikibot.input_choice("Upload?", [("Yes", "Y"), ("No", "N"), ("Always", "A")])
			if answer == 'a':
				self.always = 'y'
				answer = 'y'
			if answer != 'y':
				continue
			try:
				imagepage = pywikibot.FilePage(pywikibot.Site(), filename)  # normalizes filename
				imagepage.text = body
			except pywikibot.exceptions.InvalidTitleError as e:
				#TODO: handle
				pywikibot.output(e)
				continue

			pywikibot.output('Uploading file to {0}...'.format(pywikibot.Site()))
			try:
				success = imagepage.upload(f,
						ignore_warnings=ignore,
						report_success=True,
                                   		chunk_size=0,
                                       		_file_key=None, _offset=0,
                                       		comment="Imagine Cimec nouă")
			except pywikibot.exceptions.APIError as error:
				if error.code == 'uploaddisabled':
					pywikibot.error(
						'Upload error: Local file uploads are disabled on %s.'
						% site)
				elif error.code in ['exists', 'fileexists-shared-forbidden']:
					database[entry]["Fișier"] = filename
					pywikibot.output("File %s already exists, ignoring" % filename)
					continue
				elif error.code == 'duplicate':
					pywikibot.output(error.message)
					target = 'File:' + error.message[error.message.find('[') + 2:error.message.find(']') - 1]
					print(target)
					page = pywikibot.Page(pywikibot.Site(), filename)
					page.put("#redirect [[%s]]" % target, "Redirecționez spre imaginea duplicat")
					database[entry]["Fișier"] = filename
					continue
				elif error.code == 'verification-error':
					if error.info.find("does not match the detected MIME") == -1:
						pywikibot.error('Upload error: ', exc_info=True)
						continue
					extension = error.info[error.info.rfind('/')+1:error.info.rfind(')')]
					f2 = f + "." + extension
					print("Moving file for subsequent run from", f, "to", f2)
					os.rename(f, f2)
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
					self.build_talk_page(imagepage, database[entry])
					self.create_redirects(imagepage, database[entry])
					with open("clasate.json", 'w') as f:
						json.dump(database, f, indent=2)
				else:
					pywikibot.output('Upload aborted.')
		with open("clasate.json", 'w') as f:
			json.dump(database, f, indent=2)

	def upload_images(self, filename):
		with open(filename, 'r', encoding="utf-8") as f:
			db = json.loads(f.read())
			#wqimport pdb
			#pdb.set_trace()
			self.upload(db)

	def create_all_redirects(self, filename):
		with open(filename, 'r', encoding="utf-8") as f:
			db = json.loads(f.read())
			count = 0
			for entry in db:
				count += 1
				if count % 100 == 0:
					print("Count", count)
				site = pywikibot.Site()
				if not db[entry].get("Fișier"):
					continue
				if not db[entry].get("Foto") or db[entry]["Foto"].find("placeholder") > -1:
					continue

				img_name = db[entry]["Foto"].split("/")[-1]
				img_name = "File:"+img_name
				page = pywikibot.Page(site, img_name)
				if page.exists():
					continue
				imagepage = pywikibot.FilePage(site, db[entry]["Fișier"])
				while imagepage.isRedirectPage():
					imagepage = imagepage.getRedirectTarget()
				if not imagepage.file_is_shared():
					continue
				if True:#imagepage.file_is_shared():
					site = pywikibot.Site('commons', 'commons')
					imagepage = pywikibot.FilePage(site, db[entry]["Fișier"])
					#print("commons:", db[entry]["Foto"], imagepage.title())
				while imagepage.isRedirectPage():
					imagepage = imagepage.getRedirectTarget()
				try:
					self.create_redirects(imagepage, db[entry])
				except Exception:
					print(db[entry]['Fișier'])
					continue

	def fix_all_image_fields(self, filename):
		with open(filename, 'r', encoding="utf-8") as f:
			db = json.loads(f.read())
		count = 0
		for entry in db.copy():
			count += 1
			if count % 1000 == 0:
				print("Count", count)
			data = db[entry]
			if data.get("Fișier"):
				continue
			img_name = data["Foto"].split("/")[-1]
			img_name = "File:"+img_name
			if img_name.find("placeholder") > -1:
				continue
			imagepage = pywikibot.FilePage(pywikibot.Site(), img_name)
			if imagepage.exists() and imagepage.isRedirectPage():
				imagepage = imagepage.getRedirectTarget()
				db[entry]["Fișier"] = imagepage.title()
				print(db[entry]["Fișier"])
		
		with open("2" + filename, 'w') as f:
			json.dump(db, f, indent=2)


def generate_categories():
	
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
	#uploader.create_all_redirects('clasate.json')
	#uploader.fix_all_image_fields('clasate.json')
