/*
 * DISCLAIMER: This software is provided "as is", without warranty of any kind,
 * express or implied. The author(s) and maintainer(s) are not responsible for
 * any damage, data loss, or other issues arising from the use of this software.
 * Use at your own risk.
 *
 * See the LICENSE file for full terms.
 */

package com.azure.storagequeue.largemessage.client;

import com.azure.storage.queue.QueueClient;
import com.azure.storage.queue.QueueClientBuilder;
import com.azure.storage.queue.models.SendMessageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Dead-letter queue (DLQ) handler for Azure Storage Queue.
 *
 * <p>Azure Storage Queue does not have native dead-letter queue support.
 * This class provides a manual DLQ mechanism by moving poison messages
 * (those that exceed a configurable maximum dequeue count) to a separate
 * "dead-letter" queue.</p>
 *
 * <p>Usage example:</p>
 * <pre>
 * // Create DLQ handler
 * DeadLetterQueueHandler dlqHandler = new DeadLetterQueueHandler(connectionString, "my-queue-dlq");
 *
 * // In your message processing loop:
 * for (LargeQueueMessage msg : client.receiveMessages(10)) {
 *     if (dlqHandler.shouldDeadLetter(msg.getDequeueCount())) {
 *         dlqHandler.sendToDeadLetterQueue(msg.getBody(), "Max dequeue count exceeded");
 *         client.deleteMessage(msg);
 *         continue;
 *     }
 *     // normal processing...
 * }
 * </pre>
 *
 * @see com.azure.storagequeue.largemessage.config.LargeMessageClientConfiguration#getDeadLetterMaxDequeueCount()
 */
public class DeadLetterQueueHandler {

    private static final Logger logger = LoggerFactory.getLogger(DeadLetterQueueHandler.class);

    /** Default suffix appended to original queue name to form the DLQ name. */
    public static final String DEFAULT_DLQ_SUFFIX = "-dlq";

    /** Default maximum dequeue count before a message is dead-lettered. */
    public static final int DEFAULT_MAX_DEQUEUE_COUNT = 5;

    private final QueueClient dlqClient;
    private final int maxDequeueCount;

    /**
     * Creates a DeadLetterQueueHandler with a connection string and queue name.
     *
     * @param connectionString the Azure Storage connection string
     * @param dlqQueueName     the name of the dead-letter queue
     */
    public DeadLetterQueueHandler(String connectionString, String dlqQueueName) {
        this(connectionString, dlqQueueName, DEFAULT_MAX_DEQUEUE_COUNT);
    }

    /**
     * Creates a DeadLetterQueueHandler with a custom max dequeue count.
     *
     * @param connectionString the Azure Storage connection string
     * @param dlqQueueName     the name of the dead-letter queue
     * @param maxDequeueCount  the maximum dequeue count before dead-lettering
     */
    public DeadLetterQueueHandler(String connectionString, String dlqQueueName, int maxDequeueCount) {
        this.maxDequeueCount = maxDequeueCount;
        this.dlqClient = new QueueClientBuilder()
                .connectionString(connectionString)
                .queueName(dlqQueueName)
                .buildClient();

        // Ensure the DLQ exists
        try {
            dlqClient.create();
            logger.info("Dead-letter queue '{}' created", dlqQueueName);
        } catch (com.azure.storage.queue.models.QueueStorageException e) {
            if (e.getErrorCode() == com.azure.storage.queue.models.QueueErrorCode.QUEUE_ALREADY_EXISTS) {
                logger.debug("Dead-letter queue '{}' already exists", dlqQueueName);
            } else {
                logger.warn("Failed to create dead-letter queue '{}': {}", dlqQueueName, e.getMessage());
            }
        }
    }

    /**
     * Creates a DeadLetterQueueHandler using an existing QueueClient.
     *
     * @param dlqClient       the QueueClient for the dead-letter queue
     * @param maxDequeueCount the maximum dequeue count before dead-lettering
     */
    public DeadLetterQueueHandler(QueueClient dlqClient, int maxDequeueCount) {
        this.dlqClient = dlqClient;
        this.maxDequeueCount = maxDequeueCount;
    }

    /**
     * Determines whether a message should be dead-lettered based on its dequeue count.
     *
     * @param dequeueCount the number of times the message has been dequeued
     * @return {@code true} if the message should be moved to the DLQ
     */
    public boolean shouldDeadLetter(long dequeueCount) {
        return dequeueCount >= maxDequeueCount;
    }

    /**
     * Sends a message to the dead-letter queue with a reason.
     *
     * @param messageBody the original message body
     * @param reason      the reason for dead-lettering
     * @return the message ID in the DLQ
     */
    public String sendToDeadLetterQueue(String messageBody, String reason) {
        return sendToDeadLetterQueue(messageBody, reason, null);
    }

    /**
     * Sends a message to the dead-letter queue with a reason and optional visibility timeout.
     *
     * @param messageBody       the original message body
     * @param reason            the reason for dead-lettering
     * @param visibilityTimeout optional visibility timeout
     * @return the message ID in the DLQ
     */
    public String sendToDeadLetterQueue(String messageBody, String reason, Duration visibilityTimeout) {
        try {
            // Wrap message with DLQ metadata
            String dlqMessage = String.format(
                    "{\"originalBody\":%s,\"deadLetterReason\":\"%s\",\"deadLetteredAt\":\"%s\"}",
                    escapeJsonString(messageBody), reason, java.time.OffsetDateTime.now().toString());

            SendMessageResult result;
            if (visibilityTimeout != null) {
                result = dlqClient.sendMessageWithResponse(dlqMessage, visibilityTimeout, null, null, null).getValue();
            } else {
                result = dlqClient.sendMessage(dlqMessage);
            }

            logger.info("Message dead-lettered to '{}'. Reason: {}. DLQ Message ID: {}",
                    dlqClient.getQueueName(), reason, result.getMessageId());
            return result.getMessageId();

        } catch (Exception e) {
            logger.error("Failed to send message to dead-letter queue", e);
            throw new RuntimeException("Failed to send message to dead-letter queue", e);
        }
    }

    /**
     * Gets the approximate number of messages in the dead-letter queue.
     *
     * @return the approximate message count
     */
    public int getApproximateMessageCount() {
        try {
            return dlqClient.getProperties().getApproximateMessagesCount();
        } catch (Exception e) {
            logger.error("Failed to get DLQ message count", e);
            return -1;
        }
    }

    /**
     * Gets the maximum dequeue count threshold.
     *
     * @return the max dequeue count
     */
    public int getMaxDequeueCount() {
        return maxDequeueCount;
    }

    /**
     * Gets the QueueClient for the dead-letter queue.
     *
     * @return the DLQ QueueClient
     */
    public QueueClient getDlqClient() {
        return dlqClient;
    }

    /**
     * Escapes a string for safe inclusion in a JSON value.
     *
     * @param value the string to escape
     * @return the JSON-escaped string (including surrounding quotes)
     */
    private String escapeJsonString(String value) {
        if (value == null) {
            return "null";
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(value);
        } catch (Exception e) {
            // Fallback: basic escape
            return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        }
    }
}
