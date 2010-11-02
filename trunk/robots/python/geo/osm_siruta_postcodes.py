#!/usr/bin/python
#-*- coding: utf-8 -*-

###########################################################################
## Small script to import Romanian postal codes from SIRUTA to           ##
## OpenStreetMap.                                                        ##
##                                                                       ##
## Copyright Strainu <strainu@strainu.ro> 2010                           ##
##                                                                       ##
## This program is free software: you can redistribute it and/or modify  ##
## it under the terms of the GNU General Public License as published by  ##
## the Free Software Foundation, version 2.                              ##
##                                                                       ##
## This program is distributed in the hope that it will be useful,       ##
## but WITHOUT ANY WARRANTY; without even the implied warranty of        ##
## MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         ##
## GNU General Public License for more details.                          ##
##                                                                       ##
## You should have received a copy of the GNU General Public License     ##
## along with this program.  If not, see <http://www.gnu.org/licenses/>. ##
##                                                                       ##
###########################################################################

import OsmApi, xml.dom.minidom, csv, urllib2, urllib, string, time, sys

class SirutaPostcodes:

    def __init__(self, 
                input_file = "siruta.csv",
                all = False):
        self._passfile = "osmpasswd"
        self._input = input_file
        self._log = "siruta_import.log"
        self._area = "20.2148438,43.6710938,29.8729492,48.2147666" #default to Romania
        self._comment = "Automated postcode import from SIRUTA 2008 (batch %d). Source: http://www.insse.ro/cms/rw/pages/siruta.ro.do "
        self._batch = 0
        self._record_count = 0
        self._always = all
        
    def log(self, header, string):
        f = open(self._log, 'a+')
        f.write(header.encode( "utf-8" ) + string.encode( "utf-8" ))
        print header.encode( "utf-8" ) + string.encode( "utf-8" )
        f.write("\n")
        f.close()
        
    def logi(self, string):
        self.log("* Info (%s): " % time.strftime("%Y-%m-%d %H:%M:%S"), string)
        
    def loge(self, string):
        self.log("* Error (%s): " % time.strftime("%Y-%m-%d %H:%M:%S"), string)
        
    def logd(self, string):
        self.log("* Debug (%s): " % time.strftime("%Y-%m-%d %H:%M:%S"), string)
        
    def pageText(self, url):
        """ Function to load HTML text of a URL """
        try:
            request = urllib2.Request(url)
            request.add_header("User-Agent", "siruta_postcodes.py 1.0")
            response = urllib2.urlopen(request)
            text = response.read()
            response.close()
            # When you load to many users, urllib2 can give this error.
        except urllib2.HTTPError, urllib2.URLError:
            loge(u"Server or connection error. Pausing for 10 seconds... " + time.strftime("%d %b %Y %H:%M:%S (UTC)", time.gmtime()) )
            response.close()
            time.sleep(10)
            return pageText(url)
        return text
        
    def searchSirutaCode(self, code, name):
        urlHead = "http://osmxapi.hypercube.telascience.org/api/0.6/node[siruta:code="
        url = urlHead + code + "][bbox=" + self._area + "]"
        xmlText = self.pageText(url)

        try:
            document = xml.dom.minidom.parseString(xmlText)
            nodes = document.getElementsByTagName("node")
            if nodes.length <> 1:
                self.loge("No node or more than 1 node were returned from OSM for code %s (%s). Please check the output manually." % (code, name))
                self.logd(str(xmlText))
                return 0
            invalid_place = 0
            has_correct_name = 0
            has_postcode = 0   
            #import pdb;
            #pdb.set_trace()
            for tag in nodes[0].childNodes:
                if tag.nodeType != tag.ELEMENT_NODE:
                    continue
                if tag.tagName != "tag" or tag.hasAttribute('k') == False or tag.hasAttribute('v') == False:
                    continue
                if tag.getAttribute('k') == "place":
                    type = tag.getAttribute('v')
                    if type <> "town" and type <> "village" and type <> "hamlet":
                        #Towns generally have round codes. This is not 100% sure, but better be safe than sorry
                        #This is not an error, as we might have cities in the data, but we just want to skip them
                        self.loge("Invalid place type for %s:  %s" % (name, type))
                        invalid_place = 1
                        break
                if tag.getAttribute('k') == "name":
                    xml_name = string.upper(tag.getAttribute('v'))
                    xml_name = string.replace(xml_name, u'Ă', u'A')
                    xml_name = string.replace(xml_name, u'Â', u'A')
                    xml_name = string.replace(xml_name, u'Î', u'I')
                    xml_name = string.replace(xml_name, u'Ş', u'S')
                    xml_name = string.replace(xml_name, u'Ș', u'S')
                    xml_name = string.replace(xml_name, u'Ţ', u'T')
                    xml_name = string.replace(xml_name, u'Ț', u'T')
                    xml_name = string.replace(xml_name, u'—', u'-')
                    xml_name = string.replace(xml_name, u'–', u'-')
                    if name <> xml_name:
                        #Name mismatch, error
                        self.loge("Name mismatch: %s <> %s" % (tag.getAttribute('v'), name))
                        break
                    else:
                        has_correct_name = 1
                if tag.getAttribute('k') == "postal_code":
                    self.loge("Place %s already has postcode %s" % (name, tag.getAttribute('v')))
                    has_postcode = 1
                    break
            
            if has_postcode == 1 or has_correct_name == 0 or invalid_place == 1:

                return 0
            else:
                self.logd("Return %s for place %s." % (nodes[0].getAttribute('id'), name))
                return nodes[0].getAttribute('id')
                    
        except Exception as inst:
            self.loge("Generic error: " + str(inst))
            return 0
        
    def addPostcode(self, api, node_id, postcode, source):
        node = api.NodeGet(node_id)
        tags = node["tag"]
        if "postal_code" in tags:
            #This should be intercepted in searchSirutaCode, 
            #but xapi could be out of sync with the main API, so we must check again
            self.logi(u"Place %s already has postcode %s. Perhaps we've already visited it?" % (tags["name"], tags["postal_code"]))
            return 
        tags["postal_code"] = unicode(postcode, "utf8")
        tags["postal_code:source"] = unicode(source, "utf8")
        node["tag"] = tags
        self.logi("Ready to add postal_code %s to %s" % (tags["postal_code"], tags["name"]))
        #self.logd(str(node))
        update = False
        if self._always == False:
            print "Do you want to update the record? ([y]es/[n]o/[a]llways)"
            line = sys.stdin.readline().strip()
            if line == 'y':
                update = True
            elif line == 'a':
                update = True
                self._always = True
        else:
            update = True
        
        if update == True:
            api.NodeUpdate(node)
            self._record_count += 1
            self.logi("Updated record %d" % self._record_count)
            if self._record_count % 1000 == 0:
                api.ChangesetClose()
                self._batch += 1
                api.ChangesetCreate({u"comment": self._comment % self._batch})

    def parseCsv(self):
        self._batch = 23
        self._record_count = 0
        reader = csv.reader(open(self._input, "r"))
        api = OsmApi.OsmApi(api="api.openstreetmap.org", passwordfile = self._passfile, debug = True)
        api.ChangesetCreate({u"comment": self._comment % self._batch})
        
        try:
            for row in reader:
                #row = [code, name, postcode, source]
                node_id = self.searchSirutaCode(row[0], row[1])
                if node_id == 0:
                    self.logi("Info about siruta code %s contained some kind of error. Please check the log for details!" % row[0])
                    continue
                self.addPostcode(api, node_id, row[2], row[3])        
            api.ChangesetClose()
        except csv.Error, e:
            loge('file %s, line %d: %s' % (self._input, reader.line_num, e))
            return

if __name__ == "__main__":
    acceptall = False
    for args in sys.argv[1:]:
        if arg == "-always":
            acceptall = True
    robot = SirutaPostcodes(all = acceptall)
    robot.parseCsv()