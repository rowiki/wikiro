#!/usr/bin/python
#-*- coding:utf-8 -*-

config = {
    'properties': {
        u'Denumire': ('', None, 'label'),
        u'Coord': ('P625', False, 'globe-coordinate'),
        u'imagine': ('P18', True, 'commonsMedia'),
        u'hartă': ('P242', False, 'commonsMedia'),
        u'colaj': ('P2716', True, 'commonsMedia'),
        u'video': ('P10', True, 'commonsMedia'),
        u'Țară': ('P17', False, 'wikibase-item'),
        u'Commons': ('P373', False, 'string'),
        u'SIRUTA': ('P843', False, 'string'),
        u'este un/o': ('P31', False, 'wikibase-item'),
        u'subdiviziuni': ('P1383', False, 'wikibase-item'),
        u'localități componente': ('P1383', False, 'wikibase-item'),
        u'fus orar': ('P421', False, 'wikibase-item'),
        u'primar': ('P6', False, 'wikibase-item'),
        u'codp': ('P281', True, 'string'),
        u'SIRUTASUP': ('P131', False, 'wikibase-item'),
        u'ISO3166-2': ('P300', False, 'string'),
        u'populație': ('P1082', False, 'quantity'),
        u'suprafață': ('P2046', False, 'quantity'),
        u'reședință pentru': ('P1376', False, 'wikibase-item'),
        u'site': ('P856', False, 'url'),
        u'stemă': ('P94', False, 'commonsMedia'),
        u'drapel': ('P41', False, 'commonsMedia'),
    },
	'counties': [
		'Alba',
		'Arad',
		'Argeș',
		'Bacău',
		'Bihor',
		'Bistrița-Năsăud',
		'Botoșani',
		'Brașov',
		'Brăila',
		'Buzău',
		'Caraș-Severin',
		'Călărași',
		'Cluj',
		'Constanța',
		'Covasna',
		'Dâmbovița',
		'Dolj',
		'Galați',
		'Giurgiu',
		'Gorj',
		'Harghita',
		'Hunedoara',
		'Ialomița',
		'Iași',
		'Ilfov',
		'Maramureș',
		'Mehedinți',
		'Mureș',
		'Neamț',
		'Olt',
		'Prahova',
		'Satu Mare',
		'Sălaj',
		'Sibiu',
		'Suceava',
		'Teleorman',
		'Timiș',
		'Tulcea',
		'Vaslui',
		'Vâlcea',
		'Vrancea'
	],
    'censuses' : {
        1859: {"q": "Q22704065", "name": u"Recensământul populației din 1859-1860", "month": None, "day": None},
        1860: {"q": "Q22704065", "name": u"Recensământul populației din 1859-1860", "month": None, "day": None}, #same as above
        1899: {"q": "Q22704088", "name": u"Recensământul populației din 1899", "month": None, "day": None},
        1912: {"q": "Q22704095", "name": u"Recensământul General al Populației din 1912", "month": None, "day": None},
        1930: {"q": "Q12739150", "name": u"Recensământul populației din 1859-1860", "month": None, "day": None},
        1941: {"q": "Q22704099", "name": u"Recensământul General al României", "month": None, "day": None},
        1948: {"q": "Q22704103", "name": u"Recensământul populației din ianuarie 1948", "month": 1, "day": None},
        1956: {"q": "Q22704106", "name": u"Recensământul populației din februarie 1956", "month": 2, "day": None},
        1966: {"q": "Q22704111", "name": u"Recensământul populației și locuințelor din martie 1966", "month": 3, "day": None},
        1977: {"q": "Q22704114", "name": u"Recensământul populației și locuințelor din anul 1977", "month": 1, "day": None},
        1992: {"q": "Q22704118", "name": u"Recensământul populației și locuințelor din anul 1992", "month": 1, "day": None},
        2002: {"q": "Q4350762", "name": u"Recensământul populației și locuințelor din anul 2002", "month": 3, "day": None},
        2011: {"q": "Q12181933", "name": u"Recensământul populației și locuințelor din anul 2011", "month": 10, "day": 31},
        2021: {"q": "Q106566382", "name": u"Recensământul populației și locuințelor din anul 2021", "month": 12, "day": 1},
    }
}

all_counties = config['counties'] + ['București']
