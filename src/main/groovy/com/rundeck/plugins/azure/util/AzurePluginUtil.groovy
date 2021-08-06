package com.rundeck.plugins.azure.util

import com.dtolabs.rundeck.core.execution.workflow.steps.StepException
import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants
import com.dtolabs.rundeck.core.storage.ResourceMeta
import com.dtolabs.rundeck.core.storage.StorageTree
import com.dtolabs.rundeck.plugins.step.PluginStepContext
import com.microsoft.azure.management.compute.DataDisk
import com.microsoft.azure.management.compute.VirtualMachine
import com.microsoft.azure.management.compute.VirtualMachineExtension
import com.rundeck.plugins.azure.plugin.AzureFailureReason
import groovy.transform.CompileStatic

/**
 * Created by luistoledo on 11/6/17.
 */
class AzurePluginUtil {

    static final String FILE_SEPARATOR = "/";


    static boolean isNotSet(String str){
        return null == str || str.length() == 0;
    }
    static boolean isSet(String str){
        return !isNotSet(str);
    }

    /**
     * Print virtual machine info.
     *
     * @param resource a virtual machine
     */
    static def printVm(VirtualMachine resource) {
        StringBuilder storageProfile = new StringBuilder().append("\n\tStorageProfile: ");

        if (resource.getPrimaryPublicIPAddress() != null) {
            storageProfile.append("\n" +
                    "\t\tIP:" + resource.getPrimaryPublicIPAddress().ipAddress())
        }
        storageProfile.append("\n" +
                "\t\tID:" + resource.id())
        storageProfile.append("\n" +
                "\t\tVM-ID:" + resource.vmId())

        if (resource.storageProfile()?.imageReference() != null) {
            storageProfile.append("\n\t\tImageReference:");
            storageProfile.append("\n\t\t\tPublisher: ").append(resource.storageProfile().imageReference().publisher());
            storageProfile.append("\n\t\t\tOffer: ").append(resource.storageProfile().imageReference().offer());
            storageProfile.append("\n\t\t\tSKU: ").append(resource.storageProfile().imageReference().sku());
            storageProfile.append("\n\t\t\tVersion: ").append(resource.storageProfile().imageReference().version());
        }

        if (resource.storageProfile()?.osDisk() != null) {
            storageProfile.append("\n\t\tOSDisk:");
            storageProfile.append("\n\t\t\tOSType: ").append(resource.storageProfile().osDisk().osType());
            storageProfile.append("\n\t\t\tName: ").append(resource.storageProfile().osDisk().name());
            storageProfile.append("\n\t\t\tCaching: ").append(resource.storageProfile().osDisk().caching());
            storageProfile.append("\n\t\t\tCreateOption: ").append(resource.storageProfile().osDisk().createOption());
            storageProfile.append("\n\t\t\tDiskSizeGB: ").append(resource.storageProfile().osDisk().diskSizeGB());
            if (resource.storageProfile().osDisk().image() != null) {
                storageProfile.append("\n\t\t\tImage Uri: ").append(resource.storageProfile().osDisk().image().uri());
            }
            if (resource.storageProfile().osDisk().vhd() != null) {
                storageProfile.append("\n\t\t\tVhd Uri: ").append(resource.storageProfile().osDisk().vhd().uri());
            }
            if (resource.storageProfile().osDisk().encryptionSettings() != null) {
                storageProfile.append("\n\t\t\tEncryptionSettings: ");
                storageProfile.append("\n\t\t\t\tEnabled: ").append(resource.storageProfile().osDisk().encryptionSettings().enabled());
                storageProfile.append("\n\t\t\t\tDiskEncryptionKey Uri: ").append(resource
                        .storageProfile()
                        .osDisk()
                        .encryptionSettings()
                        .diskEncryptionKey().secretUrl());
                storageProfile.append("\n\t\t\t\tKeyEncryptionKey Uri: ").append(resource
                        .storageProfile()
                        .osDisk()
                        .encryptionSettings()
                        .keyEncryptionKey().keyUrl());
            }
        }

        if (resource.storageProfile()?.dataDisks() != null) {
            int i = 0;
            for (DataDisk disk : resource.storageProfile().dataDisks()) {
                storageProfile.append("\n\t\tDataDisk: #").append(i++);
                storageProfile.append("\n\t\t\tName: ").append(disk.name());
                storageProfile.append("\n\t\t\tCaching: ").append(disk.caching());
                storageProfile.append("\n\t\t\tCreateOption: ").append(disk.createOption());
                storageProfile.append("\n\t\t\tDiskSizeGB: ").append(disk.diskSizeGB());
                storageProfile.append("\n\t\t\tLun: ").append(disk.lun());
                if (resource.isManagedDiskEnabled()) {
                    if (disk.managedDisk() != null) {
                        storageProfile.append("\n\t\t\tManaged Disk Id: ").append(disk.managedDisk().id());
                    }
                } else {
                    if (disk.vhd().uri() != null) {
                        storageProfile.append("\n\t\t\tVhd Uri: ").append(disk.vhd().uri());
                    }
                }
                if (disk.image() != null) {
                    storageProfile.append("\n\t\t\tImage Uri: ").append(disk.image().uri());
                }
            }
        }

        StringBuilder osProfile = new StringBuilder().append("\n\tOSProfile: ");
        if (resource.osProfile() != null) {
            osProfile.append("\n\t\tComputerName:").append(resource.osProfile().computerName());
            if (resource.osProfile().windowsConfiguration() != null) {
                osProfile.append("\n\t\t\tWindowsConfiguration: ");
                osProfile.append("\n\t\t\t\tProvisionVMAgent: ")
                        .append(resource.osProfile().windowsConfiguration().provisionVMAgent());
                osProfile.append("\n\t\t\t\tEnableAutomaticUpdates: ")
                        .append(resource.osProfile().windowsConfiguration().enableAutomaticUpdates());
                osProfile.append("\n\t\t\t\tTimeZone: ")
                        .append(resource.osProfile().windowsConfiguration().timeZone());
            }

            if (resource.osProfile().linuxConfiguration() != null) {
                osProfile.append("\n\t\t\tLinuxConfiguration: ");
                osProfile.append("\n\t\t\t\tDisablePasswordAuthentication: ")
                        .append(resource.osProfile().linuxConfiguration().disablePasswordAuthentication());
            }
        } else {
            // OSProfile will be null for a VM attached to specialized VHD.
            osProfile.append("null");
        }

        StringBuilder networkProfile = new StringBuilder().append("\n\tNetworkProfile: ");
        for (String networkInterfaceId : resource.networkInterfaceIds()) {
            networkProfile.append("\n\t\tId:").append(networkInterfaceId);
        }

        StringBuilder extensions = new StringBuilder().append("\n\tExtensions: ");
        for (Map.Entry<String, VirtualMachineExtension> extensionEntry : resource.listExtensions()?.entrySet()) {
            VirtualMachineExtension extension = extensionEntry.getValue();
            extensions.append("\n\t\tExtension: ").append(extension.id())
                    .append("\n\t\t\tName: ").append(extension.name())
                    .append("\n\t\t\tTags: ").append(extension.tags())
                    .append("\n\t\t\tProvisioningState: ").append(extension.provisioningState())
                    .append("\n\t\t\tAuto upgrade minor version enabled: ").append(extension.autoUpgradeMinorVersionEnabled())
                    .append("\n\t\t\tPublisher: ").append(extension.publisherName())
                    .append("\n\t\t\tType: ").append(extension.typeName())
                    .append("\n\t\t\tVersion: ").append(extension.versionName())
                    .append("\n\t\t\tPublic Settings: ").append(extension.publicSettingsAsJsonString());
        }

        StringBuilder msi = new StringBuilder().append("\n\tMSI: ");
        msi.append("\n\t\t\tMSI enabled:").append(resource.isManagedServiceIdentityEnabled());
        msi.append("\n\t\t\tMSI Active Directory Service Principal Id:").append(resource.managedServiceIdentityPrincipalId());
        msi.append("\n\t\t\tMSI Active Directory Tenant Id:").append(resource.managedServiceIdentityTenantId());

        StringBuilder zones = new StringBuilder().append("\n\tZones: ");
        zones.append(resource.availabilityZones());

        StringBuilder vmInfo= new StringBuilder().append("Virtual Machine: ").append(resource.id())
                .append("\n\tName: ").append(resource.name())
                .append("\n\tResource group: ").append(resource.resourceGroupName())
                .append("\n\tRegion: ").append(resource.region())
                .append("\n\tTags: ").append(resource.tags())
                .append("\n\tHardwareProfile: ")
                .append("\n\t\tSize: ").append(resource.size())
                .append(storageProfile)
                .append(osProfile)
                .append(networkProfile)
                .append(extensions)
                .append(msi)
                .append(zones)

        return vmInfo.toString()

    }



