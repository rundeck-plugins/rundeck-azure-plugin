package com.rundeck.plugins.azure.azure

import com.microsoft.azure.management.resources.fluentcore.arm.Region

/**
 * Created by luistoledo on 11/16/17.
 */
class AzureVm {

    Region region
    String name
    String username
    String password
    String resourceGroup
    String primaryNetworkIp
    NetType networkType

    boolean createResourceGroup

    VmType type
    AzureVMSizeType sizeType

    String knownImage
    AzureImage specificImage
    String storedURLImage

    @Override
    public String toString() {
        return "AzureVm{" +
                "region=" + region +
                ", name='" + name + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", resourceGroup='" + resourceGroup + '\'' +
                ", primaryNetworkIp='" + primaryNetworkIp + '\'' +
                ", networkType='" + networkType + '\'' +
                ", createResourceGroup=" + createResourceGroup +
                ", type=" + type +
                ", sizeType=" + sizeType +
                ", knownImage='" + knownImage + '\'' +
                ", specificImage=" + specificImage +
                ", storedURLImage='" + storedURLImage + '\'' +
                '}';
    }


    enum VmType {
        Linux("Linux"), Windows("Windows")

        VmType(String value) {
            this.value=value
        }
        private final String value


    }

    enum NetType {
        Public("Public"), Private("Private")

        NetType(String value) {
            this.value=value
        }
        private final String value


    }








}
