/*
 * DISCLAIMER: This software is provided "as is", without warranty of any kind,
 * express or implied. The author(s) and maintainer(s) are not responsible for
 * any damage, data loss, or other issues arising from the use of this software.
 * Use at your own risk.
 *
 * See the LICENSE file for full terms.
 */

package com.azure.storagequeue.largemessage.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link RetryHandler}.
 *
 * <p><b>Triggered by:</b> unit-tests.yml workflow ({@code mvn test} via surefire)</p>
 *
 * <p><b>What this tests:</b> The generic retry utility that wraps operations with
 * exponential-backoff retry logic. Used by BlobPayloadStore and the main client
 * to handle transient Azure Storage failures.</p>
 *
 * <p><b>Coverage summary:</b></p>
 * <ul>
 *   <li>Success on first attempt (no retry needed)</li>
 *   <li>Success after transient failures (retry kicks in)</li>
 *   <li>Failure after exhausting all retry attempts</li>
 *   <li>Runnable variant (void operations)</li>
 *   <li>Single-attempt mode (maxAttempts = 1)</li>
 * </ul>
 *
 * <p><b>Not yet covered:</b></p>
 * <ul>
 *   <li>Backoff timing verification (delays are calculated as exponential but never asserted)</li>
 *   <li>Max backoff cap ({@code Math.min(delay, maxBackoffMillis)} is implemented but not tested)</li>
 *   <li>Thread interruption during backoff sleep (handler restores interrupt flag and throws)</li>
 *   <li>Non-retryable exceptions (all exceptions are retried indiscriminately –
 *       no distinction between transient and permanent errors)</li>
 *   <li>Zero or negative maxAttempts (will NPE on null {@code lastException})</li>
 * </ul>
 */
@DisplayName("RetryHandler – exponential backoff retry logic")
class RetryHandlerTest {

    // --- Supplier variant (returns a value) -----------------------------------

    @Nested
    @DisplayName("Supplier-based retry (returns a value)")
    class SupplierRetryTests {

        @Test
        @DisplayName("Succeeds on first attempt → no retry")
        void testSuccessOnFirstAttempt() {
            RetryHandler handler = new RetryHandler(3, 10, 2.0, 1000);
            String result = handler.executeWithRetry(() -> "success");
            assertEquals("success", result);
        }

        @Test
        @DisplayName("Fails twice, succeeds on third attempt")
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
        @DisplayName("Always fails → throws after max attempts exhausted")
        void testFailsAfterMaxAttempts() {
            RetryHandler handler = new RetryHandler(2, 10, 2.0, 1000);
            RuntimeException ex = assertThrows(RuntimeException.class, () ->
                    handler.executeWithRetry(() -> {
                        throw new RuntimeException("Persistent error");
                    }));
            assertTrue(ex.getMessage().contains("failed after 2 attempts"));
        }

        @Test
        @DisplayName("maxAttempts=1 → no retries, immediate failure")
        void testSingleAttemptNoRetry() {
            RetryHandler handler = new RetryHandler(1, 10, 2.0, 1000);
            RuntimeException ex = assertThrows(RuntimeException.class, () ->
                    handler.executeWithRetry(() -> {
                        throw new RuntimeException("Fail immediately");
                    }));
            assertTrue(ex.getMessage().contains("failed after 1 attempts"));
        }
    }

    // --- Runnable variant (void operations) -----------------------------------

    @Nested
    @DisplayName("Runnable-based retry (void operations)")
    class RunnableRetryTests {

        @Test
        @DisplayName("Runnable succeeds after one transient failure")
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
    }
}
