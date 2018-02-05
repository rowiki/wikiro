package org.wikipedia.ro.model.table;

import java.util.List;

import org.wikipedia.ro.model.WikiPart;

public class WikiTable extends WikiPart {
    private List<WikiPart> caption;
    private List<WikiPart> subParts;

    public List<WikiPart> getSubParts() {
        return subParts;
    }

    public void setSubParts(List<WikiPart> subParts) {
        this.subParts = subParts;
    }

}
