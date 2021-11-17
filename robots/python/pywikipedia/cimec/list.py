#!/usr/bin/python3

from collections import OrderedDict
import itertools
import json
import pywikibot
import unicodedata

def remove_accents(input_str):
	nfkd_form = unicodedata.normalize('NFKD', input_str)
	return u"".join([c for c in nfkd_form if not unicodedata.combining(c)])

class CimecListGenerator:
	def __init__(self, keys_mapping, filename):
		self.start_template = ""
		self.line_template = ""
		self.end_template = ""
		self.wiki_to_json_keys = keys_mapping
		self.file = filename
		self.commit_description = ""
		self.list_template = "{} {}"

	def build_wiki_line(self, data):
		#print(data)
		template = "{{" + self.line_template + "\n"
		for cell in self.wiki_to_json_keys:
			ascii_cell = remove_accents(cell)
			handler = getattr(self, '_handle_' + ascii_cell, None)
			if not handler:
				print(dir(self))
				pywikibot.error("Handler for %s not found: _handle_%s" % (cell, ascii_cell));
				exit(1)

			handler_result = handler(cell, self.wiki_to_json_keys[cell], data)
			if not handler_result:
				print(data)
				pywikibot.error("Failed to build cell for %s" % cell)
				exit(2)
			template += handler_result

		template += "}}\n"
		return template

	def filtered_data_generator(self, data, filters):
		for d in data:
			for f in filters:
				if filters.get(f) != data[d].get(f):
					break
			else:
				yield data[d]

	def generate_filters(self, data, filter_keys):
		filters = OrderedDict({})
		for f in filter_keys:
			filters[f] = set()
		for d in data:
			for f in filter_keys:
					filters[f].add(data[d].get(f))

		print(filters)
		return filters

	def build_header(self, filters):
		return "\n"

	def build_footer(self, filters):
		return "\n"

	def build_list(self, data, filters):
		text = ""
		text += self.build_header(filters)
		text += "{{" + self.start_template + "}}\n"
		prefix = text
		for d in self.filtered_data_generator(data, filters):
			text += self.build_wiki_line(d)
		if text == prefix: # no lines
			return ""
		text += "{{" + self.end_template + "}}\n"
		text += self.build_footer(filters)
		return text

	def build_list_title(self, tuples):
		return self.list_template.format(*tuples)

	def build_lists(self, data, filters):
		print(filters.values())
		for tuples in itertools.product(*filters.values()):
			new_filter = OrderedDict(zip(list(filters.keys()), tuples))
			print(new_filter)
			list_name = self.build_list_title(tuples)
			list_contents = self.build_list(data, new_filter)
			print("List name:", list_name)
			print("List text:", list_contents)
			#page = pywikibot.Page(pywikibot.Site(), list_name)
			#TODO
			#page.put(list_contents, self.commit_description)
			

	def build_main_page(self, data, filters):
		pass

	def build(self, filter_keys):
		data = {}
		with open(self.file, "r") as f:
			data = json.load(f)
		pywikibot.output("Loaded data")
		filters = self.generate_filters(data, filter_keys)
		self.build_lists(data, filters)
		self.build_main_page(data, filters)


if __name__ == "__main__":
	lg = CimecListGenerator("", "clasate.json")
	lg.build(["Judet", "Domeniu"])
