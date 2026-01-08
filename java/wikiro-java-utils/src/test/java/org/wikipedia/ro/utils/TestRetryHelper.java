
package org.wikipedia.ro.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class TestRetryHelper {

    @Test
    void testRetryReturnsOnFirstAttempt() throws TimeoutException {
        String result = RetryHelper.retry(() -> "success", 3);
        assertEquals("success", result);
    }

    @Test
    void testRetryReturnsOnSecondAttempt() throws TimeoutException {
        AtomicInteger attempts = new AtomicInteger(0);
        String result = RetryHelper.retry(() -> {
            if (attempts.getAndIncrement() == 0) {
                return null;
            }
            return "second";
        }, 3);
        assertEquals("second", result);
        assertEquals(2, attempts.get());
    }

    @Test
    void testRetryThrowsTimeoutException() {
        assertThrows(TimeoutException.class, () -> {
            RetryHelper.retry(() -> null, 2);
        });
    }
}
