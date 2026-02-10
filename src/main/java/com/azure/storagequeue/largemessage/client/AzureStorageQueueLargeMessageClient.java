package com.azure.storagequeue.largemessage.client;

import com.azure.core.util.BinaryData;
import com.azure.storagequeue.largemessage.config.LargeMessageClientConfiguration;
import com.azure.storagequeue.largemessage.model.BlobPointer;
import com.azure.storagequeue.largemessage.model.LargeQueueMessage;
import com.azure.storagequeue.largemessage.store.BlobPayloadStore;
import com.azure.storagequeue.largemessage.util.RetryHandler;
import com.azure.storage.queue.QueueClient;
import com.azure.storage.queue.models.QueueMessageItem;
import com.azure.storage.queue.models.SendMessageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Azure Storage Queue Large Message Client.
 * Transparently offloads large message payloads to Azure Blob Storage.
 */
public class AzureStorageQueueLargeMessageClient {
    private static final Logger logger = LoggerFactory.getLogger(AzureStorageQueueLargeMessageClient.class);

    private final QueueClient queueClient;
    private final BlobPayloadStore payloadStore;
    private final LargeMessageClientConfiguration config;
    private final RetryHandler retryHandler;

    /**
     * Creates a new AzureStorageQueueLargeMessageClient.
     *
     * @param queueClient  the queue client
     * @param payloadStore the blob payload store (can be null in receive-only mode)
     * @param config       the configuration
     */
    public AzureStorageQueueLargeMessageClient(
            QueueClient queueClient,
            BlobPayloadStore payloadStore,
            LargeMessageClientConfiguration config) {
        this.queueClient = queueClient;
        this.payloadStore = payloadStore;
        this.config = config;
        
        // Initialize retry handler
        this.retryHandler = new RetryHandler(
            config.getRetryMaxAttempts(),
            config.getRetryBackoffMillis(),
            config.getRetryBackoffMultiplier(),
            config.getRetryMaxBackoffMillis()
        );
        
        // Ensure queue exists
        ensureQueueExists();
        
        logger.info("AzureStorageQueueLargeMessageClient initialized for queue: {}", 
                   queueClient.getQueueName());
    }

    /**
     * Ensures the queue exists, creating it if necessary.
     */
    private void ensureQueueExists() {
        try {
            // Try to create the queue - will fail silently if it already exists
            queueClient.create();
            logger.info("Queue '{}' created successfully", queueClient.getQueueName());
        } catch (com.azure.storage.queue.models.QueueStorageException e) {
            // Queue already exists - this is expected
            if (e.getErrorCode() == com.azure.storage.queue.models.QueueErrorCode.QUEUE_ALREADY_EXISTS) {
                logger.debug("Queue '{}' already exists", queueClient.getQueueName());
            } else {
                logger.warn("Failed to create queue: {}", e.getMessage());
            }
        } catch (Exception e) {
            logger.warn("Failed to check/create queue (may already exist): {}", e.getMessage());
        }
    }

    /**
     * Sends a message to the queue.
     * Large messages are automatically offloaded to blob storage.
     *
     * @param messageBody the message body
     * @return the message ID
     */
    public String sendMessage(String messageBody) {
        return sendMessage(messageBody, null, null);
    }

    /**
     * Sends a message to the queue with metadata.
     * Large messages are automatically offloaded to blob storage.
     *
     * @param messageBody the message body
     * @param metadata    optional metadata (can be null)
     * @return the message ID
     */
    public String sendMessage(String messageBody, Map<String, String> metadata) {
        return sendMessage(messageBody, metadata, null);
    }

