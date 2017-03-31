package org.wikipedia.ro.monuments.monuments_section;

import java.util.List;

import org.wikipedia.ro.monuments.monuments_section.data.Monument;

import com.google.common.base.Joiner;

public class Utils {
    public static String capitalize(String s) {
        if (null == s) {
            return null;
        }
        if (0 == s.length()) {
            return s;
        }
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    public static String joinWithConjunction(List<String> stringList, String separator, String conjunction) {
        if (null == stringList) {
            return null;
        }
        if (0 == stringList.size()) {
            return "";
        }
        if (1 == stringList.size()) {
            return stringList.get(0);
        }

        return Joiner.on(separator).join(stringList.subList(0, stringList.size() - 1)).concat(conjunction)
            .concat(stringList.get(stringList.size() - 1));
    }

    public static void printMonumentsList(final List<Monument> monumentsList) {
        for (Monument eachMon : monumentsList) {
            System.out.println(
                " - " + eachMon.name + " (" + eachMon.dating + ") din " + eachMon.settlement + ", cod " + eachMon.code);
            for (Monument eachSubMon : eachMon.submonuments) {
                System.out.println("    - " + eachSubMon.name + " (" + eachSubMon.dating + ") din " + eachSubMon.settlement
                    + ", cod " + eachSubMon.code);
            }
        }
    }
    
    public static String simplyPluralizeE(int cnt, String pluralizableString) {
        if (null == pluralizableString) {
            return null;
        }
        if (1 == cnt) {
            return pluralizableString;
        }
        return pluralizableString + "e";
    }
    
    public static String qualifyinglyPluralizeE(int cnt, String pluralizableString) {
        StringBuilder sb = new StringBuilder();
        sb.append(new NumberToWordsConvertor(cnt).convert()).append(' ');
        if (1 == cnt) {
            sb.append("singur ");
        } else if (cnt % 100 >= 20 || cnt % 100 == 0) {
            sb.append("de ");
        }
        sb.append(simplyPluralizeE(cnt, pluralizableString));
        return sb.toString();
    }
}
