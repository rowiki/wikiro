package org.wikipedia.ro.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

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
    
    public List<PartContext> search(Predicate<WikiPart> predicate) {
        List<PartContext> res = new ArrayList<>();
        for (List<WikiPart> eachPartList: getAllWikiPartCollections()) {
            for(WikiPart eachPart: eachPartList) {
                if (predicate.test(eachPart)) {
                    PartContext pc = new PartContext(eachPart);
                    pc.setParentPart(this);
                    pc.setSiblings(eachPartList);
                    
                    res.add(pc);
                }
                res.addAll(eachPart.search(predicate));
            }
        }
        return res;
    }
}
