package org.wikipedia.ro.java.wikiprojects.utils;

import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.Console;

public class WikiprojectsUtils {
    public static Credentials identifyCredentials() {
        Credentials credentials = new Credentials();
        
        credentials.username = defaultString(System.getenv("WIKI_WIKIPROJECT_MANAGER_USERNAME"), System.getProperty("WIKI_WIKIPROJECT_MANAGER_USERNAME"));

        Console c = System.console();
        if (isEmpty(credentials.username)) {
            credentials.username = c.readLine("User name: ");
            System.setProperty("WIKI_WIKIPROJECT_MANAGER_USERNAME", credentials.username);
        }

        String password = defaultString(System.getenv("WIKI_WIKIPROJECT_MANAGER_PASSWORD"), System.getProperty("WIKI_WIKIPROJECT_MANAGER_PASSWORD"));
        if (isEmpty(password)) {
            c.printf("Password for user %s: ", credentials.username);
            credentials.password = c.readPassword();
            System.setProperty("WIKI_WIKIPROJECT_MANAGER_PASSWORD", new String(credentials.password));
        } else {
            credentials.password = password.toCharArray();
        }
        return credentials;
    }
}
