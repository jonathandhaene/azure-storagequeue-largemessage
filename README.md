# Azure Storage Queue Large Message Client for Java Spring Boot

A powerful Java Spring Boot library for handling large messages with Azure Storage Queue. This library automatically offloads large message payloads to Azure Blob Storage when they exceed the configured threshold, providing a seamless experience for working with messages of any size.

## Disclaimer

> **⚠️ Disclaimer:** This software is provided "as is", without warranty of any kind, express or implied. The author(s) and maintainer(s) of this project are not responsible for any damage, data loss, service disruption, or other issues that may arise from the use of this software. Use at your own risk. Always test thoroughly in a non-production environment before deploying to production.

## Features

### Core Capabilities

* **Automatic Payload Offloading**: Transparently stores large messages in Azure Blob Storage and sends a reference pointer via Storage Queue
* **Seamless Message Resolution**: Automatically retrieves and resolves blob-stored payloads when receiving messages
* **Configurable Size Threshold**: Set custom thresholds for when messages should be offloaded (default: 64 KB - Storage Queue limit)
* **Batch Operations**: Efficiently send and receive multiple messages
* **Spring Boot Auto-Configuration**: Simple integration with Spring Boot applications

### Advanced Features

* **SAS URI Support**: Generate Shared Access Signature URIs for secure blob access without storage credentials
* **Receive-Only Mode**: Download blob payloads using only SAS URIs (no storage account credentials needed)
* **Custom Blob Naming**: Implement custom blob naming strategies (e.g., `{tenantId}/{messageId}`)
* **Custom Body Replacement**: Control what the message body becomes after blob offloading
* **Dynamic Connection Strings**: Integrate with Key Vault or other dynamic credential sources
* **Flexible Size Criteria**: Customize message offloading logic beyond simple size thresholds
* **Retry Logic**: Configurable exponential backoff with jitter for transient failures
* **GZIP Compression**: Optional transparent compression/decompression of blob payloads to save storage and bandwidth
* **Message Deduplication**: In-memory LRU hash-based deduplication to skip duplicate messages on send
* **Dead-Letter Queue**: Automatic poison message handling — messages exceeding a configurable dequeue count are moved to a DLQ
* **Orphan Blob Rollback**: Automatic cleanup of uploaded blobs when the subsequent queue send fails

## Installation

Add the dependency to your Maven `pom.xml`:

```xml
<dependency>
    <groupId>com.azure.storagequeue.largemessage</groupId>
    <artifactId>azure-storagequeue-large-message-client</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Quick Start

### Configuration

Configure your `application.yml`:

```yaml
azure:
  storage:
    connection-string: ${AZURE_STORAGE_CONNECTION_STRING}
    container-name: ${AZURE_STORAGE_CONTAINER_NAME:large-messages}
  storagequeue:
    connection-string: ${AZURE_STORAGE_CONNECTION_STRING}
    queue-name: ${AZURE_STORAGEQUEUE_QUEUE_NAME:my-queue}
    large-message-client:
      message-size-threshold: 65536    # 64 KB (Storage Queue limit)
      always-through-blob: false
      cleanup-blob-on-delete: true
      compression-enabled: false       # enable GZIP compression
      deduplication-enabled: false     # enable in-memory deduplication
      dead-letter-enabled: false       # enable dead-letter queue
      dead-letter-max-dequeue-count: 5
```

### Basic Usage

```java
@Autowired
private AzureStorageQueueLargeMessageClient client;

// Send a message - automatically offloaded if too large
client.sendMessage("Your message content here");

