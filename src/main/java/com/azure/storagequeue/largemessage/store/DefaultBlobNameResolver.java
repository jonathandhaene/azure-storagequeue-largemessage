/*
 * DISCLAIMER: This software is provided "as is", without warranty of any kind,
 * express or implied. The author(s) and maintainer(s) are not responsible for
 * any damage, data loss, or other issues arising from the use of this software.
 * Use at your own risk.
 *
 * See the LICENSE file for full terms.
 */

package com.azure.storagequeue.largemessage.store;

import java.util.UUID;

/**
 * Default implementation of BlobNameResolver that generates UUID-based blob names.
 * Uses the configured blob key prefix followed by a random UUID.
 */
public class DefaultBlobNameResolver implements BlobNameResolver {
    private final String blobKeyPrefix;

    /**
     * Creates a new DefaultBlobNameResolver with the specified prefix.
     *
     * @param blobKeyPrefix the prefix to prepend to generated blob names
     */
    public DefaultBlobNameResolver(String blobKeyPrefix) {
        this.blobKeyPrefix = blobKeyPrefix != null ? blobKeyPrefix : "";
    }

    /**
     * Resolves the blob name by generating a UUID-based name.
     *
     * @param messageId the message ID (not used in default implementation)
     * @return a blob name composed of the prefix and a random UUID
     */
    @Override
    public String resolve(String messageId) {
        return blobKeyPrefix + UUID.randomUUID().toString();
    }
}
