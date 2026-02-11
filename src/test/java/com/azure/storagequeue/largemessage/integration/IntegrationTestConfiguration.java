/*
 * DISCLAIMER: This software is provided "as is", without warranty of any kind,
 * express or implied. The author(s) and maintainer(s) are not responsible for
 * any damage, data loss, or other issues arising from the use of this software.
 * Use at your own risk.
 *
 * See the LICENSE file for full terms.
 */

package com.azure.storagequeue.largemessage.integration;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.queue.QueueClient;
import com.azure.storage.queue.QueueClientBuilder;
import com.azure.storagequeue.largemessage.client.AzureStorageQueueLargeMessageClient;
import com.azure.storagequeue.largemessage.config.DefaultMessageSizeCriteria;
import com.azure.storagequeue.largemessage.config.LargeMessageClientConfiguration;
import com.azure.storagequeue.largemessage.config.MessageSizeCriteria;
import com.azure.storagequeue.largemessage.store.BlobNameResolver;
import com.azure.storagequeue.largemessage.store.BlobPayloadStore;
import com.azure.storagequeue.largemessage.store.DefaultBlobNameResolver;
import com.azure.storagequeue.largemessage.store.DefaultMessageBodyReplacer;
import com.azure.storagequeue.largemessage.store.MessageBodyReplacer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.UUID;

/**
 * Test configuration for integration tests.
 * Creates beans with dynamic queue and container names to avoid conflicts.
 */
@TestConfiguration
public class IntegrationTestConfiguration {

    @Value("${azure.storage.connection-string}")
    private String storageConnectionString;

    @Value("${azure.storage.container-name}")
    private String baseContainerName;

    @Value("${azure.storagequeue.connection-string:${azure.storage.connection-string}}")
    private String queueConnectionString;

    @Value("${azure.storagequeue.queue-name}")
    private String baseQueueName;

    /**
     * Creates a unique container name for this test run.
     */
    public String getUniqueContainerName() {
        return baseContainerName + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Creates a unique queue name for this test run.
     */
    public String getUniqueQueueName() {
        return baseQueueName + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Bean
    @Primary
    public BlobServiceClient testBlobServiceClient() {
        return new BlobServiceClientBuilder()
                .connectionString(storageConnectionString)
                .buildClient();
    }

    @Bean
    @Primary
    public QueueClient testQueueClient() {
        String queueName = getUniqueQueueName();
        QueueClient client = new QueueClientBuilder()
                .connectionString(queueConnectionString)
                .queueName(queueName)
                .buildClient();
        
        // Create the queue
        try {
            client.create();
        } catch (Exception e) {
            // Queue might already exist, ignore
        }
        
        return client;
    }

    @Bean
    @Primary
    public LargeMessageClientConfiguration testLargeMessageClientConfiguration() {
        LargeMessageClientConfiguration config = new LargeMessageClientConfiguration();
        config.setMessageSizeThreshold(65536); // 64 KB
        config.setAlwaysThroughBlob(false);
        config.setCleanupBlobOnDelete(true);
        config.setBlobKeyPrefix("test-messages/");
        config.setRetryMaxAttempts(3);
        config.setRetryBackoffMillis(500L);
        config.setRetryBackoffMultiplier(2.0);
        config.setRetryMaxBackoffMillis(5000L);
        config.setIgnorePayloadNotFound(false);
        config.setReceiveOnlyMode(false);
        config.setTracingEnabled(false);
        
        // Set resolvers and criteria
        config.setBlobNameResolver(testBlobNameResolver(config));
        config.setBodyReplacer(testMessageBodyReplacer());
        config.setMessageSizeCriteria(testMessageSizeCriteria(config));
        
        return config;
    }

    @Bean
    @Primary
    public BlobNameResolver testBlobNameResolver(LargeMessageClientConfiguration config) {
        return new DefaultBlobNameResolver(config.getBlobKeyPrefix());
    }

    @Bean
    @Primary
    public MessageBodyReplacer testMessageBodyReplacer() {
        return new DefaultMessageBodyReplacer();
    }

    @Bean
    @Primary
    public MessageSizeCriteria testMessageSizeCriteria(LargeMessageClientConfiguration config) {
        return new DefaultMessageSizeCriteria(
            config.getMessageSizeThreshold(),
            config.isAlwaysThroughBlob()
        );
    }

    @Bean
    @Primary
    public BlobPayloadStore testBlobPayloadStore(
            BlobServiceClient blobServiceClient,
            LargeMessageClientConfiguration config) {
        String containerName = getUniqueContainerName();
        return new BlobPayloadStore(blobServiceClient, containerName, config);
    }

    @Bean
    @Primary
    public AzureStorageQueueLargeMessageClient testAzureStorageQueueLargeMessageClient(
            QueueClient queueClient,
            BlobPayloadStore payloadStore,
            LargeMessageClientConfiguration config) {
        return new AzureStorageQueueLargeMessageClient(queueClient, payloadStore, config);
    }
}
