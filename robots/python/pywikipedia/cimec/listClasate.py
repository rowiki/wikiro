#!/user/bin/python3

from collections import OrderedDict
import json
import wikiro.robots.python.pywikipedia.strainu_functions as sf
from wikiro.robots.python.pywikipedia.cimec.list import CimecListGenerator
import wikiro.robots.python.pywikipedia.cimec as cimec
import pywikibot

wiki_to_json_keys = OrderedDict([
	("Imagine", "Fișier"),
	("Nume", ["Titlu", "Categorie"]),
	("Autor", "Autor"),
	("Deținător", "Detinator"),
	("Locație", ["Localitate", "Judet"]),
	("Domeniu", "Domeniu"),
	("Categorie", "Clasare categorie"),
	("Clasare", ["Clasare Ordin", "Clasare Dată", "Clasare Index"]),
	("url_inp", "key")
	])

split_owner_by_city = [
	"Muzeul de Artă",
	"Muzeul de Etnografie",
	"Muzeul de Istorie",
	"Muzeul de Științe Naturale"
]

def remove_whitespace(string):
	return string.replace('\r', '').replace('\n', ' ').replace('\t', ' ')

class CimecClasateListGenerator(CimecListGenerator):
	def __init__(self, keys_mapping, filename):
		self.start_template = "ÎnceputTabelClasate"
		self.line_template = "Bun mobil clasat"
		self.end_template = "SfârșitTabelClasate"
		self.wiki_to_json_keys = keys_mapping
		self.file = filename
		self.list_template = "Lista bunurilor mobile din Patrimoniul Cultural Național aflate în județul {} ({})"
		self.commit_description = "Creez o listă de bunuri mobile clasate"

	def _handle_basic(self, wiki_cell, keys, data):
		line = "| " + wiki_cell + " = "
		if keys not in data:
			return line
		line += data[keys] + "\n"
		return line

	def _handle_Imagine(self, wiki_cell, keys, data):
		return self._handle_basic(wiki_cell, keys, data)

	def _handle_Nume(self, wiki_cell, keys, data):
		for key in keys:
			if data[key] != "":
				return self._handle_basic(wiki_cell, key, data)
		return "\n"

	def _handle_Autor(self, wiki_cell, keys, data):
		return self._handle_basic(wiki_cell, keys, data)

	def _handle_Detinator(self, wiki_cell, keys, data):
		owner = remove_whitespace(data[keys])
		owners = {}
		with open("clasate_detinator_to_wiki.json", "r") as f:
			owners = json.load(f)
		if owner not in owners:
			pywikibot.error("Invalid owner " + owner)
			exit(3)
		line = "\n"
		if owners[owner] != "":
			line = "| " + wiki_cell + " = [[" + owners[owner] + "|" + owner + "]]\n"
		elif owner in split_owner_by_city:
			city = sf.capitalizeWithSigns(data["Localitate"])
			line = "| " + wiki_cell + " = [["
			line += owner + " din " + city + "|"
			line += owner + "]]\n"
		else:
			line = self._handle_basic(wiki_cell, keys, data)
		return line

	def _handle_Locatie(self, wiki_cell, keys, data):
		city = sf.capitalizeWithSigns(data[keys[0]])
		county = data[keys[1]]
		if city == "":
			return "| " + wiki_cell + " = " + county
		page = pywikibot.Page(pywikibot.Site('ro'), city)
		if page.exists():
			link = city
		else:
			link = city + ", " + county
		return "| " + wiki_cell + " = [[" + link + "|" + city + "]]\n"

	def _handle_Domeniu(self, wiki_cell, keys, data):
		return self._handle_basic(wiki_cell, keys, data)

	def _handle_Categorie(self, wiki_cell, keys, data):
		return self._handle_basic(wiki_cell, keys, data)

	def _handle_Clasare(self, wiki_cell, keys, data):
		return "| " + wiki_cell + " = " + data[keys[0]] + "/" + data[keys[1]] + ", poziția " + data[keys[2]] + "\n"

	def _handle_url_inp(self, wiki_cell, keys, data):
		if keys in data:
			url = cimec.config['clasate']['item_url'].format(data[keys])
		else:
			url = cimec.config['clasate']['home_url']
		line = "| " + wiki_cell + " = " + url + "\n"
		return line


if __name__ == "__main__":
	bot = CimecClasateListGenerator(wiki_to_json_keys, "clasate.json")
	bot.build(["Judet", "Domeniu"])
