package org.wikipedia.ro.java.wikiprojects;

import java.io.IOException;

import javax.security.auth.login.LoginException;

/**
 * Hello world!
 *
 */
public class App {
    public static void main(String[] args) throws LoginException, IOException {
        if (0 == args.length) {
            System.err.println("Please specify wikiproject");
            System.exit(1);
        }

        for (String eachArg : args) {
            new WikiprojectTraverser(eachArg, "ro.wikipedia.org").traverse();
        }
    }
}
