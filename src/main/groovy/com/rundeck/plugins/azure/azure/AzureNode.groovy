package com.rundeck.plugins.azure.azure

import com.microsoft.azure.management.compute.VirtualMachine
import com.microsoft.azure.management.compute.VirtualMachineSize

class AzureNode {

    String username
    String name
    String hostname
    String tags
    String description
    String osFamily
    String osName
    String osVersion

    HashMap azureAttributes

    VirtualMachineSize size


    AzureNode(){

    }

    AzureNode(VirtualMachine vm, VirtualMachineSize size) {

        this.size = size
        //basic attributes

        this.name = vm.name()

        if(this.name==null){
           this.name = vm.osProfile()?.computerName()
        }

        this.username = vm.osProfile()?.adminUsername()

        this.hostname = vm.getPrimaryPublicIPAddress()?.ipAddress()

        if(this.hostname==null){
            //the offline machines doesn't have a IP selected
            this.hostname = "undefined"
        }

        if(this.hostname==null){
            //the offline machines doesn't have a IP selected
            this.name = this.name
        }

        this.osFamily = vm.storageProfile()?.osDisk()?.osType()?.toString()
        this.osName = vm.storageProfile()?.imageReference()?.offer()?.toString()
        this.osVersion = vm.storageProfile()?.imageReference()?.sku()?.toString()

        String osEdition = vm.size()?.toString()

        description ="Azure VM " + this.osName  + " " + osEdition

        //advanced azure attributes
        azureAttributes = new HashMap()

        //add tags, the plugin considerate the tags called Rundeck-Tags
        vm.tags().findAll {key, value -> key.contains("Tags")}.each { key, tag ->
            if(key.equals("Rundeck-Tags")){
                tags = tag
            }
        }

        //add custom attributes
        vm.tags().findAll {key, value -> !key.contains("Tags") && key.contains("Rundeck-")}.each { key, value ->
            String name = key.replace("Rundeck-","")
            azureAttributes."${name}"=value
        }

        azureAttributes.id = vm.id()
        azureAttributes.vmId = vm.vmId()
        azureAttributes.region = vm.region()?.name()
        azureAttributes.resourceGroup = vm.resourceGroupName()
        azureAttributes.status = vm.powerState()?.toString()?.replace("PowerState/","")

        if(vm.plan()!=null){
            azureAttributes."plan:name" = vm.plan().name()
            azureAttributes."plan:product" = vm.plan().product()
            azureAttributes."plan:publisher" = vm.plan().publisher()
        }

        azureAttributes."size:name" = size?.name()
        azureAttributes."size:numberOfCores" = size?.numberOfCores()
        azureAttributes."size:memoryInMB" = size?.memoryInMB()
        azureAttributes."size:maxDataDiskCount" = size?.maxDataDiskCount()
        azureAttributes."size:resourceDiskSizeInMB" = size?.resourceDiskSizeInMB()

        azureAttributes."image:type" = vm.storageProfile()?.imageReference()?.publisher()?.toString()
        azureAttributes."image:offer" = vm.storageProfile()?.imageReference()?.offer()?.toString()
        azureAttributes."image:sku" = vm.storageProfile()?.imageReference()?.sku()?.toString()
        azureAttributes."image:version" = vm.storageProfile()?.imageReference()?.version()?.toString()

        azureAttributes."osDisk:osType" = vm.storageProfile()?.osDisk()?.osType()?.toString()
        azureAttributes."osDisk:name" = vm.storageProfile()?.osDisk()?.name()?.toString()
        azureAttributes."osDisk:createOption" = vm.storageProfile()?.osDisk()?.createOption()?.toString()
        azureAttributes."osDisk:diskSizeGB" = vm.storageProfile()?.osDisk()?.diskSizeGB()?.toString()

        if(vm.instanceView().vmAgent()!=null) {
            vm.instanceView().vmAgent().statuses()?.each { status->
                azureAttributes."provisioningState:code" = status.code()
                azureAttributes."provisioningState:displayStatus" = status.displayStatus()
                azureAttributes."provisioningState:message" = status.message()
                azureAttributes."provisioningState:time" = status.time()?.toString()
            }
        }
    }

