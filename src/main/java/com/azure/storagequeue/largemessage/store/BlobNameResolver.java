package com.azure.storagequeue.largemessage.store;

/**
 * Functional interface for customizing blob naming strategies.
 * Allows users to define their own blob naming logic (e.g., {tenantId}/{messageId}).
 */
@FunctionalInterface
public interface BlobNameResolver {
    /**
     * Resolves the blob name for a given message.
     *
     * @param messageId the message ID
     * @return the blob name to use for storing the payload
     */
    String resolve(String messageId);
}
