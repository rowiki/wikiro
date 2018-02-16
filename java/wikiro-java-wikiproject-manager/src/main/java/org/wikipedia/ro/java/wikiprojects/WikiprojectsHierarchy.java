package org.wikipedia.ro.java.wikiprojects;

import java.util.HashMap;
import java.util.Map;

import org.wikipedia.ro.java.wikiprojects.model.Wikiproject;

public class WikiprojectsHierarchy {

    private WikiprojectsHierarchy() {

    }

    private static final Map<String, Wikiproject> WIKIPROJECTS = new HashMap<>();
    static {
        declareProject("Muzică rock");
        declareProject("Muzică");
        declareProject("Transport");
        declareProject("Aviație");
        declareProject("Drumuri");
        declareProject("Trenuri");
        declareProject("Istorie");
        declareProject("Istorie militară");
        declareProject("Țările lumii");
        declareProject("România");
        declareProject("Localitățile din România");
        declareProject("Comune-Sate");
        declareProject("Orașele României");

        declareParent("Muzică", "Muzică rock");
        declareParent("Transport", "Aviație");
        declareParent("Transport", "Drumuri");
        declareParent("Transport", "Trenuri");
        declareParent("Istorie", "Istorie militară");
        declareParent("Localitățile din România", "Comune-Sate");
        declareParent("Localitățile din România", "Orașele României");
        declareParent("România", "Localitățile din România");
        declareParent("Țările lumii", "România");
    }

    private static void declareParent(String parent, String child) {
        WIKIPROJECTS.get(child).getParents().add(WIKIPROJECTS.get(parent));
    }

    private static void declareProject(String projname) {
        WIKIPROJECTS.put(projname, new Wikiproject(projname));
    }

    public static boolean isParent(String parentname, String childname) {
        return WIKIPROJECTS.get(childname).getParents().contains(WIKIPROJECTS.get(parentname));
    }
}
