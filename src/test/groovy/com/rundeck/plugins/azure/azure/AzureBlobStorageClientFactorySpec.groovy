package com.rundeck.plugins.azure.azure

import com.azure.storage.blob.BlobContainerClient
import spock.lang.Specification

class AzureBlobStorageClientFactorySpec extends Specification {

    def "builds a container client scoped to the given account and container"() {
        when:
        BlobContainerClient client = AzureBlobStorageClientFactory.buildContainerClient(
                "myaccount", "bXlrZXk=", "mycontainer"
        )

        then:
        client.getAccountName() == "myaccount"
        client.getBlobContainerName() == "mycontainer"
    }

    def "honors the endpoint protocol and appends extra connection settings"() {
        when:
        BlobContainerClient client = AzureBlobStorageClientFactory.buildContainerClient(
                "myaccount", "bXlrZXk=", "mycontainer", "http", "DefaultEndpointsProtocol=http"
        )

        then:
        client.getAccountName() == "myaccount"
        client.getBlobContainerName() == "mycontainer"
        client.getBlobContainerUrl().startsWith("http://")
    }
}
