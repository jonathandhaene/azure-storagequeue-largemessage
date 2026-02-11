/*
 * DISCLAIMER: This software is provided "as is", without warranty of any kind,
 * express or implied. The author(s) and maintainer(s) are not responsible for
 * any damage, data loss, or other issues arising from the use of this software.
 * Use at your own risk.
 *
 * See the LICENSE file for full terms.
 */

package com.azure.storagequeue.largemessage.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link BlobPointer}.
 *
 * <p><b>Triggered by:</b> unit-tests.yml workflow ({@code mvn test} via surefire)</p>
 *
 * <p><b>What this tests:</b> The value object that holds {containerName, blobName}
 * and acts as the "pointer" stored in the queue message body when the real payload
 * has been offloaded to Blob Storage.</p>
 *
 * <p><b>Coverage summary:</b></p>
 * <ul>
 *   <li>Construction and getters</li>
 *   <li>JSON serialization (toJson / fromJson) and round-trip</li>
 *   <li>equals / hashCode contract</li>
 *   <li>toString readability</li>
 * </ul>
 *
 * <p><b>Not yet covered:</b></p>
 * <ul>
 *   <li>Null or empty container/blob names (constructor accepts them silently)</li>
 *   <li>Special characters in names (slashes create virtual blob directories)</li>
 *   <li>Malformed JSON input to {@code fromJson} (throws {@code RuntimeException}
 *       wrapping Jackson's {@code JsonProcessingException})</li>
 * </ul>
 */
@DisplayName("BlobPointer – blob reference value object")
class BlobPointerTest {

    // --- Construction --------------------------------------------------------

    @Nested
    @DisplayName("Construction & getters")
    class ConstructionTests {

        @Test
        @DisplayName("Constructor stores container and blob name")
        void testCreateBlobPointer() {
            BlobPointer pointer = new BlobPointer("container", "blob-name");

            assertEquals("container", pointer.getContainerName());
            assertEquals("blob-name", pointer.getBlobName());
        }
    }

    // --- JSON serialization --------------------------------------------------

    @Nested
    @DisplayName("JSON serialization")
    class JsonSerializationTests {

        @Test
        @DisplayName("toJson produces JSON containing container and blob name")
        void testToJson() {
            BlobPointer pointer = new BlobPointer("test-container", "test-blob");

            String json = pointer.toJson();

            assertNotNull(json);
            assertTrue(json.contains("test-container"));
            assertTrue(json.contains("test-blob"));
        }

        @Test
        @DisplayName("fromJson deserializes valid JSON back to BlobPointer")
        void testFromJson() {
            String json = "{\"containerName\":\"my-container\",\"blobName\":\"my-blob\"}";

            BlobPointer pointer = BlobPointer.fromJson(json);

            assertNotNull(pointer);
            assertEquals("my-container", pointer.getContainerName());
            assertEquals("my-blob", pointer.getBlobName());
        }

        @Test
        @DisplayName("Round-trip: toJson → fromJson returns equal object")
        void testRoundTripSerialization() {
            BlobPointer original = new BlobPointer("test-container", "test-blob");

            String json = original.toJson();
            BlobPointer deserialized = BlobPointer.fromJson(json);

            assertEquals(original, deserialized);
        }
    }

    // --- equals / hashCode / toString ----------------------------------------

    @Nested
    @DisplayName("equals, hashCode, toString")
    class EqualityTests {

        @Test
        @DisplayName("Equal pointers have same container + blob")
        void testEquals() {
            BlobPointer pointer1 = new BlobPointer("container", "blob");
            BlobPointer pointer2 = new BlobPointer("container", "blob");
            BlobPointer pointer3 = new BlobPointer("other-container", "blob");

            assertEquals(pointer1, pointer2);
            assertNotEquals(pointer1, pointer3);
        }

        @Test
        @DisplayName("Equal pointers produce same hashCode")
        void testHashCode() {
            BlobPointer pointer1 = new BlobPointer("container", "blob");
            BlobPointer pointer2 = new BlobPointer("container", "blob");

            assertEquals(pointer1.hashCode(), pointer2.hashCode());
        }

        @Test
        @DisplayName("toString includes container and blob name")
        void testToString() {
            BlobPointer pointer = new BlobPointer("container", "blob");

            String str = pointer.toString();

            assertNotNull(str);
            assertTrue(str.contains("container"));
            assertTrue(str.contains("blob"));
        }
    }
}
