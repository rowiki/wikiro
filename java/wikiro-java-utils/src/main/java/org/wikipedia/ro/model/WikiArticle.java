package org.wikipedia.ro.model;

import java.util.ArrayList;
import java.util.List;

public class WikiArticle {
    private String title;
    private String text;
    private List<WikiPart> parts;

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
}
