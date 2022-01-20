#!/usr/bin/python3

import wikiro.robots.python.pwb.cimec as cimec
import json
import pywikibot

class LicenseLevel:
	pd     = 0x000
	pd_no  = 0x001
	pd_old = 0x002
	cc0    = 0x008
	ccby   = 0x010
	ccbysa = 0x020
	ccx    = 0x030
	ccby3  = 0x013
	ccbysa3= 0x023
	ccby4  = 0x014
	ccbysa4= 0x024
	fop    = 0x100
	nonfree= 0x800

	@staticmethod
	def get_wiki_tl(level):
		tls = {
			LicenseLevel.pd_old: "DP-70",
			LicenseLevel.pd_no: "DP-Ro",
			LicenseLevel.cc0: "CC0",
			LicenseLevel.ccby3: "Cc-by-3.0",
			LicenseLevel.ccbysa3: "Cc-by-sa-3.0",
			LicenseLevel.ccby4: "Cc-by-4.0",
			LicenseLevel.ccbysa4: "Cc-by-sa-4.0",
			LicenseLevel.fop: "FOP Romania",
			LicenseLevel.nonfree: "subst:IUCFD",
		}
		return tls.get(level)

class CimecUploader:
	def __init__(self, site):
		self.site = site
		self.wsite = pywikibot.Site()

	def check_exists(self, filename):
		page = pywikibot.FilePage(filename)
		return page.exists()

	def replace_diacritics(self, text):
		text = text.replace(u'ş', u'ș')
		text = text.replace(u'ţ', u'ț')
		text = text.replace(u'Ş', u'Ș')
		text = text.replace(u'Ţ', u'Ț')
		text = text.replace(u'ã', u'ă')
		return text

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
		return (LicenseLevel.fop, LicenseLevel.ccbysa4)

	def build_name(self, data):
		return "File:" + data['key'] + ".jpg"

	def create_redirects(self, data, target):
		pass

	def build_info_description(self, data):
		return ""

	def build_info_source(self, data):
		return "[{} Institutul Național al Patrimoniului - Cimec]" \
			.format(cimec.config[self.site]['home_url'])

	def build_info_date(self, data):
		return "necunoscut"

	def build_info_author(self, data):
		return "{{Autor necunoscut}}"

	def build_info_license(self, data):
		return "[[#Licențiere|Vezi mai jos]]"

	def build_license_section(self, data):
		text = ""
		obj, img = self.get_copyright_status(data)
		if obj == LicenseLevel.fop:
			text += "{{{{{}}}}}\n".format(LicenseLevel.get_wiki_tl(obj))
			text += "{{{{{}}}}}\n".format(LicenseLevel.get_wiki_tl(img))
		else: #should be PD-old
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
		return "[[Categorie:Imagini Cimec]]\n"

	def build_description_page(self, data):
		text = "== Descriere fișier ==\n"
		text += self.build_info_tl(data) + "\n"
		text += "\n== Licențiere ==\n"
		text += self.build_license_section(data) + "\n"
		text += self.build_categories(data)
		return text

	def upload(self, database):
		"""
		Upload images for the whole database
		"""
		for entry in database:
			print(self.build_description_page(database[entry]))
			exit(0)

	def upload_images(self, filename):
		with open(filename, 'r') as f:
			db = json.loads(f.read())
			self.upload(db)



if __name__ == '__main__':
	uploader = CimecUploader('clasate')
	uploader.upload_images('clasate.json')
