package com.rundeck.plugins.azure.azure

import com.dtolabs.rundeck.core.plugins.configuration.PropertyValidator
import com.dtolabs.rundeck.core.plugins.configuration.ValidationException
import com.azure.resourcemanager.compute.models.KnownLinuxVirtualMachineImage
import com.azure.resourcemanager.compute.models.KnownWindowsVirtualMachineImage

/**
 * Created by luistoledo on 11/16/17.
 */
class AzureVmImageType {
    public static final List<String> VM_IMAGE_TYPE = [AzureVmImageType.LinuxType.UBUNTU_SERVER_20_04_LTS,
                                                      AzureVmImageType.LinuxType.UBUNTU_SERVER_18_04_LTS,
                                                      AzureVmImageType.LinuxType.CENTOS_8_3,
                                                      AzureVmImageType.LinuxType.DEBIAN_10,
                                                      AzureVmImageType.LinuxType.SLES_15,
                                                      AzureVmImageType.LinuxType.OPENSUSE_LEAP_15,
                                                      AzureVmImageType.WindowsType.WINDOWS_SERVER_2019_DATACENTER,
                                                      AzureVmImageType.WindowsType.WINDOWS_SERVER_2016_DATACENTER,
                                                      AzureVmImageType.WindowsType.WINDOWS_SERVER_2012_R2_DATACENTER,
                                                      AzureVmImageType.WindowsType.WINDOWS_SERVER_2019_DATACENTER_WITH_CONTAINERS,
                                                      AzureVmImageType.WindowsType.WINDOWS_DESKTOP_10_PRO
    ]

    @Override
    public String toString() {
        return "AzureVmImageType{}";
    }


    enum LinuxType {
        UBUNTU_SERVER_16_04_LTS("UBUNTU_SERVER_16_04_LTS"),
        UBUNTU_SERVER_18_04_LTS("UBUNTU_SERVER_18_04_LTS"),
        UBUNTU_SERVER_20_04_LTS("UBUNTU_SERVER_20_04_LTS"),
        DEBIAN_9("DEBIAN_9"),
        DEBIAN_10("DEBIAN_10"),
        CENTOS_8_1("CENTOS_8_1"),
        CENTOS_8_3("CENTOS_8_3"),
        OPENSUSE_LEAP_15("OPENSUSE_LEAP_15"),
        OPENSUSE_LEAP_15_1("OPENSUSE_LEAP_15_1"),
        SLES_15("SLES_15"),
        SLES_15_SP1("SLES_15_SP1"),
        REDHAT_RHEL_8_2("REDHAT_RHEL_8_2"),
        ORACLE_LINUX_8_1("ORACLE_LINUX_8_1")

        LinuxType(String value) {
            this.value=value
        }
        private final String value

        KnownLinuxVirtualMachineImage getImageValue() {

            def image
            switch (value){
                case "UBUNTU_SERVER_16_04_LTS":
                    image = KnownLinuxVirtualMachineImage.UBUNTU_SERVER_16_04_LTS
                    break
                case "UBUNTU_SERVER_18_04_LTS":
                    image = KnownLinuxVirtualMachineImage.UBUNTU_SERVER_18_04_LTS
                    break
                case "UBUNTU_SERVER_20_04_LTS":
                    image = KnownLinuxVirtualMachineImage.UBUNTU_SERVER_20_04_LTS
                    break
                case "DEBIAN_9":
                    image = KnownLinuxVirtualMachineImage.DEBIAN_9
                    break
                case "DEBIAN_10":
                    image = KnownLinuxVirtualMachineImage.DEBIAN_10
                    break
                case "CENTOS_8_1":
                    image = KnownLinuxVirtualMachineImage.CENTOS_8_1
                    break
                case "CENTOS_8_3":
                    image = KnownLinuxVirtualMachineImage.CENTOS_8_3
                    break
                case "OPENSUSE_LEAP_15":
                    image = KnownLinuxVirtualMachineImage.OPENSUSE_LEAP_15
                    break
                case "OPENSUSE_LEAP_15_1":
                    image = KnownLinuxVirtualMachineImage.OPENSUSE_LEAP_15_1
                    break
                case "SLES_15":
                    image = KnownLinuxVirtualMachineImage.SLES_15
                    break
                case "SLES_15_SP1":
                    image = KnownLinuxVirtualMachineImage.SLES_15_SP1
                    break
                case "REDHAT_RHEL_8_2":
                    image = KnownLinuxVirtualMachineImage.REDHAT_RHEL_8_2
                    break
                case "ORACLE_LINUX_8_1":
                    image = KnownLinuxVirtualMachineImage.ORACLE_LINUX_8_1
                    break
                default:
                    null
            }

            return image
        }

    }


    enum WindowsType {
        WINDOWS_SERVER_2012_R2_DATACENTER("WINDOWS_SERVER_2012_R2_DATACENTER"),
        WINDOWS_SERVER_2016_DATACENTER("WINDOWS_SERVER_2016_DATACENTER"),
        WINDOWS_SERVER_2019_DATACENTER("WINDOWS_SERVER_2019_DATACENTER"),
        WINDOWS_SERVER_2019_DATACENTER_WITH_CONTAINERS("WINDOWS_SERVER_2019_DATACENTER_WITH_CONTAINERS"),
        WINDOWS_DESKTOP_10_PRO("WINDOWS_DESKTOP_10_PRO"),
        WINDOWS_DESKTOP_10_20H1_PRO("WINDOWS_DESKTOP_10_20H1_PRO")

        WindowsType(String value) {
            this.value=value
        }
        private final String value

        KnownWindowsVirtualMachineImage getImageValue() {

            def image
            switch (value){
                case "WINDOWS_SERVER_2012_R2_DATACENTER":
                    image = KnownWindowsVirtualMachineImage.WINDOWS_SERVER_2012_R2_DATACENTER
                    break
                case "WINDOWS_SERVER_2016_DATACENTER":
                    image = KnownWindowsVirtualMachineImage.WINDOWS_SERVER_2016_DATACENTER
                    break
                case "WINDOWS_SERVER_2019_DATACENTER":
                    image = KnownWindowsVirtualMachineImage.WINDOWS_SERVER_2019_DATACENTER
                    break
                case "WINDOWS_SERVER_2019_DATACENTER_WITH_CONTAINERS":
                    image = KnownWindowsVirtualMachineImage.WINDOWS_SERVER_2019_DATACENTER_WITH_CONTAINERS
                    break
                case "WINDOWS_DESKTOP_10_PRO":
                    image = KnownWindowsVirtualMachineImage.WINDOWS_DESKTOP_10_PRO
                    break
                case "WINDOWS_DESKTOP_10_20H1_PRO":
                    image = KnownWindowsVirtualMachineImage.WINDOWS_DESKTOP_10_20H1_PRO
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
