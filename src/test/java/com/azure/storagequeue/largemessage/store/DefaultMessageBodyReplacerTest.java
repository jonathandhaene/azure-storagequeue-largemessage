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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link DefaultMessageBodyReplacer}.
 *
 * <p><b>Triggered by:</b> unit-tests.yml workflow ({@code mvn test} via surefire)</p>
 *
 * <p><b>What this tests:</b> The component that replaces a message body with a
 * serialized {@link BlobPointer} JSON string. This is the "marker" placed in the
 * queue when the real payload is in Blob Storage.</p>
 *
 * <p><b>Coverage summary:</b></p>
 * <ul>
 *   <li>Replace produces valid JSON containing container + blob</li>
 *   <li>Round-trip: replaced string can be deserialized back to BlobPointer</li>
 *   <li>Original message body is ignored (only the pointer matters)</li>
 * </ul>
 *
 * <p><b>Not yet covered:</b></p>
 * <ul>
 *   <li>Null BlobPointer argument (will NPE on {@code pointer.toJson()})</li>
 *   <li>BlobPointer with null/empty fields (Jackson serializes to JSON {@code null})</li>
 * </ul>
 */
@DisplayName("DefaultMessageBodyReplacer – pointer replacement")
class DefaultMessageBodyReplacerTest {

    @Test
    @DisplayName("Replace returns JSON containing container and blob name")
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
    @DisplayName("Replaced body can be deserialized back to BlobPointer (round-trip)")
    void testReplaceProducesValidJson() {
        DefaultMessageBodyReplacer replacer = new DefaultMessageBodyReplacer();
        BlobPointer pointer = new BlobPointer("my-container", "my-blob");

        String replaced = replacer.replace("Some large message", pointer);

        // Should be able to deserialize back to BlobPointer
        BlobPointer deserialized = BlobPointer.fromJson(replaced);
        assertEquals(pointer, deserialized);
    }

    @Test
    @DisplayName("Original body content is irrelevant – same pointer = same output")
    void testReplaceIgnoresOriginalBody() {
        DefaultMessageBodyReplacer replacer = new DefaultMessageBodyReplacer();
        BlobPointer pointer = new BlobPointer("container", "blob");

        String replaced1 = replacer.replace("Message 1", pointer);
        String replaced2 = replacer.replace("Message 2", pointer);

        // Should produce same result regardless of original body
        assertEquals(replaced1, replaced2);
    }
}
