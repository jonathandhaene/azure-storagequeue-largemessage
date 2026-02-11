/*
 * DISCLAIMER: This software is provided "as is", without warranty of any kind,
 * express or implied. The author(s) and maintainer(s) are not responsible for
 * any damage, data loss, or other issues arising from the use of this software.
 * Use at your own risk.
 *
 * See the LICENSE file for full terms.
 */

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
