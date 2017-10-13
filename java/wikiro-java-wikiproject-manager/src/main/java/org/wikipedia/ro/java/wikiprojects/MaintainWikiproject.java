package org.wikipedia.ro.java.wikiprojects;

import java.io.IOException;

import javax.security.auth.login.LoginException;

import org.wikipedia.ro.java.wikiprojects.createcats.CatTreeCreator;
import org.wikipedia.ro.java.wikiprojects.traverse.WikiprojectTraverser;


public class MaintainWikiproject {
    public static void main(String[] args) throws LoginException, IOException {
        if (0 == args.length) {
            System.err.println("Please specify command and arguments");
            System.exit(1);
        }

        if ("stats".equals(args[0])) {
                
            for (int i = 1; i < args.length; i++) {
                String eachArg = args[i];
                new WikiprojectTraverser(eachArg, "ro.wikipedia.org").traverse();
            }
            
        } else if ("categories".equals(args[0])) {
            for (int i = 1; i < args.length; i++) {
                String eachArg = args[i];
                new CatTreeCreator(eachArg, "ro.wikipedia.org").createCats();
            }
            
        }
    }
}
