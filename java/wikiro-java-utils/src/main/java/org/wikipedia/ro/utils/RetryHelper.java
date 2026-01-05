package org.wikipedia.ro.utils;

import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class RetryHelper {
    public static final Logger LOG = Logger.getLogger(RetryHelper.class.getCanonicalName());
    
    public static <T> T retry(Supplier<T> operation, int maxAttempts) throws TimeoutException {
        int attempts = 0;
        while (attempts < maxAttempts) {
            try {
                T result = operation.get();
                if (result != null) {
                    return result;
                }
            } finally {
                attempts++;
            }
        }
        throw new TimeoutException("Operation failed after " + maxAttempts + " attempts");
    }
}
