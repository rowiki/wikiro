package org.wikipedia.ro.model;

import static org.apache.commons.collections4.SetUtils.unmodifiableSet;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.trim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

/**
 * A self-made push-down automaton that receives the text of a template declaration and identifies its arguments
 * 
 * @author andrei.stroe@gmail.com
 */
public class WikiTemplate extends WikiPart {

    private final Map<String, String> rawParams = new LinkedHashMap<>();
    private final Map<String, List<? extends WikiPart>> params = new LinkedHashMap<>();
    private final Stack<String> automatonStack = new Stack<>();
    private String beforeText;

    private String templateTitle;
    private boolean singleLine = true;
    private String initialTemplateText;

    private int templateLength = 0;

    public boolean isSingleLine() {
        return singleLine;
    }

    public WikiTemplate() {
        super();
    }

    public WikiTemplate(String analyzedText) {
        this();
        fromString(analyzedText);
    }

    /**
     * Use {@code WikiTemplateParser} to build an actual model of the text and find the template in there.
     * 
     * Legacy method used to find and parse a template and store its parameters as plain strings.
     * 
     * @param analyzedText
     */
    @Deprecated
    public void fromString(String analyzedText) {
        if (null == analyzedText || 0 == analyzedText.length()) {
            return;
        }
        final char[] chars = analyzedText.toCharArray();
        int index = 0;
        StringBuilder templateTitleBuilder = new StringBuilder();
        StringBuilder templateTextBuilder = new StringBuilder("{{");
        StringBuilder beforeTextBuilder = new StringBuilder();
        while (index < chars.length - 1 && (chars[index] != '{' || chars[index + 1] != '{')) { // skip pre-template stuff
            beforeTextBuilder.append(chars[index]);
            index++;
        }
        beforeText = beforeTextBuilder.toString();
        if (index >= chars.length - 1) {
            return;
        }
        index += 2;
        while (index < chars.length && chars[index] != '|' && chars[index] != '}') { // skip initial template name
            templateTitleBuilder.append(chars[index]);
            templateTextBuilder.append(chars[index]);
            index++;
        }
        if (chars[index] == '|') {
            templateTextBuilder.append('|');
            index++;
        }
        templateTitle = trim(templateTitleBuilder.toString());

        String crtParamName = null;
        int crtParamIndex = 1;
        final StringBuilder crtBuilder = new StringBuilder();
        loop: while (index < chars.length - 1) {
            final char crtChar = chars[index];
            final char nextChar = chars[index + 1];
            templateTextBuilder.append(crtChar);

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
                    templateTextBuilder.append(nextChar);
                    if (!automatonStack.isEmpty()) {
                        crtBuilder.append(crtChar);
                        crtBuilder.append(crtChar);
                        automatonStack.pop();
                        index += 2;
                    } else {
                        if (null == crtParamName) {
                            crtParamName = String.valueOf(crtParamIndex++);
                        }
                        rawParams.put(trim(crtParamName), trim(crtBuilder.toString()));
                        crtBuilder.delete(0, crtBuilder.length());
                        crtParamName = null;
                        templateLength = 2 + index;
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
                    if (null == crtParamName) {
                        crtParamName = String.valueOf(crtParamIndex++);
                    }
                    rawParams.put(trim(crtParamName), trim(crtBuilder.toString()));
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
                    templateTextBuilder.append(nextChar);
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
        initialTemplateText = templateTextBuilder.toString();
        if (initialTemplateText.matches("\\{\\{\\s*" + templateTitle + "\\s*\\}\\}")) {
            rawParams.remove("1");
        }
    }

    public Map<String, String> getParams() {
        return Collections.unmodifiableMap(rawParams);
    }

    public int getTemplateLength() {
        if (0 == templateLength) {
            return toString().length();
        }
        return templateLength;
    }

    public String getTemplateTitle() {
        return templateTitle;
    }

    public void setParam(String key, String value) {
        rawParams.put(key, value);
        params.put(key, Arrays.asList(new PlainText(value)));
    }

    public List<WikiPart> getParam(String key) {
        return Collections.unmodifiableList(params.get(key));
    }

    public void setParam(String key, List<WikiPart> value) {
        params.put(key, value);
    }

    public void removeParam(String key) {
        rawParams.remove(key);
        params.remove(key);
    }

    public void setTemplateTitle(String title) {
        templateTitle = title;
    }

    public void setSingleLine(boolean singleLine) {
        this.singleLine = singleLine;
    }

    public String getInitialTemplateText() {
        return initialTemplateText;
    }

    public Set<String> getParamNames() {
        return unmodifiableSet(new HashSet<String>(rawParams.keySet()));
    }

    public String getBeforeText() {
        return beforeText;
    }

    public String toString() {
        if (isEmpty(templateTitle)) {
            return "";
        }
        StringBuilder sbuild = new StringBuilder("{{");
        sbuild.append(templateTitle);

        List<Entry<String, List<? extends WikiPart>>> anonEntries =
            params.entrySet().stream().filter(entry -> entry.getKey().matches("\\d+")).collect(Collectors.toList());
        List<Entry<String, List<? extends WikiPart>>> namedEntries =
            params.entrySet().stream().filter(entry -> !entry.getKey().matches("\\d+")).collect(Collectors.toList());

        int prevVal = 0;
        int gap = -1;
        for (String eachAnonKey : anonEntries.stream().map(entry -> entry.getKey()).sorted().collect(Collectors.toList())) {
            if (1 + prevVal != Integer.parseInt(eachAnonKey)) {
                gap = 1 + prevVal;
                break;
            }
            prevVal = Integer.parseInt(eachAnonKey);
        }

        final int finalGap = gap;
        if (0 < finalGap) {
            List<Entry<String, List<? extends WikiPart>>> namedAnonEntries = anonEntries.stream()
                .filter(entry -> finalGap < Integer.parseInt(entry.getKey())).collect(Collectors.toList());
            List<Entry<String, List<? extends WikiPart>>> newNamedEntries = new ArrayList<>();
            newNamedEntries.addAll(namedAnonEntries);
            newNamedEntries.addAll(namedEntries);
            namedEntries = newNamedEntries;
            anonEntries = anonEntries.stream().filter(entry -> finalGap > Integer.parseInt(entry.getKey()))
                .collect(Collectors.toList());
        }

        final String anonEntryFormat = singleLine ? "|%s" : "\n|%s";
        final String namedEntryFormat = singleLine ? "|%s=%s" : "\n| %s = %s";

        String anonEntriesStr = anonEntries.stream()
            .map(entry -> String.format(anonEntryFormat,
                entry.getValue().stream().map(value -> value.toString()).collect(Collectors.joining())))
            .collect(Collectors.joining());
        String namedEntriesStr = namedEntries.stream()
            .map(entry -> String.format(namedEntryFormat, entry.getKey(),
                entry.getValue().stream().map(value -> value.toString()).collect(Collectors.joining())))
            .collect(Collectors.joining());

        sbuild.append(anonEntriesStr).append(namedEntriesStr);
        sbuild.append(singleLine ? "" : "\n");
        sbuild.append("}}");
        return sbuild.toString();
    }

    public void setLength(int i) {
        this.templateLength = i;
    }
}
