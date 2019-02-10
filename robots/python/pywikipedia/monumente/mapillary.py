#!/usr/bin/python
# -*- coding: utf-8  -*-

'''
This script tries to obtain images of Historic Monuments from Mapillary
'''
import json
import requests
import sys
import concurrent.futures

sys.path.append("wikiro/robots/python/pywikipedia")

import monumente


def buildImageUrls(imageList):
    ret = []
    url = "https://d1cuyjsrcm0gby.cloudfront.net/{0}/thumb-2048.jpg"

    for image_data in imageList:
        ret.append(url.format(image_data["properties"]["key"]))
    return ret

def getImageList(monument):
    url = "https://a.mapillary.com/v3/images?client_id={api_key}&lookat={lon},{lat}&closeto={lon},{lat}"
    url = url.format(api_key=monumente.mapillary_key, lat=monument["Lat"], lon=monument["Lon"])
    #print(url)
    r = requests.get(url)
    return r.text

def getData(monument):
    if monument["Imagine"] != "":
        return None,None
    if monument["Lat"] == "":
        if monument["OsmLat"] == "":
            return None,None
        else:
            monument["Lat"] = monument["OsmLat"]
            monument["Lon"] = monument["OsmLon"]

    data = json.loads(getImageList(monument))
    if len(data["features"]) == 0:
        return None,None
    #print(data["features"])
    #sleep(1)
    return monument["Cod"], buildImageUrls(data["features"])

def readJson(filename, what):
    try:
        f = open(filename, "r+")
        print("Reading " + what + " file...")
        db = json.load(f)
        print("...done")
        f.close()
        return db
    except IOError:
        pywikibot.error("Failed to read " + filename + ". Trying to do without it.")
        return {}

def main():
    db_json = readJson("_".join(filter(None, ["ro", "lmi", "db.json"])), "database")
    res = {}
    executor = concurrent.futures.ThreadPoolExecutor(10)
    future_list = [executor.submit(getData, (monument)) for monument in db_json]
    for future in concurrent.futures.as_completed(future_list):
        try:
            c,l = future.result()
            if c:
                print(c)
                res[c] = l
        except:
            pass

    f = open("mapillary_data.json", "w+")
    json.dump(res, f, indent=2)
    f.close()

if __name__== "__main__":
    main()
