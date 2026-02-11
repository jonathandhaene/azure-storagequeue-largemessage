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
 * Default implementation of MessageBodyReplacer that replaces the message body 
 * with a JSON representation of the BlobPointer.
 * This is the current behavior of the library.
 */
public class DefaultMessageBodyReplacer implements MessageBodyReplacer {
    
    /**
     * Replaces the original message body with the BlobPointer JSON.
     *
     * @param originalBody the original message body (not used in default implementation)
     * @param pointer      the blob pointer referencing the stored payload
     * @return the JSON representation of the blob pointer
     */
    @Override
    public String replace(String originalBody, BlobPointer pointer) {
        return pointer.toJson();
    }
}
