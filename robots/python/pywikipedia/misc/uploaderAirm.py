#!/usr/bin/python3

import json
import os
import pywikibot
import re
import time
import wikiro.robots.python.pywikipedia.cimec.uploader as uploader

def custom_sort(filename):
	print(filename)
	if filename.find("photo_") != 0:
		return filename
	skip = len("photo_")
	return int(filename[skip:-4])

months = ['ianuarie', 'februarie', 'martie', 'aprilie', 'mai', 'iunie', 'iulie',
	'august', 'septembrie', 'octombrie', 'noiembrie', 'decembrie']
class LicenseLevel:
	pd_old = 0x000
	pd_no  = 0x001
	cc0    = 0x008
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
			LicenseLevel.ccby3: "Cc-by-sa-3.0",
			LicenseLevel.ccbysa3: "Cc-by-3.0",
			LicenseLevel.ccby4: "Cc-by-sa-4.0",
			LicenseLevel.ccbysa4: "Cc-by-4.0",
			LicenseLevel.fop: "FOP Romania",
			LicenseLevel.nonfree: "subst:IUCFD",
		}
		return tls.get(level)

class AIRMUploader:
	def __init__(self, site):
		self.site = site
		self.wsite = pywikibot.Site()

	def check_exists(self, filename):
		page = pywikibot.FilePage(filename)
		return page.exists()

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

	def build_name(self, data, i, fbid):
		y,m,d = data["img"][i]
		#y,m,d = data["img"]
		# yyyy-mm or yyyy-mm-dd
		if m != '':
			y = y[:4]
			date = months[int(m)-1] + " " + y
		elif y[0].isdigit():
			date = y
		elif y.find("anii") == 0:
			date = y.lower()
		elif y.find("NA") == 0:
			date = "dată necunoscută"
		else:
			pywikibot.output("Invalid parsed date %s" % data["img"][i])
			exit(9)
		name = " - ".join([data["nume"], date, fbid])
		return "File:AIRM - " + name + ".jpg"

	def create_redirects(self, data, target):
		pass

	def build_info_description(self, data):
		return "{{ro|[[:ro:%s|%s]]}} {{Monument ocrotit de stat|%s}}" % (data["nume"], data["nume"], data["cod"])
		#return "{{en|%s}} {{Monument ocrotit de stat|%s}}" % ( data["nume"], data["cod"])

	def build_info_source(self, data):
		return data["fb"]

	def build_info_date(self, data, idx):
		y,m,d = data["img"][idx]
		#y,m,d = data["img"]
		print(y,m,d, y[0].isdigit())
		# yyyy-mm or yyyy-mm-dd
		if y[0].isdigit():
			return y
		elif y.find("anii") == 0:
			return "{{Other date|s|%s}}" % y[5:]
		elif y.find("NA") == 0:
			return "{{Other date|?}}"
		else:
			return "{{Upload date|2021-09-20}}"

	def build_info_author(self, data):
		return "[https://www.facebook.com/Agentia-de-Inspectare-si-Restaurare-a-Monumentelor-din-Republica-Moldova-212165758808185/ Agenția de Inspectare și Restaurare a Monumentelor din Republica Moldova]"

	def build_info_license(self, data):
		return ""

	def build_license_section(self, data):
		text = "{{cc-by-sa-4.0}}\n{{LicenseReview}}\n{{FoP-Moldova}}"
		return text
			

	def build_info_tl(self, data, idx):
		return """{{{{Information
| description = {}
| source = {}
| date = {}
| author = {}
| permission = {}
}}}}""".format(self.build_info_description(data),
	       self.build_info_source(data),
	       self.build_info_date(data, idx),
	       self.build_info_author(data),
	       self.build_info_license(data))

	def build_categories(self, data):
		ret = "[[Category:" + data["cat"] + "]]\n"
		ret += "[[Category:Files from Agency for Inspection and Restoration of Monuments of Moldova]]\n"
		return ret

	def build_description_page(self, data, idx):
		text = "== {{int:filedesc}} ==\n"
		text += self.build_info_tl(data, idx) + "\n"
		text += "\n== {{int:license-header}} ==\n"
		text += self.build_license_section(data) + "\n"
		text += self.build_categories(data)
		return text

	def parse_page(self, pname):
		data = []
		page = pywikibot.Page(pywikibot.Site(), title="User:Strainu/AIRM/" + pname)
		text = page.get()
		entries = text.split("|-")
		idx = 0
		for entry in entries:
			e = {}
			idx = idx + 1
			if idx == 1:
				continue #HEADER
			e["idx"] = "{idx:02d}".format(idx=idx-1)
			e["album"] = pname

			fb = re.search("<code>([0-9]+)</code><br\s?/>(\[.*\]\))", entry)
			if not fb:
				exit(1)
			e["album"] = fb.group(1)
			e["fb"] = fb.group(2)

			code = re.search("<code>(MD-.+)</code>", entry)
			if not code:
				print(entry)
				exit(2)
			e["cod"] = code.group(1)

			cat = re.search("\[\[\:c\:Category:(.*)\|", entry)
			if not cat:
				import pdb
				pdb.set_trace()
				exit(3)
			e["cat"] = cat.group(1)

			#name = re.search("\[\[\:ro\:(.*)\|", entry)
			name = re.search("\[\[\:c\:Category:(.*)\|", entry)
			if not name:
				exit(4)
			e["nume"] = name.group(1)

			e["imgno"] = 0
			e["img"] = {}
			dates = re.findall("(NA|anii [0-9]{4}|[0-9]{4}-?([0-9]{2})?-?([0-9]{2})?)\s\(([0-9]{1,3})\)", 
						entry)
			for date, month, day, count in dates:
				for i in range(e["imgno"], e["imgno"] + int(count)):
					e["img"][i+1] = [date, month, day]
				e["imgno"] += int(count)

			data.append(e)
		print(data)
		return data


	def upload(self):
		"""
		Upload images for the whole database
		"""
		#pages = [u"Chișinău", u"Mansions", u"Churches", u"Miscellaneous"]
		pages = [u"Miscellaneous"]
		for entry in pages:
			upload_all = False
			lst = self.parse_page(entry)
			foto_dir = os.path.join(".", "AIRM", entry)
			pywikibot.output(foto_dir)
			if not os.path.exists(foto_dir):
				exit(5)
			all_folders = []
			for (root,dirs,files) in os.walk(foto_dir):
				all_folders += dirs
			#pywikibot.output(all_folders)

			for e in lst:
				folder_name = e["cod"] + " album_" + e["album"]
				folder_name = os.path.join(foto_dir, folder_name)
				if not os.path.exists(folder_name):
					exit(6)

				idx = 0
				fls = len([name for name in os.listdir(folder_name)])
				if fls != e["imgno"]:
					print('Folder', folder_name, 'Expected files',e["imgno"], 'Actual', fls)
					#exit(7)
					continue
				upload_folder = upload_all
				files = [y for y in os.listdir(folder_name)]
				files.sort(key=custom_sort)
				print(files)
				for f1 in files:
					print('f1', f1)
					if f1: # just so the indent matches
					#f2 = os.path.join(folder_name, f1)
					#files2 = [y for y in os.listdir(f2)]
					#files2.sort(key=custom_sort)
					#for f in files2:
						#date = f1.split("-")
						#if len(date) < 3:
						#	for i in range(3-len(date)):
						#		date.append('')
						#else:
						#	date[0] = f1
						#print('date', date)
						#e["img"] = date
						f=f1
						fbid = [w for w in f[:-4].split('_') if w.isdigit()]
						if len(fbid) != 1:
							print('fbid', fbid, 'from f', f)
							exit(8)
						fbid = fbid[0]
						f = os.path.join(folder_name, f)
						#f = os.path.join(f2, f)
						idx += 1
						filename = self.build_name(e, idx, fbid)
						body = self.build_description_page(e, idx)
						print('Source', f)
						print('Destination', filename)
						print(body)
						#continue
						if upload_folder:
							answer = 'y'
						else:
							answer = pywikibot.input_choice("Upload?", [("Yes", "Y"), ("No", "N"), ('Folder', 'F'), ('All', 'A')])
							if answer in ['f', 'a']:
								upload_folder = True
								upload_all = (answer == 'a')
								answer = 'y'
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
                                       					comment="New AIRM image")
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
							else:
								pywikibot.output('Upload aborted.')


	def upload_images(self, filename):
		with open(filename, 'r') as f:
			db = json.loads(f.read())
			self.upload(db)



if __name__ == '__main__':
	pywikibot.handle_args()
	uploader = AIRMUploader('clasate')
	uploader.upload()
