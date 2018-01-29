package org.wikipedia.ro.parser;

import org.wikipedia.ro.model.WikiPart;

public abstract class WikiPartParser {
    public abstract boolean startsWithMe(String wikiText);

    public abstract void parse(String wikiText, TriFunction<WikiPart, String, String, Void> resumeCallback);
}