// Receive messages - automatically resolved from blob storage
List<LargeQueueMessage> messages = client.receiveMessages(10);
for (LargeQueueMessage message : messages) {
    String body = message.getBody();
    
    // Process the message
    System.out.println("Received: " + body);
    
    // Delete the message after processing
    client.deleteMessage(message);
}
```


## Claim-Check Pattern Compliance

This library implements the [Claim-Check pattern](https://learn.microsoft.com/en-us/azure/architecture/patterns/claim-check) as described by Microsoft's Azure Architecture Center. The table below summarizes what is covered and what to be aware of before adopting this library in production.

### What's covered

| Best Practice | How this library addresses it |
|---|---|
| **Split large message into claim-check + payload** | Messages exceeding the threshold are split: the payload is uploaded to Azure Blob Storage and a `BlobPointer` JSON reference (the "claim check") is placed on the queue. |
| **Use an external data store for the payload** | Azure Blob Storage is used via `BlobPayloadStore`. |
| **Automatic offload based on message size** | `DefaultMessageSizeCriteria` compares against a configurable threshold (default 64 KB — the Storage Queue limit). An `always-through-blob` option forces all messages through blob storage. |
| **Transparent to consumers** | `receiveMessages()` automatically detects blob pointers and resolves them back to the original payload. Consumers work with `LargeQueueMessage` and never need to interact with Blob Storage directly. |
| **Claim-check token contains enough info to retrieve the payload** | `BlobPointer` contains `containerName` and `blobName`, which is sufficient to locate and download the blob. |
| **Clean up payload after consumption** | `cleanup-blob-on-delete` is enabled by default. Calling `deleteMessage()` also deletes the associated blob. |
| **Secure access to the payload store** | Optional SAS URI generation with configurable expiry enables a receive-only mode where consumers don't need storage account credentials. |
| **Retry / resilience for the external store** | All blob operations are wrapped in `RetryHandler`, which uses exponential backoff with jitter. |
| **Payload lifecycle management** | Blob TTL metadata and `cleanupExpiredBlobs()` helper support time-based payload expiry. |
| **Extensibility** | Clean interfaces (`BlobNameResolver`, `MessageBodyReplacer`, `MessageSizeCriteria`) allow customisation of blob naming, message body replacement, and offload criteria. |

### Known gaps & considerations

The following items are called out in the Claim-Check pattern guidance but are **not yet addressed** by this library. Evaluate whether they matter for your use case before adopting.

| Area | Detail | Status / Mitigation |
|---|---|---|
| **Orphaned blobs on send failure** | The send flow uploads to blob first, then enqueues. If the queue send fails, an orphaned blob is left. | **Resolved.** The library now automatically rolls back (deletes) the orphaned blob when the queue send fails. As a safety net, use the `blob-ttl-days` setting so any remaining orphaned blobs expire, or configure [Azure Blob Storage lifecycle management policies](https://learn.microsoft.com/en-us/azure/storage/blobs/lifecycle-management-overview). |
| **Best-effort blob cleanup** | Blob deletion on message delete swallows exceptions. If it fails, the blob remains. `cleanupExpiredBlobs()` exists but must be invoked manually. | **Partially resolved.** Cleanup still swallows exceptions (by design — it must not block the happy path), but `blob-ttl-days` metadata is set on upload, and you can schedule periodic calls to `cleanupExpiredBlobs()` or configure an Azure lifecycle management policy. |
| **No compression** | Payloads are stored as plain UTF-8 strings with no compression. | **Resolved.** Set `compression-enabled: true` to GZIP-compress blob payloads automatically. Compression and decompression are transparent to consumers. |
| **No message deduplication** | Consumers may receive the same claim-check more than once. The library does not provide deduplication or idempotent processing. | **Resolved (local).** Set `deduplication-enabled: true` to enable in-memory SHA-256-based deduplication with an LRU cache. Note: this is local to the JVM — for distributed deduplication, use an external store (e.g., Redis). |
| **No dead-letter queue** | Azure Storage Queue does not have native dead-letter support. The library does not provide a built-in DLQ mechanism. | **Resolved.** Set `dead-letter-enabled: true` to automatically move poison messages (exceeded `dead-letter-max-dequeue-count`) to a separate dead-letter queue. The DLQ name defaults to `<queue-name>-dlq`. |
| **No client-side encryption** | The library relies on Azure's default server-side encryption at rest. There is no option for client-side encryption of payloads before uploading. | Encrypt the message body before passing it to `sendMessage()` and decrypt after `receiveMessages()`. |
| **No Event Grid integration** | The pattern documentation highlights Azure Event Grid for automatic, event-driven claim-check flows. This library uses a pull-based approach only. | Combine this library with an Event Grid subscription on the blob container if you need push-based notification. |

## Documentation

For detailed usage examples, configuration options, and advanced features, see the [MIGRATION_GUIDE.md](MIGRATION_GUIDE.md).

## Key Differences from Service Bus Version

While this library provides similar functionality to the [Azure Service Bus version](https://github.com/jonathandhaene/SQS-Java-Spring), there are some key differences due to the nature of Azure Storage Queue:

| Feature | Service Bus | Storage Queue |
|---------|-------------|---------------|
| **Message Size Limit** | 256 KB (1 MB with Premium) | 64 KB |
| **Sessions** | ✅ Supported | ❌ Not supported |
| **Scheduled Messages** | ✅ Supported | ❌ Not supported |
| **Dead-Letter Queue** | ✅ Native support | ✅ Built-in (via `dead-letter-enabled`) |

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Related Projects

- [Azure Service Bus Large Message Client](https://github.com/jonathandhaene/SQS-Java-Spring) - Similar functionality for Azure Service Bus

---
**Disclaimer:** The maintainer(s) of this project accept no liability for any issues arising from the use of this software. See the LICENSE file for full terms.