package org.wikipedia.ro.utils;

import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.Console;

public class CredentialUtils {
    public static Credentials identifyCredentials() {
        Credentials credentials = new Credentials();
        
        credentials.username = defaultString(System.getenv("WIKI_USERNAME"), System.getProperty("WIKI_USERNAME"));

        Console c = System.console();
        if (isEmpty(credentials.username)) {
            credentials.username = c.readLine("User name: ");
            System.setProperty("WIKI_USERNAME", credentials.username);
        }

        String password = defaultString(System.getenv("WIKI_PASSWORD"), System.getProperty("WIKI_PASSWORD"));
        if (isEmpty(password)) {
            c.printf("Password for user %s: ", credentials.username);
            credentials.password = c.readPassword();
            System.setProperty("WIKI_PASSWORD", new String(credentials.password));
        } else {
            credentials.password = password.toCharArray();
        }
        return credentials;
    }
}
