# Migration Guide: Azure Storage Queue Large Message Client

## Disclaimer

> **⚠️ Disclaimer:** This software is provided "as is", without warranty of any kind, express or implied. The author(s) and maintainer(s) of this project are not responsible for any damage, data loss, service disruption, or other issues that may arise from the use of this software. Use at your own risk. Always test thoroughly in a non-production environment before deploying to production.

This guide provides comprehensive usage examples and best practices for the Azure Storage Queue Large Message Client.

## Table of Contents

1. [Installation](#installation)
2. [Configuration](#configuration)
3. [Basic Usage](#basic-usage)
4. [Advanced Features](#advanced-features)
5. [Best Practices](#best-practices)
6. [Troubleshooting](#troubleshooting)

## Installation

### Maven

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.azure.storagequeue.largemessage</groupId>
    <artifactId>azure-storagequeue-large-message-client</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Configuration

### Basic Configuration

Create an `application.yml` file with the following configuration:

```yaml
azure:
  storage:
    connection-string: DefaultEndpointsProtocol=https;AccountName=myaccount;AccountKey=mykey;EndpointSuffix=core.windows.net
    container-name: large-messages
  
  storagequeue:
    connection-string: ${azure.storage.connection-string}
    queue-name: my-queue
    
    large-message-client:
      message-size-threshold: 65536  # 64 KB
      cleanup-blob-on-delete: true
```

### Environment Variables

You can use environment variables instead of hardcoding connection strings:

```yaml
azure:
  storage:
    connection-string: ${AZURE_STORAGE_CONNECTION_STRING}
    container-name: ${AZURE_STORAGE_CONTAINER_NAME:large-messages}
  
  storagequeue:
    connection-string: ${AZURE_STORAGE_CONNECTION_STRING}
    queue-name: ${AZURE_STORAGEQUEUE_QUEUE_NAME:my-queue}
```

### Full Configuration Options

```yaml
azure:
  storagequeue:
    large-message-client:
      # Message size threshold (bytes)
      message-size-threshold: 65536
      
      # Force all messages through blob
      always-through-blob: false
      
      # Auto-cleanup blob on message delete
      cleanup-blob-on-delete: true
      
      # Blob name prefix
      blob-key-prefix: "messages/"
      
      # Retry configuration
      retry-max-attempts: 3
      retry-backoff-millis: 1000
      retry-backoff-multiplier: 2.0
      retry-max-backoff-millis: 30000
      
      # Feature flags
      ignore-payload-not-found: false
      receive-only-mode: false
      
      # Blob configuration
      blob-access-tier: Hot  # Hot, Cool, or Archive
      blob-ttl-days: 7
      
      # SAS configuration
      sas-enabled: true
      sas-token-validation-time: P7D
      
      # Tracing
      tracing-enabled: true
      
      # Compression – GZIP-compress blob payloads
      compression-enabled: false
      
      # Deduplication – skip duplicate messages (in-memory LRU cache)
      deduplication-enabled: false
      deduplication-cache-size: 10000
      
      # Dead-letter queue
      dead-letter-enabled: false
      dead-letter-queue-name: ""          # defaults to <queue-name>-dlq
      dead-letter-max-dequeue-count: 5
```

## Basic Usage

### Sending Messages

#### Simple Message

```java
@Autowired
private AzureStorageQueueLargeMessageClient client;

// Send a simple message
String messageId = client.sendMessage("Hello, World!");
System.out.println("Message sent with ID: " + messageId);
```

#### Message with Metadata

```java
// Create metadata
Map<String, String> metadata = new HashMap<>();
metadata.put("userId", "user123");
metadata.put("correlationId", "abc-123");
metadata.put("contentType", "application/json");

// Send message with metadata
String messageId = client.sendMessage("{\"data\": \"value\"}", metadata);
```

#### Message with Visibility Timeout

```java
// Send message that won't be visible for 5 minutes
Duration visibilityTimeout = Duration.ofMinutes(5);
String messageId = client.sendMessage("Delayed message", null, visibilityTimeout);
```

### Receiving Messages

#### Basic Receive

```java
// Receive up to 10 messages
List<LargeQueueMessage> messages = client.receiveMessages(10);

for (LargeQueueMessage message : messages) {
    System.out.println("Message ID: " + message.getMessageId());
    System.out.println("Body: " + message.getBody());
    System.out.println("Dequeue count: " + message.getDequeueCount());
    
    // Delete message after processing
    client.deleteMessage(message);
}
```

#### Receive with Custom Visibility Timeout

```java
// Receive messages with 30-second visibility timeout
Duration visibilityTimeout = Duration.ofSeconds(30);
List<LargeQueueMessage> messages = client.receiveMessages(5, visibilityTimeout);

for (LargeQueueMessage message : messages) {
    // Process message
    processMessage(message.getBody());
    
    // Delete after successful processing
    client.deleteMessage(message);
}
```

#### Working with Metadata

```java
List<LargeQueueMessage> messages = client.receiveMessages(10);

for (LargeQueueMessage message : messages) {
    Map<String, String> metadata = message.getMetadata();
    
    // Access metadata
    String userId = metadata.get("userId");
    String priority = metadata.get("priority");
    
    // Check if payload was from blob
    if (message.isPayloadFromBlob()) {
        System.out.println("Large message resolved from blob");
        BlobPointer pointer = message.getBlobPointer();
        System.out.println("Blob: " + pointer.getBlobName());
    }
    
    // Process and delete
    processMessage(message.getBody(), metadata);
    client.deleteMessage(message);
}
```

### Batch Operations

#### Send Multiple Messages

```java
// Send batch of messages
List<String> messageBodies = Arrays.asList(
    "Message 1",
    "Message 2",
    "Message 3"
);

List<String> messageIds = client.sendMessages(messageBodies);
System.out.println("Sent " + messageIds.size() + " messages");
```

#### Cleanup Multiple Blobs

```java
// Receive messages
List<LargeQueueMessage> messages = client.receiveMessages(10);

// Process all messages
for (LargeQueueMessage message : messages) {
    processMessage(message.getBody());
    client.deleteMessage(message);
}

// Or cleanup blobs in batch
int deletedCount = client.deletePayloadBatch(messages);
System.out.println("Deleted " + deletedCount + " blob payloads");
```

### Queue Management

#### Get Message Count

```java
int messageCount = client.getApproximateMessageCount();
System.out.println("Approximate messages in queue: " + messageCount);
```

#### Peek Messages

```java
// Peek without removing messages
List<String> peekedMessages = client.peekMessages(5);
for (String message : peekedMessages) {
    System.out.println("Peeked: " + message);
}
```

## Advanced Features

### Custom Blob Naming Strategy

Implement a custom blob naming strategy:

```java
@Configuration
public class CustomBlobNameConfig {
    
    @Bean
    public BlobNameResolver customBlobNameResolver() {
        return (messageId) -> {
            // Organize blobs by date
            String date = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
            return String.format("messages/%s/%s", date, UUID.randomUUID());
        };
    }
}
```

Multi-tenant blob naming:

```java
@Bean
public BlobNameResolver tenantBlobNameResolver() {
    return (messageId) -> {
        String tenantId = TenantContext.getCurrentTenantId();
        return String.format("%s/%s", tenantId, UUID.randomUUID());
    };
}
```

### Custom Message Body Replacement

Control what goes into the queue after offloading:

```java
@Configuration
public class CustomBodyReplacerConfig {
    
    @Bean
    public MessageBodyReplacer customBodyReplacer() {
        return (originalBody, blobPointer) -> {
            // Create a custom reference instead of JSON
            return String.format("BLOB_REF:%s/%s", 
                blobPointer.getContainerName(), 
                blobPointer.getBlobName());
        };
    }
}
```

### Custom Size Criteria

Implement custom logic for when to offload:

```java
@Configuration
public class CustomSizeCriteriaConfig {
    
    @Bean
    public MessageSizeCriteria customSizeCriteria() {
        return (messageBody, metadata) -> {
            // Offload if marked as large or exceeds size
            boolean forceLarge = "true".equals(metadata.get("forceLarge"));
            boolean exceedsSize = messageBody.length() > 50000;
            
            // Also offload JSON messages over 30KB
            boolean isJson = "application/json".equals(metadata.get("contentType"));
            boolean jsonOverLimit = isJson && messageBody.length() > 30000;
            
            return forceLarge || exceedsSize || jsonOverLimit;
        };
    }
}
```

### Dynamic Connection Strings with Azure Key Vault

```java
@Configuration
public class KeyVaultConfig {
    
    @Bean
    public StorageConnectionStringProvider keyVaultConnectionStringProvider() {
        return () -> {
            // Get Key Vault client
            SecretClient secretClient = new SecretClientBuilder()
                .vaultUrl("https://myvault.vault.azure.net/")
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();
            
            // Retrieve connection string
            KeyVaultSecret secret = secretClient.getSecret("storage-connection-string");
            return secret.getValue();
        };
    }
}
```

### SAS URI Mode (Receive-Only)

Enable receive-only mode to download blobs using SAS URIs without storage credentials:

```yaml
azure:
  storagequeue:
    large-message-client:
      receive-only-mode: true
      sas-enabled: true  # Must be enabled by senders
```

Senders must enable SAS:

```yaml
azure:
  storagequeue:
    large-message-client:
      sas-enabled: true
      sas-token-validation-time: P7D
```

### Long-Running Operations

For long-running message processing, you may need to update visibility timeout:

```java
// Receive message with 30-second visibility
Duration initialVisibility = Duration.ofSeconds(30);
List<LargeQueueMessage> messages = client.receiveMessages(1, initialVisibility);

for (LargeQueueMessage message : messages) {
    // Start long-running operation
    processLongRunningTask(message.getBody());
    
    // Note: Storage Queue doesn't support lock renewal like Service Bus
    // You must complete processing within the visibility timeout
    // or the message will become visible again
    
    // Delete after successful processing
    client.deleteMessage(message);
}
```

### Compression

Enable GZIP compression to reduce blob storage usage and network bandwidth:

```yaml
azure:
  storagequeue:
    large-message-client:
      compression-enabled: true
```

Compression is **transparent** — consumers do not need to know that compression is enabled. The library automatically compresses payloads before upload and decompresses them on download.

```java
// No code changes needed — compression is handled internally
client.sendMessage(largeJsonPayload);  // compressed automatically

List<LargeQueueMessage> msgs = client.receiveMessages(10);
String body = msgs.get(0).getBody();   // decompressed automatically
```

> **Tip:** Compression is most effective for repetitive or text-based payloads (e.g., JSON, XML). Binary payloads that are already compressed (images, archives) will not benefit.

### Message Deduplication

Enable in-memory deduplication to silently drop duplicate `sendMessage()` calls:

```yaml
azure:
  storagequeue:
    large-message-client:
      deduplication-enabled: true
      deduplication-cache-size: 10000   # max entries in LRU cache
```

```java
client.sendMessage("Hello");   // sent
client.sendMessage("Hello");   // silently skipped (returns null)
client.sendMessage("World");   // sent
```

> **Important limitations:**
> - Deduplication state is **local to the JVM** — not shared across application instances.
> - The cache uses LRU eviction. Once the cache is full the oldest entry is evicted, so a previously seen message may be accepted again.
> - For **distributed deduplication**, use an external store (e.g., Redis) and check before calling `sendMessage()`.

### Dead-Letter Queue

Enable automatic dead-lettering for poison messages:

```yaml
azure:
  storagequeue:
    large-message-client:
      dead-letter-enabled: true
      dead-letter-queue-name: "my-queue-dlq"   # optional — defaults to <queue-name>-dlq
      dead-letter-max-dequeue-count: 5
```

When a message's `dequeueCount` reaches the configured threshold, it is automatically moved to the dead-letter queue and deleted from the main queue during `receiveMessages()`.

```java
// Messages that fail processing repeatedly are automatically dead-lettered
List<LargeQueueMessage> messages = client.receiveMessages(10);
// Only non-poison messages are returned

// You can also access the DLQ handler directly
DeadLetterQueueHandler dlq = client.getDeadLetterQueueHandler();
if (dlq != null) {
    int dlqCount = dlq.getApproximateMessageCount();
    System.out.println("Dead-letter queue depth: " + dlqCount);
}
```

### Orphan Blob Rollback

If the queue send fails after a blob upload, the orphaned blob is **automatically deleted** (rolled back). No configuration is needed — this is the default behaviour.

If the rollback itself fails (e.g., network issue), the blob remains as an orphan. Use `blob-ttl-days` and/or Azure Blob lifecycle management policies as a safety net.

## Best Practices

### 1. Set Appropriate Message Size Threshold

```yaml
# Don't set too low - small messages don't need offloading
azure:
  storagequeue:
    large-message-client:
      message-size-threshold: 65536  # 64 KB
```

### 2. Enable Blob Cleanup

```yaml
# Always clean up blobs to avoid storage costs
azure:
  storagequeue:
    large-message-client:
      cleanup-blob-on-delete: true
```

### 3. Configure Blob TTL

```yaml
# Set TTL to automatically expire old blobs
azure:
  storagequeue:
    large-message-client:
      blob-ttl-days: 7
```

### 4. Use Appropriate Access Tiers

```yaml
# Use Cool tier for infrequent access
azure:
  storagequeue:
    large-message-client:
      blob-access-tier: Cool
```

### 5. Handle Errors Gracefully

```java
try {
    List<LargeQueueMessage> messages = client.receiveMessages(10);
    
    for (LargeQueueMessage message : messages) {
        try {
            processMessage(message.getBody());
            client.deleteMessage(message);
        } catch (Exception e) {
            logger.error("Failed to process message: " + message.getMessageId(), e);
            // Message will become visible again after visibility timeout
            // Implement poison message handling if dequeue count is high
            if (message.getDequeueCount() > 5) {
                handlePoisonMessage(message);
            }
        }
    }
} catch (Exception e) {
    logger.error("Failed to receive messages", e);
}
```

### 6. Implement Poison Message Handling

```java
private void handlePoisonMessage(LargeQueueMessage message) {
    // Move to poison message queue or log for investigation
    try {
        // Send to dead-letter queue (you must create this)
        QueueClient poisonQueue = getPoisonQueueClient();
        poisonQueue.sendMessage(message.getBody());
        
        // Delete from main queue
        client.deleteMessage(message);
        
        logger.warn("Moved poison message to DLQ: " + message.getMessageId());
    } catch (Exception e) {
        logger.error("Failed to handle poison message", e);
    }
}
```

### 7. Monitor and Log

```java
@Component
public class MessageProcessor {
    private static final Logger logger = LoggerFactory.getLogger(MessageProcessor.class);
    
    @Autowired
    private AzureStorageQueueLargeMessageClient client;
    
    @Scheduled(fixedDelay = 5000)
    public void processMessages() {
        try {
            List<LargeQueueMessage> messages = client.receiveMessages(10);
            
            logger.info("Received {} messages", messages.size());
            
            for (LargeQueueMessage message : messages) {
                long startTime = System.currentTimeMillis();
                
                try {
                    processMessage(message.getBody());
                    client.deleteMessage(message);
                    
                    long duration = System.currentTimeMillis() - startTime;
                    logger.info("Processed message {} in {} ms", 
                               message.getMessageId(), duration);
                    
                    // Track metrics
                    if (message.isPayloadFromBlob()) {
                        metricsService.recordBlobResolvedMessage(duration);
                    }
                } catch (Exception e) {
                    logger.error("Failed to process message: " + message.getMessageId(), e);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to receive messages", e);
        }
    }
}
```

## Troubleshooting

### Issue: Messages Not Being Offloaded

**Symptom**: Large messages fail to send or aren't being stored in blob storage.

**Solution**:
1. Check message size threshold configuration
2. Verify blob storage connection string is correct
3. Ensure blob container exists or can be created
4. Check custom `MessageSizeCriteria` if configured

### Issue: Blob Payloads Not Found

**Symptom**: Error when receiving messages: "Failed to retrieve payload from blob"

**Solution**:
1. Check blob storage connection string
2. Verify blob container name matches
3. Enable `ignore-payload-not-found` if acceptable:
   ```yaml
   large-message-client:
     ignore-payload-not-found: true
   ```

### Issue: High Storage Costs

**Symptom**: Azure Storage costs increasing unexpectedly

**Solution**:
1. Enable blob cleanup:
   ```yaml
   cleanup-blob-on-delete: true
   ```
2. Set blob TTL:
   ```yaml
   blob-ttl-days: 7
   ```
3. Use Cool or Archive access tier:
   ```yaml
   blob-access-tier: Cool
   ```

### Issue: Receive-Only Mode Not Working

**Symptom**: Cannot retrieve blob payloads in receive-only mode

**Solution**:
1. Ensure senders have SAS enabled:
   ```yaml
   sas-enabled: true
   ```
2. Check SAS token validity period
3. Verify network connectivity to blob storage

### Issue: Messages Reappearing in Queue

**Symptom**: Messages are processed but reappear in the queue

**Solution**:
1. Ensure you're calling `deleteMessage()` after processing
2. Check visibility timeout is sufficient for processing
3. Verify message is not failing during processing

## Performance Considerations

### Latency

- **Blob Download Adds Latency**: Expect 100-500ms additional latency for blob resolution
- **Use SAS URIs**: Can reduce latency by avoiding credential lookup
- **Batch Operations**: Process multiple messages to amortize overhead

### Throughput

- **Parallel Processing**: Use multiple threads/consumers for higher throughput
- **Appropriate Batch Size**: Receive 10-32 messages at once
- **Optimize Message Size**: Don't offload unless necessary

### Cost Optimization

- **Right-Size Threshold**: Set threshold close to 64 KB to minimize blob operations
- **Use Access Tiers**: Cool tier for infrequent access
- **Enable TTL**: Automatically clean up old blobs
- **Monitor Usage**: Track blob storage usage and costs

## Migration from Service Bus Version

If you're migrating from the Service Bus version of this library:

### Key Differences

1. **No Sessions**: Remove session-related code
2. **No Scheduled Messages**: Use visibility timeout instead
3. **Dead-Letter Queue**: Now built-in — enable with `dead-letter-enabled: true`
4. **Message Properties**: Use metadata envelope instead of native properties
5. **Lock Renewal**: Not supported - use appropriate visibility timeouts

### Code Changes

**Before (Service Bus)**:
```java
client.sendMessage(body, properties, sessionId);
```

**After (Storage Queue)**:
```java
client.sendMessage(body, metadata);  // No sessionId
```

**Before (Service Bus)**:
```java
client.renewMessageLock(message);
```

**After (Storage Queue)**:
```java
// Not supported - ensure visibility timeout is sufficient
```

## Additional Resources

- [Azure Storage Queue Documentation](https://docs.microsoft.com/azure/storage/queues/)
- [Azure Blob Storage Documentation](https://docs.microsoft.com/azure/storage/blobs/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)

## Support

For issues, questions, or contributions, please open an issue on GitHub.

---
**Disclaimer:** The maintainer(s) of this project accept no liability for any issues arising from the use of this software. This guide is provided for informational purposes only. Always validate configurations and code changes in your own environment.
