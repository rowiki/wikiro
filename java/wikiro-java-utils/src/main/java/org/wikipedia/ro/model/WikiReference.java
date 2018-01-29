package org.wikipedia.ro.model;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WikiReference extends WikiPart {
    private boolean tag;
    private String name;
    private String group;
    private List<WikiPart> content;
    public boolean isTag() {
        return tag;
    }
    public void setTag(boolean tag) {
        this.tag = tag;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getGroup() {
        return group;
    }
    public void setGroup(String group) {
        this.group = group;
    }
    public List<WikiPart> getContent() {
        return content;
    }
    public void setContent(List<WikiPart> content) {
        this.content = content;
    }

}
