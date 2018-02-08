package org.wikipedia.ro.parser;

import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

import org.wikipedia.ro.model.WikiLink;
import org.wikipedia.ro.model.WikiPart;

public class WikiLinkParser extends WikiPartParser<WikiLink> {

    @Override
    public boolean startsWithMe(String wikiText) {
        return null != wikiText && wikiText.startsWith("[[") && 2 < wikiText.indexOf("]]", 2)
            && (wikiText.indexOf("]]", 2) < wikiText.indexOf("[[", 2) || 0 > wikiText.indexOf("[[", 2));
    }

    @Override
    public ParseResult<WikiLink> parse(String wikiText) {
        if (!startsWithMe(wikiText)) {
            return null;
        }

        StringBuilder originalTextBuilder = new StringBuilder("[[");
        int idx = 2;
        boolean closed = false;
        Stack<String> bracketStack = new Stack<>();
        bracketStack.push("[[");
        StringBuilder labelBuilder = new StringBuilder();
        StringBuilder targetBuilder = new StringBuilder();
        StringBuilder crtBuilder = targetBuilder;
        boolean appendToBuilder;
        boolean labelPresent = false;
        while (idx < wikiText.length() && !closed) {
            char nextChar = wikiText.charAt(idx + 1);
            appendToBuilder = true;
            switch (wikiText.charAt(idx)) {
            case '{':
                if ('{' == nextChar) {
                    bracketStack.push("{{");
                    originalTextBuilder.append("{");
                    appendToBuilder = false;
                    idx++;
                }
                break;
            case '}':
                if ('}' == nextChar && "{{".equals(bracketStack.peek())) {
                    bracketStack.pop();
                    originalTextBuilder.append("}");
                    appendToBuilder = false;

                    idx++;
                }
                break;
            case ']':
                if (']' == nextChar && "[[".equals(bracketStack.peek()) && 1 == bracketStack.size()) {
                    closed = true;
                    originalTextBuilder.append("]");
                    bracketStack.pop();
                    appendToBuilder = false;
                    idx++;
                }
                break;
            case '|':
                if ("[[".equals(bracketStack.peek()) && 1 == bracketStack.size()) {
                    appendToBuilder = false;
                    labelPresent = true;
                    crtBuilder = labelBuilder;
                }
                break;
            default:
                break;
            }

            if (appendToBuilder) {
                crtBuilder.append(wikiText.charAt(idx));
            }
            originalTextBuilder.append(wikiText.charAt(idx));
            idx++;
        }

        WikiLink identifiedLink = new WikiLink();
        identifiedLink.setTarget(targetBuilder.toString());

        if (labelPresent) {
            AggregatingParser labelParser = new AggregatingParser();
            List<ParseResult<? extends WikiPart>> labelParts = labelParser.parse(labelBuilder.toString());
            identifiedLink
                .setLabelStructure(labelParts.stream().map(ParseResult::getIdentifiedPart).collect(Collectors.toList()));
        }

        return new ParseResult<>(identifiedLink, originalTextBuilder.toString(), wikiText.substring(idx));
    }

}
