/*
 * DISCLAIMER: This software is provided "as is", without warranty of any kind,
 * express or implied. The author(s) and maintainer(s) are not responsible for
 * any damage, data loss, or other issues arising from the use of this software.
 * Use at your own risk.
 *
 * See the LICENSE file for full terms.
 */

package com.azure.storagequeue.largemessage.store;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link DefaultBlobNameResolver}.
 *
 * <p><b>Triggered by:</b> unit-tests.yml workflow ({@code mvn test} via surefire)</p>
 *
 * <p><b>What this tests:</b> The component that generates unique blob names for
 * offloaded message payloads. Each call produces a UUID, optionally prepended
 * with a configurable prefix (e.g. "messages/").</p>
 *
 * <p><b>Coverage summary:</b></p>
 * <ul>
 *   <li>No prefix → raw UUID returned</li>
 *   <li>With prefix → prefix + UUID</li>
 *   <li>Null prefix → handled gracefully</li>
 *   <li>Uniqueness → two calls never produce the same name</li>
 * </ul>
 *
 * <p><b>Not yet covered:</b></p>
 * <ul>
 *   <li>Prefix with special characters (slashes, dots, unicode)</li>
 *   <li>Very long prefix values (max blob name length is 1024 chars)</li>
 *   <li>Thread safety of UUID generation under concurrent access</li>
 * </ul>
 */
@DisplayName("DefaultBlobNameResolver – UUID-based blob naming")
class DefaultBlobNameResolverTest {

    @Nested
    @DisplayName("Prefix handling")
    class PrefixTests {

        @Test
        @DisplayName("No prefix → returns a valid UUID")
        void testResolveWithoutPrefix() {
            DefaultBlobNameResolver resolver = new DefaultBlobNameResolver("");

            String blobName = resolver.resolve("message-123");

            assertNotNull(blobName);
            assertFalse(blobName.isEmpty());
            // Should be a UUID
            assertTrue(blobName.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
        }

        @Test
        @DisplayName("With prefix → returns prefix + UUID")
        void testResolveWithPrefix() {
            DefaultBlobNameResolver resolver = new DefaultBlobNameResolver("messages/");

            String blobName = resolver.resolve("message-123");

            assertNotNull(blobName);
            assertTrue(blobName.startsWith("messages/"));
            // Should be prefix + UUID
            String uuidPart = blobName.substring("messages/".length());
            assertTrue(uuidPart.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
        }

        @Test
        @DisplayName("Null prefix → handled without error")
        void testResolveWithNullPrefix() {
            DefaultBlobNameResolver resolver = new DefaultBlobNameResolver(null);

            String blobName = resolver.resolve("message-123");

            assertNotNull(blobName);
            assertFalse(blobName.isEmpty());
        }
    }

    @Nested
    @DisplayName("Uniqueness")
    class UniquenessTests {

        @Test
        @DisplayName("Two calls with same input → different blob names")
        void testResolveGeneratesUniqueName() {
            DefaultBlobNameResolver resolver = new DefaultBlobNameResolver("");

            String blobName1 = resolver.resolve("message-123");
            String blobName2 = resolver.resolve("message-123");

            assertNotEquals(blobName1, blobName2);
        }
    }
}
