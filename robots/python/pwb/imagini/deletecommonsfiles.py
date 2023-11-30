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
	ex = re.compile(u"\{\{(N|n)owCommons(\|([\w\:\-\_–\.\,\\\"\(\)\&\'\'§„”“\ żóéáìüäàăşţőâÉÎĂŞȘŢȚÂșțîáöéüíПийднкторуябБльавВерс]*))?")
	ex2 = re.compile(u"\{\{(A|a)cumCommons(\|([\w\:\-\_–\.\,\\\"\(\)\&\'\'§„”“\ żóéáìüäàăşţőâÉÎĂŞȘŢȚÂșțîáöéüíПийднкторуябБльавВерс]*))?")
	res = re.findall(ex, text)
	
	if not res:
		res = re.findall(ex2, text)

	wikipedia.output(page.title())
	wikipedia.output("res: " + repr(res))
	if res:
		if res[0][1]:
			newfname = res[0][2]
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
			commonsDPpersonal = ((u"{{pd-user-w|ro|wikipedia|" + localfileuploader[0].lower() + u"}}") in cmtext.lower() or u"{{PD-self" in cmtext or u"{{PD-user-ro|" + localfileuploader[0] in cmtext)
			lcDPPersonal = (u"{{DP-personal}}" in text or u"{{DP-oferit" in text or u"{{DP-utilizator}}" in text)
			lcGFDL = u"{{GFDL" in text
			cmGFDL = ((u"{{GFDL-user-w|ro|wikipedia|" + localfileuploader[0] + u"}}") in cmtext or u"{{self|gfdl" in cmtext.lower() or u"{{GFDL" in cmtext or u"{{picswiss" in cmtext.lower())

			lcCC = u"{{cc-by" in text.lower() or u"{{creative commons" in text.lower() or u"{{cc-sa" in text.lower()
			cmCC = (u"{{cc-by" in cmtext.lower() or u"{{self|cc-by" in cmtext.lower() or u"{{attribution" in cmtext.lower() or u"{{self|cc-by-" in cmtext.lower() or u"{{cc-sa" in cmtext.lower() or u"{{cc0" in cmtext.lower())
			
			cmCOA = (u"{{pd-romaniagov" in cmtext.lower() or u"{{pd-ro-exempt" in cmtext.lower() or u"{{pd-ro-symbol" in cmtext.lower() or u"{{PD-money-Romania}}" in cmtext)
			localCOA = (u"{{stemă" in text.lower() or u"{{dp-ro" in text.lower())
			
			localUC = u"{{utilizare cinstită" in text.lower() or u"{{Carte-copertă" in text.lower() or u"{{utilizarecinstită" in text.lower() or u"{{timbre" in text.lower() or u"{{stemă" in text.lower() or "u{{logo" in text.lower()
			
			cmEuroCoin = u"{{money-eu" in cmtext.lower() or u"{{Euro coin common face}}" in cmtext
			localPDGovUS = u"{{dp-guvsua" in text.lower() or u"{{pd-guvsua" in text.lower()
			cmPDGovUS = u"{{pd-usgov" in cmtext.lower();
			
			localPD = (u"{{pd}}" in text.lower() or u"{{dp}}" in text.lower() or u"{{dp-inapt" in text.lower() or u"{{fără drepturi" in text.lower() or u"{{dp-legătură" in text.lower() or u"{{dp-utilizator" in text.lower());
			cmPD = u"{{pd-" in cmtext.lower() or u"{{no rights reserved" in cmtext.lower() or u"{{copyrighted free use" in cmtext.lower()
			
			localPDOld = (u"{{dp-artă" in text.lower()) or u"{{dp-70" in text.lower()
			cmPDOld = (u"{{pd-art" in cmtext.lower() or u"{{pd-old" in cmtext.lower()) or (u"{{PD-EU-no author disclosure}}" in cmtext or u"{{pd-old" in cmtext.lower())

			cmPDCanada = u"{{PD-Canada" in cmtext
			lcPDCanada = u"{{DP-Canada" in text 
			cmPDGermania = u"{{PD-Coa-Germany" in cmtext
			cmPDTransnistria = u"{{PD-PMR-exempt" in cmtext
			lcPDRusia = u"{{DP-Rusia" in text
			cmPDRusia = u"{{PD-RU-exempt" in cmtext
			cmCOA = u"{{wappenrecht" in cmtext.lower()
			cmPDGermania = u"{{Coa-Germany-b1945".lower() in cmtext.lower() or u"{{Flag-Germany-b1945".lower() in cmtext.lower()
			
			isOK = False
			isOK = isOK or cmEuroCoin
			isOK = isOK or (localPDGovUS and cmPDGovUS)
			isOK = isOK or cmPDGermania
			isOK = isOK or (lcPDCanada and cmPDCanada)
			isOK = isOK or cmPDTransnistria
			isOK = isOK or (localPD and cmPD)
			
			isBothFree = (lcGFDL or lcCC or lcDPPersonal) and (cmGFDL or cmCC or cmPD)
			isOK = isOK or isBothFree
			isCmFree = cmGFDL or cmCC or cmPD
			isOK = isOK or isCmFree
			isOK = isOK or (localCOA and cmCOA)
			isOK = isOK or (lcDPPersonal and commonsDPpersonal)
			isOK = isOK or (cmPDOld and localPDOld)
			isOK = isOK or (lcPDRusia and cmPDRusia)
			isOK = isOK or cmCOA
			isOK = isOK or cmPDGermania
			
			if isOK:
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
					if eachpage.isRedirectPage():
						wikipedia.output(u"Este redirect!")
						continue;
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
					
				deletereason = "Duplicat al unei imagini de la Commons: [[:commons:File:" + newlocalfname + "]]"
				page.delete(reason=deletereason , prompt=False)
				raport += u"* [[:"
				raport += page.title()
				raport += u"]]\n"
				imgsterse += 1
	else:
		wikipedia.output("Notfound")
raport += u"\nÎn total " + str(imgsterse) + u" pagini şterse."
rappage = wikipedia.Page(site, u"Utilizator:Andrebot/Ştergerea fişierelor copiate la commons/Raport")
#rappage.put(raport, u"Creat raport")
