# -*- coding: utf-8 -*-
import pywikibot
from pywikibot import page,pagegenerators

site = pywikibot.Site()
cat = pywikibot.Category(site, u"Categorie:Cereri de deblocare")
gen = pagegenerators.CategorizedPageGenerator(cat)

#get all outstanding requests
reqs = []
for eachpage in gen:
  reqs.append(eachpage.title(with_ns=False))
 
#if there are any
if len(reqs) > 0:
  #get all subscribers
  subscriberspage = page.Page(site, u"Utilizator:Andrebot/Abonați newsletter sysop")
  subscriberlinks = subscriberspage.linkedPages()
  for subscribername in subscriberlinks:
    pywikibot.output(u"Subscriber: " + subscribername.title(with_ns=False))
    subscribertalkpage = page.Page(site, u"Discuție Utilizator:" + subscribername.title(with_ns=False))
    #grab the subscriber's page
    tptext = subscribertalkpage.get()
    
    # check which users he doesn't know of
    newrequests = []
    for req in reqs:
      if tptext.find(u"Andrebot.Newsletter.Notificat:" + req) < 0:
        newrequests.append(req)
      else:
        pywikibot.output(subscribername.title(with_ns=False) + u" already knows about " + req)
    if len(newrequests) > 0:
      pywikibot.output(u"Notifying " + subscribername.title(with_ns=False) + " about " + repr(newrequests))
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
    
      subscribertalkpage.put(tptext, minor=False, summary = u"Robot: notificare cereri de deblocare")
    else:
      pywikibot.output(subscribername.title(with_ns=False) + u" already informed")
