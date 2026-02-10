# Azure Storage Queue Large Message Client for Java Spring Boot

A powerful Java Spring Boot library for handling large messages with Azure Storage Queue. This library automatically offloads large message payloads to Azure Blob Storage when they exceed the configured threshold, providing a seamless experience for working with messages of any size.

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

## Documentation

For detailed usage examples, configuration options, and advanced features, see the [MIGRATION_GUIDE.md](MIGRATION_GUIDE.md).

## Key Differences from Service Bus Version

While this library provides similar functionality to the [Azure Service Bus version](https://github.com/jonathandhaene/SQS-Java-Spring), there are some key differences due to the nature of Azure Storage Queue:

| Feature | Service Bus | Storage Queue |
|---------|-------------|---------------|
| **Message Size Limit** | 256 KB (1 MB with Premium) | 64 KB |
| **Sessions** | ✅ Supported | ❌ Not supported |
| **Scheduled Messages** | ✅ Supported | ❌ Not supported |
| **Dead-Letter Queue** | ✅ Native support | ⚠️ Manual implementation |

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Related Projects

- [Azure Service Bus Large Message Client](https://github.com/jonathandhaene/SQS-Java-Spring) - Similar functionality for Azure Service Bus