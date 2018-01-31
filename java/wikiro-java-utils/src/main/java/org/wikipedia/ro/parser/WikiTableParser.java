package org.wikipedia.ro.parser;

import org.wikipedia.ro.model.WikiTable;

public class WikiTableParser extends WikiPartParser<WikiTable> {

    public boolean startsWithMe(String wikiText) {
        return null != wikiText && wikiText.startsWith("{|");
    }

    @Override
    public ParseResult<WikiTable> parse(String wikiText) {
        // TODO Auto-generated method stub
        return null;
    }

}
