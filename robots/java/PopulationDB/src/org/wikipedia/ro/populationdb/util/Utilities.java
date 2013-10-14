package org.wikipedia.ro.populationdb.util;

import java.awt.Color;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public class Utilities {
    private static final char SEMN_MOALE_BG = 'Ь';
    private static final Map<Character, String> charmap = new HashMap<Character, String>() {
        {
            put('А', "A");
            put('Б', "B");
            put('В', "V");
            put('Г', "G");
            put('Д', "D");
            put('Е', "E");
            put('Ж', "J");
            put('З', "Z");
            put('И', "I");
            put('Й', "I");
            put('К', "K");
            put('Л', "L");
            put('М', "M");
            put('Н', "N");
            put('О', "O");
            put('П', "P");
            put('Р', "R");
            put('С', "S");
            put('Т', "T");
            put('У', "U");
            put('Ф', "F");
            put('Х', "H");
            put('Ц', "Ț");
            put('Й', "I");
            put('Ч', "CI");
            put('Ш', "Ș");
            put('Щ', "ȘT");
            put('Ю', "IU");
            put('Я', "EA");
            put('Ъ', "Ă");
        }
    };

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

    public static String colorToHtml(final Color color) {
        final StringBuilder sb = new StringBuilder("#");
        sb.append(StringUtils.substring(StringUtils.leftPad(Integer.toHexString(color.getRed()), 2, '0'), 0, 2));
        sb.append(StringUtils.substring(StringUtils.leftPad(Integer.toHexString(color.getGreen()), 2, '0'), 0, 2));
        sb.append(StringUtils.substring(StringUtils.leftPad(Integer.toHexString(color.getBlue()), 2, '0'), 0, 2));
        return sb.toString();
    }

    public static String transliterateBg(final String in) {
        final StringBuilder transformedString = new StringBuilder();
        Character prev = null;
        Character crt = null;
        for (int i = 0; i < in.length(); i++) {
            String transformedChar = null;
            crt = in.charAt(i);
            switch (crt.charValue()) {
            case 'Г':
                if (i + 1 >= in.length()) {
                    transformedChar = charmap.get(crt);
                } else if (Arrays.asList('Е', 'И', 'Ю', 'Я').contains(in.charAt(i + 1))) {
                    transformedChar = "GH";
                } else if (in.charAt(i + 1) == 'ь') {
                    transformedChar = "GHI";
                } else {
                    transformedChar = charmap.get(crt);
                }
                break;
            case 'Й':
                if (i + 1 >= in.length()) {
                    transformedChar = charmap.get(crt);
                } else if (null != prev && prev.charValue() == 'И' && i == in.length() - 1) {
                    transformedChar = "";
                } else {
                    transformedChar = charmap.get(crt);
                }
                break;
            case 'Ч':
                if (i + 1 >= in.length()) {
                    transformedChar = charmap.get(crt);
                } else if (Arrays.asList('Е', 'И', 'Ю', 'Я').contains(in.charAt(i + 1))) {
                    transformedChar = "C";
                } else if (in.charAt(i + 1) == 'А') {
                    transformedChar = "CE";
                } else {
                    transformedChar = charmap.get(crt);
                }
                break;
            case 'К':
                if (i + 1 >= in.length()) {
                    transformedChar = charmap.get(crt);
                } else if (in.charAt(i + 1) == 'ь') {
                    transformedChar = "CHI";
                } else {
                    transformedChar = charmap.get(crt);
                }
                break;
            case SEMN_MOALE_BG:
                if (i + 1 >= in.length()) {
                    transformedChar = "";
                } else if (prev == 'С' && in.charAt(i + 1) == 'О') {
                    transformedChar = "I";
                } else {
                    transformedChar = "";
                }
                break;
            case 'Я':
                if (null == prev || Arrays.asList('А', 'Е', 'О', 'У', 'Ъ', 'Ю', 'Я', 'Э').contains(prev)) {
                    transformedChar = "IA";
                } else if (Arrays.asList('И', 'Й').contains(prev)) {
                    transformedChar = "A";
                } else {
                    transformedChar = charmap.get(crt);
                }
                break;
            case ' ':
                crt = null;
                transformedChar = " ";
                break;
            default:
                transformedChar = StringUtils.defaultString(charmap.get(crt), String.valueOf(crt));
            }
            transformedString.append(transformedChar);
            prev = crt;
        }
        return transformedString.toString();
    }

}
