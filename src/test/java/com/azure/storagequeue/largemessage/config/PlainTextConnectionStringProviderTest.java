/*
 * DISCLAIMER: This software is provided "as is", without warranty of any kind,
 * express or implied. The author(s) and maintainer(s) are not responsible for
 * any damage, data loss, or other issues arising from the use of this software.
 * Use at your own risk.
 *
 * See the LICENSE file for full terms.
 */

package com.azure.storagequeue.largemessage.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PlainTextConnectionStringProvider}.
 *
 * <p><b>Triggered by:</b> unit-tests.yml workflow ({@code mvn test} via surefire)</p>
 *
 * <p><b>What this tests:</b> The simplest {@link StorageConnectionStringProvider}
 * implementation – stores and returns a plain-text connection string. This is used
 * when the connection string is passed directly (e.g. from Spring config), as opposed
 * to being fetched from Key Vault or Managed Identity.</p>
 *
 * <p><b>Coverage summary:</b></p>
 * <ul>
 *   <li>Normal value → returned as-is</li>
 *   <li>Null value → returns null (no validation)</li>
 *   <li>Idempotency → multiple calls return same value</li>
 * </ul>
 *
 * <p><b>Not yet covered:</b></p>
 * <ul>
 *   <li>Empty string ({@code ""}) – stored as-is but will fail Azure SDK
 *       downstream (e.g. {@code BlobServiceClientBuilder.connectionString("")})</li>
 *   <li>Whitespace-only strings (no validation; will cause Azure SDK failures)</li>
 * </ul>
 */
@DisplayName("PlainTextConnectionStringProvider – connection string storage")
class PlainTextConnectionStringProviderTest {

    @Test
    @DisplayName("Returns the connection string that was passed to the constructor")
    void testGetConnectionString() {
        String connectionString = "DefaultEndpointsProtocol=https;AccountName=test;AccountKey=key";
        PlainTextConnectionStringProvider provider = new PlainTextConnectionStringProvider(connectionString);

        assertEquals(connectionString, provider.getConnectionString());
    }

    @Test
    @DisplayName("Null connection string → returns null")
    void testGetConnectionStringWithNullValue() {
        PlainTextConnectionStringProvider provider = new PlainTextConnectionStringProvider(null);

        assertNull(provider.getConnectionString());
    }

    @Test
    @DisplayName("Multiple calls return the same value (idempotent)")
    void testGetConnectionStringMultipleTimes() {
        String connectionString = "test-connection-string";
        PlainTextConnectionStringProvider provider = new PlainTextConnectionStringProvider(connectionString);

        // Should return same value each time
        assertEquals(connectionString, provider.getConnectionString());
        assertEquals(connectionString, provider.getConnectionString());
    }
}
