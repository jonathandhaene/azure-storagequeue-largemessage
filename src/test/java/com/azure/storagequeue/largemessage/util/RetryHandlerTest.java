/*
 * DISCLAIMER: This software is provided "as is", without warranty of any kind,
 * express or implied. The author(s) and maintainer(s) are not responsible for
 * any damage, data loss, or other issues arising from the use of this software.
 * Use at your own risk.
 *
 * See the LICENSE file for full terms.
 */

package com.azure.storagequeue.largemessage.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link RetryHandler}.
 * Validates retry logic, exponential backoff, and error propagation.
 */
class RetryHandlerTest {

    @Test
    void testSuccessOnFirstAttempt() {
        RetryHandler handler = new RetryHandler(3, 10, 2.0, 1000);
        String result = handler.executeWithRetry(() -> "success");
        assertEquals("success", result);
    }

    @Test
    void testSuccessAfterRetries() {
        RetryHandler handler = new RetryHandler(3, 10, 2.0, 1000);
        int[] attempts = {0};
        String result = handler.executeWithRetry(() -> {
            attempts[0]++;
            if (attempts[0] < 3) {
                throw new RuntimeException("Transient error");
            }
            return "success";
        });
        assertEquals("success", result);
        assertEquals(3, attempts[0]);
    }

    @Test
    void testFailsAfterMaxAttempts() {
        RetryHandler handler = new RetryHandler(2, 10, 2.0, 1000);
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                handler.executeWithRetry(() -> {
                    throw new RuntimeException("Persistent error");
                }));
        assertTrue(ex.getMessage().contains("failed after 2 attempts"));
    }

    @Test
    void testRunnableRetry() {
        RetryHandler handler = new RetryHandler(3, 10, 2.0, 1000);
        int[] attempts = {0};
        handler.executeWithRetry(() -> {
            attempts[0]++;
            if (attempts[0] < 2) {
                throw new RuntimeException("Transient error");
            }
        });
        assertEquals(2, attempts[0]);
    }

    @Test
    void testSingleAttemptNoRetry() {
        RetryHandler handler = new RetryHandler(1, 10, 2.0, 1000);
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                handler.executeWithRetry(() -> {
                    throw new RuntimeException("Fail immediately");
                }));
        assertTrue(ex.getMessage().contains("failed after 1 attempts"));
    }
}
