package org.wikipedia.ro.utils;

import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.apache.commons.lang3.StringUtils.removeEnd;

import java.text.ParseException;
import java.text.RuleBasedCollator;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class TextUtils {
    private TextUtils() {
        
    }
    
    public static String RO_COLLATION_DESCRIPTION = "<  0 < 1 < 2 < 3 < 4 < 5 < 6 < 7 < 8 < 9 "
        + "< a, A < ă, Ă < â, Â‚ < b, B < c, C < d, D < e, E < f, F < g, G < h, H < i, I"
        + "< î, Î < j, J < k, K < l, L < m, M < n, N < o, O < p, P < q, Q < r, R"
        + "< s, S < ș, Ș < t, T < ț, Ț < u, U < v, V < w, W < x, X < y, Y < z, Z";
    private static RuleBasedCollator RO_COLLATION;

    public static RuleBasedCollator getRoCollation() {
        try {
            RO_COLLATION = Optional.ofNullable(RO_COLLATION).orElse(new RuleBasedCollator(RO_COLLATION_DESCRIPTION));
            return RO_COLLATION;
        } catch (ParseException e) {
            return null;
        }
    }
    
    public static int compareRoStrings(String s1, String s2) {
        if (s1 == null) {
            return -1;
        }
        if (s2 == null) {
            return 1;
        }
        return getRoCollation().compare(s1, s2);
    }
    
    public static String de(final int number, final String singular, final String plural) {
        if (number == 1) {
            return singular;
        }
        if (number == 0) {
            return plural;
        }

        final int mod100 = number % 100;
        if (mod100 == 0 || mod100 > 19) {
            return "de&nbsp;" + plural;
        } else {
            return plural;
        }
    }

    public static String capitalizeName(final String name) {
        final String onlyLower = StringUtils.lowerCase(name);
        final String[] lowerItems = StringUtils.splitByCharacterType(onlyLower);
        final StringBuilder sb = new StringBuilder();

        final List<String> notCapitalized = Arrays.asList("de", "din", "pe", "sub", "peste", "la", "cel", "lui", "cu", "a", "al", "pentru", "și");

        for (final String item : lowerItems) {
            sb.append(notCapitalized.contains(item) ? item : StringUtils.capitalize(item));
        }
        return StringUtils.capitalize(sb.toString());
    }
    
    public static String deCamelCaseize(String exceptionClassName) {
        Pattern deCamelCaseizerPattern = Pattern.compile("(\\p{javaUpperCase})(\\p{javaLowerCase}+)");
        Matcher deCamelCaseizingMatcher = deCamelCaseizerPattern.matcher(exceptionClassName);
        StringBuffer sbuf = new StringBuffer();
        while (deCamelCaseizingMatcher.find()) {
            deCamelCaseizingMatcher.appendReplacement(sbuf, lowerCase(deCamelCaseizingMatcher.group(0)));
            sbuf.append(' ');
        }
        deCamelCaseizingMatcher.appendTail(sbuf);
        String deCamelCasizedExceptionName = sbuf.toString();
        return deCamelCasizedExceptionName;
    }

    public static String formatError(Exception e) {
        if (null == e) {
            return null;
        }
        String exceptionClassName = e.getClass().getSimpleName();
        String simpleExceptionClassName = removeEnd(removeEnd(exceptionClassName, "Error"), "Exception");
        return String.format("%s: %s", deCamelCaseize(simpleExceptionClassName), e.getMessage());
    }

}
