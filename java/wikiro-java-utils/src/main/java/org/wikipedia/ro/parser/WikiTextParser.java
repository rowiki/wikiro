package org.wikipedia.ro.parser;

import org.wikipedia.ro.model.WikiPart;

public class WikiTextParser extends WikiPartParser {

    @Override
    public boolean startsWithMe(String wikiText) {
        return true;
    }

    @Override
    public void parse(String wikiText, TriFunction<WikiPart, String, String, Void> resumeCallback) {
        // TODO Auto-generated method stub
        
    }

}
