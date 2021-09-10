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

	def parse_item(self, k):
		url = self.config['item_url'].format(k)
		print('Parsing ', url)
		try:
			r = requests.get(url)
		except:
			print(r.status_code)
			return {}
	
		html = r.text
		parsed_html = BeautifulSoup(html, 'html.parser')
		rows = parsed_html.find_all('div', attrs={'class':'row'})


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
