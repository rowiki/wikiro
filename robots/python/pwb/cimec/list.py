#!/usr/bin/python3

from collections import OrderedDict
import itertools
import json
import pywikibot
import unicodedata

import wikiro.robots.python.pwb.cimec as cimec

wikitext_size_limit = 1000000
expanded_size_limit = 1 * 1024 * 1024
max_optimization_level = 10

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
		self.section = None
		self.introduction = ""
		self.categorie = {}
		self.optimized = 0
		self.main_art_name = ""

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
		old_template = ""
		while old_template != template:
			old_template = template
			template = old_template.replace("\n\n", "\n")
		return template

	def filtered_data_generator(self, data, filters):
		for d in sorted(data):
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
		for f in filter_keys:
			filters[f] = sorted(filters[f], reverse=True)


		print(filters)
		return filters

	def build_header(self, filters):
		return "{{" + self.start_template + "}}\n"

	def build_footer(self, filters):
		return "{{" + self.end_template + "}}\n"

	def build_list_contents(self, data, filters):
		text = ""
		text += self.build_header(filters)
		prefix = text
		for d in self.filtered_data_generator(data, filters):
			text += self.build_wiki_line(d)
		if text == prefix: # no lines
			return ""
		text += self.build_footer(filters)
		return text

	def count_list_size(self, data, filters):
		count = 0
		for d in self.filtered_data_generator(data, filters):
			count +=1
		return count

	def build_list_title(self, tuples):
		return self.list_template.format(*tuples).strip()

	def build_list_intro(self, tuples):
		return self.introduction.format(*tuples).strip()

	def build_category_list(self, filters):
		txt = ""
		for f in filters.keys():
			if f in self.categories:
				txt += "[[Categorie:" + self.categories[f].format(filters[f]) + "]]\n"

		return txt.strip()

	def process_filter_display(self, filter_one):
		return filter_one

	def build_lists(self, data, filters, extra_filters=None):
		print(filters.values())
		for tuples in itertools.product(*filters.values()):
			print(tuples)
			new_filter = OrderedDict(zip(list(filters.keys()), tuples))
			#if new_filter["Judet"].find("București") == -1:
			#	continue
			display_filter = self.process_filter_display(new_filter)
			if extra_filters:
				if self.should_split_page(data, new_filter):
					old_list_template = self.list_template
					self.list_template += " ({" + str(len(filters)) + "})"
					print('List template', self.list_template)
					for extra_tuples in itertools.product(*extra_filters.values()):
						extra_filter = OrderedDict(zip(list(extra_filters.keys()), extra_tuples))
						content_filter = {**new_filter, **extra_filter}
						if content_filter["Judet"].find("București") > -1 and content_filter["Domeniu"].find("naturii") > -1 and content_filter["Clasare categorie"].find("Tezaur") > -1:
							continue
						self.build_single_list(data, [content_filter], display_filter)
					self.list_template = old_list_template
				else:
					content_filters = []
					for extra_tuples in itertools.product(*extra_filters.values()):
						extra_filter = OrderedDict(zip(list(extra_filters.keys()), extra_tuples))
						content_filter = {**new_filter, **extra_filter}
						content_filters.append(content_filter)
					self.build_single_list(data, content_filters, display_filter)
					
			else:
				self.build_single_list(data, new_filter, display_filter)

	def build_talk_page(self, single_filter):
		return "{{DateCimec}}"

	def build_single_list(self, data, content_filters, display_filter):
		print(content_filters)
		print(display_filter)

		list_contents = ""
		for content_filter in content_filters:
			one_list = self.build_list_contents(data, content_filter)
			if one_list and len(content_filters) > 1:
				keys_diff = set(content_filter.keys()) - set(display_filter.keys())
				list_contents += "== " + " ".join([content_filter[k] for k in keys_diff]) + " ==\n"
			list_contents += one_list
		if list_contents == "":
			return

		html = pywikibot.Site().expand_text(list_contents)
		over_limit = (html.find("post-expand include size too large") > -1)
		lenhtml = len(html)
		lenwiki = len(list_contents)
		#print(html)
		del html
		print("Depth:", self.optimized)
		print("Over limit; ", over_limit)
		print("Wikitext length:", lenwiki)
		print("HTML length:", lenhtml)
		if self.optimized < max_optimization_level and \
			(over_limit or
			 lenwiki > wikitext_size_limit or \
			 lenhtml > expanded_size_limit):
			self.optimized += 1
			self.build_single_list(data, content_filters, display_filter)
			self.optimized -= 1
			return

		if len(content_filters) == 1:
			keys_diff = set(content_filters[0].keys()) - set(display_filter.keys())
			title_display_filter = display_filter.copy()
			for key in keys_diff:
				#TODO: lower() ignores proper nouns
				title_display_filter[key] = content_filters[0][key].lower()
		else:
			title_display_filter = display_filter
		list_name = self.build_list_title(title_display_filter.values())
		print("List name:", list_name)
			
		intro = self.build_list_intro(display_filter.values())
		categories = self.build_category_list(display_filter)

		old_text = ""
		page = pywikibot.Page(pywikibot.Site(), list_name)
		if page.exists():
			old_text = page.get()
		if self.section == None:
			text = "\n".join([intro, list_contents, categories])
			text = text.strip()
			text = text.replace("\r\n", "\n")
			if len(old_text) == len(text):
				return
			pywikibot.showDiff(old_text, text)
			#print("List text:", text[-2000:])
			pass
		else:
			section_title = "== " + self.section + " =="
			list_contents = section_title + "\n" + list_contents
			text = old_text.replace(section_title, list_contents)
		choice = 'y'#pywikibot.input_choice("Write page?", [('Yes', 'Y'), ('No', 'N')])
		if choice == 'y':
			ttext = self.build_talk_page(display_filter)
			page.put(text, self.commit_description)
			tpage = page.toggleTalkPage()
			if not tpage.exists():
				tpage.put(ttext, self.commit_description)

	
	def count_lists(self, data, filters):
		count = 0
		for tuples in itertools.product(*filters.values()):
			new_filter = OrderedDict(zip(list(filters.keys()), tuples))
			partial_count = self.count_list_size(data, new_filter)
			if partial_count:
				print(new_filter, partial_count)
			count += partial_count
		return count

	def build_main_page(self, data, filters, extra_filters):
		text = "{| class=\"wikitable center\"\n"
		text += "! " + "/".join(filters.keys()) + "\n"
		keys = sorted(filters.keys())
		print(filters[keys[0]])
		for pkey in reversed(filters[keys[0]]):
			text += "! " + pkey + "\n"
		for skey in reversed(filters[keys[1]]):
			text += "|-\n"
			text += "! " + skey + "\n"
			for pkey in reversed(filters[keys[0]]):
				new_filter = OrderedDict(zip(list(keys), (pkey, skey)))
				display_filter = self.process_filter_display(new_filter)
				title = self.build_list_title(reversed(display_filter.values()))
				p = pywikibot.Page(pywikibot.Site(), title)
				if p.exists():
					text += "| [[" + title + "#Tezaur|tezaur]]<br/>[[" + title + "#Fond|fond]]\n"
				else:
					text += "| "
					old_list_template = self.list_template
					self.list_template += " ({" + str(len(filters)) + "})"
					for extra_tuples in itertools.product(*extra_filters.values()):
						extra = ''.join(extra_tuples).lower()
						elems = list(reversed(display_filter.values()))
						elems.append(extra)
						title = self.build_list_title(elems)
						p = pywikibot.Page(pywikibot.Site(), title)
						if not p.exists():
							continue
						text += "[[" + title + "|" + extra + "]]<br/>"
					self.list_template = old_list_template
					text += "\n"
		text += "|}\n[[Categorie:Bunuri mobile clasate în patrimoniul național al României]]"
		page = pywikibot.Page(pywikibot.Site(), self.main_art_name)
		page.put(text, "Populez tabelul cu liste")

		pass

	def should_split_page(self, data, filters):
		return False

	def prepare_data(self, filter_keys):
		data = {}
		with open(self.file, "r") as f:
			data = json.load(f)
		pywikibot.output("Loaded data")
		if type(filter_keys) == list:
			filters = self.generate_filters(data, filter_keys)
		else:
			filters = filter_keys
		return data, filters

	def build(self, filter_keys, extra_filter_keys=None):
		data, filters = self.prepare_data(filter_keys)
		extra_filters = None
		if extra_filter_keys:
			extra_filters = self.generate_filters(data, extra_filter_keys)
		#self.build_lists(data, filters, extra_filters)
		self.build_main_page(data, filters, extra_filters)

	def count(self, filter_keys):
		data, filters = self.prepare_data(filter_keys)
		return self.count_lists(data, filters)

if __name__ == "__main__":
	lg = CimecListGenerator("", "clasate.json")
	lg.build(["Judet", "Domeniu"])
