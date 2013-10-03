package org.wikipedia.ro.populationdb.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;

import org.apache.commons.lang3.StringUtils;

/**
 * A self-made push-down automaton that receives the text of a template declaration and identifies its arguments
 * @author andrei.stroe@gmail.com
 */
public class ParameterReader {

    private final String analyzedText;
    private final Map<String, String> params = new LinkedHashMap<String, String>();
    private final Stack<String> automatonStack = new Stack<String>();

    public ParameterReader(final String text) {
        analyzedText = text;
    }

    public void run() {
        final char[] chars = analyzedText.toCharArray();
        int index = 0;
        while (chars[index] != '|') { // skip initial template name
            index++;
        }
        index++;
        String crtParamName = null;
        int crtParamIndex = 1;
        final StringBuilder crtBuilder = new StringBuilder();
        loop: while (index < chars.length - 1) {
            final char crtChar = chars[index];
            final char nextChar = chars[index + 1];

            switch (crtChar) {
            case '=':
                if (automatonStack.isEmpty() && crtParamName == null) {
                    crtParamName = crtBuilder.toString();
                    crtBuilder.delete(0, crtBuilder.length());
                } else {
                    crtBuilder.append(crtChar);
                }
                index++;
                break;
            case '}':
            case ']':
                if (nextChar == crtChar) {
                    if (!automatonStack.isEmpty()) {
                        crtBuilder.append(crtChar);
                        crtBuilder.append(crtChar);
                        automatonStack.pop();
                        index += 2;
                    } else {
                        params.put(StringUtils.defaultString(StringUtils.trim(crtParamName), String.valueOf(crtParamIndex++)),
                            StringUtils.trim(crtBuilder.toString()));
                        crtBuilder.delete(0, crtBuilder.length());
                        crtParamName = null;
                        break loop;
                    }
                    break;
                } else {
                    crtBuilder.append(crtChar);
                    index++;
                }

                break;
            case '|':
                if (automatonStack.isEmpty()) {
                    params.put(StringUtils.defaultString(StringUtils.trim(crtParamName), String.valueOf(crtParamIndex++)),
                        StringUtils.trim(crtBuilder.toString()));
                    crtParamName = null;
                    crtBuilder.delete(0, crtBuilder.length());
                } else {
                    crtBuilder.append(crtChar);
                }
                index++;
                break;
            case '{':
            case '[':
                if (nextChar == crtChar) {
                    automatonStack.push("" + crtChar + nextChar);
                    crtBuilder.append(crtChar);
                    crtBuilder.append(nextChar);
                    index += 2;
                } else {
                    crtBuilder.append(crtChar);
                    index++;
                }
                break;
            default:
                crtBuilder.append(crtChar);
                index++;
            }
        }

    }

    public Map<String, String> getParams() {
        return params;
    }

}
