/*
 * DISCLAIMER: This software is provided "as is", without warranty of any kind,
 * express or implied. The author(s) and maintainer(s) are not responsible for
 * any damage, data loss, or other issues arising from the use of this software.
 * Use at your own risk.
 *
 * See the LICENSE file for full terms.
 */

package com.azure.storagequeue.largemessage.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link LargeMessageClientConfiguration}.
 * Validates default values and property setters for all configuration options,
 * including the new compression, deduplication, and dead-letter queue properties.
 */
class LargeMessageClientConfigurationTest {

    @Test
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
    void testCompressionDefaults() {
        LargeMessageClientConfiguration config = new LargeMessageClientConfiguration();
        assertFalse(config.isCompressionEnabled());
    }

    @Test
    void testCompressionSetterGetter() {
        LargeMessageClientConfiguration config = new LargeMessageClientConfiguration();
        config.setCompressionEnabled(true);
        assertTrue(config.isCompressionEnabled());
    }

    @Test
    void testDeduplicationDefaults() {
        LargeMessageClientConfiguration config = new LargeMessageClientConfiguration();
        assertFalse(config.isDeduplicationEnabled());
        assertEquals(10_000, config.getDeduplicationCacheSize());
    }

    @Test
    void testDeduplicationSetterGetter() {
        LargeMessageClientConfiguration config = new LargeMessageClientConfiguration();
        config.setDeduplicationEnabled(true);
        config.setDeduplicationCacheSize(5000);
        assertTrue(config.isDeduplicationEnabled());
        assertEquals(5000, config.getDeduplicationCacheSize());
    }

    @Test
    void testDeadLetterDefaults() {
        LargeMessageClientConfiguration config = new LargeMessageClientConfiguration();
        assertFalse(config.isDeadLetterEnabled());
        assertEquals("", config.getDeadLetterQueueName());
        assertEquals(5, config.getDeadLetterMaxDequeueCount());
    }

    @Test
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
    void testMessageSizeThreshold() {
        LargeMessageClientConfiguration config = new LargeMessageClientConfiguration();
        config.setMessageSizeThreshold(1024);
        assertEquals(1024, config.getMessageSizeThreshold());
    }

    @Test
    void testBlobAccessTier() {
        LargeMessageClientConfiguration config = new LargeMessageClientConfiguration();
        config.setBlobAccessTier("Cool");
        assertEquals("Cool", config.getBlobAccessTier());
    }

    @Test
    void testBlobTtlDays() {
        LargeMessageClientConfiguration config = new LargeMessageClientConfiguration();
        config.setBlobTtlDays(30);
        assertEquals(30, config.getBlobTtlDays());
    }

    @Test
    void testSasTokenValidationTime() {
        LargeMessageClientConfiguration config = new LargeMessageClientConfiguration();
        Duration customDuration = Duration.ofHours(12);
        config.setSasTokenValidationTime(customDuration);
        assertEquals(customDuration, config.getSasTokenValidationTime());
    }

    @Test
    void testConstants() {
        assertEquals(65536, LargeMessageClientConfiguration.DEFAULT_MESSAGE_SIZE_THRESHOLD);
        assertEquals("ExtendedPayloadSize", LargeMessageClientConfiguration.RESERVED_METADATA_KEY);
        assertNotNull(LargeMessageClientConfiguration.BLOB_POINTER_MARKER);
        assertNotNull(LargeMessageClientConfiguration.USER_AGENT_VALUE);
    }
}
