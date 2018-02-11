package org.wikipedia.ro.model;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WikiTag extends WikiPart {
    private String tagName;
    private boolean selfClosing;
    private boolean closing;
    private Map<String, List<WikiPart>> attributes = new LinkedHashMap<>();

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

    public void setAttribute(String k, List<WikiPart> v) {
        attributes.put(k, v);
    }

    public void removeAttribute(String k) {
        attributes.remove(k);
    }

    @Override
    public String toString() {
        StringBuilder sbuild = new StringBuilder("<");
        sbuild.append(closing ? "/" : "").append(tagName);

        if (!attributes.isEmpty()) {
            sbuild.append(' ');
        }

        sbuild.append(attributes.entrySet().stream()
            .map(entry -> String.format("%s=\"%s\"", entry.getKey(),
                entry.getValue().stream().map(value -> value.toString()).collect(Collectors.joining())))
            .collect(Collectors.joining(" "))).append(selfClosing ? " /" : "").append('>');

        return sbuild.toString();
    }

    @Override
    protected Collection<List<WikiPart>> getAllWikiPartCollections() {
        return attributes.values();
    }

}
