package org.wikipedia.ro.parser;

import org.wikipedia.ro.model.WikiPart;

public class WikiLinkParser extends WikiPartParser {

    @Override
    public boolean startsWithMe(String wikiText) {
        return null != wikiText && wikiText.startsWith("[[") && 2 < wikiText.indexOf("]]");
    }

    @Override
    public void parse(String wikiText, TriFunction<WikiPart, String, String, Void> resumeCallback) {
        // TODO Auto-generated method stub
        
    }

}
