#!/usr/bin/python
# -*- coding: utf-8  -*-

import json, csv, codecs

def coerce(text):
    if text:
        return unicode(text, 'utf-8')
    else:
        return text

def csvToJson( inFile, outFile=None, field=u"Cod"):
    out = None;

    with open( inFile, 'r') as csvFile:
        #Note this reads the first line as the keys we can add specific keys with:
        #csv.DictReader( csvFile, fieldnames=<LIST HERE>, restkey=None, restval=None, )
        csvDict = csv.DictReader( csvFile, restkey=None, restval=None, delimiter='|')
        out = {}
        for row in csvDict:
            if field not in row:
                return None
            if row[field] <> "":
                _hash = row[field]
            else:
                _hash = row["descrierea"][:50]
            #print _hash
            #out.update({_hash: dict([(key, coerce(value)) for key, value in row.iteritems()])})
            out.update({_hash: row})

    if outFile and out:
        with open( outFile, 'w' ) as jsonFile:
            jsonFile.write( json.dumps( out, indent=2) );

    return out

def fun( file1, file2):
    out = None;
    json1 = csvToJson(file1, field=u"adresa")
    #print json.dumps(json1, indent=2)
    f = open("pre_" + file2, "w+")
    csv3 = csv.writer(f)
    with open( file2, 'r') as csvFile:
        csv2 = csv.reader(csvFile)
        for row in csv2:
            if row[3] <> "":
                _hash = row[3].replace("„", "\"").replace("”", "\"")
            else:
                _hash = row[5][:50].replace("„", "\"").replace("”", "\"")
            if _hash in json1:
                row[5] = json1[_hash]['descrierea']
                row.append(json1[_hash]['identificator'])
            else:
                print "no match" + _hash
                row.append("")
            print len(row)
            csv3.writerow(row)
    f.close()

if __name__ == "__main__":
    fun("/tmp/inp_orig.csv", "muzeu_cu_caractere_pt_articole.csv")
