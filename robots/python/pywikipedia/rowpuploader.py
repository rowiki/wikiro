# -*- coding: utf-8 -*-
import os,sys,wikipedia, codecs, catlib, pagegenerators, upload, datetime, re

site = wikipedia.getSite()
cmsite = wikipedia.getSite("commons", "commons")
pattern = unicode(sys.argv[1], 'utf-8')
nfpattern = sys.argv[2]
categ = unicode(sys.argv[3], 'utf-8')
startno = int(sys.argv[4])
endno = int(sys.argv[5])
if len(sys.argv) >= 7:
        extranamefile = sys.argv[6]
else:
        extranamefile = None

crtfileindex = 0
extranames = []
if extranamefile <> None and os.path.exists(extranamefile):
	extranames = open(extranamefile).readlines()

for i in range(startno, endno + 1): 
	origfilename = pattern.replace("_number_", repr(i))
	filename = u"Fișier:" + origfilename
	wikipedia.output(filename)
	page = wikipedia.ImagePage(site, filename)
	if (not page.exists()):
		wikipedia.output('page not found')
		continue

	url = page.fileUrl()
	#uo = wikipedia.MyURLopener()
	#datasrc = uo.open(url)
	pagetext = u""
	pagetext = page.get()
	print repr(pagetext)
	ex = re.compile(u"\{\{([\w \-\:\.]+[\s]*)((\|(([\w \-]+[\s]*=)?[\s]*[\w \-\„\”\{\}\[\]\(\)\u2018\,\.\?]*[\s]*)[\s]*)*)\}\}", re.U)
	res = re.findall(ex, pagetext)
	if res:
		print "Match:" + repr(res)
	if res[0][0].startswith(u"Informații") or res[0][0].startswith(u"Informaţii"):
		params = re.split(u"\|", res[0][1], re.U)
		for param in params:
			pelems = re.split(u"=", param, re.U)
			print repr(pelems)
			if pelems[0].startswith("Descriere"):
				origdescr = pelems[1].lstrip().rstrip()
			elif pelems[0].startswith("Sursa"):
				origsrc = pelems[1].lstrip().rstrip()
			elif pelems[0].startswith("Data"):
				origdate = pelems[1].lstrip().rstrip()
			elif pelems[0].startswith("Autor"):
				origauthor = pelems[1].lstrip().rstrip()
	versionhistory = page.getVersionHistory(reverseOrder=True, revCount=1)
	origuploader = versionhistory[0][2]

	months = [u'ianuarie', u'februarie', u'martie', u'aprilie',  u'mai',  u'iunie',  u'iulie',  u'august',  u'septembrie',  u'octombrie',  u'noiembrie',  u'decembrie']
	dateparts = origdate.split()
	year = int(dateparts.pop())
	month = months.index(dateparts.pop()) + 1
	if (len(dateparts) > 0):
		day = dateparts.pop()
		actualdate = datetime.date(year, month, day)
		newdate = actualdate.strftime('%Y-%m-%d')
	else:
		actualdate = datetime.date(year, month, 1)
		newdate = actualdate.strftime('%Y-%m')
	
	description = u"=={{int:filedesc}}==\n"
	description += u"{{Information\n"
	description += u"|Description={{ro|" + origdescr + u"}}\n"
	description += u"|Date=" + newdate + u"\n"
	if origauthor == origuploader:
		description += u"|Source=Uploaded to ro.wikipedia by the author, as [[:ro:Fişier:" + origfilename + u"|]]\n"
	else:
		description += u"|Source=Uploaded to ro.wikipedia by the [[:ro:Utilizator:" + origuploader + u"|]], as [[:ro:Fişier:" + origfilename + u"|]]\n" 
	if origauthor == origuploader:
		description += u"|Author=[[:ro:Utilizator:" + origuploader + "|]] at ro.wikipedia\n"
	else:
		description += u"|Author=" + origauthor + u", uploaded by [[:ro:Utilizator:" + origuploader + "|]] at ro.wikipedia\n"
	description += u"|Permission={{self|cc-by-sa-2.5|author=" + origauthor + u"}}\n"
	description += u"}}\n"
	description += u"==Original history==\n" + page.getFileVersionHistoryTable()
	description += u"[[Category:" + categ + "]]"

	wikipedia.output(description);

	if crtfileindex < len(extranames):
		alteredpattern = nfpattern.replace("_number_", "_" + extranames[crtfileindex].strip() + "__number_")
	else:
		alteredpattern = nfpattern
	newname = alteredpattern.replace("_number_", repr(i))
	newname = newname.replace(" ","_").replace("__", "_")
	crtfileindex = crtfileindex + 1
	uploader = upload.UploadRobot(useFilename = newname, description = description, targetSite = cmsite, url = url, verifyDescription = False, keepFilename = True)
	uploadresult = uploader.run()
	
	if uploadresult <> None:
		wikipedia.output("File uploaded as " + uploadresult)
		pagetext = pagetext + u"{{NowCommons|File:" + newname + u"}}";
		page.put(pagetext, comment = u"mutat la Commons")
	else:
		wikipedia.output("File not uploaded")