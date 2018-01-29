package org.wikipedia.ro.parser;

import java.util.Arrays;
import java.util.List;

import org.wikipedia.ro.model.WikiPart;

import static org.apache.commons.lang3.StringUtils.*;

public class WikiTagParser extends WikiPartParser {

    private List<String> validTagNames = Arrays.asList("div", "font", "table", "tr", "th", "td", "span", "br", "tt");
    
    @Override
    public boolean startsWithMe(String wikiText) {
        if (null == wikiText || !wikiText.startsWith("<")) {
            return false;
        }
        String tagFromTagName = trim(removeStart(wikiText, "<"));
        if (validTagNames.contains(split(tagFromTagName)[0])) {
            return true;
        }
        return false;
    }

    @Override
    public void parse(String wikiText, TriFunction<WikiPart, String, String, Void> resumeCallback) {
        // TODO Auto-generated method stub
        
    }
    
}
