package org.wikipedia.ro.utils;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class TextUtils {
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
}
