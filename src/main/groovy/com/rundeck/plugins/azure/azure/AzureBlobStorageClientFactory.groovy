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

        return new BlobContainerClientBuilder()
                .connectionString(connectionString)
                .containerName(containerName)
                .buildClient()
    }
}
