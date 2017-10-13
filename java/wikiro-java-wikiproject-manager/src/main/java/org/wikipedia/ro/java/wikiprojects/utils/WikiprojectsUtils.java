package org.wikipedia.ro.java.wikiprojects.utils;

import java.io.Console;

public class WikiprojectsUtils {
    public static Credentials identifyCredentials() {
        Credentials credentials = new Credentials();
        
        credentials.username = System.getenv("WIKI_WIKIPROJECT_MANAGER_USERNAME");
        String password = System.getenv("WIKI_WIKIPROJECT_MANAGER_PASSWORD");

        Console c = System.console();
        if (null == credentials.username) {
            credentials.username = c.readLine("User name: ");
        }
        if (null == password) {
            c.printf("Password: ");
            credentials.password = c.readPassword();
        } else {
            credentials.password = password.toCharArray();
        }
        return credentials;
    }
}
