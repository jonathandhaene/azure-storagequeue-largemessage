/*
 * DISCLAIMER: This software is provided "as is", without warranty of any kind,
 * express or implied. The author(s) and maintainer(s) are not responsible for
 * any damage, data loss, or other issues arising from the use of this software.
 * Use at your own risk.
 *
 * See the LICENSE file for full terms.
 */

package com.azure.storagequeue.largemessage.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Handles GZIP compression and decompression of message payloads.
 *
 * <p>When compression is enabled, payloads are compressed using GZIP before being stored
 * in Azure Blob Storage. This reduces storage costs and network bandwidth usage.
 * The compressed data is stored as raw bytes when going to blob storage.</p>
 *
 * <p>For inline queue messages, compressed data is Base64-encoded so it remains
 * a valid UTF-8 string in the queue message body.</p>
 *
 * @see com.azure.storagequeue.largemessage.config.LargeMessageClientConfiguration#isCompressionEnabled()
 */
public class CompressionHandler {

    private static final Logger logger = LoggerFactory.getLogger(CompressionHandler.class);

    /**
     * Compresses a string payload using GZIP.
     *
     * @param payload the UTF-8 string payload to compress
     * @return the GZIP-compressed bytes
     * @throws IOException if compression fails
     */
    public static byte[] compress(String payload) throws IOException {
        byte[] inputBytes = payload.getBytes(StandardCharsets.UTF_8);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
            gzos.write(inputBytes);
            gzos.finish();
            byte[] compressed = baos.toByteArray();
            logger.debug("Compressed payload from {} bytes to {} bytes",
                    inputBytes.length, compressed.length);
            return compressed;
        }
    }

    /**
     * Decompresses GZIP-compressed bytes back to a UTF-8 string.
     *
     * @param compressedBytes the GZIP-compressed bytes
     * @return the decompressed string payload
     * @throws IOException if decompression fails
     */
    public static String decompress(byte[] compressedBytes) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(compressedBytes);
             GZIPInputStream gzis = new GZIPInputStream(bais);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = gzis.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            return baos.toString(StandardCharsets.UTF_8);
        }
    }

    /**
     * Compresses a string and returns it as a Base64-encoded string.
     * Useful for storing compressed data in queue messages (which must be valid strings).
     *
     * @param payload the string to compress
     * @return Base64-encoded GZIP-compressed string
     * @throws IOException if compression fails
     */
    public static String compressToBase64(String payload) throws IOException {
        byte[] compressed = compress(payload);
        return Base64.getEncoder().encodeToString(compressed);
    }

    /**
     * Decompresses a Base64-encoded GZIP-compressed string.
     *
     * @param base64Compressed the Base64-encoded compressed string
     * @return the decompressed original string
     * @throws IOException if decompression fails
     */
    public static String decompressFromBase64(String base64Compressed) throws IOException {
        byte[] compressed = Base64.getDecoder().decode(base64Compressed);
        return decompress(compressed);
    }
}
