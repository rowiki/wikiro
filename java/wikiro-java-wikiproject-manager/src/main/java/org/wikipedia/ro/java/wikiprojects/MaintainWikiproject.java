package org.wikipedia.ro.java.wikiprojects;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.security.auth.login.LoginException;

import static org.apache.commons.lang3.StringUtils.*;
import org.wikipedia.ro.java.wikiprojects.createcats.CatTreeCreator;
import org.wikipedia.ro.java.wikiprojects.createproj.WikiprojectCreator;
import org.wikipedia.ro.java.wikiprojects.stubs.StubClassifier;
import org.wikipedia.ro.java.wikiprojects.traverse.WikiprojectTraverser;

public class MaintainWikiproject {
    public static void main(String[] args) throws LoginException, IOException {
        if (0 == args.length) {
            System.err.println("Please specify command and arguments");
            System.err.println("Available commands:");
            System.err.println(" - stubs - classify stubs");
            System.err.println(" - stats - compute stats");
            System.err.println(" - categories - create wikiproject category tree");
            System.err.println(" - create- create wikiproject pages, including category tree");
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

        } else if ("stubs".equals(args[0])) {
            if (1 < args.length) {
                List<String> synonyms = new ArrayList<String>();
                int startIdx = 0;
                if (2 < args.length) {
                    for (int i = 2; i < args.length; i++) {
                        if (startsWith(args[i], "-synonym:")) {
                            synonyms.add(removeStart(args[i], "-synonym:"));
                            continue;
                        }
                        try {
                            startIdx = Integer.parseInt(args[2]);
                        } catch (NumberFormatException nfe) {
                            System.err.println("Not a number: " + args[2] + ". Defaulting to 0.");
                        }
                    }
                }
                new StubClassifier(args[1], "ro.wikipedia.org", startIdx, synonyms).classifyStubs();
            }
        } else if ("create".equals(args[0])) {
            if (3 < args.length) {
                new WikiprojectCreator(args[1], "ro.wikipedia.org", args[2], args[3]).createWikiproject();
            } else {
                System.err.println("Please specify project name, description and image");
            }
        }
    }
}
