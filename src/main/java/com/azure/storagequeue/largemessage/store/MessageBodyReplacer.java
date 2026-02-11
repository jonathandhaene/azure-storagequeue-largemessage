/*
 * DISCLAIMER: This software is provided "as is", without warranty of any kind,
 * express or implied. The author(s) and maintainer(s) are not responsible for
 * any damage, data loss, or other issues arising from the use of this software.
 * Use at your own risk.
 *
 * See the LICENSE file for full terms.
 */

package com.azure.storagequeue.largemessage.store;

import com.azure.storagequeue.largemessage.model.BlobPointer;

/**
 * Functional interface for customizing message body replacement after blob offloading.
 * Allows users to control what the message body becomes after the payload is stored in blob storage.
 */
@FunctionalInterface
public interface MessageBodyReplacer {
    /**
     * Replaces the original message body with a custom value after blob offloading.
     *
     * @param originalBody the original message body before offloading
     * @param pointer      the blob pointer referencing the stored payload
     * @return the replacement message body
     */
    String replace(String originalBody, BlobPointer pointer);
}
