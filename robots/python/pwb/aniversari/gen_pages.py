#!/usr/bin/python

from collections import OrderedDict
import copy
import datetime
from dateutil.relativedelta import relativedelta
import hashlib
import math
import pywikibot
from pywikibot.data import sparql
import re
import sys
import time
import urllib.parse

sys.path.append("/home/andrei/pywikibot-core/wikiro/robots/python/pwb")
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

all_sections = [i.lower() for i in sections] + \
               ["note", "premii nobel", "medalia fields", "nedatate",
                "arte, științe, literatură și filozofie", "legături externe"]

calendars = [ "Q1985727", "Q1985786" ]

events = {}

MULTIPLE_DATE_PENALTY = -3
MULTIPLE_SOURCES_BONUS = 1
SCORE_LIMIT = 5

def get_section_index(page, name):
    req = pywikibot.Site()._simple_request(action='parse', prop='sections', page=page)
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
            page = pywikibot.Page(pywikibot.Site(),  "%d" % year)
        else:
            page = pywikibot.Page(pywikibot.Site(),  "%d %s" % (day, month))
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
            page = pywikibot.Page(pywikibot.Site(),  "%d" % year)
        else:
            page = pywikibot.Page(pywikibot.Site(),  "%d %s" % (day, month))
    try:
        page.put(text, comment)
    except pywikibot.PageNotSaved:
        return False
    return True


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
    site = pywikibot.Site()
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

def get_wikidata_events(day, month, year, event):
    """Query Wikidata for all persons which had the `event` happen in the given
       date (e.g. born in 2017)

       @param day: The day we're working on or None
       @param month: The month we're working on or None
       @param year: The year we're working on or None
       @param event: The event we're working on
       @param_type event: string

       @rtype dictionary
    """

    if event == "Nașteri":
        fflt = "born"
        sflt = "died"
        fid = "P569"
        sid = "P570"
        optional = "OPTIONAL"
        other_prefix = "d"
    elif event == "Decese":
        fflt = "died"
        sflt = "born"
        fid = "P570"
        sid = "P569"
        optional = ""
        other_prefix = "n"
    else:
        return

    if not year:
        return
    stime = datetime.datetime.strptime("{}-{}-{}".format(year, month or 1, day or 1), '%Y-%m-%d')
    if day:
        tdiff = relativedelta(days=+1)
    elif month:
        tdiff = relativedelta(months=+1)
    else:
        tdiff = relativedelta(years=+1)
    etime = stime + tdiff

    query = """select ?item ?itemLabel ?itemDescription ?born ?bornP ?died ?diedP ?article where {
    ?item wdt:P31 wd:Q5;
          p:%s/psv:%s [
                wikibase:timePrecision ?%sP ;
                wikibase:timePrecision "11"^^xsd:integer ;
                wikibase:timeValue ?%s ;
              ] .
    %s {
    ?item p:%s/psv:%s [
                wikibase:timePrecision ?%sP ;
                wikibase:timePrecision "11"^^xsd:integer ;
                wikibase:timeValue ?%s ;
              ] .
    }
    filter (?%s >= "%sZ"^^xsd:dateTime && ?%s < "%sZ"^^xsd:dateTime)

    ?article schema:about ?item .
    ?article schema:isPartOf <https://ro.wikipedia.org/> . #Targeting Wikipedia language where subjects has no article.
    SERVICE wikibase:label {
       bd:serviceParam wikibase:language "ro"
    }
} LIMIT 100""" % (fid, fid, fflt, fflt,
                 optional, sid, sid, sflt, sflt,
                 fflt, stime.isoformat(), fflt, etime.isoformat())

    query_object = sparql.SparqlQuery(max_retries=0)
    try:
        data = query_object.query(query)
    except:
        data = None
    if not data:
        return

    ret = {}
    results = data['results']['bindings']

    for result in results:
        if int(result['bornP']['value']) < 11:
            continue
        if 'diedP' in result and int(result['diedP']['value']) < 11:
            continue

        if 'itemDescription' not in result:
            continue

        name = result['itemLabel']['value']
        if name in ret:
            # multiple values in a field, we don't bother with that
            del ret[name]
            continue

        if sflt in result:
            s = datetime.datetime.strptime(result[sflt]['value'],'%Y-%m-%dT%H:%M:%SZ')
            other_text = " ({prefix}. [[{y}]])".format(prefix=other_prefix, y=s.year)
        else:
            other_text = ""
        d = datetime.datetime.strptime(result[fflt]['value'],'%Y-%m-%dT%H:%M:%SZ')
        desc = result['itemDescription']['value']
        desc = desc[0].lower() + desc[1:]
        article = urllib.parse.unquote(result['article']['value'][len("https://ro.wikipedia.org/wiki/"):]).replace('_',' ')
        if article != name:
            line = "[[{article}|{name}]], {desc}{other}".format(article=article, name=name, desc=desc, other=other_text)
        else:
            line = "[[{article}]], {desc}{other}".format(article=article, desc=desc, other=other_text)
        hash256 = hashlib.sha256(line.strip().encode('utf-8')).hexdigest()

        ret[name] = { 'day': d.day, 'month': months[d.month-1], 'year': d.year,
                      'event': event, 'line': line, 'hash': hash256 }

    print(ret)
    return ret

