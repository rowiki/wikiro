# -*- coding: utf-8 -*-
import pywikibot
from pywikibot import pagegenerators

site = pywikibot.Site()
cat = pywikibot.Category(site, u"Categorie:Răspunsuri imagini")
gen = pagegenerators.CategorizedPageGenerator(cat)

#get all outstanding requests
reqs = []
for page in gen:
  reqs.append(page.title(with_ns=False))

#if there are any
if len(reqs) > 0:
  #get all subscribers
  subscriberspage = pywikibot.Page(site, u"Utilizator:Andrebot/Abonați newsletter sysop")
  subscriberlinks = subscriberspage.linkedPages()
  for subscribername in subscriberlinks:
    pywikibot.output(u"Subscriber: " + subscribername.title(with_ns=False))
    subscribertalkpage = pywikibot.Page(site, u"Discuție Utilizator:" + subscribername.title(with_ns=False))
    #grab the subscriber's page
    tptext = subscribertalkpage.get()

    # check which users he doesn't know of
    newrequests = []
    for req in reqs:
      if tptext.find(u"Strainubot.Newsletter.Notificat:" + req) < 0:
        newrequests.append(req)
      else:
        pywikibot.output(subscribername.title(with_ns=False) + u" already knows about " + req)
    if len(newrequests) > 0:
        pywikibot.output(u"Notifying " + subscribername.title(with_ns=False) + " about " + repr(newrequests))
        tptext = tptext + u"\n" + u"== Răspunsuri avertizări imagini ==\n"
        if len(newrequests) == 1:
            tptext = tptext + u"Utilizatorul [[Discuție Utilizator:" + newrequests[0] + u"|" + newrequests[0] + u"]] a lăsat un răspuns la un mesaj de avertizare despre o imagine."
            tptext = tptext + u"<!--Strainubot.Newsletter.Notificat:" + newrequests[0] + u"-->"
        else:
            tptext = tptext + u"Utilizatorii (" + str(len(reqs)) + u" la număr):"
        for req in newrequests:
            tptext = tptext + u"\n* [[Discuție Utilizator:" + req + u"|" + req + u"]] <!--Strainubot.Newsletter.Notificat:" + req + u"-->"
            tptext = tptext + u"\nau lăsat un răspuns la un mesaj de avertizare despre o imagine."
        tptext = tptext + u" Încărcarea imaginilor și drepturile de autor sunt subiecte sensibile. Este important ca toate neclaritățile cu privire la avertizări să fie lămurite pentru a evita apariția unor nemulțumiri ce pot fi evitate."
        tptext = tptext + u"\n\n<small>Notă: Ați primit acest mesaj automat deoarece sunteți înscris(ă) pe lista de la pagina [[Utilizator:Andrebot/Abonați newsletter sysop]]. Pentru a vă dezabona, ștergeți-vă numele din acea listă."
        tptext = tptext + u"Puteți șterge acest mesaj după rezolvarea cererilor. Dacă o faceți înainte de rezolvarea cererilor, veți primi notificarea din nou până când cererea va fi rezolvată.</small>"
        tptext = tptext + u"&nbsp;&ndash;~~~~"

        subscribertalkpage.put(tptext, minorEdit=False, summary = u"Robot: notificare răspunsuri la avertizări pentru imagini")
    else:
        pywikibot.output(subscribername.title(with_ns=False) + u" already informed")
else:
   pywikibot.output(u"Nothing to do!")
