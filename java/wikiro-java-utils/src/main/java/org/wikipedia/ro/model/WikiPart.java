package org.wikipedia.ro.model;

import java.util.List;

public abstract class WikiPart {
    protected String initialText;

    public String getInitialText() {
        return initialText;
    }

    public void setInitialText(String initialText) {
        this.initialText = initialText;
    }
    
    protected String partsListToString(List<WikiPart> parts) {
        StringBuilder sbuild = new StringBuilder();
        for (WikiPart part: parts) {
            sbuild.append(part.toString());
        }
        return sbuild.toString();
    }


}
