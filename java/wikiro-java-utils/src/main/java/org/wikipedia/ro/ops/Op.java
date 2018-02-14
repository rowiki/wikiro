package org.wikipedia.ro.ops;

import java.io.IOException;

import javax.security.auth.login.LoginException;

import org.wikipedia.ro.model.WikiPage;

/**
 * An operation to be performed on a wiki page.
 * 
 * @author acstroe
 *
 */
public abstract class Op {
    protected WikiPage page;
    
    protected String summary;

    public String getSummary() {
        return summary;
    }

    public Op setSummary(String summary) {
        this.summary = summary;
        return this;
    }

    public Op(WikiPage page) {
        super();
        this.page = page;
    }
 
    /**
     * Performs changes on the page's structure. No saving operations should be described here.
     */
    protected abstract void changePageStructure();
    
    /**
     * Actually executes the operation modelled by this op.
     * 
     * @throws LoginException if there is a login problem on the wiki when trying to save
     * @throws IOException if some I/O error occurs when contacting the wiki
     */
    public void execute() throws LoginException, IOException {
        this.changePageStructure();
        page.assembleText();
        page.getWiki().edit(page.getTitle(), page.getText(), summary);
    }
}
