#!/usr/bin/python

import datetime
import math
import pywikibot
import sys
import re

sys.path.append("/home/andrei/pywikibot-core/wikiro/robots/python/pywikipedia")
import strainu_functions as sf

months = ["ianuarie", "februarie", "martie", "aprilie", "mai", "iunie",
"iulie", "august", "septembrie", "octombrie", "noiembrie", "decembrie"]

events = {
	"Nașteri": [2, "P569"],
	"Decese": [3, "P570"],
}

calendars = [ "Q1985727", "Q1985786" ] 

MULTIPLE_DATE_PENALTY = -3
MULTIPLE_SOURCES_BONUS = 1
ienr = re.compile(r"([1-9]\d{0,3}) (î\.\s?[Hh]r\.?|î\.\s?e\.\s?n\.?)")
yr = re.compile(r"([1-9]\d{0,3})")

def year_to_int(stry):
    year = None
    try:
        year = int(stry)
    except:
        m = ienr.search(stry)
        if m:
            return -int(m.group(1))
        m = yr.search(stry)
        if m:
            return int(m.group(1))
    return year


def get_line_elements(text):
    elem = {}
    lines = [x for x in text.split("\n") if len(x) and x[0] == '*']
    for line in lines:
        l = line.split(':', 1)
        if len(l) < 2: #no need to parse more if we can't identify names 
            continue
        y, name = l
        year = year_to_int(sf.extractLink(y) or y)
        name = sf.extractLink(name)
        if year == None or name == None:
            print(line)
            print(year)
            print(name)
        else:
            elem[name] = { 'year': year, 'line': line }
    return elem


#conversion code based on [[:c:Module:Calendar]]
def _jdn2date(jdn, gregorian):
    f = jdn + 1401
    if gregorian > 0:
        f = f + math.floor((math.floor((4 * jdn + 274277) / 146097) * 3) / 4) - 38
    e = 4 * f + 3
    g = math.floor(math.fmod(e, 1461) / 4)
    h = 5 * g + 2
    day   = math.floor(math.fmod (h,153) / 5) + 1
    month = math.fmod (math.floor(h/153) + 2, 12) + 1
    year  = math.floor(e/1461) - 4716 + math.floor((14 - month) / 12)
	
    #If year is less than 1, subtract one to convert from a zero based date system to the
    #common era system in which the year -1 (1 B.C.E) is followed by year 1 (1 C.E.).
    if year < 1:
        year = year - 1
	
    return (int(year), int(month), int(day))


def _date2jdn(year, month, day, gregorian):
    if not year:
        return None
    elif year < 0:
        #If year is less than 0, add one to convert from  the common era system in which
        #the year -1 (1 B.C.E) is followed by year 1 (1 C.E.) to a zero based date system
        year = year + 1

    a = math.floor((14-month) / 12) #will be 1 for January and February, and 0 for other months.
    y = year + 4800 - a             #years since year –4800
    m = month + 12*a - 3            #month number where 10 for January, 11 for February, 0 for March, 1 for April
    c = math.floor((153*m + 2)/5)   #number of days since March 1
    if gregorian > 0:
        b = math.floor(y/4) - math.floor(y/100) + math.floor(y/400) #number of leap years since y==0 (year –4800)
        d = 32045               #offset so the result will be 0 for January 1, 4713 BCE
    else:
        b = math.floor(y/4)           # number of leap years since y==0 (year –4800)
        d = 32083                     # offset so the result will be 0 for January 1, 4713 BCE
    return day + c + 365*y + b - d


#Convert a date from Gregorian to Julian calendar
def gregorian_to_julian(year, month, day):
    jdn = _date2jdn(year, month, day, 1)
    if jdn:
        return _jdn2date(jdn, 0)
    else:
        return (year, month, day)


#Convert a date from Julian to Gregorian calendar
def julian_to_gregorian(year, month, day):
    jdn = _date2jdn(year, month, day, 0)
    if jdn:
        return _jdn2date(jdn, 1)
    else:
        return (year, month, day)


def convert_calendar(date):
    #print(date)
    if not date.calendarmodel:
        return date
    c = date.calendarmodel.replace("http://www.wikidata.org/entity/", "")
    if c not in calendars:
        return date
    if c == calendars[0]:
        (y,m,d) = gregorian_to_julian(date.year, date.month, date.day)
        c = calendars[1]
    else:
        (y,m,d) = julian_to_gregorian(date.year, date.month, date.day)
        c = calendars[0]
    newdate = pywikibot.WbTime(year=y, month=m, day=d, 
              calendarmodel="http://www.wikidata.org/entity/" + c)
    #print(newdate)
    return newdate

