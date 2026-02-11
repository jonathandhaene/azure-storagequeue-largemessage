package com.azure.storagequeue.largemessage.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PlainTextConnectionStringProvider.
 */
class PlainTextConnectionStringProviderTest {

    @Test
    void testGetConnectionString() {
        String connectionString = "DefaultEndpointsProtocol=https;AccountName=test;AccountKey=key";
        PlainTextConnectionStringProvider provider = new PlainTextConnectionStringProvider(connectionString);
        
        assertEquals(connectionString, provider.getConnectionString());
    }

    @Test
    void testGetConnectionStringWithNullValue() {
        PlainTextConnectionStringProvider provider = new PlainTextConnectionStringProvider(null);
        
        assertNull(provider.getConnectionString());
    }

    @Test
    void testGetConnectionStringMultipleTimes() {
        String connectionString = "test-connection-string";
        PlainTextConnectionStringProvider provider = new PlainTextConnectionStringProvider(connectionString);
        
        // Should return same value each time
        assertEquals(connectionString, provider.getConnectionString());
        assertEquals(connectionString, provider.getConnectionString());
    }
}
