/*
 * DISCLAIMER: This software is provided "as is", without warranty of any kind,
 * express or implied. The author(s) and maintainer(s) are not responsible for
 * any damage, data loss, or other issues arising from the use of this software.
 * Use at your own risk.
 *
 * See the LICENSE file for full terms.
 */

package com.azure.storagequeue.largemessage.config;

import com.azure.storagequeue.largemessage.store.BlobNameResolver;
import com.azure.storagequeue.largemessage.store.MessageBodyReplacer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the Azure Storage Queue Large Message Client.
 */
@Component
@ConfigurationProperties(prefix = "azure.storagequeue.large-message-client")
public class LargeMessageClientConfiguration {
    
    /**
     * Default message size threshold: 64 KB (65,536 bytes).
     * Storage Queue messages have a 64 KB limit, so this is the maximum.
     */
    public static final int DEFAULT_MESSAGE_SIZE_THRESHOLD = 65536;

    /**
     * Reserved metadata key for storing the original payload size.
     */
    public static final String RESERVED_METADATA_KEY = "ExtendedPayloadSize";

    /**
     * Metadata marker indicating the message body contains a blob pointer.
     */
    public static final String BLOB_POINTER_MARKER = "com.azure.storagequeue.largemessage.BlobPointer";

    /**
     * Large message client user agent identifier.
     */
    public static final String USER_AGENT_VALUE = "AzureStorageQueueLargeMessageClient/1.0.0-SNAPSHOT";

    private int messageSizeThreshold = DEFAULT_MESSAGE_SIZE_THRESHOLD;
    private boolean alwaysThroughBlob = false;
    private boolean cleanupBlobOnDelete = true;
    private String blobKeyPrefix = "";
    
    // Retry configuration
    private int retryMaxAttempts = 3;
    private long retryBackoffMillis = 1000L;
    private double retryBackoffMultiplier = 2.0;
    private long retryMaxBackoffMillis = 30000L;
    
    // Feature toggles
    private boolean ignorePayloadNotFound = false;
    private boolean receiveOnlyMode = false;
    
    // Blob configuration
    private String blobAccessTier = null; // Hot, Cool, Archive
    private int blobTtlDays = 0; // 0 = disabled
    
    // SAS URI configuration
    private boolean sasEnabled = false;
    private java.time.Duration sasTokenValidationTime = java.time.Duration.ofDays(7);
    
    // Compression
    private boolean compressionEnabled = false;

    // Deduplication
    private boolean deduplicationEnabled = false;
    private int deduplicationCacheSize = 10_000;

    // Dead-letter queue
    private boolean deadLetterEnabled = false;
    private String deadLetterQueueName = "";
    private int deadLetterMaxDequeueCount = 5;

    // Tracing
    private boolean tracingEnabled = true;
    
    // Custom resolvers and criteria (transient - not serializable from YAML)
    // These are runtime-injected beans that should not be persisted to configuration files
    private transient BlobNameResolver blobNameResolver;
    private transient MessageBodyReplacer bodyReplacer;
    private transient MessageSizeCriteria messageSizeCriteria;

    /**
     * Gets the message size threshold in bytes.
     * Messages exceeding this size will be offloaded to blob storage.
     *
     * @return the message size threshold
     */
    public int getMessageSizeThreshold() {
        return messageSizeThreshold;
    }

    public void setMessageSizeThreshold(int messageSizeThreshold) {
        this.messageSizeThreshold = messageSizeThreshold;
    }

    /**
     * Indicates whether all messages should be stored in blob storage,
     * regardless of size.
     *
     * @return true if all messages should go through blob, false otherwise
     */
    public boolean isAlwaysThroughBlob() {
        return alwaysThroughBlob;
    }

    public void setAlwaysThroughBlob(boolean alwaysThroughBlob) {
        this.alwaysThroughBlob = alwaysThroughBlob;
    }

