package com.azure.storagequeue.largemessage.config;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DefaultMessageSizeCriteria.
 */
class DefaultMessageSizeCriteriaTest {

    @Test
    void testShouldOffloadWhenExceedsThreshold() {
        DefaultMessageSizeCriteria criteria = new DefaultMessageSizeCriteria(100, false);
        
        String largeMessage = "a".repeat(101);
        Map<String, String> metadata = new HashMap<>();
        
        assertTrue(criteria.shouldOffload(largeMessage, metadata));
    }

    @Test
    void testShouldNotOffloadWhenBelowThreshold() {
        DefaultMessageSizeCriteria criteria = new DefaultMessageSizeCriteria(100, false);
        
        String smallMessage = "a".repeat(50);
        Map<String, String> metadata = new HashMap<>();
        
        assertFalse(criteria.shouldOffload(smallMessage, metadata));
    }

    @Test
    void testShouldNotOffloadWhenExactlyAtThreshold() {
        DefaultMessageSizeCriteria criteria = new DefaultMessageSizeCriteria(100, false);
        
        String message = "a".repeat(100);
        Map<String, String> metadata = new HashMap<>();
        
        assertFalse(criteria.shouldOffload(message, metadata));
    }

    @Test
    void testShouldOffloadWhenAlwaysThroughBlobEnabled() {
        DefaultMessageSizeCriteria criteria = new DefaultMessageSizeCriteria(100, true);
        
        String smallMessage = "small";
        Map<String, String> metadata = new HashMap<>();
        
        assertTrue(criteria.shouldOffload(smallMessage, metadata));
    }

    @Test
    void testShouldOffloadWithEmptyMessage() {
        DefaultMessageSizeCriteria criteria = new DefaultMessageSizeCriteria(10, true);
        
        String emptyMessage = "";
        Map<String, String> metadata = new HashMap<>();
        
        assertTrue(criteria.shouldOffload(emptyMessage, metadata));
    }

    @Test
    void testMetadataNotUsedInDefaultImplementation() {
        DefaultMessageSizeCriteria criteria = new DefaultMessageSizeCriteria(100, false);
        
        String message = "a".repeat(101);
        Map<String, String> metadata1 = new HashMap<>();
        metadata1.put("key", "value");
        
        Map<String, String> metadata2 = new HashMap<>();
        
        // Should produce same result regardless of metadata
        assertEquals(
            criteria.shouldOffload(message, metadata1),
            criteria.shouldOffload(message, metadata2)
        );
    }
}
