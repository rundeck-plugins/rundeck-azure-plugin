package com.rundeck.plugins.azure.azure

import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.BlobContainerClientBuilder

class AzureBlobStorageClientFactory {

    static BlobContainerClient buildContainerClient(String storageAccount, String accessKey,
                                                      String containerName,
                                                      String endpointProtocol = "https",
                                                      String extraConnectionSettings = null) {

        String connectionString = "DefaultEndpointsProtocol=${endpointProtocol};AccountName=${storageAccount};AccountKey=${accessKey}"
        if (extraConnectionSettings) {
            connectionString += ";${extraConnectionSettings}"
        }

        // Azure Blob container names must be lowercase; normalize here so callers that don't
        // already lowercase (e.g. list/delete/endpoint plugins) don't hit a surprising runtime failure.
        return new BlobContainerClientBuilder()
                .connectionString(connectionString)
                .containerName(containerName?.toLowerCase())
                .buildClient()
    }
}
