package org.wikipedia.ro.utility;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.upperCase;

import java.io.Console;
import java.io.IOException;
import java.util.Optional;

import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;

import org.wikibase.Wikibase;
import org.wikibase.WikibaseException;
import org.wikipedia.Wiki;
import org.wikipedia.ro.utils.Credentials;

public abstract class AbstractExecutable {

    protected Wiki wiki;
    protected Wikibase dwiki;

    protected Credentials identifyCredentials(String target) {
        Credentials credentials = new Credentials();

        credentials.username = Optional.ofNullable(System.getenv("WIKI_USERNAME_" + upperCase(target)))
            .orElse(System.getProperty("WIKI_USERNAME_" + upperCase(target)));
        
        Console c = System.console();
        if (isEmpty(credentials.username)) {
            credentials.username = c.readLine(String.format("%s user name: ", target));
            System.setProperty("WIKI_USERNAME_" + upperCase(target), credentials.username);
        }

        String password = Optional.ofNullable(System.getenv("WIKI_PASSWORD_" + upperCase(target)))
            .orElse(System.getProperty("WIKI_PASSWORD_" + upperCase(target)));
        if (isEmpty(password)) {
            c.printf("%s password for user %s: ", target, credentials.username);
            credentials.password = c.readPassword();
            System.setProperty("WIKI_PASSWORD_" + upperCase(target), new String(credentials.password));
        } else {
            credentials.password = password.toCharArray();
        }
        return credentials;
    }

    protected void init() throws FailedLoginException, IOException {
        wiki = Wiki.newSession("ro.wikipedia.org");
        dwiki = new Wikibase();

        Credentials wikiCreds = identifyCredentials("rowiki");
        wiki.login(wikiCreds.username, wikiCreds.password);
        Credentials dwikiCreds = identifyCredentials("dwiki");
        dwiki.login(dwikiCreds.username, dwikiCreds.password);
        wiki.setMarkBot(true);
        dwiki.setMarkBot(true);
        
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                if (null != wiki) wiki.logout();
                if (null != dwiki) dwiki.logout();
            }
        });
    }

    protected abstract void execute() throws IOException, WikibaseException, LoginException;

    public void doExecution() {

        try {
            init();
            
            execute();
            
            
        } catch (LoginException | IOException | WikibaseException e) {
            e.printStackTrace();
            wiki.logout();
            dwiki.logout();
        }
    }
}
