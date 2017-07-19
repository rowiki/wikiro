package org.wikipedia.ro.transliterator;

import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.apache.commons.lang3.StringUtils.split;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

/**
 * Transliterates greek names based on the UN/ELOT system
 * 
 * @author astroe
 * 
 */
public class GreekTransliterator extends Transliterator {

    private static final Map<Character, String> unElotMap = new HashMap<Character, String>() {
        {
            put('α', "a");
            put('β', "v");
            put('γ', "g");
            put('δ', "d");
            put('ε', "e");
            put('ζ', "z");
            put('η', "i");
            put('θ', "th");
            put('ι', "i");
            put('κ', "k");
            put('λ', "l");
            put('μ', "m");
            put('ν', "n");
            put('ξ', "x");
            put('ο', "o");
            put('π', "p");
            put('ρ', "r");
            put('σ', "s");
            put('ς', "s");
            put('τ', "t");
            put('υ', "y");
            put('φ', "f");
            put('χ', "ch");
            put('ψ', "ps");
            put('ω', "o");
            put('ή', "i");
            put('έ', "e");
            put('ί', "i");
            put('ό', "o");
            put('ώ', "o");
            put('ύ', "y");
            put('ά', "a");
            put('ΐ', "i");
            put('ΰ', "y");
            put('ϊ', "i");
            put('ϋ', "y");
        }
    };

    @Override
    public String transliterate() {
        final StringBuilder transformedString = new StringBuilder();
        Character crt = null;
        for (int i = 0; i < text.length(); i++) {
            String transformedChar = null;
            crt = text.charAt(i);

            if (Character.isUpperCase(crt)) {
                crt = Character.toLowerCase(crt);
            }
            switch (crt.charValue()) {
            case 'ύ':
            case 'υ':
                if (0 == i) {
                    transformedChar = unElotMap.get(crt);
                } else if (Arrays.asList('α', 'ε', 'η', 'ά', 'έ', 'ή').contains(Character.toLowerCase(text.charAt(i - 1)))) {
                    if (text.length() >= i + 1
                        || Arrays.asList('θ', 'κ', 'ξ', 'π', 'σ', 'τ', 'φ', 'χ', 'ψ').contains(
                            Character.toLowerCase(text.charAt(i + 1)))) {
                        transformedChar = "f";
                    } else if (Arrays.asList('β', 'γ', 'δ', 'ζ', 'λ', 'μ', 'ν', 'ρ', 'α', 'ε', 'η', 'ι', 'ο', 'υ', 'ω', 'ά',
                        'έ', 'ή', 'ί', 'ό', 'ύ', 'ώ', 'ΐ', 'ΰ', 'ϊ', 'ϋ')
                        .contains(Character.toLowerCase(text.charAt(i + 1)))) {
                        transformedChar = "v";
                    }
                } else if (Arrays.asList('ό', 'ο').contains(Character.toLowerCase(text.charAt(i - 1)))) {
                    transformedChar = "u";
                }
                if (null == transformedChar) {
                    transformedChar = unElotMap.get(crt);
                }
                break;
            case 'γ':
                if (i + 1 >= text.length()) {
                    transformedChar = unElotMap.get(crt);
                } else if (Arrays.asList('γ', 'ξ', 'χ').contains(Character.toLowerCase(text.charAt(i + 1)))) {
                    transformedChar = "n";
                } else {
                    transformedChar = unElotMap.get(crt);
                }
                break;
            case 'μ':
                if (0 == i) {
                    transformedChar = "b";
                    i++;
                } else {
                    transformedChar = unElotMap.get(crt);
                }
                break;
            default:
                transformedChar = defaultString(unElotMap.get(crt), String.valueOf(crt));
            }

            transformedString.append(capitalize(lowerCase(transformedChar)));
        }

        final String[] transformedStringParts = split(lowerCase(transformedString.toString()), "-");
        for (int i = 0; i < transformedStringParts.length; i++) {
            final String[] transformedSpaceSeparatedParts = split(lowerCase(transformedStringParts[i]), " ");
            for (int j = 0; j < transformedSpaceSeparatedParts.length; j++) {
                transformedSpaceSeparatedParts[j] = capitalize(lowerCase(transformedSpaceSeparatedParts[j]));
            }
            transformedStringParts[i] = StringUtils.join(transformedSpaceSeparatedParts, " ");
        }

        return StringUtils.join(transformedStringParts, "-");
    }

    public GreekTransliterator(final String txt) {
        super(txt);
    }

}
