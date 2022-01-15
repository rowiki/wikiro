#!/user/bin/python3

from collections import OrderedDict
import copy
import json
import wikiro.robots.python.pywikipedia.strainu_functions as sf
from wikiro.robots.python.pywikipedia.cimec.list import CimecListGenerator
import wikiro.robots.python.pywikipedia.cimec as cimec
import pywikibot

min_count_for_split = 1000

wiki_to_json_keys = OrderedDict([
	("Imagine", "Fișier"),
	("Nume", ["Titlu", "Categorie"]),
	("Autor", "Autor"),
	("Deținător", "Detinator"),
	("Locație", ["Localitate", "Judet"]),
	("Descriere", [ "Descriere",
			"Descriere avers",
			"Descriere revers",
			"Descriere exergă",
			"Legendă revers"
			]),
	("Dimensiuni", "Dimensiuni"),
	("Material", ["Material", "Material/Tehnică (text)"]),
	("Descoperit", "Loc de descoperire"),
	("Școală", "Școală"),
	("Datare", "Datare"),
	("url_inp", "key")
	#("Domeniu", "Domeniu"),
	#("Categorie", "Clasare categorie"),
	#("Clasare", ["Clasare Ordin", "Clasare Dată", "Clasare Index"]),
	])

cat_list = {
	"Judet": "Bunuri mobile clasate în patrimoniul național al României aflate în {}",
	"Domeniu": "Bunuri mobile din domeniul {} clasate în patrimoniul național al României"
}

split_owner_by_county = [
	"Muzeul Județean"
]
split_owner_by_city = [
	"Muzeul de Artă",
	"Muzeul de Etnografie",
	"Muzeul de Istorie",
	"Muzeul de Științe Naturale"
]

def remove_whitespace(string):
	return string.replace('\r', '').replace('\n', ' ').replace('\t', ' ')

def capitalize(string):
	return sf.capitalizeWithSigns(string.lower(), keep=[" de ", " din ", " lui ","-"])

