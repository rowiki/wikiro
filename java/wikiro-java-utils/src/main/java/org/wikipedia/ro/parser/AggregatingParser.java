package org.wikipedia.ro.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.wikipedia.ro.model.WikiPart;

public class AggregatingParser {
    protected static final List<WikiPartParser<? extends WikiPart>> ALL_PARSERS =
        Arrays.asList(new WikiFileParser(), new WikiLinkParser(), new WikiTemplateParser(),
            new WikiTableParser(), new WikiTagParser(), new WikiTextParser());

    public List<ParseResult<? extends WikiPart>> parse(String wikiText) {
        if (null == wikiText) {
            return null;
        }

        List<ParseResult<? extends WikiPart>> resultList = new ArrayList<ParseResult<? extends WikiPart>>();
        ParseResult<? extends WikiPart> parseResult = null;
        String toParse = wikiText;
        while (null != toParse
            && 0 < toParse.length()) {
            for (WikiPartParser<? extends WikiPart> eachParser : ALL_PARSERS) {
                if (eachParser.startsWithMe(toParse)) {
                    parseResult = eachParser.parse(toParse);
                    if (null != parseResult) {
                        resultList.add(parseResult);
                        toParse = parseResult.getUnparsedString();
                        break;
                    }
                }
            }
        }
        return resultList;
    }

}
