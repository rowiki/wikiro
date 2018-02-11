package org.wikipedia.ro.model;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class PlainText extends WikiPart {
    private String text;

    public String getText() {
        return text;
    }

    public PlainText() {
        super();
        // TODO Auto-generated constructor stub
    }

    public void setText(String text) {
        this.text = text;
    }

    public PlainText(String text) {
        super();
        this.text = text;
    }
    
    public String toString() {
        return text;
    }

    @Override
    protected Collection<List<WikiPart>> getAllWikiPartCollections() {
        return Collections.emptyList();
    }

    
}
