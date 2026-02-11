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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Provides message deduplication by tracking message content hashes.
 *
 * <p>Azure Storage Queue does not natively support message deduplication.
 * This handler uses an in-memory LRU cache of SHA-256 content hashes to detect
 * and skip duplicate messages within a configurable time window.</p>
 *
 * <p><strong>Limitations:</strong></p>
 * <ul>
 *   <li>Deduplication state is local to the JVM — not shared across instances.</li>
 *   <li>Cache entries expire based on capacity (LRU eviction), not time.</li>
 *   <li>For distributed deduplication, use an external store (e.g., Redis).</li>
 * </ul>
 *
 * @see com.azure.storagequeue.largemessage.config.LargeMessageClientConfiguration#isDeduplicationEnabled()
 */
public class DeduplicationHandler {

    private static final Logger logger = LoggerFactory.getLogger(DeduplicationHandler.class);

    /** Default maximum number of message hashes to retain in the deduplication cache. */
    public static final int DEFAULT_CACHE_SIZE = 10_000;

    private final Set<String> seenHashes;
    private final int maxCacheSize;

    /**
     * Creates a new DeduplicationHandler with default cache size.
     */
    public DeduplicationHandler() {
        this(DEFAULT_CACHE_SIZE);
    }

    /**
     * Creates a new DeduplicationHandler with a specified cache size.
     *
     * @param maxCacheSize the maximum number of message hashes to retain
     */
    public DeduplicationHandler(int maxCacheSize) {
        this.maxCacheSize = maxCacheSize;
        // Thread-safe LRU set backed by a LinkedHashMap
        this.seenHashes = Collections.newSetFromMap(
            Collections.synchronizedMap(new LinkedHashMap<String, Boolean>(maxCacheSize + 1, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                    return size() > maxCacheSize;
                }
            })
        );
    }

    /**
     * Checks whether a message body has already been seen (is a duplicate).
     *
     * <p>If the message has not been seen, it is recorded in the cache and
     * {@code false} is returned. If it has been seen, {@code true} is returned.</p>
     *
     * @param messageBody the message body to check
     * @return {@code true} if the message is a duplicate, {@code false} otherwise
     */
    public boolean isDuplicate(String messageBody) {
        String hash = computeHash(messageBody);
        boolean alreadySeen = !seenHashes.add(hash);
        if (alreadySeen) {
            logger.debug("Duplicate message detected (hash: {})", hash.substring(0, 8));
        }
        return alreadySeen;
    }

    /**
     * Marks a message body as seen without checking for duplicates.
     *
     * @param messageBody the message body to mark as seen
     */
    public void markSeen(String messageBody) {
        seenHashes.add(computeHash(messageBody));
    }

    /**
     * Clears the deduplication cache.
     */
    public void clearCache() {
        seenHashes.clear();
        logger.debug("Deduplication cache cleared");
    }

    /**
     * Returns the current number of entries in the deduplication cache.
     *
     * @return the cache size
     */
    public int getCacheSize() {
        return seenHashes.size();
    }

    /**
     * Computes a SHA-256 hash of the given message body.
     *
     * @param messageBody the message body
     * @return the hex-encoded hash string
     */
    private String computeHash(String messageBody) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(messageBody.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed to be available in all JVMs
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