    static Map<String, Object> getRenderOpt(String value, boolean secondary, boolean password = false, boolean storagePassword = false) {
        Map<String, Object> ret = new HashMap<>();
        ret.put(StringRenderingConstants.GROUP_NAME,value);
        if(secondary){
            ret.put(StringRenderingConstants.GROUPING,"secondary");
        }
        if(password){
            ret.put("displayType",StringRenderingConstants.DisplayType.PASSWORD)
        }
        if(storagePassword){
            ret.put(StringRenderingConstants.SELECTION_ACCESSOR_KEY,StringRenderingConstants.SelectionAccessor.STORAGE_PATH)
            ret.put(StringRenderingConstants.STORAGE_PATH_ROOT_KEY,"keys")
            ret.put(StringRenderingConstants.STORAGE_FILE_META_FILTER_KEY, "Rundeck-data-type=password")
        }

        return ret;
    }

    static String getPasswordFromKeyStorage(String path, PluginStepContext context){
        ResourceMeta contents = context.getExecutionContext().getStorageTree().getResource(path).getContents();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        contents.writeContent(byteArrayOutputStream);
        String password = new String(byteArrayOutputStream.toByteArray());

        return password;

    }

    static String getPasswordFromKeyStorage(String path, StorageTree storage) {
        try{
            ResourceMeta contents = storage.getResource(path).getContents()
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()
            contents.writeContent(byteArrayOutputStream)
            String password = new String(byteArrayOutputStream.toByteArray())

            return password
        }catch(Exception e){
            throw new StepException("error accessing ${path}: ${e.message}", AzureFailureReason.KeyStorage)
        }

    }

    static void printMessage(String message, boolean debug, String level){


        if(level.equals("debug") && debug){
            System.out.println("[debug] " + message);
        }
        if(level.equals("info")){
            System.out.println("[info] " + message);
        }

        if(level.equals("warning")){
            System.out.println("[warning] " + message);
        }

        if(level.equals("error")){
            System.err.println("[error] " + message);
        }

    }


    static boolean hasWildcards(String path) {
        return path.matches(".*[?*]+.*");
    }

    static boolean isDirectory(String path) {
        return path.endsWith(FILE_SEPARATOR);
    }
}
