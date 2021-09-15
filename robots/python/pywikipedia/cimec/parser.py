from bs4 import BeautifulSoup
import wikiro.robots.python.pywikipedia.cimec as cimec
import json
import requests
import sys
import time

class CimecParser:
	def __init__(self, site):
		if site not in cimec.config:
			raise Exception("Trying to parse invalid site %s" % site)
		self.config = cimec.config[site]
		self.total = sys.maxsize
		self.db = {}

	def enhance_page_item_key(self, key):
		return key.replace(':', '')

	def enhance_page_item_value(self, value):
		return self.replace_diacritics(value)

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
			print('Request failed:', url)
			time.sleep(5)
			return None
	
		html = r.text
		parsed_html = BeautifulSoup(html, 'html.parser')
		rows = parsed_html.find_all('div', attrs={'class':'row'})
		data = {}
		for row in rows:
			key, value = self.parse_single_data_row(row)
			if key:
				data[key] = value

		#print('Details', data)
		return data


	def parse_total_number(self, parsed_html):
		total = parsed_html.find('div', attrs={'class':'rezultate'})
		if total:
			total = total.text.strip()
			total = total[total.find("din"):]
			self.total = int(''.join(filter(str.isdigit, total)))
			print('Total', self.total)

	def merge_info(self, line, page):
		for key, value in page.items():
			if line.get(key) and value != line[key]:
				line[key + '_2'] = value
			else:
				line[key] = value
		return line

	def parse_row(self, row, default_key, parseDetails=True):
		line = {}
		for col in row.find_all('td'):
			#print(col.attrs['data-title'])
			line[col.attrs['data-title']] = self.enhance_table_cell(col)
		key, line = self.enhance_table_line(line)
		if not key:
			key = default_key

		if parseDetails:
			details = self.parse_item_page(line['key'])
			if details == None:
				return False
			print('Merging for key', key)
			self.db[key] = self.merge_info(line, details)
			#time.sleep(1)
		else:
			self.db[key] = line
		print('Key', key)
		print('Line', self.db[key])
		return True

	def parse_list(self, offset, parseDetails=True):
		page = int(offset / self.config['page_size'] + 1)
		url = self.config['list_url'].format(offset, page)
		print('Parsing ', url)
		try:
			r = requests.get(url)
		except:
			print('Request failed:', url)
			time.sleep(5)
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
		retry = []
		for row in rows:
			done = self.parse_row(row, offset, parseDetails)
			if not done:
				retry.append((offset, row))
			offset += 1

		#retry once for now
		for offset, row in retry:
			done = self.parse_row(row, offset, parseDetails)
			if not done:
				return False
		return True

	def enhance_table_cell(self, tag):
		text = tag.text.strip()
		return self.replace_diacritics(text)

	def replace_diacritics(self, text):
		text = text.replace(u'ş', u'ș')
		text = text.replace(u'ţ', u'ț')
		text = text.replace(u'Ş', u'Ș')
		text = text.replace(u'Ţ', u'Ț')
		text = text.replace(u'ã', u'ă')
		return text

	def enhance_table_line(self, line):
		# enhancement is specific per site
		return None, line

	def parse(self, start, filename, useCache=False, parseDetails=True):
		retry_list = []
		if useCache:
			with open(filename, 'r') as f:
				self.db = json.loads(f.read())
		while self.total and start < self.total:
			success = self.parse_list(start, parseDetails)
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
	#parser.parse(1, 'clasate.json')
	parser.parse_item_page('70224FCA28D44E3DB19503580579F37F')
