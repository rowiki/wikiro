package org.wikipedia.ro.ops;

import java.io.IOException;

import javax.security.auth.login.LoginException;

import org.wikipedia.ro.model.WikiPage;

public class MovePageOp extends Op {

    private String newTitle;

    public MovePageOp(WikiPage article, String newTitle) {
        super(article);
        this.newTitle = newTitle;
    }

    protected void changePageStructure() {
    }

    @Override
    public void execute() throws LoginException, IOException {
        if (null != newTitle && !newTitle.equals(page.getTitle())) {
            page.getWiki().move(page.getTitle(), newTitle, summary);
        }
    }

}
