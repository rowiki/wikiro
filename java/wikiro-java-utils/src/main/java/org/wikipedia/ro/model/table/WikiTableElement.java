package org.wikipedia.ro.model.table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.wikipedia.ro.model.WikiPart;

public abstract class WikiTableElement extends WikiPart {

    protected List<WikiPart> subParts = new ArrayList<>();
    protected List<WikiPart> attribs = new ArrayList<>();

    public List<WikiPart> getSubParts() {
        return Collections.unmodifiableList(subParts);
    }

    public void setSubParts(List<WikiPart> subParts) {
        if (null == subParts) {
            return;
        }
        if (null != this.subParts) {
            this.subParts.clear();
        }
        for (WikiPart eachSubPart : subParts) {
            this.addSubPart(eachSubPart);
        }
    }

    public void addSubPart(WikiPart subPart) {
        if (null == this.subParts) {
            this.subParts = new ArrayList<>();
        }
        this.subParts.add(subPart);
    }

    public void removeSubPart(int idx) {
        this.subParts.remove(idx);
    }
    
    public List<WikiPart> getAttribs() {
        return attribs;
    }

    public void setAttribs(List<WikiPart> attribs) {
        this.attribs = attribs;
    }

    @Override
    protected Collection<List<WikiPart>> getAllWikiPartCollections() {
        return Arrays.asList(attribs, subParts);
    }

}
