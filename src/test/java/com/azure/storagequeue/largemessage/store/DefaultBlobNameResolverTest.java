package com.azure.storagequeue.largemessage.store;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DefaultBlobNameResolver.
 */
class DefaultBlobNameResolverTest {

    @Test
    void testResolveWithoutPrefix() {
        DefaultBlobNameResolver resolver = new DefaultBlobNameResolver("");
        
        String blobName = resolver.resolve("message-123");
        
        assertNotNull(blobName);
        assertFalse(blobName.isEmpty());
        // Should be a UUID
        assertTrue(blobName.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
    }

    @Test
    void testResolveWithPrefix() {
        DefaultBlobNameResolver resolver = new DefaultBlobNameResolver("messages/");
        
        String blobName = resolver.resolve("message-123");
        
        assertNotNull(blobName);
        assertTrue(blobName.startsWith("messages/"));
        // Should be prefix + UUID
        String uuidPart = blobName.substring("messages/".length());
        assertTrue(uuidPart.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
    }

    @Test
    void testResolveWithNullPrefix() {
        DefaultBlobNameResolver resolver = new DefaultBlobNameResolver(null);
        
        String blobName = resolver.resolve("message-123");
        
        assertNotNull(blobName);
        assertFalse(blobName.isEmpty());
    }

    @Test
    void testResolveGeneratesUniqueName() {
        DefaultBlobNameResolver resolver = new DefaultBlobNameResolver("");
        
        String blobName1 = resolver.resolve("message-123");
        String blobName2 = resolver.resolve("message-123");
        
        assertNotEquals(blobName1, blobName2);
    }
}
