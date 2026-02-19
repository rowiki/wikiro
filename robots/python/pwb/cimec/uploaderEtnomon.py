#!/usr/bin/python3
# -*- coding: utf-8  -*-

import json
import os
import re
from pathlib import Path

import requests

import pywikibot
import uploader
import wikiro.robots.python.pwb.cimec as cimec
from uploader import LicenseLevel

author_cache = {}

class EtnomonUploader(uploader.CimecUploader):
	def __init__(self, site):
		super().__init__(site)
		self.always = False

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
		date = data.get("Datare", "")
		for year in re.finditer("(19|20)[0-9]{2}", date):
			if int(year.group(0)) > 1924:
				obj = LicenseLevel.fop
				break
		else:
			if date.find("XX") > -1 and (date.find("mij") > -1 or
										 date.find("jumătate") > -1):
				# mid-20th century, assume FOP
				obj = LicenseLevel.fop
		return obj, img

	def build_name(self, data):
		title = " - ".join([data.get("Denumirea în muzeu", ""),
							data.get("Muzeu", ""),
							data.get("key", "")]).replace("\"", "")
		extension = ".jpg"
		if data.get("Foto"):
			extension = data["Foto"][data["Foto"].rfind("."):]
		name = "File:" + title + extension
		return name

	def get_local_path(self, data):
		img_name = data["Foto"].split("/")[-1]
		img_path = os.path.join(".", f"cimec_{self.site}", img_name)
		return Path(img_path)

	def create_redirects(self, target, data):
		img_name = data["Foto"].split("/")[-1]
		img_name = "File:"+img_name
		text = "#redirect [[" + target.title() + "]]"
		page = pywikibot.Page(target.site, img_name)
		if page.exists():
			return
		page.put(text, "Redirecționare către imagine Cimec")

	def build_info_description(self, data):
		return data.get("Descriere", data.get("Denumirea în muzeu", ""))

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
		cats = f"[[Categorie:Exponate etnografice din {data.get('Muzeu', '')}]]"

		return cats

	def build_talk_page(self, page, data):
		url = cimec.config[self.site]['item_url'].format(data["key"])
		text = "{{DateCimec|1=%s|2=CC-BY-SA-4.0}}" % url
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

			# already uploaded, continue
			if "Foto" not in database[entry]:
				continue

			filename = self.build_name(database[entry])
			if self.check_exists(filename):
				database[entry]["Fișier"] = filename
				continue
			try:
				body = self.build_description_page(database[entry])
				database[entry]["Fișier"] = filename
				local_image = self.get_local_path(database[entry])
				if not self.download_file(database[entry], local_image):
					pywikibot.output("Could not download image for %s" % filename)
					continue
				print(filename)
				print(body)
			except Exception as e:
				pywikibot.output(e)
				continue
			answer = (self.always or
					  pywikibot.input_choice("Upload?",
											 [("Yes", "Y"), ("No", "N"), ("Always", "A")]))
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
				success = imagepage.upload(str(local_image),
						ignore_warnings=True,
						report_success=True,
						chunk_size=0,
						comment=f"Imagine Cimec {self.site} nouă")
			except pywikibot.exceptions.APIError as error:
				if error.code == 'uploaddisabled':
					pywikibot.error(
						'Upload error: Local file uploads are disabled on %s.'
						% self.site)
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
					self.dump_database(database)
				else:
					pywikibot.output('Upload aborted.')
		self.dump_database(database)

	def upload_images(self, filename):
		with open(filename, 'r') as f:
			db = json.loads(f.read())
			#wqimport pdb
			#pdb.set_trace()
			self.upload(db)

	def download_file(self, data, local_image):
		url = cimec.config[self.site]['home_url'] + data["Foto"].replace(
			"thumb", "medium")
		with requests.get(url, stream=True) as r:
			try:
				r.raise_for_status()
			except:
				return False
			with open(local_image, 'wb') as f:
				for chunk in r.iter_content(chunk_size=8192):
					f.write(chunk)
		return local_image.exists() and local_image.stat().st_size > 0


if __name__ == '__main__':
	#generate_categories()
	uploader = EtnomonUploader('etnomon')
	uploader.upload_images(f'{uploader.site}.json')
	#uploader.create_all_redirects('clasate.json')
	#uploader.fix_all_image_fields('clasate.json')
