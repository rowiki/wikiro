package org.wikipedia.ro.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

import org.wikipedia.ro.model.WikiPart;

public class AggregatingParser {
    private List<WikiPartParser> parsers =
        Arrays.asList(new WikiLinkParser(), new WikiFileParser(), new WikiTemplateParser(), new WikiReferenceParser(),
            new WikiTableParser(), new WikiTagParser(), new WikiTextParser());

    private List<WikiPart> identifiedParts = new ArrayList<WikiPart>();

    private TriFunction<WikiPart, String, String, Void> resumeCallback = (parsedPart, parsedText, restOfWikiText) -> {
        if (null != parsedPart) {
            identifiedParts.add(parsedPart);
        }
        
        parse(restOfWikiText);
        
        return null;
    };
    
    public void parse(String wikiText) {
        if (null == wikiText) {
            return;
        }

        for (WikiPartParser eachParser : parsers) {
            if (eachParser.startsWithMe(wikiText)) {
                eachParser.parse(wikiText, resumeCallback);
            }
        }
    }

}
