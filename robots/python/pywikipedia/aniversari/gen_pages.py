#!/usr/bin/python

import copy
from collections import OrderedDict
import datetime
import math
import pywikibot
import sys
import re
import time
import hashlib

sys.path.append("/home/andrei/pywikibot-core/wikiro/robots/python/pywikipedia")
import strainu_functions as sf

months = ["ianuarie", "februarie", "martie", "aprilie", "mai", "iunie",
"iulie", "august", "septembrie", "octombrie", "noiembrie", "decembrie"]

week = ["luni", "marți", "miercuri", "joi", "vineri", "sâmbătă", "duminică"]

ordinal = ["prima", "a doua", "a treia", "a patra", "a cincea", "a șasea",
"a șaptea", "a opta", "a noua", "a zecea", "a unsprezecea", "a douăsprezecea"]

sections = [
	"Evenimente",
	"Nașteri",
	"Decese",
]

calendars = [ "Q1985727", "Q1985786" ] 

events = {}

MULTIPLE_DATE_PENALTY = -3
MULTIPLE_SOURCES_BONUS = 1
SCORE_LIMIT = 5

def get_section_index(page, name):
    req = pywikibot.getSite()._simple_request(action='parse', prop='sections', page=page)
    json = req.submit()
    #print(json)
    if not json or 'parse' not in json or 'sections' not in json['parse']:
         return None
    for section in json['parse']['sections']:
         #print(section['anchor'])
         if section['anchor'] == name:
             return section['index']
    return None 

ienr = re.compile(r"([1-9]\d{0,3}) (î\.\s?[Hh]r\.?|î\.\s?e\.\s?n\.?)")
yr = re.compile(r"([1-9]\d{0,3})")
dr = re.compile(r"([1-3]?\d) (%s)" % "|".join(months))

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

def parse_date(strd):
    m = dr.search(strd)
    if m:
        return (int(m.group(1)), str(m.group(2)))
    return (None, None)

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


def get_event_text(page, day, month, year, event):
    if type(month) == int:
        month = months[month - 1]
    if not page:
        if year:
            page = pywikibot.Page(pywikibot.getSite(),  "%d" % year)
        else:
            page = pywikibot.Page(pywikibot.getSite(),  "%d %s" % (day, month))
        if not page.exists():
            pywikibot.error("ERROR get_event_text")
            return ""
        if page.isRedirectPage():
            page = page.getRedirectTarget()
    section = get_section_index(page, event)
    if not section:
        return ""
    #print("Section: %s" % section)
    page.site.loadrevisions(page=page, content=True,section=section)
    text = page.get()
    return text


def set_event_text(page, day, month, year, text, comment):
    if type(month) == int:
        month = months[month - 1]
    if not page:
        if year:
            page = pywikibot.Page(pywikibot.getSite(),  "%d" % year)
        else:
            page = pywikibot.Page(pywikibot.getSite(),  "%d %s" % (day, month))
    try:
        page.put(text, comment)
    except pywikibot.PageNotSaved:
        return False
    return True


def get_line_elements(text, day, month, year, event):
    global events
    elem = OrderedDict({})
    lines = [x for x in text.split("\n") if len(x) and x[0] == '*']
    for line in lines:
        l = line.split(':', 1)
        if len(l) < 2: #no need to parse more if we can't identify names 
            continue
        d, name = l
        hash256 = hashlib.sha256(name.strip().encode('utf-8')).hexdigest()
        if not year:
            y = year_to_int(sf.extractLink(d) or d)
            name = sf.extractLink(name)
            if y == None or name == None:
                print(line)
                print(y)
                print(name)
            else:
                elem[name] = { 'year': y, 'line': line, 
                               'hash': hash256 }
        else:
            d_day, d_month = parse_date(sf.extractLink(d) or d)
            if month and month != d_month:
                continue
            name = sf.extractLink(name)
            if d_day == None or d_month == None or name == None:
                print(line)
                print(year)
                print(name)
            else:
                elem[name] = { 'day': day, 'month': month, 'line': line,
                               'hash': hash256 }
            
        events[hash256] = elem
    return elem


def get_date_line(date, event, entry):
    global events
    # we should already have this data
    e = get_line_elements(None, date.day, months[date.month-1], None, event)
    if entry not in e:
        return None

    return e[entry]['line']
    

def replace_date_entry(entry, date, event, oldline, newline):
    text = get_event_text(None, date.day, date.month, None, "Full")
    newtext = text.replace(oldline, newline)
    if newtext == text:
        return False
    return set_event_text(None, date.day, date.month, None, newtext, "Modific intrarea despre [[%s]] din secțiunea '%s'" % (entry, event))


def remove_date_entry(entry, date, event, line):
    global events
    r = replace_date_entry(entry, date, event, line + "\n", "")
    if r:
        e = get_line_elements(None, date.day, months[date.month-1], None, event)
        e.pop(entry)
    return r


