package org.wikipedia.ro.parser;

import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.apache.commons.lang3.StringUtils.split;
import static org.apache.commons.lang3.StringUtils.trim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

import org.wikipedia.ro.model.WikiPart;
import org.wikipedia.ro.model.WikiTag;

public class WikiTagParser extends WikiPartParser<WikiTag> {

    private List<String> validTagNames = Arrays.asList("div", "font", "table", "tr", "th", "td", "span", "br", "tt", "center");

    @Override
    public boolean startsWithMe(String wikiText) {
        if (null == wikiText || !wikiText.startsWith("<")) {
            return false;
        }
        String tagFromTagName = trim(removeStart(wikiText, "<"));
        return validTagNames.contains(split(tagFromTagName, " \n/>")[0]);
    }

    @Override
    public ParseResult<WikiTag> parse(String wikiText) {
        if (!startsWithMe(wikiText)) {
            return null;
        }
        // Automaton states:
        // -1 -- before starting <
        // 0 -- reading tag name
        // 2 -- reading attr or closing;
        // 4 -- expecting attr value;
        // 6 -- expecting >
        StringBuilder originalTextBuilder = new StringBuilder();
        StringBuilder tagNameBuilder = new StringBuilder();
        StringBuilder attrNameBuilder = new StringBuilder();
        StringBuilder attrValueBuilder = new StringBuilder();
        Stack<String> attrValueBracketing = new Stack<>();

        WikiTag tagUC = new WikiTag();

        boolean isFinishedReading = false;

        int idx = 0;
        int state = -1;
        while (!isFinishedReading && idx < wikiText.length()) {
            char crtChar = wikiText.charAt(idx);
            switch (state) {
            case -1:
                if ('<' == crtChar) {
                    state = 0;
                }
                break;
            case 0:
                if (Character.isWhitespace(crtChar) && 0 < tagNameBuilder.length()) {
                    state = 2;
                    tagUC.setTagName(tagNameBuilder.toString().trim());
                } else if ('/' == crtChar && 0 < tagNameBuilder.length()) {
                    state = 6;
                    tagUC.setTagName(tagNameBuilder.toString().trim());
                    tagUC.setSelfClosing(true);
                } else if ('/' == crtChar && 0 == tagNameBuilder.length()) {
                    tagUC.setClosing(true);
                } else if ('>' == crtChar) {
                    isFinishedReading = true;
                    tagUC.setTagName(tagNameBuilder.toString().trim());
                } else if (!Character.isWhitespace(crtChar)) {
                    tagNameBuilder.append(crtChar);
                }
                break;
            case 2:
                if (!Character.isWhitespace(crtChar) && !"=/>".contains(String.valueOf(crtChar))) {
                    attrNameBuilder.append(crtChar);
                } else if ('=' == crtChar && 0 < attrNameBuilder.length()) {
                    state = 4;
                } else if ('/' == crtChar && 0 < attrNameBuilder.length()) {
                    tagUC.setAttribute(attrNameBuilder.toString().trim(), new ArrayList<WikiPart>());
                    tagUC.setSelfClosing(true);
                    state = 6;
                } else if ('/' == crtChar && 0 == attrNameBuilder.length()) {
                    tagUC.setSelfClosing(true);
                    state = 6;
                } else if ('>' == crtChar && 0 < attrNameBuilder.length()) {
                    isFinishedReading = true;
                    tagUC.setAttribute(attrNameBuilder.toString().trim(), new ArrayList<WikiPart>());
                } else if ('>' == crtChar && 0 == attrNameBuilder.length()) {
                    isFinishedReading = true;
                }
                break;
            case 4:
                boolean isPartOfAttrName = true;
                if ('\'' == crtChar || '"' == crtChar) {
                    if (!attrValueBracketing.isEmpty() && String.valueOf(crtChar).equals(attrValueBracketing.peek())) {
                        state = 2;
                        AggregatingParser attrValueParser = new AggregatingParser();
                        List<ParseResult<WikiPart>> parsedValues =
                            attrValueParser.parse(attrValueBuilder.toString());
                        tagUC.setAttribute(attrNameBuilder.toString().trim(),
                            parsedValues.stream().map(ParseResult::getIdentifiedPart).collect(Collectors.toList()));
                        attrNameBuilder.setLength(0);
                        attrValueBuilder.setLength(0);
                        attrValueBracketing.pop();
                        isPartOfAttrName = false;
                    } else if (attrValueBracketing.isEmpty() && 0 == attrValueBuilder.length()) {
                        attrValueBracketing.push(String.valueOf(crtChar));
                        isPartOfAttrName = false;
                    } else if (attrValueBracketing.isEmpty() && 0 != attrValueBuilder.length()) {
                        return null;
                    }
                }
                if (isPartOfAttrName) {
                    if (!Character.isWhitespace(crtChar)) {
                        attrValueBuilder.append(crtChar);
                    }
                    if (Character.isWhitespace(crtChar) && attrValueBracketing.isEmpty() && 0 < attrNameBuilder.length()) {
                        tagUC.setAttribute(attrNameBuilder.toString().trim(),
                            new AggregatingParser().parse(attrValueBuilder.toString()).stream()
                                .map(ParseResult::getIdentifiedPart).collect(Collectors.toList()));
                        state = 2;
                        attrNameBuilder.setLength(0);
                        attrValueBuilder.setLength(0);
                    }
                }
                break;
            case 6:
                if ('>' != crtChar) {
                    return null;
                }
                isFinishedReading = true;
                break;
            default:
                break;
            }

            originalTextBuilder.append(crtChar);
            idx++;
        }
        if (validTagNames.stream().anyMatch(tag -> tag.equalsIgnoreCase(tagUC.getTagName()))) {
            ParseResult<WikiTag> resultingTag = new ParseResult<>();
            resultingTag.setIdentifiedPart(tagUC);
            tagUC.setInitialText(originalTextBuilder.toString());
            resultingTag.setParsedString(originalTextBuilder.toString());
            resultingTag.setUnparsedString(wikiText.substring(idx));
            return resultingTag;
        }
        return null;

    }

    public void setValidTagNames(List<String> validTagNames) {
        this.validTagNames = validTagNames;
    }

}