def equal_dates(date1, date2):
    if date1.year != date2.year:
        return False
    if date1.month != date2.month:
        return False
    if date1.day != date2.day:
        return False
    return True

def treat(page, day, month, event):
    title = "%d %s#%s" % (day, month, event)
    print(title)
    ret = ""
    page.site.loadrevisions(page=page, content=True,section=events[event][0])
    text = page.get()
    people = get_line_elements(text)
    site = pywikibot.getSite()
    for person in people:
        link = pywikibot.Page(site, person)
        if not link.exists():
            continue
        if page.isRedirectPage():
            page = page.getRedirectTarget()
        #if link.title() not in text:
        #    continue
        #follow = False
        #try:
        #    year = int(link.title())
        #except:
        #    follow = True
        #if not follow:
        #    continue

        try:
            item = link.data_item()
        except:
            continue
        #if "P31" not in item.claims:
        #    continue
        #follow = False
        #for claim in item.claims["P31"]:
        #    if claim.getTarget().title() == "Q5":
        #        follow = True
        #        break
        #if not follow:
        #    #print("Not a person: %s" % link)
        #    continue

        score = 0
        mydate = pywikibot.WbTime(year=people[person]['year'], month = int(1 + months.index(month)), day=day)
        pno = events[event][1]
        qitem = item.title()
        #print(item)
        if pno not in item.claims:
            r = "|- style=\"background-color:#ffff88\"\n|  %s || [[%s]] || [[%s]] || %d %s %d || [[:d:%s|%s]] || [fără dată] || 0\n" % (event, person, title, mydate.day, month, mydate.year, qitem, qitem)
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
            r = "|- style=\"background-color:#88ffff\"\n|  %s || [[%s]] || [[%s]] || %d %s %d || [[:d:%s#%s|%s]] || [date multiple] || %d\n" % (event, person, title, mydate.day, month, mydate.year, qitem, pno, qitem, score)
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
           r = "|- style=\"background-color:#ffff88\"\n|  %s || [[%s]] || [[%s]] || %d %s %d || [[:d:%s#%s|%s]] || %s %s %d || %d\n" % (event, person, title, mydate.day, month, mydate.year, qitem, pno, qitem, d, m, date.year, score)
           ret += r
           continue

        if not equal_dates(date, mydate):
            otherdate = convert_calendar(date)
            if not equal_dates(otherdate, mydate):
                r = "|- style=\"background-color:#ff8888\"\n|  %s || [[%s]] || [[%s]] || %d %s %d || [[:d:%s#%s|%s]] || %d %s %s || %d\n" % (event, person, title, mydate.day, month, mydate.year, qitem, pno, qitem, date.day, months[date.month-1], date.year, score)
            else:
                #different calendar, same date
                r = "|- style=\"background-color:#88ff88\"\n|  %s || [[%s]] || [[%s]] || %d %s %d || [[:d:%s#%s|%s]] || %d %s %s || 0\n" % (event, person, title, mydate.day, month, mydate.year, qitem, pno, qitem, date.day, months[date.month-1], date.year)
            ret += r

    return ret

def main():
    text = """Tabelul de mai jos conține informații despre erorile găsite în datele de naștere și deces ale personalităților menționate în paginile zilelor. 

Legendă:
* liniile cu fundal <span style="background-color:#ff8888">roșu</span> reprezintă nepotriviri certe (datele sunt complete în ambele părți, dar nu se potrivesc)
* liniile cu fundal <span style="background-color:#ffff88">galben</span> reprezintă intrări unde Wikidata nu are date complete
* liniile cu fundal <span style="background-color:#88ffff">albastru</span> reprezintă intrări unde Wikidata are mai multe date posibile, toate cu același rank
* liniile cu fundal <span style="background-color:#88ff88">verde</span> reprezintă diferențe de calendar (gregorian vs. julian) 

Scorul este alocat automat pe baza numărului de posibile date de naștere de la wikidata (%d/dată) și pe baza numărului de surse ce susțin data aleasă de algoritm (+%d/sursă, 0 dacă nu este aleasă nicio dată). Scorul are rolul de a prioritiza rezolvarea problemelor ușoare. '''Scor mai mare înseamnă încredere mai mare în datele de la Wikidata'''.
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
            page = pywikibot.Page(pywikibot.getSite(),  "%d %s" % (day, month))
            if not page.exists():
                continue
            for event in events.keys():
                text += treat(page, day, month, event)
    page = pywikibot.Page(pywikibot.getSite(), "Utilizator:Strainu/aniversări")
    page.put(text + "|}", "Update nepotriviri")

if __name__ == "__main__":
    import cProfile
    cProfile.run('main()', 'profiling_aniversari.txt')
    #main()