def split_page_in_sections(page):
    """Split a page in a list of sections

       @param page	The page to split
       @returns		The list of sections including the section headers
    """
    ptext = page.get()
    slist = re.split("\={2,}\s*([^\=]+)\s*\={2,}\n", ptext, flags=re.I)

    return slist

def sort_line_dict(d):
    """Helper function for sorting line entries in aniversaries"""
    for month_d in range(len(months)):
        if months[month_d] == d['month']:
            break

    return(d['year'], month_d+1, d['day'])

def fill_year_from_wikidata(page, year):
    """Add events from Wikidata to year page

       @param page: The year page
       @param year: The year as integer
    """
    for event in sections[1:]:
        for month in range(1,13):
            text = page.get()
            slist = split_page_in_sections(page)
            wdlines = get_wikidata_events(None, month, year, event)
            if wdlines == None or not len(wdlines):
                print("No data found in Wikidata for section {} in {}".format(event, year))
                continue

            old_text = None

            for slist_index in range(len(slist)):
                m = sf.stripLink(slist[slist_index].strip()) or slist[slist_index].strip()
                if m.lower() == event.lower():
                    for month_index in range(slist_index+1, len(slist)):
                        n = sf.stripLink(slist[month_index].strip()) or slist[month_index].strip()
                        if n.lower() == months[month-1].lower():
                            # Found month section
                            old_text = slist[month_index + 1]
                            break
                        elif slist[month_index].strip().lower() in all_sections:
                            old_text = "".join(slist[slist_index+1:month_index])
                            break
                    break

            if not old_text:
                print("No section found for {} {}-{}".format(event, year, month))
                continue

            wlines = get_line_elements(old_text, None, None, year, event)

            for line in wdlines:
                if line in wlines:
                    continue
                if wdlines[line]['month'] != months[month-1]:
                    continue
                wlines[line] = wdlines[line]

            wlines = sorted(wlines.values(), key=sort_line_dict)
            new_text = old_text
            prev_line = None
            for line in wlines:
                if new_text.find(line['line']) == -1:
                    line_text = "* [[{day} {month}]]: {line}".format(day=line['day'], month=line['month'], line=line['line'])
                    if prev_line:
                        prev_line_text = "* [[{day} {month}]]: {line}".format(day=prev_line['day'], month=prev_line['month'], line=prev_line['line'])
                        new_text = new_text.replace(prev_line_text, prev_line_text + "\n" + line_text)
                    else:
                        new_text = line_text + "\n" + new_text
                prev_line = line

            if new_text == old_text:
                print("No new data for {} {}-{}".format(event, year, month))
                continue

            text = text.replace(old_text, new_text)

            #pywikibot.showDiff(text, text2)
            page.put(text, "Actualizare {} cu date de la Wikidata".format(event))

