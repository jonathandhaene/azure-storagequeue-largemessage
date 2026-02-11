/*
 * DISCLAIMER: This software is provided "as is", without warranty of any kind,
 * express or implied. The author(s) and maintainer(s) are not responsible for
 * any damage, data loss, or other issues arising from the use of this software.
 * Use at your own risk.
 *
 * See the LICENSE file for full terms.
 */

package com.azure.storagequeue.largemessage.config;

import com.azure.storagequeue.largemessage.client.AzureStorageQueueLargeMessageClient;
import com.azure.storagequeue.largemessage.store.BlobNameResolver;
import com.azure.storagequeue.largemessage.store.BlobPayloadStore;
import com.azure.storagequeue.largemessage.store.DefaultBlobNameResolver;
import com.azure.storagequeue.largemessage.store.DefaultMessageBodyReplacer;
import com.azure.storagequeue.largemessage.store.MessageBodyReplacer;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.queue.QueueClient;
import com.azure.storage.queue.QueueClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot auto-configuration for Azure Storage Queue Large Message Client.
 * Automatically creates and configures beans when appropriate properties are set.
 */
@Configuration
@EnableConfigurationProperties(LargeMessageClientConfiguration.class)
public class AzureLargeMessageClientAutoConfiguration {

    @Value("${azure.storage.connection-string:}")
    private String storageConnectionString;

    @Value("${azure.storage.container-name:large-messages}")
    private String containerName;

    @Value("${azure.storagequeue.connection-string:${azure.storage.connection-string:}}")
    private String queueConnectionString;

    @Value("${azure.storagequeue.queue-name:my-queue}")
    private String queueName;

    /**
     * Creates a BlobServiceClient bean.
     * Only created when not in receive-only mode.
     * Uses custom StorageConnectionStringProvider if available, otherwise falls back to property.
     *
     * @return the BlobServiceClient instance
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "azure.storagequeue.large-message-client.receive-only-mode", havingValue = "false", matchIfMissing = true)
    public BlobServiceClient blobServiceClient(
            @Autowired(required = false) StorageConnectionStringProvider connectionStringProvider) {
        
        // Get connection string from provider if available, otherwise use property
        String connectionString;
        if (connectionStringProvider != null) {
            connectionString = connectionStringProvider.getConnectionString();
        } else {
            connectionString = storageConnectionString;
        }
        
        if (connectionString == null || connectionString.isEmpty()) {
            throw new IllegalStateException(
                "Azure Storage connection string is required. " +
                "Please set azure.storage.connection-string property, " +
                "AZURE_STORAGE_CONNECTION_STRING environment variable, " +
                "or provide a StorageConnectionStringProvider bean."
            );
        }
        return new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();
    }

    /**
     * Creates a QueueClient bean for the configured queue.
     *
     * @return the QueueClient instance
     */
    @Bean
    @ConditionalOnMissingBean
    public QueueClient queueClient() {
        String connectionString = queueConnectionString;
        
        if (connectionString == null || connectionString.isEmpty()) {
            throw new IllegalStateException(
                "Azure Storage Queue connection string is required. " +
                "Please set azure.storagequeue.connection-string or azure.storage.connection-string property."
            );
        }
        
        return new QueueClientBuilder()
                .connectionString(connectionString)
                .queueName(queueName)
                .buildClient();
    }

    /**
     * Creates a BlobPayloadStore bean.
     * Only created when not in receive-only mode.
     *
     * @param blobServiceClient the blob service client
     * @return the BlobPayloadStore instance
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "azure.storagequeue.large-message-client.receive-only-mode", havingValue = "false", matchIfMissing = true)
    public BlobPayloadStore blobPayloadStore(BlobServiceClient blobServiceClient, LargeMessageClientConfiguration config) {
        return new BlobPayloadStore(blobServiceClient, containerName, config);
    }

    /**
     * Creates a default BlobNameResolver bean if none is provided.
     *
     * @param config the large message client configuration
     * @return the BlobNameResolver instance
     */
    @Bean
    @ConditionalOnMissingBean
    public BlobNameResolver blobNameResolver(LargeMessageClientConfiguration config) {
        return new DefaultBlobNameResolver(config.getBlobKeyPrefix());
    }

    /**
     * Creates a default MessageBodyReplacer bean if none is provided.
     *
     * @return the MessageBodyReplacer instance
     */
    @Bean
    @ConditionalOnMissingBean
    public MessageBodyReplacer messageBodyReplacer() {
        return new DefaultMessageBodyReplacer();
    }

    /**
     * Creates a default MessageSizeCriteria bean if none is provided.
     *
     * @param config the large message client configuration
     * @return the MessageSizeCriteria instance
     */
    @Bean
    @ConditionalOnMissingBean
    public MessageSizeCriteria messageSizeCriteria(LargeMessageClientConfiguration config) {
        return new DefaultMessageSizeCriteria(
            config.getMessageSizeThreshold(),
            config.isAlwaysThroughBlob()
        );
    }

    /**
     * Creates an AzureStorageQueueLargeMessageClient bean.
     * Works in both regular mode (with BlobPayloadStore) and receive-only mode (without).
     *
     * @param config              the large message client configuration
     * @param queueClient         the queue client
     * @param payloadStoreProvider optional payload store provider
     * @param blobNameResolver    the blob name resolver
     * @param bodyReplacer        the message body replacer
     * @param messageSizeCriteria the message size criteria
     * @return the AzureStorageQueueLargeMessageClient instance
     */
    @Bean
    @ConditionalOnMissingBean
    public AzureStorageQueueLargeMessageClient azureStorageQueueLargeMessageClient(
            LargeMessageClientConfiguration config,
            QueueClient queueClient,
            org.springframework.beans.factory.ObjectProvider<BlobPayloadStore> payloadStoreProvider,
            BlobNameResolver blobNameResolver,
            MessageBodyReplacer bodyReplacer,
            MessageSizeCriteria messageSizeCriteria) {
        
        // Set custom resolvers in config
        config.setBlobNameResolver(blobNameResolver);
        config.setBodyReplacer(bodyReplacer);
        config.setMessageSizeCriteria(messageSizeCriteria);
        
        // Get BlobPayloadStore if available (not in receive-only mode)
        BlobPayloadStore payloadStore = payloadStoreProvider.getIfAvailable();
        
        if (config.isReceiveOnlyMode() && payloadStore != null) {
            throw new IllegalStateException(
                "BlobPayloadStore should not be configured in receive-only mode"
            );
        }
        
        if (!config.isReceiveOnlyMode() && payloadStore == null) {
            throw new IllegalStateException(
                "BlobPayloadStore is required when not in receive-only mode. " +
                "Ensure azure.storage.connection-string is properly configured."
            );
        }
        
        return new AzureStorageQueueLargeMessageClient(
            queueClient,
            payloadStore,
            config
        );
    }
}
