package org.wikipedia.ro.model;

import java.util.Collection;
import java.util.HashMap;
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

    public WikiTag setTagName(String tagName) {
        this.tagName = tagName;
        return this;
    }

    public boolean isSelfClosing() {
        return selfClosing;
    }

    public WikiTag setSelfClosing(boolean selfClosing) {
        this.selfClosing = selfClosing;
        return this;
    }

    public boolean isClosing() {
        return closing;
    }

    public WikiTag setClosing(boolean closing) {
        this.closing = closing;
        return this;
    }

    public Map<String, List<WikiPart>> getAttributes() {
        return attributes;
    }
    
    public WikiTag clearAttributes() {
        this.attributes = new HashMap<>();
        return this;
    }

    public WikiTag setAttribute(String k, List<WikiPart> v) {
        attributes.put(k, v);
        return this;
    }

    public WikiTag removeAttribute(String k) {
        attributes.remove(k);
        return this;
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
