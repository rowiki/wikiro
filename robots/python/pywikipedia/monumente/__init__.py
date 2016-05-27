#!/usr/bin/python
# -*- coding: utf-8  -*-

#
# (C) Strainu, 2012-2015
#
# Distributed under the terms of the GPLv2 license.
#
#


def filterOne(contents, countryconfig):
	'''
	Any kind of filtering of the data of a monument should happen in this functions
	'''
	#if countryconfig.get('idField') == 'ID': #WLEMD
	#	contents[countryconfig.get('idField')] = "-".join([u"MD", contents['raion'], contents['tip'], "%03d" % int(contents['ID'])])
	return contents
