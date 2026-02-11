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
 * Unit tests for {@link DeduplicationHandler}.
 * Validates SHA-256-based in-memory deduplication behaviour, cache eviction, and clearing.
 */
class DeduplicationHandlerTest {

    @Test
    void testFirstMessageIsNotDuplicate() {
        DeduplicationHandler handler = new DeduplicationHandler();
        assertFalse(handler.isDuplicate("Hello, world!"));
    }

    @Test
    void testSameMessageIsDuplicate() {
        DeduplicationHandler handler = new DeduplicationHandler();
        handler.isDuplicate("Hello, world!");
        assertTrue(handler.isDuplicate("Hello, world!"));
    }

    @Test
    void testDifferentMessagesAreNotDuplicates() {
        DeduplicationHandler handler = new DeduplicationHandler();
        assertFalse(handler.isDuplicate("Message 1"));
        assertFalse(handler.isDuplicate("Message 2"));
        assertFalse(handler.isDuplicate("Message 3"));
    }

    @Test
    void testMarkSeenRecordsDuplicate() {
        DeduplicationHandler handler = new DeduplicationHandler();
        handler.markSeen("Test message");
        assertTrue(handler.isDuplicate("Test message"));
    }

    @Test
    void testClearCacheResetsDeduplication() {
        DeduplicationHandler handler = new DeduplicationHandler();
        handler.isDuplicate("Test message");
        assertTrue(handler.isDuplicate("Test message"));

        handler.clearCache();
        assertFalse(handler.isDuplicate("Test message"));
    }

    @Test
    void testCacheSizeTracking() {
        DeduplicationHandler handler = new DeduplicationHandler();
        assertEquals(0, handler.getCacheSize());

        handler.isDuplicate("msg1");
        handler.isDuplicate("msg2");
        handler.isDuplicate("msg3");
        assertEquals(3, handler.getCacheSize());

        // Duplicate should not increase cache size
        handler.isDuplicate("msg1");
        assertEquals(3, handler.getCacheSize());
    }

    @Test
    void testLruEvictionWhenCacheIsFull() {
        DeduplicationHandler handler = new DeduplicationHandler(3);

        handler.isDuplicate("msg1");
        handler.isDuplicate("msg2");
        handler.isDuplicate("msg3");
        assertEquals(3, handler.getCacheSize());

        // Adding a 4th should evict the oldest (msg1)
        handler.isDuplicate("msg4");
        assertEquals(3, handler.getCacheSize());

        // msg1 should no longer be considered a duplicate
        assertFalse(handler.isDuplicate("msg1"));
    }

    @Test
    void testEmptyStringIsHandled() {
        DeduplicationHandler handler = new DeduplicationHandler();
        assertFalse(handler.isDuplicate(""));
        assertTrue(handler.isDuplicate(""));
    }

    @Test
    void testUnicodeMessagesHandled() {
        DeduplicationHandler handler = new DeduplicationHandler();
        assertFalse(handler.isDuplicate("こんにちは世界 🌍"));
        assertTrue(handler.isDuplicate("こんにちは世界 🌍"));
    }
}
