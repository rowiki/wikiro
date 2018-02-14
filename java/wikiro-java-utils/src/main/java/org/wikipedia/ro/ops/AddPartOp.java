package org.wikipedia.ro.ops;

import org.wikipedia.ro.model.WikiPage;
import org.wikipedia.ro.model.WikiPart;

public class AddPartOp extends Op {

    private int index = -1;
    private WikiPart part;
    
    public AddPartOp(WikiPage article, int index, WikiPart part) {
        super(article);
        this.index = index;
        this.part = part;
    }

    public AddPartOp(WikiPage article, WikiPart part) {
        super(article);
        this.part = part;
    }

    @Override
    protected void changePageStructure() {
        if (index < 0) {
            page.addPart(part);
        } else {
            page.addPart(index, part);
        }
    }

}
