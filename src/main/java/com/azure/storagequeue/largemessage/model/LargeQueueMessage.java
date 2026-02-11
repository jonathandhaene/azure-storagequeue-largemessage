/*
 * DISCLAIMER: This software is provided "as is", without warranty of any kind,
 * express or implied. The author(s) and maintainer(s) are not responsible for
 * any damage, data loss, or other issues arising from the use of this software.
 * Use at your own risk.
 *
 * See the LICENSE file for full terms.
 */

package com.azure.storagequeue.largemessage.model;

import java.util.Map;

/**
 * Wrapper for received Storage Queue messages with large message client support.
 * Contains the message body (resolved from blob if needed) and metadata.
 */
public class LargeQueueMessage {
    private final String messageId;
    private final String body;
    private final Map<String, String> metadata;
    private final boolean payloadFromBlob;
    private final BlobPointer blobPointer;
    private final long dequeueCount;
    private final String popReceipt;

    public LargeQueueMessage(
            String messageId,
            String body,
            Map<String, String> metadata,
            boolean payloadFromBlob,
            BlobPointer blobPointer,
            long dequeueCount,
            String popReceipt) {
        this.messageId = messageId;
        this.body = body;
        this.metadata = metadata;
        this.payloadFromBlob = payloadFromBlob;
        this.blobPointer = blobPointer;
        this.dequeueCount = dequeueCount;
        this.popReceipt = popReceipt;
    }

    /**
     * Gets the unique message identifier.
     *
     * @return the message ID
     */
    public String getMessageId() {
        return messageId;
    }

    /**
     * Gets the message body (resolved from blob storage if needed).
     *
     * @return the message body
     */
    public String getBody() {
        return body;
    }

    /**
     * Gets the metadata associated with the message.
     *
     * @return the metadata map
     */
    public Map<String, String> getMetadata() {
        return metadata;
    }

    /**
     * Indicates whether the payload was retrieved from blob storage.
     *
     * @return true if payload was from blob, false otherwise
     */
    public boolean isPayloadFromBlob() {
        return payloadFromBlob;
    }

    /**
     * Gets the blob pointer if the payload was stored in blob storage.
     *
     * @return the blob pointer, or null if payload was not from blob
     */
    public BlobPointer getBlobPointer() {
        return blobPointer;
    }

    /**
     * Gets the number of times this message has been dequeued.
     *
     * @return the dequeue count
     */
    public long getDequeueCount() {
        return dequeueCount;
    }

    /**
     * Gets the pop receipt for message operations (delete, update).
     *
     * @return the pop receipt
     */
    public String getPopReceipt() {
        return popReceipt;
    }

    @Override
    public String toString() {
        return "LargeQueueMessage{" +
                "messageId='" + messageId + '\'' +
                ", bodyLength=" + (body != null ? body.length() : 0) +
                ", payloadFromBlob=" + payloadFromBlob +
                ", blobPointer=" + blobPointer +
                ", dequeueCount=" + dequeueCount +
                '}';
    }
}
