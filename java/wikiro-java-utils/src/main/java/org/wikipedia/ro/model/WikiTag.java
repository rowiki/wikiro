package org.wikipedia.ro.model;

import java.util.List;
import java.util.Map;

public class WikiTag extends WikiPart {
    private String tagName;
    private boolean selfClosing;
    private boolean closing;
    private Map<String, List<WikiPart>> attributes;

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public boolean isSelfClosing() {
        return selfClosing;
    }

    public void setSelfClosing(boolean selfClosing) {
        this.selfClosing = selfClosing;
    }

    public boolean isClosing() {
        return closing;
    }

    public void setClosing(boolean closing) {
        this.closing = closing;
    }

    public Map<String, List<WikiPart>> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, List<WikiPart>> attributes) {
        this.attributes = attributes;
    }

}