    /**
     * Sends a message to the queue with metadata and visibility timeout.
     * Large messages are automatically offloaded to blob storage.
     *
     * @param messageBody       the message body
     * @param metadata          optional metadata (can be null)
     * @param visibilityTimeout optional visibility timeout (can be null)
     * @return the message ID
     */
    public String sendMessage(String messageBody, Map<String, String> metadata, Duration visibilityTimeout) {
        try {
            logger.debug("Sending message to queue: {}", queueClient.getQueueName());
            
            if (metadata == null) {
                metadata = new HashMap<>();
            }
            
            // Check if message should be offloaded to blob
            boolean shouldOffload = config.getMessageSizeCriteria() != null && 
                                   config.getMessageSizeCriteria().shouldOffload(messageBody, metadata);
            
            String finalMessageBody = messageBody;
            BlobPointer blobPointer = null;
            
            if (shouldOffload) {
                if (payloadStore == null) {
                    throw new IllegalStateException(
                        "Payload store is required for sending messages that exceed size threshold. " +
                        "Ensure azure.storage.connection-string is configured."
                    );
                }
                
                logger.debug("Message exceeds size threshold. Offloading to blob storage.");
                
                // Generate blob name
                String blobName = config.getBlobNameResolver().resolve(UUID.randomUUID().toString());
                
                // Store payload in blob
                blobPointer = retryHandler.executeWithRetry(() -> 
                    payloadStore.storePayload(blobName, messageBody)
                );
                
                // Generate SAS URI if enabled
                if (config.isSasEnabled()) {
                    String sasUri = payloadStore.generateSasUri(blobPointer, config.getSasTokenValidationTime());
                    metadata.put("BlobSasUri", sasUri);
                    logger.debug("Generated SAS URI for blob: {}", blobName);
                }
                
                // Replace message body with blob pointer
                finalMessageBody = config.getBodyReplacer().replace(messageBody, blobPointer);
                
                // Add marker to metadata
                metadata.put(LargeMessageClientConfiguration.BLOB_POINTER_MARKER, "true");
                metadata.put(LargeMessageClientConfiguration.RESERVED_METADATA_KEY, 
                           String.valueOf(messageBody.getBytes(StandardCharsets.UTF_8).length));
                
                logger.debug("Payload offloaded to blob: {}", blobName);
            }
            
            // Serialize metadata into message (Storage Queue doesn't have native metadata)
            String messageWithMetadata = serializeMessageWithMetadata(finalMessageBody, metadata);
            
            // Send message to queue
            SendMessageResult result;
            if (visibilityTimeout != null) {
                result = retryHandler.executeWithRetry(() -> 
                    queueClient.sendMessageWithResponse(
                        messageWithMetadata, 
                        visibilityTimeout, 
                        null, 
                        null, 
                        null
                    ).getValue()
                );
            } else {
                result = retryHandler.executeWithRetry(() -> 
                    queueClient.sendMessage(messageWithMetadata)
                );
            }
            
            logger.debug("Message sent successfully. Message ID: {}", result.getMessageId());
            return result.getMessageId();
            
        } catch (Exception e) {
            logger.error("Failed to send message to queue", e);
            throw new RuntimeException("Failed to send message to queue", e);
        }
    }

    /**
     * Sends multiple messages to the queue.
     *
     * @param messageBodies the list of message bodies
     * @return list of message IDs
     */
    public List<String> sendMessages(List<String> messageBodies) {
        return messageBodies.stream()
            .map(this::sendMessage)
            .collect(Collectors.toList());
    }

    /**
     * Receives messages from the queue.
     * Automatically resolves blob payloads if present.
     *
     * @param maxMessages maximum number of messages to receive (1-32)
     * @return list of large queue messages
     */
    public List<LargeQueueMessage> receiveMessages(int maxMessages) {
        return receiveMessages(maxMessages, null);
    }

    /**
     * Receives messages from the queue with visibility timeout.
     * Automatically resolves blob payloads if present.
     *
     * @param maxMessages       maximum number of messages to receive (1-32)
     * @param visibilityTimeout optional visibility timeout (can be null)
     * @return list of large queue messages
     */
    public List<LargeQueueMessage> receiveMessages(int maxMessages, Duration visibilityTimeout) {
        try {
            logger.debug("Receiving up to {} messages from queue: {}", maxMessages, queueClient.getQueueName());
            
            // Validate max messages
            if (maxMessages < 1 || maxMessages > 32) {
                throw new IllegalArgumentException("maxMessages must be between 1 and 32");
            }
            
            // Receive messages from queue
            Iterable<QueueMessageItem> queueMessages;
            if (visibilityTimeout != null) {
                queueMessages = queueClient.receiveMessages(maxMessages, visibilityTimeout, null, null);
            } else {
                queueMessages = queueClient.receiveMessages(maxMessages);
            }
            
            // Process and resolve messages
            List<LargeQueueMessage> result = new ArrayList<>();
            for (QueueMessageItem queueMessage : queueMessages) {
                try {
                    LargeQueueMessage largeMessage = processReceivedMessage(queueMessage);
                    result.add(largeMessage);
                } catch (Exception e) {
                    logger.error("Failed to process received message: {}", queueMessage.getMessageId(), e);
                    // Continue processing other messages
                }
            }
            
            logger.debug("Received and processed {} messages", result.size());
            return result;
            
        } catch (Exception e) {
            logger.error("Failed to receive messages from queue", e);
            throw new RuntimeException("Failed to receive messages from queue", e);
        }
    }

