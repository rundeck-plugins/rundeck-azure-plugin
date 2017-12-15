package com.rundeck.plugins.azure.azure

import com.dtolabs.rundeck.core.plugins.configuration.PropertyValidator
import com.dtolabs.rundeck.core.plugins.configuration.ValidationException
import com.microsoft.azure.management.compute.KnownLinuxVirtualMachineImage
import com.microsoft.azure.management.compute.KnownWindowsVirtualMachineImage

/**
 * Created by luistoledo on 11/16/17.
 */
class AzureVmImageType {
    public static final List<String> VM_IMAGE_TYPE = [AzureVmImageType.LinuxType.UBUNTU_SERVER_16_04_LTS,
                                                      AzureVmImageType.LinuxType.UBUNTU_SERVER_14_04_LTS,
                                                      AzureVmImageType.LinuxType.CENTOS_7_2,
                                                      AzureVmImageType.LinuxType.DEBIAN_8,
                                                      AzureVmImageType.LinuxType.SLES_12_SP1,
                                                      AzureVmImageType.LinuxType.OPENSUSE_LEAP_42_1,
                                                      AzureVmImageType.WindowsType.WINDOWS_SERVER_2008_R2_SP1,
                                                      AzureVmImageType.WindowsType.WINDOWS_SERVER_2012_R2_DATACENTER,
                                                      AzureVmImageType.WindowsType.WINDOWS_SERVER_2012_DATACENTER,
                                                      AzureVmImageType.WindowsType.WINDOWS_SERVER_2016_TECHNICAL_PREVIEW_WITH_CONTAINERS,
                                                      AzureVmImageType.WindowsType.WINDOWS_SERVER_TECHNICAL_PREVIEW
    ]

    @Override
    public String toString() {
        return "AzureVmImageType{}";
    }


    enum LinuxType {
        UBUNTU_SERVER_14_04_LTS("UBUNTU_SERVER_14_04_LTS"),
        UBUNTU_SERVER_16_04_LTS("UBUNTU_SERVER_16_04_LTS"),
        CENTOS_7_2("CENTOS_7_2"),
        DEBIAN_8("DEBIAN_8"),
        SLES_12_SP1("SLES_12_SP1"),
        OPENSUSE_LEAP_42_1("OPENSUSE_LEAP_42_1")

        LinuxType(String value) {
            this.value=value
        }
        private final String value

        KnownLinuxVirtualMachineImage getImageValue() {

            def image
            switch (value){
                case "UBUNTU_SERVER_14_04_LTS":
                    image = KnownLinuxVirtualMachineImage.UBUNTU_SERVER_14_04_LTS
                    break
                case "UBUNTU_SERVER_16_04_LTS":
                    image = KnownLinuxVirtualMachineImage.UBUNTU_SERVER_16_04_LTS
                    break
                case "CENTOS_7_2":
                    image = KnownLinuxVirtualMachineImage.CENTOS_7_2
                    break
                case "DEBIAN_8":
                    image = KnownLinuxVirtualMachineImage.DEBIAN_8
                    break
                case "OPENSUSE_LEAP_42_1":
                    image = KnownLinuxVirtualMachineImage.OPENSUSE_LEAP_42_1
                    break
                case "SLES_12_SP1":
                    image = KnownLinuxVirtualMachineImage.SLES_12_SP1
                    break
                default:
                    null
            }

            return image
        }

    }


    enum WindowsType {
        WINDOWS_SERVER_TECHNICAL_PREVIEW("WINDOWS_SERVER_TECHNICAL_PREVIEW"),
        WINDOWS_SERVER_2016_TECHNICAL_PREVIEW_WITH_CONTAINERS("WINDOWS_SERVER_2016_TECHNICAL_PREVIEW_WITH_CONTAINERS"),
        WINDOWS_SERVER_2012_R2_DATACENTER("WINDOWS_SERVER_2012_R2_DATACENTER"),
        WINDOWS_SERVER_2012_DATACENTER("WINDOWS_SERVER_2012_DATACENTER"),
        WINDOWS_SERVER_2008_R2_SP1("WINDOWS_SERVER_2008_R2_SP1")

        WindowsType(String value) {
            this.value=value
        }
        private final String value

        KnownWindowsVirtualMachineImage getImageValue() {

            def image
            switch (value){
                case "WINDOWS_SERVER_TECHNICAL_PREVIEW":
                    image = KnownWindowsVirtualMachineImage.WINDOWS_SERVER_TECHNICAL_PREVIEW
                    break
                case "WINDOWS_SERVER_2016_TECHNICAL_PREVIEW_WITH_CONTAINERS":
                    image = KnownWindowsVirtualMachineImage.WINDOWS_SERVER_2016_TECHNICAL_PREVIEW_WITH_CONTAINERS
                    break
                case "WINDOWS_SERVER_2012_R2_DATACENTER":
                    image = KnownWindowsVirtualMachineImage.WINDOWS_SERVER_2012_R2_DATACENTER
                    break
                case "WINDOWS_SERVER_2012_DATACENTER":
                    image = KnownWindowsVirtualMachineImage.WINDOWS_SERVER_2012_DATACENTER
                    break
                case "WINDOWS_SERVER_2008_R2_SP1":
                    image = KnownWindowsVirtualMachineImage.WINDOWS_SERVER_2008_R2_SP1
                    break
                default:
                    null
            }

            return image
        }

    }

    static class ValidateImage implements PropertyValidator {
        @Override
        boolean isValid(final String value) throws ValidationException {

            if(value.startsWith("\${")){
                return true
            }

            boolean exists=false


            AzureVmImageType.WindowsType.values().each {reg->
                if(reg.value.equals(value)){
                    exists=true
                }

            }

            AzureVmImageType.LinuxType.values().each {reg->
                if(reg.value.equals(value)){
                    exists=true
                }
            }

            return exists
        }
    }



}
