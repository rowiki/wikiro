package org.wikipedia.ro.parser;

import org.wikipedia.ro.model.WikiPart;

public abstract class WikiPartParser<T extends WikiPart> {
    public abstract boolean startsWithMe(String wikiText);

    public abstract ParseResult<T> parse(String wikiText);
}
