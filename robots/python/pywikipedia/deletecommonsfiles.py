# -*- coding: utf-8 -*-
import wikipedia, codecs, catlib, pagegenerators, re, urllib

site = wikipedia.getSite()
cat = catlib.Category(site, u"Categorie:Acum la commons")
gen = pagegenerators.CategorizedPageGenerator(cat)
cmsite = wikipedia.getSite('commons','commons')
print cmsite

raport = u"Am şters următoarele pagini:\n"
imgsterse = 0

for page in gen:
    if page.isImage():
	text = page.get()
	ex = re.compile(u"\{\{NowCommons(\|([\w\:\-\_\.\(\)\ ăşţâĂŞȘŢȚÂșșțáöéüí]*))?")
	res = re.findall(ex, text)
 	
 	wikipedia.output(page.title())
 	wikipedia.output("res: " + repr(res))
 	if res:
		if res[0][1]:
			newfname = res[0][1]
		else:
			newfname = page.title()
		
		newlocalfname = newfname.replace(u"File:", u"")
		newlocalfname = newlocalfname.replace(u"Fișier:", u"")
		wikipedia.output(u"New file name:" + newlocalfname)
		cmpage = wikipedia.ImagePage(cmsite, u"File:" + newlocalfname)
		    
		if cmpage.exists() and cmpage.isImage():
		
			"""Intai vedem daca e pusa licenta bine"""
			localfileuploader = page.getLatestUploader()
			cmtext = cmpage.get()
			isDPpersonal = (u"{{PD-user-w|ro|wikipedia|" + localfileuploader[0] + u"}}") in cmtext and (u"{{DP-personal}}" in text or u"{{DP-oferit" in text)
			wikipedia.output("isdppersonal: " + repr(isDPpersonal))
			isGFDL = ((u"{{GFDL-user-w|ro|wikipedia|" + localfileuploader[0] + u"}}") in cmtext or u"{{self|GFDL|" in cmtext) and u"{{GFDL}}" in text
			wikipedia.output("isgfdl: " + repr(isGFDL))
			isCC = (u"{{cc-by" in cmtext.lower()  or u"{{self|cc-by-sa" in cmtext.lower()) and u"{{cc-by" in text.lower()
			wikipedia.output("iscc: " + repr(isCC))
			isCOAcm = (u"{{pd-romaniagov" in cmtext.lower() or u"{{pd-ro-exempt" in cmtext.lower() or u"{{pd-ro-symbol" in cmtext.lower())
			isCOAlc = (u"{{stemă" in text.lower() or u"{{dp-ro" in text.lower())
			isCOA = isCOAcm and isCOAlc
			wikipedia.output("iscoacm: " + repr(isCOAcm))
			wikipedia.output("iscoalc: " + repr(isCOAlc))
			wikipedia.output("iscoa: " + repr(isCOA))
			if isDPpersonal or isGFDL or isCC or isCOA:
				""" Verificam cine se leaga aici
				cmfileurl = cmpage.fileUrl()
				localfileurl = page.fileUrl()
				
				cmds = urllib.urlopen(cmfileurl)
				lcds = urllib.urlopen(localfileurl)
				
				cmdata = cmds.read()
				lcdata = lcds.read()
				
				if cmdata == lcdata:
					wikipedia.output(u"---Fişierele se potrivesc---")
				else:
					wikipedia.output(u"---Fişierele sunt diferite---")

				"""
				changers = page.usingPages()
				for eachpage in changers:
					wikipedia.output(u"Changing in page:")
					wikipedia.output(repr(eachpage))
					eachtext = eachpage.get()
					
					pagetitleraw = page.title()
					pagetitleraw = pagetitleraw[len(u"Fişier:"):]
					pagetitlewithu = pagetitleraw.replace(" ", "_")
					pagetitlewou = pagetitleraw.replace("_", " ")
					
					wikipedia.output(u" \"" + pagetitleraw + u"\" with \"" + newlocalfname + u"\"")
					replacedtext = eachtext.replace(pagetitlewou, newlocalfname)
					replacedtext = replacedtext.replace(pagetitlewithu, newlocalfname)
					print(repr(replacedtext))
					eachpage.put(replacedtext, comment = u"Înlocuit cu copia de la Commons")
					
						
				page.delete(reason="Duplicat al unei imagini de la Commons", prompt=False)
				raport += u"* [[:"
				raport += page.title()
				raport += u"]]\n"
				imgsterse += 1
	else:
		wikipedia.output("Notfound")
raport += u"\nÎn total " + str(imgsterse) + u" pagini şterse."
rappage = wikipedia.Page(site, u"Utilizator:Andrebot/Ştergerea fişierelor copiate la commons/Raport")
#rappage.put(raport, u"Creat raport")