    String getField(String name){
        if(null == name){
            return null;
        }
        switch (name) {
            case "short_description": return description
            case "tags": return getTags()
            case "hostname": return getHostname()
            case "name": return getName()
            case "username": return getUsername()
            case "osName": return getOsName()
            case "osVersion": return getOsVersion()
            case "osFamily": return getOsFamily()

            case "azure_region": return getAzureAttributes()!=null ? getAzureAttributes().get("region"):null
            case "azure_resourceGroup": return getAzureAttributes()!=null ? getAzureAttributes().get("resourceGroup"):null
            case "azure_status": return getAzureAttributes()!=null ? getAzureAttributes().get("status"):null
            case "azure_id": return getAzureAttributes()!=null ? getAzureAttributes().get("id"):null
            case "azure_vmId": return getAzureAttributes()!=null ? getAzureAttributes().get("vmId"):null

            case "azure_size_name": return getAzureAttributes()!=null ? getAzureAttributes().get("size:name"):null
            case "azure_size_numberOfCores": return getAzureAttributes()!=null ? getAzureAttributes().get("size:numberOfCores"):null
            case "azure_size_memoryInMB": return getAzureAttributes()!=null ? getAzureAttributes().get("size:memoryInMB"):null
            case "azure_size_maxDataDiskCount": return getAzureAttributes()!=null ? getAzureAttributes().get("size:maxDataDiskCount"):null
            case "azure_size_resourceDiskSizeInMB": return getAzureAttributes()!=null ? getAzureAttributes().get("size:resourceDiskSizeInMB"):null

            case "azure_image_type": return getAzureAttributes()!=null ? getAzureAttributes().get("image:type"):null
            case "azure_image_offer": return getAzureAttributes()!=null ? getAzureAttributes().get("image:offer"):null
            case "azure_image_sku": return getAzureAttributes()!=null ? getAzureAttributes().get("image:sku"):null
            case "azure_image_version": return getAzureAttributes()!=null ? getAzureAttributes().get("image:version"):null

            case "azure_osDisk_osType": return getAzureAttributes()!=null ? getAzureAttributes().get("osDisk:osType"):null
            case "azure_osDisk_name": return getAzureAttributes()!=null ? getAzureAttributes().get("osDisk:name"):null
            case "azure_osDisk_createOption": return getAzureAttributes()!=null ? getAzureAttributes().get("osDisk:createOption"):null
            case "azure_osDisk_diskSizeGB": return getAzureAttributes()!=null ? getAzureAttributes().get("osDisk:diskSizeGB"):null

            case "azure_provisioningState_code": return getAzureAttributes()!=null ? getAzureAttributes().get("provisioningState:code"):null
            case "azure_provisioningState_displayStatus": return getAzureAttributes()!=null ? getAzureAttributes().get("provisioningState:displayStatus"):null
            case "azure_provisioningState_message": return getAzureAttributes()!=null ? getAzureAttributes().get("provisioningState:message"):null
            case "azure_provisioningState_time": return getAzureAttributes()!=null ? getAzureAttributes().get("provisioningState:time"):null

            case "azure_plan_name": return getAzureAttributes()!=null ? getAzureAttributes().get("plan:name"):null
            case "azure_plan_product": return getAzureAttributes()!=null ? getAzureAttributes().get("plan:product"):null
            case "azure_plan_publisher": return getAzureAttributes()!=null ? getAzureAttributes().get("plan:publisher"):null



            default:return getAzureAttributes()!=null ? getAzureAttributes().get(name):null;
        }
    }


    @Override
    public String toString() {
        return "AzureNode{" +
                "username='" + username + '\'' +
                ", name='" + name + '\'' +
                ", hostname='" + hostname + '\'' +
                ", tags='" + tags + '\'' +
                ", description='" + description + '\'' +
                ", osFamily='" + osFamily + '\'' +
                ", osName='" + osName + '\'' +
                ", osVersion='" + osVersion + '\'' +
                ", azureAttributes=" + azureAttributes +
                ", size=" + size +
                '}';
    }

    public Map toMap(){
        Map map=[:]
        map.name=this.name
        map.ip= this.hostname
        map.username=this.username
        map.osFamily=this.osFamily
        map.osName=this.osName
        map.osVersion = this.osVersion
        map.description= this.description
        map.tags= this.tags

        map.putAll(this.azureAttributes)

        return map
    }
}
