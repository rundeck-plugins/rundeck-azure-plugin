package com.rundeck.plugins.azure.azure

import com.microsoft.azure.management.compute.ImageReference

/**
 * Created by luistoledo on 11/17/17.
 */
class AzureImage {

    String publisher
    String offer
    String sku
    String version


    AzureImage(String publisher, String offer, String sku, String version) {
        this.publisher = publisher
        this.offer = offer
        this.sku = sku
        this.version = version
    }

    ImageReference getImageReference(){
        return new ImageReference()
                .withPublisher(publisher)
                .withOffer(offer)
                .withSku(sku)
                .withVersion(version);
    }

    @Override
    public String toString() {
        return "AzureImage{" +
                "publisher='" + publisher + '\'' +
                ", offer='" + offer + '\'' +
                ", sku='" + sku + '\'' +
                ", version='" + version + '\'' +
                '}';
    }
}
