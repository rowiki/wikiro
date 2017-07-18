package org.wikipedia.ro.toolbox.operations;

import java.io.IOException;

import javax.security.auth.login.LoginException;

import org.wikibase.WikibaseException;

public interface WikiOperation {
    String execute() throws IOException, WikibaseException, LoginException;
    String[] getStatus();
}
