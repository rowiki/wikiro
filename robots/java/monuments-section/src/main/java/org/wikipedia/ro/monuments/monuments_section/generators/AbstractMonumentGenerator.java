package org.wikipedia.ro.monuments.monuments_section.generators;

import static org.wikipedia.ro.monuments.monuments_section.Utils.joinWithConjunction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.wikipedia.ro.monuments.monuments_section.data.Monument;

public abstract class AbstractMonumentGenerator {
    protected static String[][] MONUMENT_TYPE_DESCRIPTIONS = new String[][] {
        new String[] { "monument istoric", "monumentul istoric", "monumente istorice", "monumentele istorice" },
        new String[] { "sit arheologic", "situl arheologic", "situri arheologice", "siturile arheologice" },
        new String[] { "monument istoric de arhitectură", "monumentul istoric de arhitectură",
            "monumente istorice de arhitectură", "monumentele istorice de arhitectură" },
        new String[] { "monument de for public", "monumentul de for public", "monumente de for public",
            "monumentele de for public" },
        new String[] { "monument memorial sau funerar", "monumentul memorial sau funerar",
            "monumente memoriale sau funerare", "monumentele memoriale sau funerare" } };

    public abstract String generate(List<Monument> monList);
    

    protected List<List<Monument>> splitMonumentsByType(List<Monument> monList) {
        Map<Integer, List<Monument>> listsMap = new HashMap<Integer, List<Monument>>();
        for (Monument eachMon : monList) {
            List<Monument> specificMonumentList = listsMap.get(eachMon.type);
            if (null == specificMonumentList) {
                specificMonumentList = new ArrayList<Monument>();
            }
            specificMonumentList.add(eachMon);
            listsMap.put(eachMon.type, specificMonumentList);
        }
        return new ArrayList<List<Monument>>(listsMap.values());
    }
    
    protected List<String> generateMonumentsListDescription(List<Monument> monumentsList) {
        int monIdx = 0;
        List<String> monumentDescriptions = new ArrayList<String>();
        for (Monument eachMonument : monumentsList) {
            StringBuilder eachMonumentDescription = new StringBuilder(retrieveMonumentMention(eachMonument));
            if (0 < eachMonument.submonuments.size()) {
                eachMonumentDescription.append("\u00A0\u2014 ansamblu alcătuit din ")
                    .append(retrieveSubmonumentsText(eachMonument));
                if (monIdx < monumentsList.size() - 1) {
                    eachMonumentDescription.append("\u00A0\u2014");
                }
            }

            monumentDescriptions.add(eachMonumentDescription.toString());
            monIdx++;
        }
        return monumentDescriptions;
    }

    protected String retrieveMonumentMention(Monument theMonument) {
        StringBuilder monumentMentionBuilder = new StringBuilder(theMonument.name);
        if (null != theMonument.dating && 0 < theMonument.dating.trim().length()) {
            monumentMentionBuilder.append(" (").append(theMonument.dating).append(")");
        }
        return monumentMentionBuilder.toString();
    }

    protected String retrieveSubmonumentsText(Monument theMonument) {
        if (theMonument.submonuments.size() == 0) {
            return "";
        }

        List<String> submonumentDescriptions = new ArrayList<String>();
        List<Monument> sortedSubmonuments = new ArrayList<Monument>(theMonument.submonuments);
        Collections.sort(sortedSubmonuments,
            (Monument m1, Monument m2) -> m1.supplementalCodeNumber.compareTo(m2.supplementalCodeNumber));
        for (Monument eachSubmon : sortedSubmonuments) {
            submonumentDescriptions.add(retrieveMonumentMention(eachSubmon));
        }
        return joinWithConjunction(submonumentDescriptions, ", ", " și ");
    }

}
