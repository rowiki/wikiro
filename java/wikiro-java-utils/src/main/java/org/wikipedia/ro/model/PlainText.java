package org.wikipedia.ro.model;

public class PlainText extends WikiPart {
    private String text;

    public String getText() {
        return text;
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
    
}
