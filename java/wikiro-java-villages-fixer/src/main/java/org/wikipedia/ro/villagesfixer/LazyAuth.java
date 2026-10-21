package org.wikipedia.ro.villagesfixer;

import java.io.Console;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Authentication for one wiki site, performed only when needed. Each site gets its own instance, so ro.wikipedia and
 * Wikidata log in (and ask for credentials) independently of each other.
 * <ul>
 * <li>{@link #authenticated()} is for writes: logs in on first use and returns the wiki object.</li>
 * <li>{@link #read(ThrowingSupplier)} is for reads: tries without logging in; if the failure looks like an
 * authentication problem (see {@link #isAuthFailure(Throwable)}), logs in and retries once.</li>
 * </ul>
 * Checked exceptions of the wrapped operations are rethrown unchanged (sneaky throw), so callers keep their existing
 * signatures.
 */
final class LazyAuth<W> {

    private static final Logger LOG = LoggerFactory.getLogger(LazyAuth.class);

    // Messages (MediaWiki API error codes and typical phrases) that indicate the request needed authentication.
    // Tune this if a site reports login problems differently.
    private static final Pattern AUTH_MESSAGE = Pattern.compile("assert(?:user|bot)failed|not logged in|notloggedin"
        + "|must be logged in|login required|readapidenied|permissiondenied|permission denied|badtoken|invalid csrf"
        + "|HTTP (?:error )?(?:401|403)", Pattern.CASE_INSENSITIVE);

    @FunctionalInterface
    interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    @FunctionalInterface
    interface LoginAction<W> {
        void login(W wiki, String username, char[] password) throws Exception;
    }

    @FunctionalInterface
    interface LogoutAction<W> {
        void logout(W wiki) throws Exception;
    }

    private final String siteName;
    private final String userEnvVar;
    private final String passwordEnvVar;
    private final W wiki;
    private final LoginAction<W> loginAction;
    private final LogoutAction<W> logoutAction;

    private String username;
    private char[] password;
    private boolean loggedIn;

    LazyAuth(String siteName, String userEnvVar, String passwordEnvVar, W wiki, LoginAction<W> loginAction,
             LogoutAction<W> logoutAction) {
        this.siteName = siteName;
        this.userEnvVar = userEnvVar;
        this.passwordEnvVar = passwordEnvVar;
        this.wiki = wiki;
        this.loginAction = loginAction;
        this.logoutAction = logoutAction;
    }

    /** For writes: logs in if not logged in yet, then returns the wiki object. */
    synchronized W authenticated() {
        if (!loggedIn) {
            login();
        }
        return wiki;
    }

    /** Forces a new login (e.g. after the session expired). */
    synchronized void relogin() {
        login();
    }

    synchronized boolean isLoggedIn() {
        return loggedIn;
    }

    /** For reads: try as is, and only on an authentication-looking failure log in and retry once. */
    <T> T read(ThrowingSupplier<T> operation) {
        try {
            return operation.get();
        } catch (Exception | AssertionError e) {
            if (!isAuthFailure(e)) {
                throw LazyAuth.<RuntimeException>sneakyThrow(e);
            }
            LOG.warn("Read on {} failed with an authentication-related error ({}); logging in and retrying", siteName,
                e.toString());
        }
        relogin();
        try {
            return operation.get();
        } catch (Exception e) {
            throw LazyAuth.<RuntimeException>sneakyThrow(e);
        }
    }

    /** Logs out only if a login actually happened. */
    synchronized void logoutIfLoggedIn() {
        if (loggedIn && null != logoutAction) {
            try {
                logoutAction.logout(wiki);
            } catch (Exception e) {
                LOG.warn("Logout from {} failed", siteName, e);
            }
        }
        loggedIn = false;
    }

    static boolean isAuthFailure(Throwable throwable) {
        int depth = 0;
        for (Throwable t = throwable; null != t && depth < 10; t = t.getCause(), depth++) {
            if (t instanceof javax.security.auth.login.LoginException || t instanceof AssertionError
                || t instanceof SecurityException) {
                return true;
            }
            if (null != t.getMessage() && AUTH_MESSAGE.matcher(t.getMessage()).find()) {
                return true;
            }
        }
        return false;
    }

    private void login() {
        resolveCredentials();
        LOG.info("Logging in to {} with username: {}", siteName, username);
        try {
            loginAction.login(wiki, username, password);
        } catch (Exception e) {
            throw LazyAuth.<RuntimeException>sneakyThrow(e);
        }
        loggedIn = true;
        LOG.info("Login to {} successful", siteName);
    }

    private void resolveCredentials() {
        if (null == username) {
            username = System.getenv(userEnvVar);
        }
        if (null == password) {
            String envPassword = System.getenv(passwordEnvVar);
            password = null == envPassword ? null : envPassword.toCharArray();
        }
        if (null != username && null != password) {
            return;
        }
        Console console = System.console();
        if (null == console) {
            throw new IllegalStateException(String.format(
                "Authentication on %s is required but no credentials are available: set %s and %s or run from a console",
                siteName, userEnvVar, passwordEnvVar));
        }
        if (null == username) {
            username = console.readLine("%s username:", siteName);
        }
        if (null == password) {
            password = console.readPassword("%s password:", siteName);
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> RuntimeException sneakyThrow(Throwable t) throws E {
        throw (E) t;
    }
}
