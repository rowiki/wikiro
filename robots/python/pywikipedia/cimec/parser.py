from bs4 import BeautifulSoup
import json
import requests
import sys

config = {
	'clasate':
	{
		'list_url': 'http://clasate.cimec.ro/Lista.asp?start={}&pageno={}',
		'item_url': 'http://clasate.cimec.ro/Detaliu.asp?k={}',
		'page_size': 50,
	},
	'ghidulmuzeelor':
	{
		'list_url': 'http://ghidulmuzeelor.cimec.ro/Filtru-Domenii.asp?start={}&pageno={}',
		'item_url': 'http://ghidulmuzeelor.cimec.ro/id.asp?k={}',
		'page_size': 20,
	},
}
class CimecParser:
	def __init__(self, site):
		if site not in config:
			raise Exception("Trying to parse invalid site %s" % site)
		self.config = config[site]
		self.total = sys.maxsize
		self.db = {}

	def enhance_page_item_key(self, key):
		return key.replace(':', '')

	def enhance_page_item_value(self, value):
		return value

	def parse_single_data_row(self, row):
		cols = [div for div in row.find_all('div') if div['class'][0].find('col') == 0]
		if len(cols) != 2:
			return None, None
		label = cols[0].find('b')
		if not label:
			return None, None
		label = label.text.strip()
		label = self.enhance_page_item_key(label)

		text = cols[1].text.strip()
		text = self.enhance_page_item_value(text)
		return label, text


	def parse_item_page(self, k):
		url = self.config['item_url'].format(k)
		print('Parsing ', url)
		r = None
		try:
			r = requests.get(url)
		except:
			print(r.status_code)
			return {}
	
		html = r.text
		parsed_html = BeautifulSoup(html, 'html.parser')
		rows = parsed_html.find_all('div', attrs={'class':'row'})
		data = {}
		for row in rows:
			key, value = self.parse_single_data_row(row)
			if key:
				data[key] = value

		print(data)


	def parse_total_number(self, parsed_html):
		total = parsed_html.find('div', attrs={'class':'rezultate'})
		if total:
			total = total.text.strip()
			total = total[total.find("din"):]
			self.total = int(''.join(filter(str.isdigit, total)))
			print('Total', self.total)

	def parse_list(self, offset):
		page = int(offset / 50 + 1)
		url = self.config['list_url'].format(offset, page)
		print('Parsing ', url)
		try:
			r = requests.get(url)
		except:
			print('Request failed:', url)
			sleep(5)
			return False

		html = r.text
		parsed_html = BeautifulSoup(html, 'html.parser')
		self.parse_total_number(parsed_html)

		table = parsed_html.find(id='myTable')
		if table:
			table = table.tbody
		else:
			return False
		rows = table.find_all('tr')
		for row in rows:
			line = {}
			for col in row.find_all('td'):
				#print(col.attrs['data-title'])
				line[col.attrs['data-title']] = self.enhance_table_cell(col)
			key, line = self.enhance_table_line(line)
			if not key:
				key = offset
			offset += 1
			print('Key', key)
			print('Line', line)
			self.db[key] = line

		return True

	def enhance_table_cell(self, tag):
		text = tag.text.strip()
		text = text.replace(u'ş', u'ș')
		text = text.replace(u'ţ', u'ț')
		text = text.replace(u'Ş', u'Ș')
		text = text.replace(u'Ţ', u'Ț')
		text = text.replace(u'ã', u'ă')
		return text

	def enhance_table_line(self, line):
		# enhancement is specific per site
		return None, line

	def parse(self, start, filename, useCache=False):
		retry_list = []
		if start > 1:
			with open(filename, 'r') as f:
				self.db = json.loads(f.read())
		while self.total and start < self.total:
			success = self.parse_list(start)
			if success == False:
				retry_list.append(start)
			start += self.config['page_size']
			with open(filename, 'w') as f:
				f.write(json.dumps(self.db, indent=2))
		#just one retry for now
		for start in retry_list:
			success = self.parse_list(start)
			with open(filename, 'w') as f:
				f.write(json.dumps(self.db, indent=2))
		print ('DB size', len(self.db))


if __name__ == '__main__':
	parser = CimecParser('clasate')
	parser.parse(1, 'clasate.json')
