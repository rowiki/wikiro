package org.wikipedia.ro.populationdb.util;

import java.io.IOException;

import javax.security.auth.login.LoginException;

import org.wikibase.Wikibase;
import org.wikipedia.Wiki;

public class WikiEditExecutor implements Executor {

	private Wiki wiki;
	private Wikibase dwiki;

	public WikiEditExecutor(Wiki wiki, Wikibase dwiki) {
		super();
		this.wiki = wiki;
		this.dwiki = dwiki;
	}

	public void save(String title, String text, String comment)
			throws LoginException, IOException {
		boolean success = true;
		do {
			try {
				wiki.edit(title, text, comment);
			} catch (Throwable t) {
				success = false;
			}
		} while (!success);
	}

	public void link(String fromwiki, String fromtitle, String towiki,
			String totitle) throws Exception {
		boolean success = true;
		do {
			try {
				dwiki.linkPages(fromwiki, fromtitle, towiki, totitle);
			} catch (Throwable t) {
				success = false;
			}
		} while (!success);
	}

}