    /**
     * Indicates whether blob payloads should be automatically deleted
     * when messages are consumed.
     *
     * @return true if blob cleanup is enabled, false otherwise
     */
    public boolean isCleanupBlobOnDelete() {
        return cleanupBlobOnDelete;
    }

    public void setCleanupBlobOnDelete(boolean cleanupBlobOnDelete) {
        this.cleanupBlobOnDelete = cleanupBlobOnDelete;
    }

    /**
     * Gets the prefix to use for blob names when storing payloads.
     *
     * @return the blob key prefix
     */
    public String getBlobKeyPrefix() {
        return blobKeyPrefix;
    }

    public void setBlobKeyPrefix(String blobKeyPrefix) {
        this.blobKeyPrefix = blobKeyPrefix;
    }

    /**
     * Gets the maximum number of retry attempts for operations.
     *
     * @return the maximum retry attempts
     */
    public int getRetryMaxAttempts() {
        return retryMaxAttempts;
    }

    public void setRetryMaxAttempts(int retryMaxAttempts) {
        this.retryMaxAttempts = retryMaxAttempts;
    }

    /**
     * Gets the initial backoff delay in milliseconds for retries.
     *
     * @return the initial backoff delay
     */
    public long getRetryBackoffMillis() {
        return retryBackoffMillis;
    }

    public void setRetryBackoffMillis(long retryBackoffMillis) {
        this.retryBackoffMillis = retryBackoffMillis;
    }

    /**
     * Gets the backoff multiplier for exponential backoff.
     *
     * @return the backoff multiplier
     */
    public double getRetryBackoffMultiplier() {
        return retryBackoffMultiplier;
    }

    public void setRetryBackoffMultiplier(double retryBackoffMultiplier) {
        this.retryBackoffMultiplier = retryBackoffMultiplier;
    }

    /**
     * Gets the maximum backoff delay in milliseconds.
     *
     * @return the maximum backoff delay
     */
    public long getRetryMaxBackoffMillis() {
        return retryMaxBackoffMillis;
    }

    public void setRetryMaxBackoffMillis(long retryMaxBackoffMillis) {
        this.retryMaxBackoffMillis = retryMaxBackoffMillis;
    }

    /**
     * Indicates whether payload not found errors should be ignored.
     *
     * @return true if errors should be ignored, false otherwise
     */
    public boolean isIgnorePayloadNotFound() {
        return ignorePayloadNotFound;
    }

    public void setIgnorePayloadNotFound(boolean ignorePayloadNotFound) {
        this.ignorePayloadNotFound = ignorePayloadNotFound;
    }

    /**
     * Indicates whether receive-only mode is enabled (SAS URI mode).
     *
     * @return true if receive-only mode is enabled, false otherwise
     */
    public boolean isReceiveOnlyMode() {
        return receiveOnlyMode;
    }

    public void setReceiveOnlyMode(boolean receiveOnlyMode) {
        this.receiveOnlyMode = receiveOnlyMode;
    }

    /**
     * Gets the blob access tier (Hot, Cool, Archive).
     *
     * @return the blob access tier, or null if not configured
     */
    public String getBlobAccessTier() {
        return blobAccessTier;
    }

    public void setBlobAccessTier(String blobAccessTier) {
        this.blobAccessTier = blobAccessTier;
    }

    /**
     * Gets the blob TTL (time-to-live) in days.
     *
     * @return the TTL in days, or 0 if disabled
     */
    public int getBlobTtlDays() {
        return blobTtlDays;
    }

    public void setBlobTtlDays(int blobTtlDays) {
        this.blobTtlDays = blobTtlDays;
    }

    /**
     * Indicates whether SAS URI generation is enabled.
     *
     * @return true if SAS is enabled, false otherwise
     */
    public boolean isSasEnabled() {
        return sasEnabled;
    }

    public void setSasEnabled(boolean sasEnabled) {
        this.sasEnabled = sasEnabled;
    }

    /**
     * Gets the SAS token validation time duration.
     *
     * @return the validation time duration
     */
    public java.time.Duration getSasTokenValidationTime() {
        return sasTokenValidationTime;
    }