def add_date_entry(entry, date, event, line):
    global events
    e = get_line_elements(None, date.day, months[date.month-1], None, event)
    if entry in e: #already in the page, don't do anything
        if line != e[entry]['line']:
            return replace_date_entry(entry, date, event, e[entry]['line'], line)
        else:
            return True
    max_date = -10000 #TODO
    oldline = None
    for elem in e:
        if e[elem]['year'] > max_date and e[elem]['year'] <= date.year:
            max_date = e[elem]['year']
            oldline = e[elem]['line']
    if oldline == None:
        return False #TODO: we can do more here

    newline = oldline + "\n" + line
    r = replace_date_entry(entry, date, event, oldline, newline)
    if r:
        e[entry] = { 'year': date.year, 'line': line }
    return r


def fix_date(entry, olddate, newdate, event):
    pywikibot.output("Fixing " + entry)
    line = get_date_line(olddate, event, entry)
    newline = line
    if newdate and olddate.year != newdate.year:
        newline = line.replace(str(olddate.year), str(newdate.year))
    else:
        return False
    print("Trying to move line: " + line + "\nto line " + newline)
    r = remove_date_entry(entry, olddate, event, line)
    if r == False:
        return r
    if newdate == None:
        return r
    r = add_date_entry(entry, newdate, event, newline)
    if r == False:
        r = add_date_entry(entry, olddate, event, line)
        if r == False:
            pywikibot.error("Moving the entry %s failed and an invalid state was created. Please check all changes done by the bot" % entry)
            exit(1)
        return False
    pywikibot.output("Fixed " + entry)
    return True

def treat_date(page, day, month, event):
    title = "%d %s#%s" % (day, month, event)
    print(title)
    ret = ""
    people = copy.deepcopy(get_line_elements(page, day, month, None, event))
    site = pywikibot.getSite()
    for person in people:
        link = pywikibot.Page(site, person)
        if not link.exists():
            continue
        if page.isRedirectPage():
            page = page.getRedirectTarget()

        try:
            item = link.data_item()
        except:
            continue

        score = 0
        mydate = pywikibot.WbTime(year=people[person]['year'], month = int(1 + months.index(month)), day=day)
        pno = sections[event]
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
            if score < -SCORE_LIMIT and fix_date(person, mydate, None, event):
                continue
            r = "|- style=\"background-color:#88ffff\"\n|  %s || [[%s]] || [[%s]] || %d %s %d || [[:d:%s#%s|%s]] || [date multiple] || %d\n" % (event, person, title, mydate.day, month, mydate.year, qitem, pno, qitem, score)
            ret += r
            continue

        if preferred.getTarget() == None:
            print("Impossible to extract reliable data for %s (wrong date)" % link)
            continue

        date = preferred.getTarget()
        sources = preferred.getSources()
        score += MULTIPLE_SOURCES_BONUS * len(sources)
        precise = True
        if date.precision < 11:
           d = "[fără zi]"
           precise = False
        else:
           d = str(date.day)
        if date.precision < 10:
           m = "[fără lună]"
           precise = False
        else:
           m = months[date.month -1]
        if not precise:
           r = "|- style=\"background-color:#ffff88\"\n|  %s || [[%s]] || [[%s]] || %d %s %d || [[:d:%s#%s|%s]] || %s %s %d || %d\n" % (event, person, title, mydate.day, month, mydate.year, qitem, pno, qitem, d, m, date.year, score)
           ret += r
           continue

        if not equal_dates(date, mydate):
            otherdate = convert_calendar(date)
            if not equal_dates(otherdate, mydate):
                if score >= SCORE_LIMIT and fix_date(person, mydate, date, event):
                    continue
                r = "|- style=\"background-color:#ff8888\"\n|  %s || [[%s]] || [[%s]] || %d %s %d || [[:d:%s#%s|%s]] || %d %s %d || %d\n" % (event, person, title, mydate.day, month, mydate.year, qitem, pno, qitem, date.day, months[date.month-1], date.year, score)
            else:
                #different calendar, same date
                r = "|- style=\"background-color:#88ff88\"\n|  %s || [[%s]] || [[%s]] || %d %s %d || [[:d:%s#%s|%s]] || %d %s %d || 0\n" % (event, person, title, mydate.day, month, mydate.year, qitem, pno, qitem, date.day, months[date.month-1], date.year)
            ret += r

    return ret

def get_year_line(date, event, entry):
    global events
    # we should already have this data
    e = get_line_elements(None, None, None, date.year, event)
    if entry not in e:
        return None

    return e[entry]['line']
    

def replace_year_entry(entry, date, event, oldline, newline):
    text = get_event_text(None, None, None, date.year, "Full")
    newtext = text.replace(oldline, newline)
    #print("Replace")
    #print(oldline)
    #print(newline)
    if newtext == text:
        return True
    return set_event_text(None, None, None, date.year, newtext, "Modific intrarea despre [[%s]] din secțiunea '%s'" % (entry, event))


def remove_year_entry(entry, date, event, line):
    global events
    #print("Removing " + str(line))
    r = replace_year_entry(entry, date, event, line + "\n", "")
    if r:
        e = get_line_elements(None, None, None, date.year, event)
        e.pop(entry)
    return r


