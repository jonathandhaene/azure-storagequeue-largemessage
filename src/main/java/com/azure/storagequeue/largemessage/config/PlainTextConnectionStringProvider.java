/*
 * DISCLAIMER: This software is provided "as is", without warranty of any kind,
 * express or implied. The author(s) and maintainer(s) are not responsible for
 * any damage, data loss, or other issues arising from the use of this software.
 * Use at your own risk.
 *
 * See the LICENSE file for full terms.
 */

package com.azure.storagequeue.largemessage.config;

/**
 * Plain text implementation of StorageConnectionStringProvider.
 * Wraps a static connection string (current behavior).
 */
public class PlainTextConnectionStringProvider implements StorageConnectionStringProvider {
    private final String connectionString;

    /**
     * Creates a new PlainTextConnectionStringProvider with the specified connection string.
     *
     * @param connectionString the storage connection string
     */
    public PlainTextConnectionStringProvider(String connectionString) {
        this.connectionString = connectionString;
    }

    /**
     * Gets the storage connection string.
     *
     * @return the storage connection string
     */
    @Override
    public String getConnectionString() {
        return connectionString;
    }
}