    /**
     * Processes a received queue message, resolving blob payloads if necessary.
     *
     * @param queueMessage the queue message
     * @return the large queue message
     */
    private LargeQueueMessage processReceivedMessage(QueueMessageItem queueMessage) {
        try {
            // Deserialize message with metadata
            MessageWithMetadata parsed = deserializeMessageWithMetadata(queueMessage.getBody().toString());
            String messageBody = parsed.body;
            Map<String, String> metadata = parsed.metadata;
            
            boolean isPayloadFromBlob = false;
            BlobPointer blobPointer = null;
            
            // Check if message contains blob pointer
            if ("true".equals(metadata.get(LargeMessageClientConfiguration.BLOB_POINTER_MARKER))) {
                logger.debug("Message contains blob pointer. Resolving from blob storage.");
                isPayloadFromBlob = true;
                
                // Parse blob pointer
                blobPointer = BlobPointer.fromJson(messageBody);
                
                // Retrieve payload from blob
                String resolvedPayload = retrievePayloadFromBlob(blobPointer, metadata);
                if (resolvedPayload != null) {
                    messageBody = resolvedPayload;
                }
                
                // Clean up marker from metadata
                metadata.remove(LargeMessageClientConfiguration.BLOB_POINTER_MARKER);
                metadata.remove(LargeMessageClientConfiguration.RESERVED_METADATA_KEY);
                metadata.remove("BlobSasUri");
            }
            
            return new LargeQueueMessage(
                queueMessage.getMessageId(),
                messageBody,
                metadata,
                isPayloadFromBlob,
                blobPointer,
                queueMessage.getDequeueCount(),
                queueMessage.getPopReceipt()
            );
            
        } catch (Exception e) {
            logger.error("Failed to process received message", e);
            throw new RuntimeException("Failed to process received message", e);
        }
    }

    /**
     * Retrieves payload from blob storage.
     * Tries SAS URI first if available, then falls back to storage credentials.
     *
     * @param blobPointer the blob pointer
     * @param metadata    the message metadata
     * @return the resolved payload, or null if not found and ignorePayloadNotFound is enabled
     */
    private String retrievePayloadFromBlob(BlobPointer blobPointer, Map<String, String> metadata) {
        try {
            // Try SAS URI first if available
            String sasUri = metadata.get("BlobSasUri");
            if (sasUri != null && config.isReceiveOnlyMode()) {
                logger.debug("Using SAS URI to retrieve blob in receive-only mode");
                return retryHandler.executeWithRetry(() -> 
                    payloadStore.getPayloadFromSasUri(sasUri)
                );
            }
            
            if (sasUri != null && payloadStore != null) {
                logger.debug("Using SAS URI to retrieve blob with storage credentials available");
                return retryHandler.executeWithRetry(() -> 
                    payloadStore.getPayloadFromSasUri(sasUri)
                );
            }
            
            // Fall back to storage credentials
            if (payloadStore != null) {
                logger.debug("Using storage credentials to retrieve blob");
                return retryHandler.executeWithRetry(() -> 
                    payloadStore.getPayload(blobPointer)
                );
            }
            
            throw new IllegalStateException(
                "Cannot retrieve blob payload. Neither SAS URI nor storage credentials are available."
            );
            
        } catch (Exception e) {
            logger.error("Failed to retrieve payload from blob", e);
            if (config.isIgnorePayloadNotFound()) {
                logger.warn("Ignoring payload not found error due to configuration");
                return null;
            }
            throw new RuntimeException("Failed to retrieve payload from blob", e);
        }
    }

    /**
     * Deletes a message from the queue.
     *
     * @param message the large queue message
     */
    public void deleteMessage(LargeQueueMessage message) {
        try {
            logger.debug("Deleting message: {}", message.getMessageId());
            
            // Delete message from queue
            queueClient.deleteMessage(message.getMessageId(), message.getPopReceipt());
            
            // Delete blob payload if configured and present
            if (config.isCleanupBlobOnDelete() && message.isPayloadFromBlob() && message.getBlobPointer() != null) {
                deletePayload(message);
            }
            
            logger.debug("Message deleted successfully: {}", message.getMessageId());
            
        } catch (Exception e) {
            logger.error("Failed to delete message: {}", message.getMessageId(), e);
            throw new RuntimeException("Failed to delete message", e);
        }
    }

