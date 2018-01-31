package org.wikipedia.ro.parser;

import org.wikipedia.ro.model.WikiPart;

public class ParseResult<T extends WikiPart> {

    private T identifiedPart;
    private String parsedString;
    private String unparsedString;

    public ParseResult() {
        super();
        // TODO Auto-generated constructor stub
    }

    public ParseResult(T identifiedPart, String parsedString, String unparsedString) {
        super();
        this.identifiedPart = identifiedPart;
        this.parsedString = parsedString;
        this.unparsedString = unparsedString;
    }

    public T getIdentifiedPart() {
        return identifiedPart;
    }

    public void setIdentifiedPart(T identifiedPart) {
        this.identifiedPart = identifiedPart;
    }

    public String getParsedString() {
        return parsedString;
    }

    public void setParsedString(String parsedString) {
        this.parsedString = parsedString;
    }

    public String getUnparsedString() {
        return unparsedString;
    }

    public void setUnparsedString(String unparsedString) {
        this.unparsedString = unparsedString;
    }
}
