package org.wikipedia.ro.ops;

import java.util.function.Consumer;

import org.wikipedia.ro.model.WikiPage;

public class GenericEditingOp extends Op {
    private Consumer<WikiPage> operation;

    public GenericEditingOp(WikiPage article, Consumer<WikiPage> operation) {
        super(article);
        this.operation = operation;
    }

    @Override
    protected void changePageStructure() {
        operation.accept(page);
    }
    
}
