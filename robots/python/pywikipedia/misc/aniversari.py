#!/usr/bin/python

import pywikibot

months = [
"ianuarie",
"februarie",
"martie",
"aprilie",
"mai",
"iunie",
"iulie",
"august",
"septembrie",
"octombrie",
"noiembrie",
"decembrie"
]

events = {
	"Nașteri": [2, "P569"],
	"Decese": [3, "P570"],
}

MULTIPLE_DATE_PENALTY = -3
MULTIPLE_SOURCES_BONUS = 1


def treat(day, month, event):
    title = str(day) + " " + month + "#" + event
    page = pywikibot.Page(pywikibot.getSite(), title)
    if not page.exists():
        return ""
    print(title)
    ret = ""
    page.site.loadrevisions(page=page, content=True,section=events[event][0])
    text = page.get()
    for link in page.site.pagelinks(page, namespaces=[0], follow_redirects=True):
        if not link.exists():
            continue
        if link.title() not in text:
            continue
        follow = False
        try:
            year = int(link.title())
        except:
            follow = True
        if not follow:
            continue
        try:
            item = link.data_item()
        except:
            continue
        if "P31" not in item.claims:
            continue
        follow = False
        for claim in item.claims["P31"]:
            if claim.getTarget().title() == "Q5":
                follow = True
                break
        if not follow:
            #print("Not a person: %s" % link)
            continue
        score = 0
        pno = events[event][1]
        #print(item)
        if pno not in item.claims:
            r = "|- style=\"background-color:#ffff88\"\n|  %s || [[%s]] || [[%s]] || %d %s || [[:d:%s|%s]] || [fără dată] || 0\n" % (event, link.title(), title, day, month, item.title(), item.title())
            ret += r
            continue
        preferred = None
        if len(item.claims[pno]) > 1:
            score += MULTIPLE_DATE_PENALTY * (len(item.claims[pno]) - 1)
            for c in item.claims[pno]:
                if preferred != None and c.getRank() == "preferred":
                    print("Impossible to extract reliable data for %s (wrong rank)" % link)
                    continue
                if c.getRank() == "preferred":
                    preferred = c
        else:
            preferred = item.claims[pno][0]
        if preferred == None:
            r = "|- style=\"background-color:#88ffff\"\n|  %s || [[%s]] || [[%s]] || %d %s || [[:d:%s|%s]] || [date multiple] || %d\n" % (event, link.title(), title, day, month, item.title(), item.title(), score)
            ret += r
            continue
        if preferred.getTarget() == None:
            print("Impossible to extract reliable data for %s (wrong date)" % link)
            continue
        date = preferred.getTarget()
        sources = preferred.getSources()
        score += MULTIPLE_SOURCES_BONUS * len(sources)
        if date.day == 0 or date.month == 0:
           d = date.day or "[fără zi]"
           d = str(d)
           if date.month:
               m = months[date.month -1]
           else:
               m = "[fără lună]"
           r = "|- style=\"background-color:#ffff88\"\n|  %s || [[%s]] || [[%s]] || %d %s || [[:d:%s#%s|%s]] || %s %s %d || %d\n" % (event, link.title(), title, day, month, item.title(), pno, item.title(), m, d, date.year, score)
           ret += r
           continue
        if date.day != day or \
           date.month != months.index(month) + 1:
            #print(date.month)
            r = "|- style=\"background-color:#ff8888\"\n|  %s || [[%s]] || [[%s]] || %d %s || [[:d:%s#%s|%s]] || %d %s || %d\n" % (event, link.title(), title, day, month, item.title(), pno, item.title(), date.day, months[date.month-1], score)
            ret += r
    #exit(0)
    return ret

def main():
    text = """Tabelul de mai jos conține informații despre erorile găsite în datele de naștere și deces ale personalităților menționate paginile zilelor. Liniile cu fundal <span style=\"background-color:#ff8888\">roșu</span> reprezintă nepotriviri certe (datele sunt complete în ambele părți, dar nu se potrivesc), liniile cu fundal <span style=\"background-color:#ffff88\">galben</span> reprezintă intrări unde Wikidata nu are date complete iar liniile cu fundal <span style=\"background-color:#88ffff\">albastru</span> reprezintă intrări unde Wikidata are mai multe date posibile, toate cu același rank. Scorul este alocat automat pe baza numărului de posibile date de naștere de la wikidata (%d/dată) și pe baza numărului de surse ce susțin data aleasă de algoritm (+%d/sursă, 0 dacă nu este aleasă nicio dată). Scorul are rolul de a prioritiza rezolvarea problemelor ușoare (scor mai mare înseamnă încredere mai mare în datele de la Wikidata).
{| class=\"wikitable sortable\"
! Secțiune
! Articol
! Pagină aniversări
! Dată Wikipedia
! Item Wikidata
! Dată Wikidata
! Scor
""" % (MULTIPLE_DATE_PENALTY, MULTIPLE_SOURCES_BONUS)
    for month in months:
        for day in range(1,32):
            for event in events.keys():
                text += treat(day, month, event)
        page = pywikibot.Page(pywikibot.getSite(), "Utilizator:Strainu/aniversări")
        page.put(text + "|}", "Update nepotriviri")

if __name__ == "__main__":
	main()
