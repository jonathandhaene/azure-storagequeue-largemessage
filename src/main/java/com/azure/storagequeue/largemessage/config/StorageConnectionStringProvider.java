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
 * Interface for providing storage connection strings dynamically.
 * Allows integration with dynamic credential sources like Azure Key Vault.
 */
public interface StorageConnectionStringProvider {
    /**
     * Gets the storage connection string.
     * This method is called when the connection string is needed.
     *
     * @return the storage connection string
     */
    String getConnectionString();
}
