package org.wikipedia.ro.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WikiLink extends WikiPart {
    private String target = null;
    public WikiLink() {
        super();
        // TODO Auto-generated constructor stub
    }
    
    public WikiLink(String target) {
        super();
        this.target = target;
    }
    
    public WikiLink(String target, String label) {
        super();
        this.target = target;
        this.label = label;
    }

    private String label = null;

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
    
    private static Pattern LINK_PATTERN = Pattern.compile("\\[\\[\\s*([^\\]\\|]+)(?:\\s*\\|([^\\]]+))?\\]\\]");
    
    public static WikiLink fromString(String linkText) {
        if (null == linkText) {
            return null;
        }
        Matcher linkMatcher = LINK_PATTERN.matcher(linkText);
        if (linkMatcher.find()) {
            WikiLink lnk = new WikiLink();
            lnk.setTarget(linkMatcher.group(1).trim().replaceAll("(\\s)+", " "));
            lnk.setLabel(linkMatcher.group(2));
            return lnk;
        }
        return null;
    }

    
}
