package org.wikipedia.ro.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.wikipedia.ro.model.WikiPart;

public class AggregatingParser {
    public static final List<WikiPartParser<? extends WikiPart>> ALL_PARSERS =
        Arrays.asList(new WikiLinkParser(), new WikiFileParser(), new WikiTemplateParser(), new WikiReferenceParser(),
            new WikiTableParser(), new WikiTagParser(), new WikiTextParser());

    public List<WikiPart> parse(String wikiText) {
        if (null == wikiText) {
            return null;
        }

        List<WikiPart> identifiedParts = new ArrayList<WikiPart>();
        ParseResult<? extends WikiPart> parseResult = null;
        do {
            for (WikiPartParser<? extends WikiPart> eachParser : ALL_PARSERS) {
                if (eachParser.startsWithMe(wikiText)) {
                    parseResult = eachParser.parse(wikiText);
                    if (null != parseResult) {
                        identifiedParts.add(parseResult.getIdentifiedPart());
                    }
                }
            }
        } while (null != parseResult && null != parseResult.getUnparsedString()
            && 0 < parseResult.getUnparsedString().length());
        return identifiedParts;
    }

}
