package org.wikipedia.ro.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class WikiPart {
    protected String initialText;

    public String getInitialText() {
        return initialText;
    }

    public WikiPart setInitialText(String initialText) {
        this.initialText = initialText;
        return this;
    }
    
    protected String partsListToString(List<? extends WikiPart> parts) {
        StringBuilder sbuild = new StringBuilder();
        for (WikiPart part: parts) {
            sbuild.append(part.toString());
        }
        return sbuild.toString();
    }

    protected abstract Collection<List<WikiPart>> getAllWikiPartCollections();
    
    public List<WikiPart> getAllSubParts() {
        List<WikiPart> subParts = new ArrayList<>();

        for (List<WikiPart> eachSubPartCollection: getAllWikiPartCollections()) {
            for (WikiPart eachSubPart: eachSubPartCollection) {
                subParts.add(eachSubPart);
                subParts.addAll(eachSubPartCollection);
            }
        }
        return subParts;
    }
}
