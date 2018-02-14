package org.wikipedia.ro.ops;

import org.wikipedia.ro.model.WikiPage;

public class RemovePartOp extends Op {
    private int index = -1;

    public RemovePartOp(WikiPage article, int index) {
        super(article);
        this.index = index;
    }

    protected void changePageStructure() {
        if (0 <= index && page.getParts().size() > index) {
            page.removeParts(index);
        }
    }

}
