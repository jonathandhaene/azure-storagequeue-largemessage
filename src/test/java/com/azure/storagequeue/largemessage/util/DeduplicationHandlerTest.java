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
 * Unit tests for {@link DeduplicationHandler}.
 *
 * <p><b>Triggered by:</b> unit-tests.yml workflow ({@code mvn test} via surefire)</p>
 *
 * <p><b>What this tests:</b> The optional in-memory deduplication filter. Uses
 * SHA-256 hashing with an LRU cache to detect duplicate message bodies and
 * prevent double-processing.</p>
 *
 * <p><b>Coverage summary:</b></p>
 * <ul>
 *   <li>First-seen message → not a duplicate</li>
 *   <li>Same message again → detected as duplicate</li>
 *   <li>Different messages → each is unique</li>
 *   <li>markSeen API → pre-registers a message</li>
 *   <li>clearCache → resets all state</li>
 *   <li>Cache size tracking and LRU eviction</li>
 *   <li>Edge cases: empty string, unicode</li>
 * </ul>
 *
 * <p><b>Not yet covered:</b></p>
 * <ul>
 *   <li>Null message body</li>
 *   <li>Thread safety under concurrent access</li>
 *   <li>Very large cache sizes (memory pressure)</li>
 *   <li>Hash collision scenarios (unlikely but theoretically possible)</li>
 *   <li>Integration with the actual message receive pipeline</li>
 * </ul>
 */
@DisplayName("DeduplicationHandler – SHA-256 LRU duplicate detection")
class DeduplicationHandlerTest {

    // --- Basic duplicate detection -------------------------------------------

    @Nested
    @DisplayName("Basic duplicate detection")
    class BasicDuplicateDetectionTests {

        @Test
        @DisplayName("First message is never a duplicate")
        void testFirstMessageIsNotDuplicate() {
            DeduplicationHandler handler = new DeduplicationHandler();
            assertFalse(handler.isDuplicate("Hello, world!"));
        }

        @Test
        @DisplayName("Same message seen twice → duplicate")
        void testSameMessageIsDuplicate() {
            DeduplicationHandler handler = new DeduplicationHandler();
            handler.isDuplicate("Hello, world!");
            assertTrue(handler.isDuplicate("Hello, world!"));
        }

        @Test
        @DisplayName("Different messages are each unique")
        void testDifferentMessagesAreNotDuplicates() {
            DeduplicationHandler handler = new DeduplicationHandler();
            assertFalse(handler.isDuplicate("Message 1"));
            assertFalse(handler.isDuplicate("Message 2"));
            assertFalse(handler.isDuplicate("Message 3"));
        }
    }

    // --- markSeen / clearCache API -------------------------------------------

    @Nested
    @DisplayName("markSeen & clearCache API")
    class CacheManagementTests {

        @Test
        @DisplayName("markSeen pre-registers a message as seen")
        void testMarkSeenRecordsDuplicate() {
            DeduplicationHandler handler = new DeduplicationHandler();
            handler.markSeen("Test message");
            assertTrue(handler.isDuplicate("Test message"));
        }

        @Test
        @DisplayName("clearCache resets all deduplication state")
        void testClearCacheResetsDeduplication() {
            DeduplicationHandler handler = new DeduplicationHandler();
            handler.isDuplicate("Test message");
            assertTrue(handler.isDuplicate("Test message"));

            handler.clearCache();
            assertFalse(handler.isDuplicate("Test message"));
        }
    }

    // --- Cache size and eviction ---------------------------------------------

    @Nested
    @DisplayName("Cache size tracking and LRU eviction")
    class CacheSizeTests {

        @Test
        @DisplayName("Cache size increases with unique messages, not duplicates")
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
        @DisplayName("LRU eviction: oldest entry removed when cache is full")
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
    }

    // --- Edge cases ----------------------------------------------------------

    @Nested
    @DisplayName("Edge cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Empty string is handled as a valid message")
        void testEmptyStringIsHandled() {
            DeduplicationHandler handler = new DeduplicationHandler();
            assertFalse(handler.isDuplicate(""));
            assertTrue(handler.isDuplicate(""));
        }

        @Test
        @DisplayName("Unicode messages (CJK, emoji) are handled correctly")
        void testUnicodeMessagesHandled() {
            DeduplicationHandler handler = new DeduplicationHandler();
            assertFalse(handler.isDuplicate("こんにちは世界 🌍"));
            assertTrue(handler.isDuplicate("こんにちは世界 🌍"));
        }
    }
}
