package com.azure.storagequeue.largemessage.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BlobPointer.
 */
class BlobPointerTest {

    @Test
    void testCreateBlobPointer() {
        BlobPointer pointer = new BlobPointer("container", "blob-name");
        
        assertEquals("container", pointer.getContainerName());
        assertEquals("blob-name", pointer.getBlobName());
    }

    @Test
    void testToJson() {
        BlobPointer pointer = new BlobPointer("test-container", "test-blob");
        
        String json = pointer.toJson();
        
        assertNotNull(json);
        assertTrue(json.contains("test-container"));
        assertTrue(json.contains("test-blob"));
    }

    @Test
    void testFromJson() {
        String json = "{\"containerName\":\"my-container\",\"blobName\":\"my-blob\"}";
        
        BlobPointer pointer = BlobPointer.fromJson(json);
        
        assertNotNull(pointer);
        assertEquals("my-container", pointer.getContainerName());
        assertEquals("my-blob", pointer.getBlobName());
    }

    @Test
    void testRoundTripSerialization() {
        BlobPointer original = new BlobPointer("test-container", "test-blob");
        
        String json = original.toJson();
        BlobPointer deserialized = BlobPointer.fromJson(json);
        
        assertEquals(original, deserialized);
    }

    @Test
    void testEquals() {
        BlobPointer pointer1 = new BlobPointer("container", "blob");
        BlobPointer pointer2 = new BlobPointer("container", "blob");
        BlobPointer pointer3 = new BlobPointer("other-container", "blob");
        
        assertEquals(pointer1, pointer2);
        assertNotEquals(pointer1, pointer3);
    }

    @Test
    void testHashCode() {
        BlobPointer pointer1 = new BlobPointer("container", "blob");
        BlobPointer pointer2 = new BlobPointer("container", "blob");
        
        assertEquals(pointer1.hashCode(), pointer2.hashCode());
    }

    @Test
    void testToString() {
        BlobPointer pointer = new BlobPointer("container", "blob");
        
        String str = pointer.toString();
        
        assertNotNull(str);
        assertTrue(str.contains("container"));
        assertTrue(str.contains("blob"));
    }
}
