package org.wikipedia.ro.parser;

import org.wikipedia.ro.model.WikiPart;

public class WikiTableParser extends WikiPartParser {

    public boolean startsWithMe(String wikiText) {
        return null != wikiText && wikiText.startsWith("{|");
    }

    @Override
    public void parse(String wikiText, TriFunction<WikiPart, String, String, Void> resumeCallback) {
        // TODO Auto-generated method stub
        
    }

}
