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

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CompressionHandler}.
 * Validates GZIP compression/decompression and Base64 round-trip behaviour.
 */
class CompressionHandlerTest {

    @Test
    void testCompressAndDecompress() throws IOException {
        String original = "Hello, this is a test message for compression.";
        byte[] compressed = CompressionHandler.compress(original);
        assertNotNull(compressed);
        assertTrue(compressed.length > 0);

        String decompressed = CompressionHandler.decompress(compressed);
        assertEquals(original, decompressed);
    }

    @Test
    void testCompressToBase64AndBack() throws IOException {
        String original = "The quick brown fox jumps over the lazy dog.";
        String base64Compressed = CompressionHandler.compressToBase64(original);
        assertNotNull(base64Compressed);
        assertFalse(base64Compressed.isEmpty());

        String decompressed = CompressionHandler.decompressFromBase64(base64Compressed);
        assertEquals(original, decompressed);
    }

    @Test
    void testCompressionReducesSizeForRepetitiveData() throws IOException {
        // Repetitive data should compress well
        String repetitiveData = "ABCDEFGHIJ".repeat(1000);
        byte[] compressed = CompressionHandler.compress(repetitiveData);
        assertTrue(compressed.length < repetitiveData.length(),
                "Compressed size should be smaller for repetitive data");
    }

    @Test
    void testRoundTripWithEmptyString() throws IOException {
        String original = "";
        String base64 = CompressionHandler.compressToBase64(original);
        String result = CompressionHandler.decompressFromBase64(base64);
        assertEquals(original, result);
    }

    @Test
    void testRoundTripWithUnicodeCharacters() throws IOException {
        String original = "Hello 世界! Ñoño résumé 🎉";
        String base64 = CompressionHandler.compressToBase64(original);
        String result = CompressionHandler.decompressFromBase64(base64);
        assertEquals(original, result);
    }

    @Test
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

    @Test
    void testDecompressInvalidDataThrowsException() {
        assertThrows(Exception.class, () ->
                CompressionHandler.decompressFromBase64("dGhpcyBpcyBub3QgZ3ppcA=="));
    }
}
