package org.wikipedia.ro.parser;

import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.apache.commons.lang3.StringUtils.split;
import static org.apache.commons.lang3.StringUtils.trim;

import java.util.Arrays;
import java.util.List;

import org.wikipedia.ro.model.WikiTag;

public class WikiTagParser extends WikiPartParser<WikiTag> {

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
    public ParseResult<WikiTag> parse(String wikiText) {
        // TODO Auto-generated method stub
        return null;
    }

}