def add_year_entry(entry, date, event, line):
    global events
    #print("Adding line " + str(line))
    e = get_line_elements(None, None, None, date.year, event)
    if entry in e: #already in the page, don't do anything
        if line != e[entry]['line']:
            return replace_year_entry(entry, date, event, e[entry]['line'], line)
        else:
            return True
    oldline = None
    for elem in e:
        if months.index(e[elem]['month']) + 1 < date.month or \
		(months.index(e[elem]['month']) + 1 == date.month and e[elem]['day'] <= date.day):
            oldline = e[elem]['line']
    if oldline == None:
        oldline = "== %s ==" % event #TODO: we can do more here

    newline = oldline + "\n" + line
    r = replace_year_entry(entry, date, event, oldline, newline)
    if r:
        e[entry] = { 'day': date.day, 'month': months[date.month - 1], 'line': line }
    return r

def fix_year(entry, olddate, newdate, event):
    # if the difference is too big, better leave a human to handle it
    if newdate and olddate and math.fabs(newdate.year - olddate.year) > SCORE_LIMIT:
        return False
    pywikibot.output("Fixing " + entry)
    line = get_year_line(olddate, event, entry)
    newline = line
    if newdate and (olddate.month != newdate.month or olddate.day != newdate.day):
        o = "%d %s" % (olddate.day, months[olddate.month - 1])
        n = "%d %s" % (newdate.day, months[newdate.month - 1])
        newline = line.replace(o, n)
    else:
        return False
    print("Trying to move line: %s\n to line: %s" % (line, newline))
    r = remove_year_entry(entry, olddate, event, line)
    if r == False:
        return r
    if newdate == None:
        return r
    r = add_year_entry(entry, newdate, event, newline)
    if r == False:
        r = add_year_entry(entry, olddate, event, line)
        if r == False:
            pywikibot.error("Moving the entry failed and an invalid state was created. Please check all changes done by the bot")
            exit(1)
        return False
    pywikibot.output("Fixed " + entry)
    return True

def treat_year(page, year):
    for month in range(len(months)):
        treat_month_year(page, year, month + 1)

def treat_month_year(page, year, month):
    publish = False
    try:
        month_s = months[month-1]
        print("month_s", month_s)
        mpage = pywikibot.Page(pywikibot.getSite(), "{} {}".format(month_s, year))
    except Exception as e:
        #TODO
        print("Cannot get month page", e)
        #return
    x = datetime.datetime(year, month, 1)
    text = "{{{{Antet an|{year}}}}}\n{{{{Calendar|{year}|{month_d}}}}}\n'''{month_s} {year}''' a fost {ordinal} lună a anului și a început într-o zi de {weekday}.\n".format(
            year=year, month_d=month, ordinal=ordinal[month-1], month_s=month_s, weekday=week[x.weekday()])
    ptext = page.get()
    slist = re.split("\={2,}\s*([^\=]+)\s*\={2,}\n", ptext, flags=re.I)

    all_sections = [i.lower() for i in sections] + ["note", "premii nobel", "arte, științe, literatură și filozofie", "nedatate", "legături externe", "medalia fields"]

    for section in sections:
        for slist_index in range(len(slist)):
            if slist[slist_index].strip().lower() == section.lower():
                print(section)
                print(slist_index)
                for month_index in range(slist_index+1, len(slist)):
                    if slist[month_index].strip().lower() == month_s.lower():
                        # Found month section
                        print(month_s)
                        print(month_index)
                        text += "\n== " + section + " ==\n"
                        publish = True
                        break
                    elif slist[month_index].strip().lower() in all_sections:
                        # No section for month, try manual parsing
                        print("Unexpected section %s" % slist[month_index].strip())
                        people = treat_month_year_manual(page, year, month_s, slist, slist_index, month_index)
                        if len(people):
                            text += "\n== " + section + " ==\n"
                            publish = True
                            for person in people:
                                text += people[person]["line"] + "\n"
                        month_index = len(slist)
                        break
                else:
                    month_index = len(slist)
                break
        else:
            continue

        if month_index < len(slist) and slist_index < len(slist):
            text += slist[month_index+1].strip()

    if publish:
        text += "\n== Note ==\n<references />\n"
        text += "[[Categorie:%d|%s]]\n" % (year, month_s)
        text += "[[Categorie:%s|%d]]\n" % (month_s, year)
        if len(text) < 1000:
            #don't publish stubs
            return
        mpage.put(text, "Actualizare articol pe baza articolului [[%s]]" % page.title())

def treat_month_year_manual(page, year, month, ev_list, start_index, end_index):
    people = OrderedDict({})
    for index in range(start_index, end_index):
        people.update(get_line_elements(ev_list[index], None, month, year, ev_list[start_index].strip().lower()))
    print(people)
    return people

def main():
    for year in range(2020, 1+time.localtime().tm_year):
        #for suffix in ["", " î.Hr."]:
        page = pywikibot.Page(pywikibot.getSite(),  "%d" % (year))
        if not page.exists():
            continue
        if page.isRedirectPage():
            page = page.getRedirectTarget()
        #if suffix != "":
        #     year = -year
        treat_year(page, year)

if __name__ == "__main__":
    #import cProfile
    #cProfile.run('main()', 'profiling_aniversari.txt')
    main()
