package org.wikipedia.ro.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WikiLink extends WikiPart {
    private String target = null;
    private List<WikiPart> labelStructure = new ArrayList<>();

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
        setLabel(label);
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getLabel() {
        return partsListToString(labelStructure);
    }

    public void setLabel(String label) {
        if (null == label) {
            labelStructure.clear();
        } else {
            labelStructure = Arrays.asList(new PlainText(label));
        }
    }

    public List<? extends WikiPart> getLabelStructure() {
        return labelStructure;
    }

    public void setLabelStructure(List<WikiPart> labelStructure) {
        this.labelStructure = labelStructure;
    }

    private static final Pattern LINK_PATTERN = Pattern.compile("\\[\\[\\s*([^\\]\\|]+)(?:\\s*\\|([^\\]]+))?\\]\\]");

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

    @Override
    public String toString() {
        StringBuilder sbuild = new StringBuilder("[[");
        sbuild.append(target);
        if (!labelStructure.isEmpty()) {
            String labelStr = partsListToString(labelStructure);
            if (null != labelStr && !labelStr.equals(target)) {
                sbuild.append("|");
                sbuild.append(labelStr);
            }
        }
        sbuild.append("]]");
        return sbuild.toString();
    }

    @Override
    protected Collection<List<WikiPart>> getAllWikiPartCollections() {
        return Arrays.asList(labelStructure);
    }

}
