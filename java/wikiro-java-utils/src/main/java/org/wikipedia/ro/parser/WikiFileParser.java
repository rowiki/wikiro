package org.wikipedia.ro.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.wikipedia.ro.model.WikiPart;

public class WikiFileParser extends WikiPartParser {

    private static Pattern FILE_START_MATCHER = Pattern.compile("\\[\\[(Fișier|File|Imagine|Image|Media)");

    public boolean startsWithMe(String s) {
        Matcher fileStartMatcher = FILE_START_MATCHER.matcher(s);
        if (!fileStartMatcher.find() || 0 < fileStartMatcher.start()) {
            return false;
        }
        return true;
    }

    @Override
    public void parse(String wikiText, TriFunction<WikiPart, String, String, Void> resumeCallback) {
        // TODO Auto-generated method stub
        
    }
}
