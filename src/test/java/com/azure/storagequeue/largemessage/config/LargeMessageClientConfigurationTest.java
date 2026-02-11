/*
 * DISCLAIMER: This software is provided "as is", without warranty of any kind,
 * express or implied. The author(s) and maintainer(s) are not responsible for
 * any damage, data loss, or other issues arising from the use of this software.
 * Use at your own risk.
 *
 * See the LICENSE file for full terms.
 */

package com.azure.storagequeue.largemessage.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link LargeMessageClientConfiguration}.
 *
 * <p><b>Triggered by:</b> unit-tests.yml workflow ({@code mvn test} via surefire)</p>
 *
 * <p><b>What this tests:</b> The central configuration POJO that holds all client settings.
 * Validates that default values are sensible and that every setter/getter pair works.
 * This configuration drives the behaviour of almost every other component in the library.</p>
 *
 * <p><b>Coverage summary:</b></p>
 * <ul>
 *   <li>Core defaults (threshold, blob flags, retry params, tracing)</li>
 *   <li>Compression config (enabled flag)</li>
 *   <li>Deduplication config (enabled flag + cache size)</li>
 *   <li>Dead-letter queue config (enabled, queue name, max dequeue count)</li>
 *   <li>Individual setter/getter validation</li>
 *   <li>Public constants accessibility</li>
 * </ul>
 *
 * <p><b>Not yet covered:</b></p>
 * <ul>
 *   <li>Invalid / negative values (e.g. negative threshold, 0 retry attempts)</li>
 *   <li>Null values for string properties (prefix, access tier, DLQ name)</li>
 *   <li>Interaction between related flags (e.g. receiveOnly + alwaysThroughBlob)</li>
 *   <li>Spring property binding (actual YAML → config deserialization)</li>
 * </ul>
 */
@DisplayName("LargeMessageClientConfiguration – config defaults & setters")
class LargeMessageClientConfigurationTest {

    // --- Default values ------------------------------------------------------

    @Nested
    @DisplayName("Default values after construction")
    class DefaultValueTests {

        @Test
        @DisplayName("Core defaults: 64KB threshold, retry 3x, tracing on")
        void testDefaultValues() {
            LargeMessageClientConfiguration config = new LargeMessageClientConfiguration();

            assertEquals(65536, config.getMessageSizeThreshold());
            assertFalse(config.isAlwaysThroughBlob());
            assertTrue(config.isCleanupBlobOnDelete());
            assertEquals("", config.getBlobKeyPrefix());
            assertEquals(3, config.getRetryMaxAttempts());
            assertEquals(1000L, config.getRetryBackoffMillis());
            assertEquals(2.0, config.getRetryBackoffMultiplier());
            assertEquals(30000L, config.getRetryMaxBackoffMillis());
            assertFalse(config.isIgnorePayloadNotFound());
            assertFalse(config.isReceiveOnlyMode());
            assertNull(config.getBlobAccessTier());
            assertEquals(0, config.getBlobTtlDays());
            assertFalse(config.isSasEnabled());
            assertEquals(Duration.ofDays(7), config.getSasTokenValidationTime());
            assertTrue(config.isTracingEnabled());
        }

        @Test
        @DisplayName("Compression is disabled by default")
        void testCompressionDefaults() {
            LargeMessageClientConfiguration config = new LargeMessageClientConfiguration();
            assertFalse(config.isCompressionEnabled());
        }

        @Test
        @DisplayName("Deduplication is disabled by default, cache size = 10,000")
        void testDeduplicationDefaults() {
            LargeMessageClientConfiguration config = new LargeMessageClientConfiguration();
            assertFalse(config.isDeduplicationEnabled());
            assertEquals(10_000, config.getDeduplicationCacheSize());
        }

        @Test
        @DisplayName("Dead-letter is disabled by default, max dequeue = 5")
        void testDeadLetterDefaults() {
            LargeMessageClientConfiguration config = new LargeMessageClientConfiguration();
            assertFalse(config.isDeadLetterEnabled());
            assertEquals("", config.getDeadLetterQueueName());
            assertEquals(5, config.getDeadLetterMaxDequeueCount());
        }
    }

    // --- Setter / getter pairs -----------------------------------------------

    @Nested
    @DisplayName("Setter / getter validation")
    class SetterGetterTests {

        @Test
        @DisplayName("Compression enabled flag")
        void testCompressionSetterGetter() {
            LargeMessageClientConfiguration config = new LargeMessageClientConfiguration();
            config.setCompressionEnabled(true);
            assertTrue(config.isCompressionEnabled());
        }

        @Test
        @DisplayName("Deduplication enabled flag and cache size")
        void testDeduplicationSetterGetter() {
            LargeMessageClientConfiguration config = new LargeMessageClientConfiguration();
            config.setDeduplicationEnabled(true);
            config.setDeduplicationCacheSize(5000);
            assertTrue(config.isDeduplicationEnabled());
            assertEquals(5000, config.getDeduplicationCacheSize());
        }

        @Test
        @DisplayName("Dead-letter enabled, queue name, and max dequeue count")
        void testDeadLetterSetterGetter() {
            LargeMessageClientConfiguration config = new LargeMessageClientConfiguration();
            config.setDeadLetterEnabled(true);
            config.setDeadLetterQueueName("my-dlq");
            config.setDeadLetterMaxDequeueCount(10);
            assertTrue(config.isDeadLetterEnabled());
            assertEquals("my-dlq", config.getDeadLetterQueueName());
            assertEquals(10, config.getDeadLetterMaxDequeueCount());
        }

        @Test
        @DisplayName("Message size threshold")
        void testMessageSizeThreshold() {
            LargeMessageClientConfiguration config = new LargeMessageClientConfiguration();
            config.setMessageSizeThreshold(1024);
            assertEquals(1024, config.getMessageSizeThreshold());
        }

        @Test
        @DisplayName("Blob access tier")
        void testBlobAccessTier() {
            LargeMessageClientConfiguration config = new LargeMessageClientConfiguration();
            config.setBlobAccessTier("Cool");
            assertEquals("Cool", config.getBlobAccessTier());
        }

        @Test
        @DisplayName("Blob TTL days")
        void testBlobTtlDays() {
            LargeMessageClientConfiguration config = new LargeMessageClientConfiguration();
            config.setBlobTtlDays(30);
            assertEquals(30, config.getBlobTtlDays());
        }

        @Test
        @DisplayName("SAS token validation duration")
        void testSasTokenValidationTime() {
            LargeMessageClientConfiguration config = new LargeMessageClientConfiguration();
            Duration customDuration = Duration.ofHours(12);
            config.setSasTokenValidationTime(customDuration);
            assertEquals(customDuration, config.getSasTokenValidationTime());
        }
    }

    // --- Constants ------------------------------------------------------------

    @Nested
    @DisplayName("Public constants")
    class ConstantTests {

        @Test
        @DisplayName("Constants are accessible and have expected values")
        void testConstants() {
            assertEquals(65536, LargeMessageClientConfiguration.DEFAULT_MESSAGE_SIZE_THRESHOLD);
            assertEquals("ExtendedPayloadSize", LargeMessageClientConfiguration.RESERVED_METADATA_KEY);
            assertNotNull(LargeMessageClientConfiguration.BLOB_POINTER_MARKER);
            assertNotNull(LargeMessageClientConfiguration.USER_AGENT_VALUE);
        }
    }
}