    /**
     * Deletes the blob payload for a message.
     *
     * @param message the large queue message
     */
    public void deletePayload(LargeQueueMessage message) {
        if (message == null || !message.isPayloadFromBlob() || message.getBlobPointer() == null) {
            logger.debug("No blob payload to delete");
            return;
        }
        
        try {
            logger.debug("Deleting blob payload: {}", message.getBlobPointer().getBlobName());
            
            if (payloadStore == null) {
                logger.warn("Cannot delete blob payload: payload store not available");
                return;
            }
            
            retryHandler.executeWithRetry(() -> {
                payloadStore.deletePayload(message.getBlobPointer());
                return null;
            });
            
            logger.debug("Blob payload deleted successfully");
            
        } catch (Exception e) {
            logger.error("Failed to delete blob payload", e);
            // Don't throw - blob cleanup is best-effort
        }
    }

    /**
     * Deletes blob payloads for multiple messages.
     *
     * @param messages the list of large queue messages
     * @return the number of successfully deleted blobs
     */
    public int deletePayloadBatch(List<LargeQueueMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return 0;
        }
        
        int deletedCount = 0;
        for (LargeQueueMessage message : messages) {
            try {
                deletePayload(message);
                deletedCount++;
            } catch (Exception e) {
                logger.warn("Failed to delete blob for message {}: {}", 
                           message.getMessageId(), e.getMessage());
                // Continue with other messages
            }
        }
        
        logger.debug("Deleted {} blob payloads out of {} messages", deletedCount, messages.size());
        return deletedCount;
    }

    /**
     * Peeks messages from the queue without removing them.
     *
     * @param maxMessages maximum number of messages to peek (1-32)
     * @return list of peeked messages (bodies only, no blob resolution)
     */
    public List<String> peekMessages(int maxMessages) {
        try {
            logger.debug("Peeking up to {} messages from queue", maxMessages);
            
            if (maxMessages < 1 || maxMessages > 32) {
                throw new IllegalArgumentException("maxMessages must be between 1 and 32");
            }
            
            return queueClient.peekMessages(maxMessages, null, null).stream()
                .map(msg -> msg.getBody().toString())
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            logger.error("Failed to peek messages", e);
            throw new RuntimeException("Failed to peek messages", e);
        }
    }

    /**
     * Gets the approximate number of messages in the queue.
     *
     * @return the approximate message count
     */
    public int getApproximateMessageCount() {
        try {
            return queueClient.getProperties().getApproximateMessagesCount();
        } catch (Exception e) {
            logger.error("Failed to get message count", e);
            throw new RuntimeException("Failed to get message count", e);
        }
    }

    /**
     * Serializes a message with metadata into a single string.
     * Format: JSON with body and metadata fields.
     *
     * @param body     the message body
     * @param metadata the metadata map
     * @return serialized message
     */
    private String serializeMessageWithMetadata(String body, Map<String, String> metadata) {
        try {
            Map<String, Object> envelope = new HashMap<>();
            envelope.put("body", body);
            envelope.put("metadata", metadata);
            
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(envelope);
        } catch (Exception e) {
            logger.error("Failed to serialize message with metadata", e);
            throw new RuntimeException("Failed to serialize message with metadata", e);
        }
    }

    /**
     * Deserializes a message with metadata from a string.
     *
     * @param messageString the serialized message
     * @return the message with metadata
     */
    private MessageWithMetadata deserializeMessageWithMetadata(String messageString) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> envelope = mapper.readValue(messageString, Map.class);
            
            String body = (String) envelope.get("body");
            @SuppressWarnings("unchecked")
            Map<String, String> metadata = (Map<String, String>) envelope.getOrDefault("metadata", new HashMap<>());
            
            return new MessageWithMetadata(body, metadata);
        } catch (Exception e) {
            // If deserialization fails, treat the entire message as body with no metadata
            logger.debug("Message does not have metadata envelope. Treating as plain message.");
            return new MessageWithMetadata(messageString, new HashMap<>());
        }
    }

    /**
     * Internal class to hold message body and metadata together.
     */
    private static class MessageWithMetadata {
        final String body;
        final Map<String, String> metadata;

        MessageWithMetadata(String body, Map<String, String> metadata) {
            this.body = body;
            this.metadata = metadata;
        }
    }
}
