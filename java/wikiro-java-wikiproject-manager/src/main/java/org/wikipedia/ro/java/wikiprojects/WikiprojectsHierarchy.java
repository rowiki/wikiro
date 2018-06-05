package org.wikipedia.ro.java.wikiprojects;

import java.util.HashMap;
import java.util.Map;

import org.wikipedia.ro.java.wikiprojects.model.Wikiproject;

public class WikiprojectsHierarchy {

    private WikiprojectsHierarchy() {

    }

    private static final Map<String, Wikiproject> WIKIPROJECTS = new HashMap<>();
    static {
        declareParent("Muzică", "Muzică rock");
        declareParent("Muzică", "Muzică pop");
        declareParent("Muzică", "Hip hop");
        declareParent("Muzică", "Muzică clasică");
        declareParent("Muzică", "Muzică românească");
        declareParent("Muzică", "Jazz");
        declareParent("Transport", "Aviație");
        declareParent("Transport", "Drumuri");
        declareParent("Transport", "Trenuri");
        declareParent("Istorie", "Istorie militară");
        declareParent("Localitățile din România", "Comune-Sate");
        declareParent("Localitățile din România", "Orașele României");
        declareParent("România", "Localitățile din România");
        declareParent("România", "Istoria României");
        declareParent("România", "Muzică românească");
        declareParent("România", "România în Primul Război Mondial");
        declareParent("Țările lumii", "România");
        declareParent("Țările lumii", "Irlanda");
        declareParent("Țările lumii", "Australia");
        declareParent("Istorie", "Arheologie");
        declareParent("Istorie", "Istoria României");
        declareParent("Economie", "Afaceri");
        declareParent("Economie", "Transport");
        declareParent("Geografie", "Râuri");
        declareParent("Geografie", "Munți");
        declareParent("Film", "Televiziune");
    }

    private static void declareParent(String parent, String child) {
        declareProject(parent);
        declareProject(child);
        WIKIPROJECTS.get(child).getParents().add(WIKIPROJECTS.get(parent));
    }

    private static void declareProject(String projname) {
        if (!WIKIPROJECTS.containsKey(projname)) {
            WIKIPROJECTS.put(projname, new Wikiproject(projname));
        }
    }

    public static boolean isParent(String parentname, String childname) {
        if (!WIKIPROJECTS.containsKey(childname) || !WIKIPROJECTS.containsKey(parentname)) {
            return false;
        }
        if (WIKIPROJECTS.get(childname).getParents().contains(WIKIPROJECTS.get(parentname))) {
            return true;
        }
        
        for (Wikiproject eachParentOfChild: WIKIPROJECTS.get(childname).getParents()) {
            if (isParent(parentname, eachParentOfChild.getName())) {
                return true;
            }
        }
        return false;
    }
}
