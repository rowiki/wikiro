package org.wikipedia.ro.java.wikiprojects.utils;

import static org.apache.commons.lang3.StringUtils.defaultString;

import java.io.Console;

public class WikiprojectsUtils {
    public static Credentials identifyCredentials() {
        Credentials credentials = new Credentials();
        
        credentials.username = defaultString(System.getenv("WIKI_WIKIPROJECT_MANAGER_USERNAME"), System.getProperty("WIKI_WIKIPROJECT_MANAGER_USERNAME"));
        String password = defaultString(System.getenv("WIKI_WIKIPROJECT_MANAGER_PASSWORD"), System.getProperty("WIKI_WIKIPROJECT_MANAGER_PASSWORD"));

        Console c = System.console();
        if (null == credentials.username) {
            credentials.username = c.readLine("User name: ");
            System.setProperty("WIKI_WIKIPROJECT_MANAGER_USERNAME", credentials.username);
        }
        if (null == password) {
            c.printf("Password: ");
            credentials.password = c.readPassword();
            System.setProperty("WIKI_WIKIPROJECT_MANAGER_PASSWORD", new String(credentials.password));
        } else {
            credentials.password = password.toCharArray();
        }
        return credentials;
    }
}
