package org.wikipedia.ro.populationdb.util;

import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.lowerCase;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class UkrainianTransliterator extends Transliterator {
    private static final char SEMN_MOALE = 'Ь';
    private static final char SEMN_TARE = '`';
    private static final Map<Character, String> charmap = new HashMap<Character, String>() {
        {
            put('А', "A");
            put('Б', "B");
            put('В', "V");
            put('Г', "H");
            put('Ґ', "G");
            put('Д', "D");
            put('Е', "E");
            put('Є', "IE");
            put('І', "I");
            put('Ї', "II");
            put('Ж', "J");
            put('З', "Z");
            put('И', "Î");
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
            put('Щ', "ȘCI");
            put('Ю', "IU");
            put('Я', "EA");
            put('Ъ', "Ă");
        }
    };

    public UkrainianTransliterator(final String txt) {
        super(txt);
    }

    @Override
    public String transliterate() {
        final StringBuilder transformedString = new StringBuilder();
        Character prev = null;
        Character crt = null;
        Character originalCrt = null;
        for (int i = 0; i < text.length(); i++) {
            String transformedChar = null;
            originalCrt = crt = text.charAt(i);

            if (Character.isLowerCase(crt)) {
                crt = Character.toUpperCase(crt);
            }
            switch (crt.charValue()) {
            case 'Ч':
                if (i + 1 >= text.length()) {
                    transformedChar = charmap.get(crt);
                } else if (Arrays.asList('Е', 'І', 'Ю', 'Я', 'Ї', 'Є').contains(Character.toUpperCase(text.charAt(i + 1)))) {
                    transformedChar = "C";
                } else if (Arrays.asList('И', 'О', 'У').contains(Character.toUpperCase(text.charAt(i + 1)))) {
                    transformedChar = "CI";
                } else if (text.charAt(i + 1) == 'А') {
                    transformedChar = "CE";
                } else {
                    transformedChar = charmap.get(crt);
                }
                break;
            case 'Щ':
                if (i + 1 >= text.length()) {
                    transformedChar = charmap.get(crt);
                } else if (Arrays.asList('Е', 'І', 'Ю', 'Я', 'Ї', 'Є').contains(Character.toUpperCase(text.charAt(i + 1)))) {
                    transformedChar = "ȘC";
                } else if (Arrays.asList('И', 'О', 'У').contains(Character.toUpperCase(text.charAt(i + 1)))) {
                    transformedChar = "ȘCI";
                } else if (text.charAt(i + 1) == 'А') {
                    transformedChar = "ȘCE";
                } else {
                    transformedChar = charmap.get(crt);
                }
                break;
            case 'К':
                if (i + 1 >= text.length()) {
                    transformedChar = charmap.get(crt);
                } else if (text.charAt(i + 1) == 'ь') {
                    transformedChar = "CHI";
                } else {
                    transformedChar = charmap.get(crt);
                }
                break;
            case SEMN_MOALE:
                if (i + 1 >= text.length()) {
                    transformedChar = "";
                } else if (prev == 'С' && Character.toUpperCase(text.charAt(i + 1)) == 'О') {
                    transformedChar = "I";
                } else {
                    transformedChar = "";
                }
                break;
            case SEMN_TARE:
                transformedChar = "";
                break;
            case 'Я':
                if (null == prev || Arrays.asList('А', 'Е', 'О', 'У', 'Ъ', 'Ю', 'Я', 'Э', 'И').contains(prev)) {
                    transformedChar = "IA";
                } else if (Arrays.asList('І', 'Й', 'Ї').contains(prev)) {
                    transformedChar = "A";
                } else {
                    transformedChar = charmap.get(crt);
                }
                break;
            case 'Є':
                if (null == prev || Arrays.asList('А', 'Е', 'О', 'У', 'Ъ', 'Ю', 'Я', 'Э', 'И').contains(prev)) {
                    transformedChar = "IE";
                } else if (Arrays.asList('І', 'Й', 'Ї').contains(prev)) {
                    transformedChar = "E";
                } else {
                    transformedChar = charmap.get(crt);
                }
                break;
            case 'Ї':
                if (null == prev || Arrays.asList('А', 'Е', 'О', 'У', 'Ъ', 'Ю', 'Я', 'Э', 'И').contains(prev)) {
                    transformedChar = "II";
                } else if (Arrays.asList('І', 'Й', 'Ї').contains(prev)) {
                    transformedChar = "I";
                } else {
                    transformedChar = charmap.get(crt);
                }
                break;
            case ' ':
                crt = null;
                transformedChar = " ";
                break;
            default:
                transformedChar = defaultString(charmap.get(crt), String.valueOf(crt));
            }

            transformedString.append(Character.isLowerCase(originalCrt) ? lowerCase(transformedChar)
                : capitalize(lowerCase(transformedChar)));
            prev = crt;
        }
        return transformedString.toString();
    }

}
