package com.azure.storagequeue.largemessage.config;

import java.util.Map;

/**
 * Functional interface for customizing message size criteria.
 * Provides maximum flexibility in determining when to offload messages to blob storage.
 */
@FunctionalInterface
public interface MessageSizeCriteria {
    /**
     * Determines whether a message should be offloaded to blob storage.
     *
     * @param messageBody the message body
     * @param metadata    the message metadata
     * @return true if the message should be offloaded, false otherwise
     */
    boolean shouldOffload(String messageBody, Map<String, String> metadata);
}
