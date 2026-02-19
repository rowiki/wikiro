#!/usr/bin/python3
# -*- coding: utf-8  -*-

import re
import parser


class EtnomonParser(parser.CimecParser):
	def __init__(self):
		super().__init__('etnomon')

	def parse_total_number(self, parsed_html):
		total = parsed_html.find('div', attrs={'class':'col-md-2'})
		if total:
			total = total.text.strip()
			self.total = int(''.join(filter(str.isdigit, total)))
			print('Total', self.total)

	def get_content_table(self, parsed_html):
		table = parsed_html.find('table', class_='table')
		return table

	def enhance_table_cell(self, tag):
		if tag.attrs['data-title'] == 'Foto':
			img = tag.find('img')
			value = img['src']
			value = value.replace('small', 'medium')
		#elif tag.attrs['data-title'].find('Denumirea') != -1:
		#	value = re.sub("\r\n\s+?", " ", tag.text.strip())
		elif tag.attrs['data-title'] == 'Număr':
			value = super().enhance_table_cell(tag)
			a = tag.find('a')
			link = a['href']
			mo = re.search("ID=([0-9A-Fa-f]+)", link)
			if mo:
				value += " " + mo.group(1).strip()
		else:
			value = super().enhance_table_cell(tag)
		return value

	def enhance_table_line(self, line):
		key, line = super().enhance_table_line(line)
		#print(line, flush=True)
		#print('Line', line)
		if 'Număr' in line:
			s = line['Număr'].split(' ')
			if len(s) > 1:
				line['Număr'] = s[0]
				line['key'] = s[1]
			else:
				line['key'] = ''
			key = line['Număr'] + "_" + line['key']
		return key, line

	def parse_row(self, row, default_key, parseDetails=True):
		line = {}
		columns = ["Număr", "Foto", "Denumirea în muzeu", "Denumirea locală",
				   "Muzeu", "Provenienţă", "Zona etnografică", "Etnia",
				   "Datare"]
		#print(row)
		if len(row.find_all('td')) == 0:
			print("Header row, skipping", flush=True)
			return True
		for idx, col in enumerate(row.find_all('td')):
			col.attrs['data-title'] = columns[idx]
			line[columns[idx]] = self.enhance_table_cell(col)
		key, line = self.enhance_table_line(line)
		if not key:
			key = default_key

		if parseDetails:
			details = self.parse_item_page(line['key'])
			if details is None:
				return False
			#print('Merging for key', key)
			self.db[key] = self.merge_info(line, details)
			#time.sleep(1)
		else:
			self.db[key] = line
		#print('Key', key)
		#print('Line', self.db[key])
		return True



if __name__ == "__main__":
	parser = EtnomonParser()
	parser.parse(0, 'etnomon.json')
	#parser.parse_with_database('clasate.json')
