package org.wikipedia.ro.utils;

import static org.apache.commons.collections4.SetUtils.unmodifiableSet;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.trim;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * A self-made push-down automaton that receives the text of a template declaration and identifies its arguments
 * 
 * @author andrei.stroe@gmail.com
 */
public class WikiTemplate {

    private final Map<String, String> params = new LinkedHashMap<String, String>();
    private final Stack<String> automatonStack = new Stack<String>();
    private String beforeText;

    private String templateTitle;
    private boolean singleLine = false;
    private String initialTemplateText;

    private int templateLength = 0;

    public WikiTemplate() {
        super();
    }
    
    public WikiTemplate(String analyzedText) {
        this();
        fromString(analyzedText);
    }
    
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
                        params.put(trim(crtParamName), trim(crtBuilder.toString()));
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
                    params.put(trim(crtParamName), trim(crtBuilder.toString()));
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
    }

    public Map<String, String> getParams() {
        return params;
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
        params.put(key, value);
    }

    public void removeParam(String key) {
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
        return unmodifiableSet(new HashSet<String>(params.keySet()));
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
        for (Map.Entry<String, String> eachParamEntry: params.entrySet()) {
            sbuild.append(singleLine ? ' ' : '\n');
            sbuild.append("| ").append(eachParamEntry.getKey()).append(" = ").append(eachParamEntry.getValue());
        }
        sbuild.append(singleLine ? ' ' : '\n');
        sbuild.append("}}");
        return sbuild.toString();
    }
}
