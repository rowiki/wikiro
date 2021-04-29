package org.wikipedia.ro.monuments.monuments_section;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Objects;

import org.wikipedia.ro.monuments.monuments_section.data.Monument;

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
	String firstConjunctedPart = stringList.stream().limit(stringList.size() - 1).collect(Collectors.joining(separator));
	String secondConjunctedPart = stringList.stream().skip(stringList.size() - 1).collect(Collectors.joining());
        return Stream.of(firstConjunctedPart, secondConjunctedPart).filter(Objects::nonNull).filter(s -> !s.isEmpty()).collect(Collectors.joining(conjunction));
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
        return simplyPluralizeByGrammarSet(cnt, new String[] { pluralizableString, "", pluralizableString + "e", "" });
    }

    public static String simplyPluralizeByGrammarSet(int cnt, String[] grammarSet) {
        if (null == grammarSet || 0 == grammarSet.length) {
            return null;
        }
        if (1 == cnt || 3 > grammarSet.length) {
            return grammarSet[0];
        }
        return grammarSet[2];

    }

    public static String qualifyinglyPluralizeE(int cnt, String pluralizableString) {
        return qualifyinglyPluralizeByGrammarSet(cnt, new String[] { pluralizableString, "", pluralizableString + "e", "" });
    }

    public static String qualifyinglyPluralizeByGrammarSet(int cnt, String[] grammarSet) {
        StringBuilder sb = new StringBuilder();
        sb.append(new NumberToWordsConvertor(cnt).convert()).append(' ');
        if (1 == cnt) {
            sb.append("singur ");
        } else if (cnt % 100 >= 20 || cnt % 100 == 0) {
            sb.append("de ");
        }
        sb.append(simplyPluralizeByGrammarSet(cnt, grammarSet));
        return sb.toString();

    }
}
