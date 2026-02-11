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
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link DefaultMessageSizeCriteria}.
 *
 * <p><b>Triggered by:</b> unit-tests.yml workflow ({@code mvn test} via surefire)</p>
 *
 * <p><b>What this tests:</b> The decision logic that determines whether a message body
 * should be offloaded to Blob Storage based on size threshold and the
 * {@code alwaysThroughBlob} flag.</p>
 *
 * <p><b>Coverage summary:</b></p>
 * <ul>
 *   <li>Threshold boundary: above, below, exactly at</li>
 *   <li>Always-through-blob override (forces offload regardless of size)</li>
 *   <li>Edge case: empty message body</li>
 *   <li>Metadata is intentionally ignored by the default implementation</li>
 * </ul>
 *
 * <p><b>Not yet covered:</b></p>
 * <ul>
 *   <li>Null message body (will NPE on {@code messageBody.getBytes()})</li>
 *   <li>Multi-byte characters – source correctly uses {@code getBytes(UTF_8).length}
 *       but no test verifies that multi-byte chars (e.g. emoji) are measured by byte count</li>
 *   <li>Threshold of 0 or negative values (no validation exists in source)</li>
 * </ul>
 */
@DisplayName("DefaultMessageSizeCriteria – offload decision logic")
class DefaultMessageSizeCriteriaTest {

    // --- Size threshold tests ------------------------------------------------

    @Nested
    @DisplayName("Size threshold boundary tests")
    class SizeThresholdTests {

        @Test
        @DisplayName("Message exceeding threshold → should offload")
        void testShouldOffloadWhenExceedsThreshold() {
            DefaultMessageSizeCriteria criteria = new DefaultMessageSizeCriteria(100, false);

            String largeMessage = "a".repeat(101);
            Map<String, String> metadata = new HashMap<>();

            assertTrue(criteria.shouldOffload(largeMessage, metadata));
        }

        @Test
        @DisplayName("Message below threshold → should NOT offload")
        void testShouldNotOffloadWhenBelowThreshold() {
            DefaultMessageSizeCriteria criteria = new DefaultMessageSizeCriteria(100, false);

            String smallMessage = "a".repeat(50);
            Map<String, String> metadata = new HashMap<>();

            assertFalse(criteria.shouldOffload(smallMessage, metadata));
        }

        @Test
        @DisplayName("Message exactly at threshold → should NOT offload (boundary)")
        void testShouldNotOffloadWhenExactlyAtThreshold() {
            DefaultMessageSizeCriteria criteria = new DefaultMessageSizeCriteria(100, false);

            String message = "a".repeat(100);
            Map<String, String> metadata = new HashMap<>();

            assertFalse(criteria.shouldOffload(message, metadata));
        }
    }

    // --- Always-through-blob override ----------------------------------------

    @Nested
    @DisplayName("Always-through-blob mode")
    class AlwaysThroughBlobTests {

        @Test
        @DisplayName("Small message with alwaysThroughBlob=true → should offload")
        void testShouldOffloadWhenAlwaysThroughBlobEnabled() {
            DefaultMessageSizeCriteria criteria = new DefaultMessageSizeCriteria(100, true);

            String smallMessage = "small";
            Map<String, String> metadata = new HashMap<>();

            assertTrue(criteria.shouldOffload(smallMessage, metadata));
        }

        @Test
        @DisplayName("Empty message with alwaysThroughBlob=true → should offload")
        void testShouldOffloadWithEmptyMessage() {
            DefaultMessageSizeCriteria criteria = new DefaultMessageSizeCriteria(10, true);

            String emptyMessage = "";
            Map<String, String> metadata = new HashMap<>();

            assertTrue(criteria.shouldOffload(emptyMessage, metadata));
        }
    }

    // --- Metadata handling ---------------------------------------------------

    @Nested
    @DisplayName("Metadata handling")
    class MetadataTests {

        @Test
        @DisplayName("Metadata has no effect on offload decision")
        void testMetadataNotUsedInDefaultImplementation() {
            DefaultMessageSizeCriteria criteria = new DefaultMessageSizeCriteria(100, false);

            String message = "a".repeat(101);
            Map<String, String> metadata1 = new HashMap<>();
            metadata1.put("key", "value");

            Map<String, String> metadata2 = new HashMap<>();

            // Should produce same result regardless of metadata
            assertEquals(
                criteria.shouldOffload(message, metadata1),
                criteria.shouldOffload(message, metadata2)
            );
        }
    }
}