def get_line_elements(text, day, month, year, event):
    """Extract all events from `text` assuming they're in standardized format

       @param text: The text to work on
       @param day: The day we're working on or None
       @param month: The month we're working on or None
       @param year: The year we're working on or None
       @param event: The event we're working on or None
    """
    global events
    elem = OrderedDict({})
    lines = [x for x in text.split("\n") if len(x) and x[0] == '*']
    for line in lines:
        l = line.split(':', 1)
        if len(l) < 2: #no need to parse more if we can't identify names
            continue
        d, contents = l
        hash256 = hashlib.sha256(contents.strip().encode('utf-8')).hexdigest()
        if not year:
            y = year_to_int(sf.extractLink(d) or d)
            name = sf.extractLink(contents)
            if y == None or name == None:
                print(line)
                print(y)
                print(name)
            else:
                elem[name] = { 'day': day, 'month': month, 'year': y,
                               'event': event, 'line': contents.strip(), 'hash': hash256 }
        else:
            d_day, d_month = parse_date(sf.extractLink(d) or d)
            if month and month != d_month:
                continue
            name = sf.extractLink(contents)
            if d_day == None or d_month == None or name == None:
                print(line)
                print(year)
                print(name)
            else:
                elem[name] = { 'day': d_day, 'month': d_month, 'year': year,
                               'event': event, 'line': contents.strip(), 'hash': hash256 }

        events[hash256] = elem
    return elem


def split_year_data(page, year):
    """Extract data from year page for montly pages

       @param page: The year page object
       @param year: The numerical value for the year
       @rtype: void
       """
    for month in range(len(months)):
        split_year_to_month(page, year, month + 1)

def split_year_to_month(page, year, month):
    """Construct the monthly page from the data extracted from the year page

       @param page: The year page object
       @param year: The numerical value for the year
       @rtype: void
    """
    publish = False
    try:
        month_s = months[month-1]
        print("month_s", month_s)
        mpage = pywikibot.Page(pywikibot.Site(), "{} {}".format(month_s, year))
    except Exception as e:
        #TODO
        print("Cannot get month page", e)
        #return
    x = datetime.datetime(year, month, 1)
    text = """{{{{Articol generat automat|[[{year}]]}}}}{{{{Antet an|{year}}}}}
{{{{Calendar|{year}|{month_d}}}}}
'''{month_s} {year}''' a fost {ordinal} lună a anului și a început într-o zi de {weekday}.
""".format(year=year, month_d=month, ordinal=ordinal[month-1],
           month_s=month_s.capitalize(), weekday=week[x.weekday()])
    slist = split_page_in_sections(page)

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
                        people = split_year_to_month_manual(page, year, month_s, slist, slist_index, month_index)
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
        mpage.put(text, "Actualizare articol pe baza articolului [[%s]]" % page.title())

def split_year_to_month_manual(page, year, month, ev_list, start_index, end_index):
    people = OrderedDict({})
    for index in range(start_index, end_index):
        people.update(get_line_elements(ev_list[index], None, month, year, ev_list[start_index].strip().lower()))
    print(people)
    return people

def use_wikidata():
    for year in range(1980, 1+time.localtime().tm_year):
        page = pywikibot.Page(pywikibot.Site(),  "%d" % (year))
        if not page.exists():
            continue
        if page.isRedirectPage():
            page = page.getRedirectTarget()

        fill_year_from_wikidata(page, year)

def split_years():
    for year in range(1980, 1+time.localtime().tm_year):
        page = pywikibot.Page(pywikibot.Site(),  "%d" % (year))
        if not page.exists():
            continue
        if page.isRedirectPage():
            page = page.getRedirectTarget()

        split_year_data(page, year)

if __name__ == "__main__":
    #import cProfile
    #cProfile.run('main()', 'profiling_aniversari.txt')
    #split_years()
    use_wikidata()
