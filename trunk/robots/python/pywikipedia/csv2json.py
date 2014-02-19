#!/usr/bin/python

import json, csv, codecs

def csvToJson( inFile, outFile=None):
    out = None;

    with open( inFile, 'r') as csvFile:
        #Note this reads the first line as the keys we can add specific keys with:
        #csv.DictReader( csvFile, fieldnames=<LIST HERE>, restkey=None, restval=None, )
        csvDict = csv.DictReader( csvFile, restkey=None, restval=None, )
        out = {}
        for row in csvDict:
            if u"Cod" not in row:
                return None
            out.update({row["Cod"]: dict([(key, unicode(value, 'utf-8')) for key, value in row.iteritems()])})

    if outFile and out:
        with open( outFile, 'w' ) as jsonFile:
            jsonFile.write( json.dumps( out, indent=2) );

    return out


if __name__ == "__main__":
	csvToJson("other_monument_data.csv", "other_monument_data.json")
	print csvToJson("other_monument_data.csv")
