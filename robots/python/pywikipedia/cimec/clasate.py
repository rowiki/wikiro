from bs4 import BeautifulSoup
import wikiro.robots.python.pywikipedia.cimec as cimec
import json
import parser
import re
import requests
import time

class ClasateParser(parser.CimecParser):
	def __init__(self):
		super().__init__('clasate')

	def split_owner_city(self, owner):
		city = ""
		mo = re.fullmatch("(.*) - ([A-ZĂÂȘȚÎ]+)", owner)
		if mo:
			owner = mo.group(1).strip()
			city = mo.group(2).strip()
		return owner, city

	def split_order_info(self, order):
		o_no = order
		o_date = None
		o_cat = None
		o_index = None
		order = order.replace("\r","")
		order = order.replace("\n","")
		#print('Order', order)
		
		mo = re.match("([0-9]{1,4})/([0-9]{2}\.[0-9]{2}\.[0-9]{4})(Fond|Tezaur)\s*Poziția\s*([0-9]+)\s?", order)
		if mo:
			#print('Groups', mo.groups())
			o_no  = mo.group(1).strip()
			o_date  = mo.group(2).strip()
			o_cat  = mo.group(3).strip()
			o_index  = mo.group(4).strip()
		return o_no, o_date, o_cat, o_index
		
	def enhance_table_cell(self, tag):
		value = None
		if tag.attrs['data-title'] == 'Foto':
			img = tag.find('img')
			value = img['src']
			value = value.replace('small', 'medium')
		elif tag.attrs['data-title'] == 'Nr Ord':
			value = super().enhance_table_cell(tag)
			a = tag.find('a')
			link = a['href']
			#print('Link', link)
			mo = re.search("k=([0-9A-Fa-f]+)", link);
			if mo:
				value += " " + mo.group(1).strip()	
		else:
			value = super().enhance_table_cell(tag)
		return value


	def enhance_table_line(self, line):
		key, line = super().enhance_table_line(line)
		#print('Line', line)
		if 'Detinator' in line:
			owner, city = self.split_owner_city(line['Detinator'])
			line['Detinator'] = owner
			line['Localitate'] = city
		if 'Clasare' in line:
			o_no, o_date, o_cat, o_index = self.split_order_info(line['Clasare'])
			if o_date:
				line['Clasare Ordin'] = o_no
				line['Clasare Dată'] = o_date
				line['Clasare categorie'] = o_cat
				line['Clasare Index'] = o_index
				del line['Clasare']
				key = "_".join([line['Clasare Ordin'],  line['Clasare Dată'], line['Clasare categorie'], line['Clasare Index']])
		if 'Nr Ord' in line:
			s = line['Nr Ord'].split(' ')
			if len(s) > 1:
				line['Nr Ord'] = s[0]
				line['key'] = s[1]
			else:
				line['key'] = ''
			key = line['Nr Ord'] + "_" + key
		return key, line

	def parse_with_database(self, filename):
		with open(filename, 'r') as f:
			self.db = json.loads(f.read())

		url = self.config['list_url'].format(1, 1)
		#print('Parsing ', url)
		try:
			#r = requests.get(url)
			pass
		except:
			print('Request failed:', url)
			return

		#html = r.text
		#parsed_html = BeautifulSoup(html, 'html.parser')
		#self.parse_total_number(parsed_html)
		self.total = 78000

		missing = list(range(0, self.total+1))
		keys = list(self.db.keys())
		for e in keys:
			idx = int(self.db[e].get('Nr Ord') or 0)
			missing[idx] = 0
			if e.find(str(idx)) != 0:
				key = str(idx) + "_" + e
				print("New key", key, "e", e, "index", idx)
				self.db[key] = self.db[e]
				del self.db[e]

		for e in missing:
			if e == 0:
				continue
			p_size = self.config['page_size']
			if e % p_size == 0:
				start = e - p_size + 1
			else:
				start = int(e / p_size) * p_size + 1
			print("***Missing", e, "start", start)
			self.parse_list(start)
			for i in range(start, start+50):
				missing[i] = 0

			with open(filename, 'w') as f:
				json.dump(self.db, f, indent=2)



if __name__ == "__main__":
	parser = ClasateParser()
	#parser.parse(8551, 'clasate.json')
	parser.parse_with_database('clasate.json')
