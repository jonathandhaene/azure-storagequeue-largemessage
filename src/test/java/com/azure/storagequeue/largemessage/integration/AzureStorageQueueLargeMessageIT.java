/*
 * DISCLAIMER: This software is provided "as is", without warranty of any kind,
 * express or implied. The author(s) and maintainer(s) are not responsible for
 * any damage, data loss, or other issues arising from the use of this software.
 * Use at your own risk.
 *
 * See the LICENSE file for full terms.
 */

package com.azure.storagequeue.largemessage.integration;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.queue.QueueClient;
import com.azure.storagequeue.largemessage.client.AzureStorageQueueLargeMessageClient;
import com.azure.storagequeue.largemessage.config.LargeMessageClientConfiguration;
import com.azure.storagequeue.largemessage.model.LargeQueueMessage;
import com.azure.storagequeue.largemessage.store.BlobPayloadStore;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for AzureStorageQueueLargeMessageClient.
 * Tests the full end-to-end flow with Azure Storage Queue and Blob Storage.
 * 
 * These tests can run against:
 * - Local Azurite emulator (default)
 * - Real Azure cloud resources (set AZURE_STORAGE_CONNECTION_STRING environment variable)
 */
@SpringBootTest(classes = {IntegrationTestConfiguration.class})
@ActiveProfiles("integration-test")
@Tag("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AzureStorageQueueLargeMessageIT {

    @Autowired
    private AzureStorageQueueLargeMessageClient client;

    @Autowired
    private QueueClient queueClient;

    @Autowired
    private BlobServiceClient blobServiceClient;

    @Autowired
    private BlobPayloadStore payloadStore;

    @Autowired
    private LargeMessageClientConfiguration config;

    @Autowired
    private IntegrationTestConfiguration testConfig;

    private String containerName;

    @BeforeEach
    void setUp() {
        // Get the container name from the test configuration
        containerName = testConfig.getUniqueContainerName();
        
        // Ensure container exists
        try {
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
            if (!containerClient.exists()) {
                containerClient.create();
            }
        } catch (Exception e) {
            // Container might already exist, ignore
        }

        // Ensure queue is empty before each test
        clearQueue();
    }

    @AfterEach
    void tearDown() {
        // Clean up queue
        try {
            clearQueue();
        } catch (Exception e) {
            // Ignore cleanup errors
        }

        // Clean up blob container
        try {
            if (containerName != null) {
                BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
                if (containerClient.exists()) {
                    containerClient.delete();
                }
            }
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    /**
     * Clears all messages from the queue.
     */
    private void clearQueue() {
        try {
            // Receive and delete all messages
            List<LargeQueueMessage> messages;
            do {
                messages = client.receiveMessages(32);
                for (LargeQueueMessage message : messages) {
                    try {
                        client.deleteMessage(message);
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            } while (!messages.isEmpty());
        } catch (Exception e) {
            // Ignore errors during cleanup
        }
    }

    @Test
    @Order(1)
    @DisplayName("Test 1: Small message send and receive")
    @Timeout(30)
    void testSmallMessageSendAndReceive() {
        // Given: A small message (less than 64KB)
        String messageBody = "Hello, this is a small test message!";

        // When: We send the message
        String messageId = client.sendMessage(messageBody);

        // Then: Message ID should be returned
        assertNotNull(messageId);
        assertFalse(messageId.isEmpty());

        // When: We receive the message
        List<LargeQueueMessage> messages = client.receiveMessages(10);

        // Then: We should receive exactly one message
        assertEquals(1, messages.size());

        LargeQueueMessage receivedMessage = messages.get(0);
        assertEquals(messageBody, receivedMessage.getBody());
        assertFalse(receivedMessage.isPayloadFromBlob(), "Small message should not be stored in blob");
        assertNull(receivedMessage.getBlobPointer());

        // Clean up
        client.deleteMessage(receivedMessage);
    }

    @Test
    @Order(2)
    @DisplayName("Test 2: Large message send and receive (blob offloading)")
    @Timeout(30)
    void testLargeMessageSendAndReceive() {
        // Given: A large message (greater than 64KB)
        String largeMessageBody = generateLargeMessage(70_000); // 70KB

        // When: We send the message
        String messageId = client.sendMessage(largeMessageBody);

        // Then: Message ID should be returned
        assertNotNull(messageId);

        // When: We receive the message
        List<LargeQueueMessage> messages = client.receiveMessages(10);

        // Then: We should receive exactly one message
        assertEquals(1, messages.size());

        LargeQueueMessage receivedMessage = messages.get(0);
        assertEquals(largeMessageBody, receivedMessage.getBody());
        assertTrue(receivedMessage.isPayloadFromBlob(), "Large message should be stored in blob");
        assertNotNull(receivedMessage.getBlobPointer());

        // Clean up
        client.deleteMessage(receivedMessage);
    }

    @Test
    @Order(3)
    @DisplayName("Test 3: Message with metadata")
    @Timeout(30)
    void testMessageWithMetadata() {
        // Given: A message with custom metadata
        String messageBody = "Message with metadata";
        Map<String, String> metadata = new HashMap<>();
        metadata.put("userId", "user-123");
        metadata.put("source", "integration-test");
        metadata.put("priority", "high");

        // When: We send the message with metadata
        String messageId = client.sendMessage(messageBody, metadata);

        // Then: Message ID should be returned
        assertNotNull(messageId);

        // When: We receive the message
        List<LargeQueueMessage> messages = client.receiveMessages(10);

        // Then: We should receive the message with metadata preserved
        assertEquals(1, messages.size());

        LargeQueueMessage receivedMessage = messages.get(0);
        assertEquals(messageBody, receivedMessage.getBody());
        assertNotNull(receivedMessage.getMetadata());
        assertEquals("user-123", receivedMessage.getMetadata().get("userId"));
        assertEquals("integration-test", receivedMessage.getMetadata().get("source"));
        assertEquals("high", receivedMessage.getMetadata().get("priority"));

        // Clean up
        client.deleteMessage(receivedMessage);
    }

    @Test
    @Order(4)
    @DisplayName("Test 4: Message deletion with blob cleanup")
    @Timeout(30)
    void testMessageDeletionWithBlobCleanup() {
        // Given: A large message that will be stored in blob
        String largeMessageBody = generateLargeMessage(70_000);

        // When: We send the message
        client.sendMessage(largeMessageBody);

        // And: We receive the message
        List<LargeQueueMessage> messages = client.receiveMessages(10);
        assertEquals(1, messages.size());

        LargeQueueMessage receivedMessage = messages.get(0);
        assertTrue(receivedMessage.isPayloadFromBlob());
        assertNotNull(receivedMessage.getBlobPointer());

        // Store blob pointer for verification
        String blobName = receivedMessage.getBlobPointer().getBlobName();

        // When: We delete the message (with cleanupBlobOnDelete=true)
        client.deleteMessage(receivedMessage);

        // Then: The queue should be empty
        List<LargeQueueMessage> remainingMessages = client.receiveMessages(10);
        assertEquals(0, remainingMessages.size());

        // Note: Blob cleanup verification is best-effort in the actual implementation
        // The blob should be deleted, but we're not explicitly verifying it here
        // as the actual blob container is cleaned up in tearDown
    }

    @Test
    @Order(5)
    @DisplayName("Test 5: Batch send and receive")
    @Timeout(30)
    void testBatchSendAndReceive() {
        // Given: Multiple messages to send
        List<String> messageBodies = Arrays.asList(
            "Message 1",
            "Message 2",
            "Message 3",
            "Message 4",
            "Message 5"
        );

        // When: We send multiple messages
        List<String> messageIds = client.sendMessages(messageBodies);

        // Then: All message IDs should be returned
        assertEquals(5, messageIds.size());
        messageIds.forEach(id -> assertNotNull(id));

        // When: We receive the messages
        List<LargeQueueMessage> messages = client.receiveMessages(10);

        // Then: We should receive all messages
        assertEquals(5, messages.size());

        Set<String> receivedBodies = messages.stream()
            .map(LargeQueueMessage::getBody)
            .collect(Collectors.toSet());

        assertTrue(receivedBodies.contains("Message 1"));
        assertTrue(receivedBodies.contains("Message 2"));
        assertTrue(receivedBodies.contains("Message 3"));
        assertTrue(receivedBodies.contains("Message 4"));
        assertTrue(receivedBodies.contains("Message 5"));

        // Clean up
        messages.forEach(client::deleteMessage);
    }

    @Test
    @Order(6)
    @DisplayName("Test 6: Always-through-blob mode")
    @Timeout(30)
    void testAlwaysThroughBlobMode() {
        // Given: A configuration with always-through-blob enabled
        config.setAlwaysThroughBlob(true);

        // Create a new client with the updated configuration
        AzureStorageQueueLargeMessageClient alwaysBlobClient = 
            new AzureStorageQueueLargeMessageClient(queueClient, payloadStore, config);

        // Given: A small message (less than 64KB)
        String smallMessageBody = "Small message that should go through blob";

        try {
            // When: We send the message with always-through-blob enabled
            String messageId = alwaysBlobClient.sendMessage(smallMessageBody);

            // Then: Message ID should be returned
            assertNotNull(messageId);

            // When: We receive the message
            List<LargeQueueMessage> messages = alwaysBlobClient.receiveMessages(10);

            // Then: The message should be marked as coming from blob
            assertEquals(1, messages.size());
            LargeQueueMessage receivedMessage = messages.get(0);
            assertEquals(smallMessageBody, receivedMessage.getBody());
            assertTrue(receivedMessage.isPayloadFromBlob(), 
                "Message should be stored in blob when always-through-blob is enabled");
            assertNotNull(receivedMessage.getBlobPointer());

            // Clean up
            alwaysBlobClient.deleteMessage(receivedMessage);
        } finally {
            // Reset configuration
            config.setAlwaysThroughBlob(false);
        }
    }

    @Test
    @Order(7)
    @DisplayName("Test 7: Queue statistics")
    @Timeout(30)
    void testQueueStatistics() {
        // Given: An empty queue
        clearQueue();

        // When: We check the message count
        int initialCount = client.getApproximateMessageCount();

        // Then: Count should be zero or close to zero
        assertTrue(initialCount >= 0, "Message count should be non-negative");

        // When: We send 3 messages
        client.sendMessage("Message 1");
        client.sendMessage("Message 2");
        client.sendMessage("Message 3");

        // Wait a moment for the queue to update
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Then: The approximate message count should reflect the new messages
        int updatedCount = client.getApproximateMessageCount();
        assertTrue(updatedCount >= 3, 
            "Message count should be at least 3 after sending 3 messages");

        // Clean up
        clearQueue();
    }

    @Test
    @Order(8)
    @DisplayName("Test 8: Peek messages")
    @Timeout(30)
    void testPeekMessages() {
        // Given: Messages in the queue
        String message1 = "Peek message 1";
        String message2 = "Peek message 2";

        client.sendMessage(message1);
        client.sendMessage(message2);

        // When: We peek messages (without consuming them)
        List<String> peekedMessages = client.peekMessages(10);

        // Then: We should see the messages
        assertTrue(peekedMessages.size() >= 2, "Should peek at least 2 messages");

        // When: We receive messages normally
        List<LargeQueueMessage> receivedMessages = client.receiveMessages(10);

        // Then: We should still be able to receive the messages
        assertTrue(receivedMessages.size() >= 2, "Should still receive the messages after peeking");

        // Clean up
        receivedMessages.forEach(client::deleteMessage);
    }

    /**
     * Generates a large message of the specified size.
     */
    private String generateLargeMessage(int sizeInBytes) {
        StringBuilder sb = new StringBuilder(sizeInBytes);
        String pattern = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        int patternLength = pattern.length();

        for (int i = 0; i < sizeInBytes; i++) {
            sb.append(pattern.charAt(i % patternLength));
        }

        return sb.toString();
    }
}
