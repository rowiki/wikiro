package org.wikipedia.ro.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class WikiFile extends WikiPart {
    private String name;
    private String size;
    private String location;
    private String border;
    private String namespace;
    private String link;
    private String alignment;
    private String alt;
    private String lang;
    private String displayType;
    private List<List<WikiPart>> captions = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getBorder() {
        return border;
    }

    public void setBorder(String border) {
        this.border = border;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getAlignment() {
        return alignment;
    }

    public void setAlignment(String alignment) {
        this.alignment = alignment;
    }

    public String getAlt() {
        return alt;
    }

    public void setAlt(String alt) {
        this.alt = alt;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public List<WikiPart> getCaption() {
        return 0 == captions.size() ? null : captions.get(0);
    }

    public void addCaption(List<WikiPart> caption) {
        captions.add(caption);
    }

    public List<List<WikiPart>> getCaptions() {
        return captions;
    }

    public void setExtraCaptions(List<List<WikiPart>> extraCaptions) {
        this.captions = extraCaptions;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getDisplayType() {
        return displayType;
    }

    public void setDisplayType(String displayType) {
        this.displayType = displayType;
    }

    @Override
    public String toString() {
        List<String> parts = new ArrayList<>();
        parts.add(namespace + ':' + name);
        if (null != displayType) {
            parts.add(displayType);
        }
        if (null != size) {
            parts.add(size);
        }
        if (null != location) {
            parts.add(location);
        }
        if (null != border) {
            parts.add(border);
        }
        if (null != alignment) {
            parts.add(alignment);
        }
        if (null != lang) {
            parts.add("lang=" + lang);
        }
        if (null != alt) {
            parts.add("alt=" + alt);
        }
        if (null != link) {
            parts.add("link=" + link);
        }
        for (List<WikiPart> eachCaption: captions) {
            parts.add(eachCaption.stream().map(elem -> elem.toString()).collect(Collectors.joining()));
        }
        
        return "[[" + parts.stream().map(elem -> elem.toString()).collect(Collectors.joining("|")) + "]]";
    }

}
