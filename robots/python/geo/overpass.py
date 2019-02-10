#!/usr/bin/python
# -*- coding: utf-8  -*-

import os
import http
import json
from urllib.parse import urlencode, quote_plus

class OverpassRequest:
	def __init__(self, api = "www.overpass-api.de", base="api", poly=None, filters=None, output=None):
		self.base = os.path.join("/", base, "interpreter")
		self._api = api
		self._poly = poly
		print (api)
		print (base)
		print (poly)
		print (filters)
		print (output)
		self._filters = filters
		self._output = output
		
	def buildRequest(self, req_type, poly, filters, output):
		req = "[out:" + output + "];"
		if type(poly) == str or type(poly) == unicode:
			p = poly
		else:
			raise NotImplementedError 
		vars = u"("
		for filts in filters:
			vars += u"." + filts + u";"
			req += u"\narea[name=\"" + p + "\"]; " + req_type + u"(area)"
			for filt in filters[filts]:
				req += u"[\"" + filt + u"\""
				f = filters[filts][filt]
				if f != None:
					if f[1] == False:#this is a negation
						req += u"!"
					req += u"~" + u"\"" + f[0] + u"\""
				req += u"]"
			req += u"->." + filts + u";"
		req += u"\n" + vars + u");"
		req += u"\nout body;"
		print (req)
		return req.encode("utf8")

	def makeRequest(self, req=None):
		if req == None:
			return None
		http_obj = http.client.HTTPSConnection(self._api)
		params = urlencode({'@data': req})
		headers = {"Content-type": "application/x-www-form-urlencoded",
				"Accept": "text/plain",
				"User-Agent": "Strainubot"}
		http_obj.request("POST", self.base, req, headers)
		response = http_obj.getresponse()
		print(response.status)
		if response.status == 200:
			ret = response.read()
			http_obj.close()
			return ret
		else:
			http_obj.close()
			return None
		
	def fetchNode(self, poly=None, filters=None, output=None):
		if poly == None:
			poly = self._poly
		if filters == None:
			filters = self._filters
		if output == None:
			output = self._output
		req = self.buildRequest("node", poly, filters, output)
		return self.makeRequest(req)


	def fetchWay(self, poly=None, filters=None, output=None):
		if poly == None:
			poly = self._poly
		if filters == None:
			filters = self._filters
		if output == None:
			output = self._output
		req = self.buildRequest("way", poly, filters, output)
		return self.makeRequest(req)
