package org.wikipedia.ro.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.wikipedia.ro.model.WikiReference;

public class WikiReferenceParser extends WikiPartParser<WikiReference> {

    private static final Pattern START_PATTERN = Pattern.compile("<\\s*ref\\s");

    public boolean startsWithMe(String wikiText) {
        if (null == wikiText) {
            return false;
        }
        Matcher startMatcher = START_PATTERN.matcher(wikiText);
        if (startMatcher.find() && 0 == startMatcher.start()) {
            return true;
        }
        return false;
    }

    @Override
    public ParseResult<WikiReference> parse(String wikiText) {
        // TODO Auto-generated method stub
        return null;
    }
}