class CimecClasateListGenerator(CimecListGenerator):
	def __init__(self, keys_mapping, filename):
		self.start_template = "ÎnceputTabelClasate"
		self.line_template = "Bun mobil clasat"
		self.end_template = "SfârșitTabelClasate"
		self.wiki_to_json_keys = keys_mapping
		self.file = filename
		self.list_template = "Bunuri mobile din domeniul {1} clasate în patrimoniul cultural național al României aflate în {0}"
		self.commit_description = "Creez o listă de bunuri mobile clasate"
		self.section = None
		self.introduction = "Acestă listă conține '''bunurile mobile din domeniul {1} clasate în [[Patrimoniul cultural național al României]]''' aflate la momentul clasării în {0}.\n\n{{{{Căutare bunuri clasate}}}}\n"
		self.location_cache = {}
		self.categories = cat_list
		self.optimized = 0
		self.main_art_name= "Bunuri mobile clasate în patrimoniul cultural național al României"

	def _handle_basic(self, wiki_cell, keys, data):
		line = "| " + wiki_cell + " = "
		if keys not in data or data[keys] == "":
			return "\n"
		line += data[keys] + "\n"
		return line

	def _handle_Imagine(self, wiki_cell, keys, data):
		#if self.optimized > 0 and data.get("Foto"):
		#	img_name = data["Foto"].split("/")[-1]
		#	img_name = "File:"+img_name
		#	if img_name.find("placeholder") > -1:
		#		return "\n"
		#	img_page = pywikibot.FilePage(pywikibot.Site(), img_name)
		#	if not data.get("Fișier") and not img_page.exists():
		#		return "\n"
		#	return "| " + wiki_cell + " = " + img_name + "\n"
		#else:
		return self._handle_basic(wiki_cell, keys, data)

	def _handle_Nume(self, wiki_cell, keys, data):
		for key in keys:
			if data[key] != "":
				ret = self._handle_basic(wiki_cell, key, data)
				idx = ret.find('=')
				if idx > -1:
					ret = ret[:idx+2] + ret[idx+2].upper() + ret[idx+3:]
				return ret
		return "\n"

	def _handle_Descriere(self, wiki_cell, keys, data):
		if self.optimized >= 3:
			return "\n"
		d = []
		last_key = None
		for key in keys:
			if key in data:
				v = self._handle_basic(key.replace(' ', '_'), key, data)
				if v != '\n':
					d.append(v)
		line = "\n".join(d)
		return line or "\n"

	def _handle_Autor(self, wiki_cell, keys, data):
		if self.optimized < 10:
			return self._handle_basic(wiki_cell, keys, data)
		return "\n"

	def _handle_Datare(self, wiki_cell, keys, data):
		if self.optimized < 9:
			return self._handle_basic(wiki_cell, keys, data)
		return "\n"

	def _handle_Detinator(self, wiki_cell, keys, data):
		line = "| " + wiki_cell + " = [["
		owner = remove_whitespace(data[keys])
		owners = {}
		with open("clasate_detinator_to_wiki.json", "r") as f:
			owners = json.load(f)
		if owner not in owners:
			pywikibot.error("Invalid owner " + owner)
			exit(3)
		if owners[owner] != "":
			if owner == owners[owner]:
				line += owners[owner] + "]]\n"
			else:
				line += owners[owner] + "|" + owner + "]]\n"
		elif owner in split_owner_by_city:
			city = capitalize(data["Localitate"])
			line += owner + " din " + city + "|"
			line += owner + "]]\n"
		elif owner in split_owner_by_county:
			county = capitalize(data["Judet"])
			line += owner + " " + county + "|"
			line += owner + "]]\n"
		else:
			line = self._handle_basic(wiki_cell, keys, data)
		return line

	def _handle_Locatie(self, wiki_cell, keys, data):
		if self.optimized not in [0,1,3]:
			return "\n"
		city = capitalize(data[keys[0]])
		county = data[keys[1]]
		if city == "":
			return "| " + wiki_cell + " = " + county + "\n"
		if self.location_cache.get(city+county) != None:
			link = self.location_cache[city+county]
		else:
			page = pywikibot.Page(pywikibot.Site('ro'), city)
			if page.exists():
				link = city
			else:
				link = city + ", " + county
			self.location_cache[city+county] = link
		if link == city:
			return "| " + wiki_cell + " = [[" + link + "]]\n"
		else:
			return "| " + wiki_cell + " = [[" + link + "|" + city + "]]\n"

	def _handle_Domeniu(self, wiki_cell, keys, data):
		return self._handle_basic(wiki_cell, keys, data)

	def _handle_Categorie(self, wiki_cell, keys, data):
		return self._handle_basic(wiki_cell, keys, data)

	def _handle_Dimensiuni(self, wiki_cell, keys, data):
		if self.optimized < 8:
			return self._handle_basic(wiki_cell, keys, data)
		return "\n"

	def _handle_Material(self, wiki_cell, keys, data):
		for key in keys:
			if self.optimized < 7 and \
				key in data and data[key] != "":
				return self._handle_basic(wiki_cell, key, data)
		return "\n"

	def _handle_Descoperit(self, wiki_cell, keys, data):
		if self.optimized in [0,3,4,5]:
			return self._handle_basic(wiki_cell, keys, data)
		return "\n"

	def _handle_Scoala(self, wiki_cell, keys, data):
		if self.optimized in [0,3,4]:
			return self._handle_basic(wiki_cell, keys, data)
		return "\n"

	def _handle_Clasare(self, wiki_cell, keys, data):
		return "| " + wiki_cell + " = " + data[keys[0]] + "/" + data[keys[1]] + ", poziția " + data[keys[2]] + "\n"

	def _handle_url_inp(self, wiki_cell, keys, data):
		if keys in data:
			url = cimec.config['clasate']['item_url'].format(data[keys])
		else:
			url = cimec.config['clasate']['home_url']
		line = "| " + wiki_cell + " = " + data[keys] + "\n"
		return line

	def build_talk_page(self, single_filter):
		ret = "{{{{DateCimec|1={0}|2=CC-BY-SA-4.0}}}}"
		url = cimec.config['clasate']['list_url'].format(1,1)
		return ret.format(url)

	def process_filter_display(self, filter_one):
		filter_one = copy.deepcopy(filter_one)
		for f in filter_one.keys():
			value = filter_one[f]
			print(f, value)
			if f == "Judet":
				if value == "București":
					value = "municipiul București"
				elif value in cimec.counties.values():
					value = "județul " + value
			else:
				value = value.lower()
			filter_one[f] = value

		return filter_one

	def build_category_list(self, filters):
		txt = ""
		for f in filters.keys():
			if f in self.categories:
				value = filters[f]
				if value == "București":
					value = "municipiul București"
				elif value in cimec.counties.values():
					value = "județul " + value
				elif f != "Judet":
					value = value.lower()
				txt += "[[Categorie:" + self.categories[f].format(value) + "]]\n"

		return txt

	def should_split_page(self, data, filters):
		count = self.count_list_size(data, filters)
		if count > min_count_for_split:
			return True
		return False

if __name__ == "__main__":
	bot = CimecClasateListGenerator(wiki_to_json_keys, "clasate.json")
	bot.build(["Judet", "Domeniu"], ["Clasare categorie"])
	#bot.count(["Judet", "Domeniu", "Clasare categorie"])
	#bot.count(["Detinator"])
	#bot.list_template = 'Lista exponatelor din Muzeul Național al Satului „Dimitrie Gusti”'
	#bot.section = 'Bunuri mobile clasate'
	#bot.build({"Detinator": ["Muzeul Național al Satului „Dimitrie Gusti”"]})
