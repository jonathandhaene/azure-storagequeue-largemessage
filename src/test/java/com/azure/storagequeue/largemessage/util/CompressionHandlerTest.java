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

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CompressionHandler}.
 *
 * <p><b>Triggered by:</b> unit-tests.yml workflow ({@code mvn test} via surefire)</p>
 *
 * <p><b>What this tests:</b> The optional GZIP compression utility. When compression
 * is enabled, message payloads are compressed before uploading to Blob Storage and
 * decompressed on retrieval. The Base64 variants are used for inline queue messages.</p>
 *
 * <p><b>Coverage summary:</b></p>
 * <ul>
 *   <li>Byte-level compress/decompress round-trip</li>
 *   <li>Base64 compress/decompress round-trip</li>
 *   <li>Compression effectiveness on repetitive data</li>
 *   <li>Edge cases: empty string, unicode characters, large payloads</li>
 *   <li>Error handling: invalid (non-GZIP) data throws exception</li>
 * </ul>
 *
 * <p><b>Not yet covered:</b></p>
 * <ul>
 *   <li>Null input handling (will NPE on {@code payload.getBytes()})</li>
 *   <li>Double compression (compressing already-compressed data)</li>
 *   <li>Integration with the client send/receive pipeline
 *       ({@code AzureStorageQueueLargeMessageClient} calls
 *       {@code compressToBase64}/{@code decompressFromBase64})</li>
 * </ul>
 */
@DisplayName("CompressionHandler – GZIP + Base64 compression")
class CompressionHandlerTest {

    // --- Basic round-trip tests ----------------------------------------------

    @Nested
    @DisplayName("Round-trip: compress then decompress")
    class RoundTripTests {

        @Test
        @DisplayName("Byte-level: compress → decompress returns original")
        void testCompressAndDecompress() throws IOException {
            String original = "Hello, this is a test message for compression.";
            byte[] compressed = CompressionHandler.compress(original);
            assertNotNull(compressed);
            assertTrue(compressed.length > 0);

            String decompressed = CompressionHandler.decompress(compressed);
            assertEquals(original, decompressed);
        }

        @Test
        @DisplayName("Base64: compressToBase64 → decompressFromBase64 returns original")
        void testCompressToBase64AndBack() throws IOException {
            String original = "The quick brown fox jumps over the lazy dog.";
            String base64Compressed = CompressionHandler.compressToBase64(original);
            assertNotNull(base64Compressed);
            assertFalse(base64Compressed.isEmpty());

            String decompressed = CompressionHandler.decompressFromBase64(base64Compressed);
            assertEquals(original, decompressed);
        }
    }

    // --- Compression effectiveness -------------------------------------------

    @Nested
    @DisplayName("Compression effectiveness")
    class EffectivenessTests {

        @Test
        @DisplayName("Repetitive data compresses to smaller size")
        void testCompressionReducesSizeForRepetitiveData() throws IOException {
            // Repetitive data should compress well
            String repetitiveData = "ABCDEFGHIJ".repeat(1000);
            byte[] compressed = CompressionHandler.compress(repetitiveData);
            assertTrue(compressed.length < repetitiveData.length(),
                    "Compressed size should be smaller for repetitive data");
        }
    }

    // --- Edge cases ----------------------------------------------------------

    @Nested
    @DisplayName("Edge cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Empty string round-trips correctly")
        void testRoundTripWithEmptyString() throws IOException {
            String original = "";
            String base64 = CompressionHandler.compressToBase64(original);
            String result = CompressionHandler.decompressFromBase64(base64);
            assertEquals(original, result);
        }

        @Test
        @DisplayName("Unicode characters (CJK, accents, emoji) round-trip correctly")
        void testRoundTripWithUnicodeCharacters() throws IOException {
            String original = "Hello 世界! Ñoño résumé 🎉";
            String base64 = CompressionHandler.compressToBase64(original);
            String result = CompressionHandler.decompressFromBase64(base64);
            assertEquals(original, result);
        }

        @Test
        @DisplayName("Large payload (10,000 lines) round-trips correctly")
        void testRoundTripWithLargePayload() throws IOException {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 10000; i++) {
                sb.append("Line ").append(i).append(": This is a test payload for compression.\n");
            }
            String original = sb.toString();
            String base64 = CompressionHandler.compressToBase64(original);
            String result = CompressionHandler.decompressFromBase64(base64);
            assertEquals(original, result);
        }
    }

    // --- Error handling ------------------------------------------------------

    @Nested
    @DisplayName("Error handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Non-GZIP Base64 data → throws exception")
        void testDecompressInvalidDataThrowsException() {
            // "this is not gzip" in Base64 – valid Base64 but not valid GZIP
            assertThrows(Exception.class, () ->
                    CompressionHandler.decompressFromBase64("dGhpcyBpcyBub3QgZ3ppcA=="));
        }
    }
}