    public void setSasTokenValidationTime(java.time.Duration sasTokenValidationTime) {
        this.sasTokenValidationTime = sasTokenValidationTime;
    }

    /**
     * Indicates whether tracing is enabled.
     *
     * @return true if tracing is enabled, false otherwise
     */
    public boolean isTracingEnabled() {
        return tracingEnabled;
    }

    public void setTracingEnabled(boolean tracingEnabled) {
        this.tracingEnabled = tracingEnabled;
    }

    /**
     * Gets the custom blob name resolver.
     *
     * @return the blob name resolver
     */
    public BlobNameResolver getBlobNameResolver() {
        return blobNameResolver;
    }

    public void setBlobNameResolver(BlobNameResolver blobNameResolver) {
        this.blobNameResolver = blobNameResolver;
    }

    /**
     * Gets the custom message body replacer.
     *
     * @return the message body replacer
     */
    public MessageBodyReplacer getBodyReplacer() {
        return bodyReplacer;
    }

    public void setBodyReplacer(MessageBodyReplacer bodyReplacer) {
        this.bodyReplacer = bodyReplacer;
    }

    /**
     * Gets the custom message size criteria.
     *
     * @return the message size criteria
     */
    public MessageSizeCriteria getMessageSizeCriteria() {
        return messageSizeCriteria;
    }

    public void setMessageSizeCriteria(MessageSizeCriteria messageSizeCriteria) {
        this.messageSizeCriteria = messageSizeCriteria;
    }

    /**
     * Indicates whether GZIP compression is enabled for blob payloads.
     * When enabled, payloads are compressed before storing in blob storage,
     * reducing storage costs and network bandwidth.
     *
     * @return true if compression is enabled, false otherwise
     */
    public boolean isCompressionEnabled() {
        return compressionEnabled;
    }

    public void setCompressionEnabled(boolean compressionEnabled) {
        this.compressionEnabled = compressionEnabled;
    }

    /**
     * Indicates whether message deduplication is enabled.
     * Uses an in-memory LRU cache of content hashes to detect duplicates.
     *
     * @return true if deduplication is enabled, false otherwise
     */
    public boolean isDeduplicationEnabled() {
        return deduplicationEnabled;
    }

    public void setDeduplicationEnabled(boolean deduplicationEnabled) {
        this.deduplicationEnabled = deduplicationEnabled;
    }

    /**
     * Gets the maximum number of message hashes to retain in the deduplication cache.
     *
     * @return the deduplication cache size
     */
    public int getDeduplicationCacheSize() {
        return deduplicationCacheSize;
    }

    public void setDeduplicationCacheSize(int deduplicationCacheSize) {
        this.deduplicationCacheSize = deduplicationCacheSize;
    }

    /**
     * Indicates whether dead-letter queue support is enabled.
     *
     * @return true if DLQ is enabled, false otherwise
     */
    public boolean isDeadLetterEnabled() {
        return deadLetterEnabled;
    }

    public void setDeadLetterEnabled(boolean deadLetterEnabled) {
        this.deadLetterEnabled = deadLetterEnabled;
    }

    /**
     * Gets the name of the dead-letter queue. If empty, defaults to
     * the main queue name with a "-dlq" suffix.
     *
     * @return the dead-letter queue name
     */
    public String getDeadLetterQueueName() {
        return deadLetterQueueName;
    }

    public void setDeadLetterQueueName(String deadLetterQueueName) {
        this.deadLetterQueueName = deadLetterQueueName;
    }

    /**
     * Gets the maximum dequeue count before a message is dead-lettered.
     *
     * @return the max dequeue count for dead-lettering
     */
    public int getDeadLetterMaxDequeueCount() {
        return deadLetterMaxDequeueCount;
    }

    public void setDeadLetterMaxDequeueCount(int deadLetterMaxDequeueCount) {
        this.deadLetterMaxDequeueCount = deadLetterMaxDequeueCount;
    }
}
