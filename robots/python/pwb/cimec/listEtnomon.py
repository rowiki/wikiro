#!/user/bin/python3
from collections import OrderedDict
from typing import Dict, Iterable, Tuple

import pywikibot
import wikiro.robots.python.pwb.cimec as cimec
import wikiro.robots.python.pwb.strainu_functions as sf
from wikiro.robots.python.pwb.cimec.list import CimecListGenerator

min_count_for_split = 1000

wiki_to_json_keys = OrderedDict([
	("imagine", "Fișier"),
	("culoare", "Zona etnografică"),
	("marcaj", None),
	("nume", "Denumirea în muzeu"),
	("datare", "Datare"),
	("origine", "Provenienţă"),
	("id-etnomon", "key")
	])

regions = {
	"Banat": "orange",
	"Dobrogea": "lightblue",
	"Moldova": "pink",
	"Muntenia": "lightgreen",
	"Oltenia": "magenta",
	"Transilvania": "papayawhip",
	"Maramureș": "lightyellow",
}

def remove_whitespace(string):
	return string.replace('\r', '').replace('\n', ' ').replace('\t', ' ')

def capitalize(string):
	return sf.capitalizeWithSigns(string.lower(), keep=[" de ", " din ", " lui ","-"])

class EtnomonListGenerator(CimecListGenerator):
	def __init__(self, keys_mapping, filename):
		self.data = None
		self.filters = None
		self.wiki_to_json_keys = keys_mapping
		self.file = filename
		self.list_template = "Bunuri mobile din domeniul {1} clasate în patrimoniul cultural național al României aflate în {0}"
		self.commit_description = "Creez o listă de exponate etnografice"
		self.section = None
		self.introduction = """Această pagină conține '''lista caselor și instalațiilor''' expuse în [[{muzeu}]]. Exponatele sunt grupate după  zona etnografică din care provin. 

Pentru a naviga prin pagină puteți folosi cuprinsul sau harta de mai jos, unde fiecare număr trimite la intrarea corespunzătoare din listă. Culoarea reprezintă zona etnografică din care provine exponatul, conform legendei."""
		self.location_cache = {}

	def build_talk_page(self, single_filter):
		ret = "{{{{DateCimec|1={0}|2=CC-BY-SA-4.0}}}}"
		url = cimec.config['etnomon']['list_url'].format(1,1)
		return ret.format(url)

	@staticmethod
	def convert_to_known_region(region):
		for known_region in regions:
			if region.find(known_region) != -1:
				return known_region
		return None

	@staticmethod
	def sort_by_field(data: Iterable[Dict], field: str) -> Iterable[Dict]:
		return sorted(data, key=lambda k: k.get(field,''))

	@staticmethod
	def sort_by_fields(data: Iterable[Dict], fields: Tuple[str]) -> Iterable[Dict]:
		return sorted(data, key=lambda k: tuple(k.get(x,'') for x in fields))

	@staticmethod
	def generate_monument_text(entry, section_level):
		if entry["key"] == "ADCD8D039DCD4C538813618C44F39663":
			print(entry)
		if entry.get('Categoria') != 'ANSAMBLU' and entry.get('Face parte din ansamblul') is not None:
			section_level += "="
		text = (f"\n{section_level} "
				f"{{{{subst:ucfirst:"
				f"{entry.get('Denumirea locală') or entry['Denumirea în muzeu']}}}}}"
				f" {section_level}\n")
		text += f"""{{{{Exponat etnografic
| nume = {{{{subst:ucfirst:{entry.get('Denumirea locală') or entry['Denumirea în muzeu']}}}}}
| imagine = {entry['Fișier']}
| datare = {entry.get('Datare','')}
| id-etnomon = {entry['key']}
| origine = {entry.get('Proveniență', entry.get('Proveniența', ''))}
| ansamblu = {entry.get('Face parte din ansamblul','')}
}}}}
{entry.get('Descriere',entry.get('Denumirea în muzeu',''))}<ref>{{{{ETNOMON|{entry['key']}|{entry['Denumirea în muzeu']}}}}}</ref>
"""
		return text

	def build_article(self,
					  data: Iterable[Dict],
					  article_items: Dict,
					  subsections: Dict,
					  full_page: bool = True):
		article_title = list(article_items.values())[0]
		text = self.introduction.format(muzeu=article_title) \
			if full_page else ""
		section_level = "==" if full_page else "==="
		if len(subsections.keys()) > 1:
			pywikibot.error("Too many subsections")
			return
		elif subsections is None or len(subsections.keys()) == 0:
			for entry in data:
				text += self.generate_monument_text(entry, section_level)
		else:
			#print(article_items)
			#print(subsections)
			elems = {}
			for e in subsections.keys():
				for subsection in subsections[e]:
					region = (self.convert_to_known_region(subsection) or
							  "Regiune necunoscută")
					#print(region)
					if region not in elems:
						elems[region] = ("\n" + section_level + region +
									  section_level + "\n")
					for entry in self.filtered_data_generator(data,{e: subsection}):
						elems[region] += self.generate_monument_text(
							entry, section_level + "="
							)
			for elem in elems.values():
				text += elem

		with open(f"ETNOMON_{article_title}.txt", "w", encoding="utf-8") as f:
			f.write(text)

	def build(self, filter_keys, extra_filter_keys=None, full_page=True):
		self.data, self.filters = self.prepare_data(filter_keys)
		for article_filter in self.filters:
			for value in self.filters.get(article_filter, []):
				filter_dict = {article_filter: value}
				filtered_data = list(self.filtered_data_generator(
					self.data, filter_dict))
				#print(filtered_data)
				extra_filters = self.generate_filters(filtered_data, extra_filter_keys)
				# one article per first level
				self.build_article(filtered_data, filter_dict, extra_filters,
								   full_page)

	def prepare_data(self, filter_keys):
		data, filters = super().prepare_data(filter_keys)
		data = data.values()
		#group ensembles together
		for entry in data:
			if entry.get("Categoria") == "ANSAMBLU":
				entry['Face parte din ansamblul'] = \
				entry.get('Denumirea în muzeu')
		data = sorted(data, key=lambda k: (k.get('Face parte din ansamblul', '*'),
										   k.get('Categoria','*')))
		return data, filters


if __name__ == "__main__":
	bot = EtnomonListGenerator(wiki_to_json_keys, "etnomon.json")
	#bot.build(["Muzeu"], ["Zona etnografică"])
	bot.build(["Muzeu"], [], False)