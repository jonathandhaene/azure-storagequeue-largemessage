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
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DefaultMessageBodyReplacer.
 */
class DefaultMessageBodyReplacerTest {

    @Test
    void testReplace() {
        DefaultMessageBodyReplacer replacer = new DefaultMessageBodyReplacer();
        BlobPointer pointer = new BlobPointer("test-container", "test-blob");
        
        String replaced = replacer.replace("Original message body", pointer);
        
        assertNotNull(replaced);
        // Should be JSON representation of the pointer
        assertTrue(replaced.contains("test-container"));
        assertTrue(replaced.contains("test-blob"));
    }

    @Test
    void testReplaceProducesValidJson() {
        DefaultMessageBodyReplacer replacer = new DefaultMessageBodyReplacer();
        BlobPointer pointer = new BlobPointer("my-container", "my-blob");
        
        String replaced = replacer.replace("Some large message", pointer);
        
        // Should be able to deserialize back to BlobPointer
        BlobPointer deserialized = BlobPointer.fromJson(replaced);
        assertEquals(pointer, deserialized);
    }

    @Test
    void testReplaceIgnoresOriginalBody() {
        DefaultMessageBodyReplacer replacer = new DefaultMessageBodyReplacer();
        BlobPointer pointer = new BlobPointer("container", "blob");
        
        String replaced1 = replacer.replace("Message 1", pointer);
        String replaced2 = replacer.replace("Message 2", pointer);
        
        // Should produce same result regardless of original body
        assertEquals(replaced1, replaced2);
    }
}
