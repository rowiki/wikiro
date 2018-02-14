package org.wikipedia.ro.model;

import java.util.List;

public class PartContext {
    private WikiPart part;
    private WikiPart parentPart;
    private WikiPage page;
    private List<WikiPart> siblings;
    
    public PartContext(WikiPart part) {
        super();
        this.part = part;
    }

    public List<WikiPart> getSiblings() {
        return siblings;
    }

    public void setSiblings(List<WikiPart> siblings) {
        this.siblings = siblings;
    }

    public WikiPart getPart() {
        return part;
    }

    public void setPart(WikiPart part) {
        this.part = part;
    }

    public WikiPart getParentPart() {
        return parentPart;
    }

    public void setParentPart(WikiPart parentPart) {
        this.parentPart = parentPart;
    }

    public WikiPage getPage() {
        return page;
    }

    public void setPage(WikiPage page) {
        this.page = page;
    }
}
