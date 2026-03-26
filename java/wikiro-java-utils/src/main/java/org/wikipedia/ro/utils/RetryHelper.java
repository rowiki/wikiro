
package org.wikipedia.ro.utils;

import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class RetryHelper {
    public static final Logger LOG = Logger.getLogger(RetryHelper.class.getCanonicalName());

    public static <T> T retry(Supplier<T> operation, int maxAttempts) throws TimeoutException {
        return retry(operation, maxAttempts, null);
    }

    public static <T> T retry(Supplier<T> operation, int maxAttempts, Runnable relogin) throws TimeoutException {
        int attempts = 0;
        Throwable ex = null;
        while (attempts < maxAttempts) {
            try {
                T result = operation.get();
                if (result != null) {
                    return result;
                }
            } catch (AssertionError e) {
                ex = e;
                LOG.warning("AssertionError on attempt " + (attempts + 1) + ": " + e.getMessage());
                if (relogin != null) {
                    try {
                        relogin.run();
                    } catch (Exception reloginEx) {
                        LOG.severe("Relogin failed: " + reloginEx.getMessage());
                    }
                }
            } catch (Exception e) {
                ex = e;
            } finally {
                attempts++;
            }
        }
        TimeoutException tex = new TimeoutException("Operation failed after " + maxAttempts + " attempts");
        tex.initCause(ex);
        throw tex;
    }
}
