package com.azure.storagequeue.largemessage.example;

import com.azure.storagequeue.largemessage.client.AzureStorageQueueLargeMessageClient;
import com.azure.storagequeue.largemessage.model.LargeQueueMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Example Spring Boot application demonstrating the Azure Storage Queue Large Message Client.
 */
@SpringBootApplication
public class ExampleApplication implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(ExampleApplication.class);

    @Autowired
    private AzureStorageQueueLargeMessageClient client;

    public static void main(String[] args) {
        SpringApplication.run(ExampleApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("Starting Azure Storage Queue Large Message Client Example");

        // Example 1: Send a small message
        logger.info("=== Example 1: Send a small message ===");
        String smallMessage = "Hello, this is a small message!";
        String messageId1 = client.sendMessage(smallMessage);
        logger.info("Sent small message with ID: {}", messageId1);

        // Example 2: Send a large message (will be offloaded to blob)
        logger.info("=== Example 2: Send a large message ===");
        StringBuilder largeMessageBuilder = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            largeMessageBuilder.append("This is a large message that will be offloaded to blob storage. ");
        }
        String largeMessage = largeMessageBuilder.toString();
        logger.info("Large message size: {} bytes", largeMessage.length());
        
        String messageId2 = client.sendMessage(largeMessage);
        logger.info("Sent large message with ID: {}", messageId2);

        // Example 3: Send a message with metadata
        logger.info("=== Example 3: Send a message with metadata ===");
        Map<String, String> metadata = new HashMap<>();
        metadata.put("userId", "user123");
        metadata.put("source", "example-app");
        metadata.put("priority", "high");
        
        String messageId3 = client.sendMessage("Message with custom metadata", metadata);
        logger.info("Sent message with metadata. ID: {}", messageId3);

        // Example 4: Receive messages
        logger.info("=== Example 4: Receive messages ===");
        List<LargeQueueMessage> messages = client.receiveMessages(10);
        logger.info("Received {} messages", messages.size());

        for (LargeQueueMessage message : messages) {
            logger.info("Processing message: {}", message.getMessageId());
            logger.info("  - Body length: {} bytes", message.getBody().length());
            logger.info("  - From blob: {}", message.isPayloadFromBlob());
            logger.info("  - Dequeue count: {}", message.getDequeueCount());
            logger.info("  - Metadata: {}", message.getMetadata());
            
            if (message.getBody().length() > 100) {
                logger.info("  - Body preview: {}...", message.getBody().substring(0, 100));
            } else {
                logger.info("  - Body: {}", message.getBody());
            }

            // Delete the message after processing
            client.deleteMessage(message);
            logger.info("Message deleted: {}", message.getMessageId());
        }

        // Example 5: Get queue statistics
        logger.info("=== Example 5: Queue statistics ===");
        int messageCount = client.getApproximateMessageCount();
        logger.info("Approximate messages in queue: {}", messageCount);

        logger.info("Example completed successfully!");
    }
}
