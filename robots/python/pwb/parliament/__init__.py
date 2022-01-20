#!/usr/bin/python
# -*- coding: utf-8 -*-

months = {
	"ian.": "01",
	"feb.": "02",
	"mar.": "03",
	"apr.": "04",
	"mai":  "05",
	"iun.": "06",
	"iul.": "07",
	"aug.": "08",
	"sep.": "09",
	"oct.": "10",
	"noi.": "11",
	"dec.": "12",
}
months_names = {
	1: "ianuarie",
	2: "februarie",
	3: "martie",
	4: "aprilie",
	5: "mai",
	6: "iunie",
	7: "iulie",
	8: "august",
	9: "septembrie",
	10: "octombrie",
	11: "noiembrie",
	12: "decembrie",
}

def niceDate(date):
	_list = date.split("-")
	if len(_list) < 3:
		return date
	return u" ".join([ "%d" % int(_list[2]), months_names[int(_list[1])], _list[0]]) 

def allCommas(s):
	s = s.replace(u"ţ", u"ț");
	s = s.replace(u"Ţ", u"Ț");
	s = s.replace(u"ş", u"ș");
	s = s.replace(u"Ş", u"Ș");
	return s;

