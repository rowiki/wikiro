# -*- coding: utf-8 -*-
import wikipedia,catlib,pagegenerators

site = wikipedia.getSite()
cat = catlib.Category(site, u"Categorie:Cereri de deblocare")
gen = pagegenerators.CategorizedPageGenerator(cat)

#get all outstanding requests
reqs = []
for page in gen:
  reqs.append(page.titleWithoutNamespace())
 
#if there are any
if len(reqs) > 0:
  #get all subscribers
  subscriberspage = wikipedia.Page(site, u"Utilizator:Andrebot/Abonați newsletter sysop")
  subscriberlinks = subscriberspage.linkedPages()
  for subscribername in subscriberlinks:
    wikipedia.output(u"Subscriber: " + subscribername.titleWithoutNamespace())
    subscribertalkpage = wikipedia.Page(site, u"Discuție Utilizator:" + subscribername.titleWithoutNamespace())
    #grab the subscriber's page
    tptext = subscribertalkpage.get()
    
    # check which users he doesn't know of
    newrequests = []
    for req in reqs:
      if tptext.find(u"Andrebot.Newsletter.Notificat:" + req) < 0:
	newrequests.append(req)
      else:
	wikipedia.output(subscribername.titleWithoutNamespace() + u" already knows about " + req)
    if len(newrequests) > 0:
      wikipedia.output(u"Notifying " + subscribername.titleWithoutNamespace() + " about " + repr(newrequests))
      tptext = tptext + u"\n" + u"== Cereri de deblocare ==\n"
      if len(newrequests) == 1:
	tptext = tptext + u"Utilizatorul [[Discuție Utilizator:" + newrequests[0] + u"|" + newrequests[0] + u"]] a cerut deblocarea."
	tptext = tptext + u"<!--Andrebot.Newsletter.Notificat:" + newrequests[0] + u"-->"
      else:
	tptext = tptext + u"Utilizatorii (" + str(len(reqs)) + u" la număr):"
	for req in newrequests:
	  tptext = tptext + u"\n* [[Discuție Utilizator:" + req + u"|" + req + u"]] <!--Andrebot.Newsletter.Notificat:" + req + u"-->"
	tptext = tptext + u"\nau cerut deblocarea."
      tptext = tptext + u"\n\n<small>Notă: Ați primit acest mesaj automat deoarece sunteți înscris(ă) pe lista de la pagina [[Utilizator:Andrebot/Abonați newsletter sysop]]. Pentru a vă dezabona, ștergeți-vă numele din acea listă."
      tptext = tptext + u"Puteți șterge acest mesaj după rezolvarea cererilor. Dacă o faceți înainte de rezolvarea cererilor, veți primi notificarea din nou până când cererea va fi rezolvată.</small>"
      tptext = tptext + u"&nbsp;&ndash;~~~~"
    
      subscribertalkpage.put(tptext, minorEdit=False, comment = u"Robot: notificare cereri de deblocare")
    else:
      wikipedia.output(subscribername.titleWithoutNamespace() + u" already informed")