#!/usr/bin/python3

import pywikibot

#TODO: keep in sync with MW
DELETED_TEXT = 1
DELETED_COMMENT = 2;
DELETED_USER = 4;
DELETED_RESTRICTED = 8;
bits = [DELETED_TEXT, DELETED_COMMENT, DELETED_USER, DELETED_RESTRICTED]
pages_list = ["Wikipedia:Afișierul administratorilor"]
patroller_message = """

== {prefix}Recuperare nerealizată de un administrator ==
{{{{small|În cazul în care acest mesaj este greșit, vă rog să raportați acest lucru [[Discuție Utilizator:Strainu|aici]].}}}}

Pe {{{{date|{date} }}}} la ora {time} utilizatorul {{{{ut|{user}}}}}, care nu are drepturi de administrator, a recuperat [https://ro.wikipedia.org/wiki/Special:Jurnal?type=delete&user={user}&page=&wpdate={date}&tagfilter=&subtype=revision anumite informații] din pagina ''{title}''. Acest lucru permite vizualizarea de conținut șters, lucru neacceptat de echipa legală a WMF. Conform [[Wikipedia:Patrulare]], decizia de retragere a drepturilor apartine administratorilor.--~~~~"""

alarm_message = """

== {prefix}Recuperare realizată de un utilizator fără drepturi ==
{{{{small|În cazul în care acest mesaj este greșit, vă rog să raportați acest lucru [[Discuție Utilizator:Strainu|aici]].}}}}

Pe {{{{date|{date} }}}} la ora {time} utilizatorul {{{{ut|{user}}}}}, care nu are drepturi de administrator sau patrulator, a recuperat [https://ro.wikipedia.org/wiki/Special:Jurnal?type=delete&user={user}&page=&wpdate={date}&tagfilter=&subtype=revision anumite informații] din pagina ''{title}''. Acest lucru nu ar trebui să se întâmple. Vă rog să verificați drepturile utilizatorului și să [[:phab:|raportați problema către dezvoltatori]].--~~~~"""

def warn(pages, log):
	for title in pages:
		try:
			page = pywikibot.Page(source=pywikibot.Site(), title=title)
			if not page.exists():
				pywikibot.warn("Could not notify " + title)
				continue
			while page.isRedirectPage():
				page = page.getRedirectTarget()
			text = page.get()
			date = log.timestamp().strftime("%Y-%m-%d")
			time = log.timestamp().strftime("%H:%M")
			if is_patroller(log.user()):
				default_message = patroller_message
				prefix = "[Important]"
			else:
				default_message = alarm_message
				prefix = "[Alarmă]"
			#prefix = "[Test]"
			#default_message = patroller_message
			message = default_message.format(
				prefix = prefix,
				date = date,
				time = time,
				user = log.user().replace(" ", "_"),
				title = log.page(),
			  )
			#print(message)
			page.put(text + message, 
				"Cer retragerea drepturilor de patrulator pentru încălcarea politicii")
		except Exception as e:
			print(e)
		
def get_user_groups(user):
	r = pywikibot.data.api.Request(parameters={'action': 'query', 'list': 'users', 'usprop': 'groups', 'ususers': user})
	data = r.submit()
	#print(data)
	if not data or not data.get("query") or \
	   not data.get("query").get("users") or \
	   not data.get("query").get("users")[0].get("groups"):
		return None
	return data.get("query").get("users")[0].get("groups")

def is_sysop(user):
	data = get_user_groups(user)
	if not data:
		return False
	return "sysop" in data

def is_patroller(user):
	data = get_user_groups(user)
	if not data:
		return False
	return "patroller" in data

def start_date():
	try:
		with open("/tmp/patroller_recovery", "r") as f:
			return pywikibot.Timestamp.fromtimestampformat(f.read().strip())
	except Exception as e:
		import datetime
		now = pywikibot.Timestamp.now()
		month = datetime.timedelta(days=30)
		now = now - month
		return now

def main():
	site = pywikibot.Site()
	print("Ultima rulare a fost la", start_date())
	logs = site.logevents(logtype='delete', end=start_date())
	for log in logs:
		try:
			if log._params.get("type") == "revision":
				old = int(log._params.get("old").get("bitmask"))
				new = int(log._params.get("new").get("bitmask"))
				error = any((bit & old) and (not (bit & new)) for bit in bits)
				#print (old, new, error)
				if error:
					if is_sysop(log.user()):
						continue
					print(old, new, log.page(), log.user())
					warn(pages_list, log)
		except KeyError:
			continue
		except:
			raise
	with open("/tmp/patroller_recovery", "w") as f:
		now = pywikibot.Timestamp.now()
		f.write(now.totimestampformat())

if __name__ == "__main__":
	main()
