#!/usr/bin/python
# -*- coding: utf-8  -*-

import json
import csv
import codecs
import collections
import sys

def csvToJson( inFile, outFile=None, field=u"Cod", delimiter=","):
    out = None

    with open(inFile, 'r') as csvFile:
        # Note this reads the first line as the keys we can add specific keys with:
        # csv.DictReader( csvFile, fieldnames=<LIST HERE>, restkey=None, restval=None, )
        csvDict = csv.DictReader(csvFile, restkey=None, restval=None, delimiter=delimiter)
        out = collections.OrderedDict({})
        for row in csvDict:
            if field not in row:
                return None
            out.update({row[field]: dict([(key, value) for key, value in row.items()])})

    if outFile and out:
        with open(outFile, 'w') as jsonFile:
            jsonFile.write(json.dumps(out, indent=2))

    return out

def csvToTemplate(inFile, outFile, templateName, pre=None, post=None):
    out = u""
    with open(inFile, 'r') as csvFile:
        csvDict = csv.DictReader(csvFile, restkey=None, restval=None, )
        i = 0
        for row in csvDict:
            i = i + 1
            print(i)
            uniDict = dict([(unicode(key, 'utf-8'), unicode(value, 'utf-8')) for key, value in row.iteritems()])
            if pre:
                out += pre + u"\n"
            out += u"{{" + templateName + "\n"
            for key, value in uniDict.iteritems():
                out += u"| " + key + u" = " + value + u"\n"
            out += u"}}\n"
            if post:
                out += post + u"\n"
        if outFile and out:
            with codecs.open(outFile, 'w+', 'utf8') as File:
                File.write(out)

def unicodeCsvReader(utf8_data, dialect=csv.excel, **kwargs):
    csv_reader = csv.reader(utf8_data, dialect=dialect, **kwargs)
    for row in csv_reader:
        if sys.version_info >= (3,0):
            yield row
        else:
            yield [unicode(cell,'utf-8') for cell in row]


if __name__ == "__main__":
    # csvToJson("other_monument_data.csv", "other_monument_data.json")
    # print csvToJson("other_monument_data.csv")
    csvToTemplate("hackathon20140222/outpLMI-lacase-2014-02-20-final.csv", "biserici_for_robot.txt",
                  "safesubst:Utilizator:Strainu/biserici", u"{{-start-}}", u"{{-stop-}}")
