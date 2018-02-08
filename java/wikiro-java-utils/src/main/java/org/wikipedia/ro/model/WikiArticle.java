package org.wikipedia.ro.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.wikipedia.Wiki;
import org.wikipedia.ro.parser.AggregatingParser;

public class WikiArticle {
    private String title;
    private String text;
    private Wiki wiki;
    private List<WikiPart> parts;

    public WikiArticle(String title, Wiki wiki) {
        super();
        this.title = title;
        this.wiki = wiki;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
        parts = new AggregatingParser().parse(text).stream().map(elem -> elem.getIdentifiedPart()).collect(Collectors.toList());
    }

    public List<? extends WikiPart> getParts() {
        return parts;
    }

    public void setParts(List<WikiPart> parts) {
        this.parts = parts;
    }

    public void addPart(WikiPart part) {
        if (null == parts) {
            parts = new ArrayList<>();
        }
        parts.add(part);
    }
    
    public void assembleText() {
        if (null == parts) {
            return;
        }
        StringBuilder textBuilder = new StringBuilder();
        for (WikiPart eachPart : parts) {
            textBuilder.append(eachPart.toString());
        }
        text = textBuilder.toString();
    }
    
    public void load() throws IOException {
        setText(wiki.getPageText(title));
    }
    
}
