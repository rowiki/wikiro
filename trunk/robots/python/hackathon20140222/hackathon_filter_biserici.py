#!/usr/bin/python
# -*- coding: utf-8  -*-

import json, csv, codecs
import sys

sys.path.append("..")
import csvUtils

if __name__ == "__main__":
	f = open("../lmi_db.json", "r+")
	db = json.load(f)
	#print json.dumps(db, indent=2)
	f.close();
	
	json = csvUtils.csvToJson("outpLMI-lacase-2014-02-20-final.csv", field="codLMI")
	#print json
	
	with open( "biserici_pt_upload.csv", "w+") as csvFile:
		wr = csv.writer(csvFile)
		i=0
		for monument in db:
			print monument["Denumire"]
			if monument["Cod"] in json and monument["Denumire"].find("[[") == -1:
				print type(json[monument["Cod"]].values()[0])
				wr.writerow(json[monument["Cod"]].values())
				



