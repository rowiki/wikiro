package org.wikipedia.ro.utils;

import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.apache.commons.lang3.StringUtils.removeEnd;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class TextUtils {
    private TextUtils() {
        
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

        final List<String> notCapitalized = Arrays.asList("de", "din", "pe", "sub", "peste", "la", "cel", "lui", "cu");

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
