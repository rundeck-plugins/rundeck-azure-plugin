package com.rundeck.plugins.azure.azure

import com.azure.core.management.Region
import com.azure.resourcemanager.compute.models.OSProfile
import com.azure.resourcemanager.compute.models.PowerState
import com.azure.resourcemanager.compute.models.StorageProfile
import com.azure.resourcemanager.compute.models.VirtualMachine
import com.azure.resourcemanager.compute.models.VirtualMachineInstanceView
import com.azure.resourcemanager.compute.models.VirtualMachineSize
import com.azure.resourcemanager.compute.models.VirtualMachineSizeTypes
import spock.lang.Specification

class AzureNodeSpec extends Specification {

    def "flattens Rundeck-Tags and Rundeck-* custom attributes but drops unrelated tags"() {
        given:
        def instanceView = Stub(VirtualMachineInstanceView) {
            vmAgent() >> null
        }
        def size = Stub(VirtualMachineSize) {
            name() >> "Standard_D2_v3"
            numberOfCores() >> 2
            memoryInMB() >> 8192
            maxDataDiskCount() >> 4
            resourceDiskSizeInMB() >> 16384
        }

        Map<String, String> vmTags = [
                "Rundeck-Tags"      : "tag1,tag2",
                "Rundeck-customAttr": "customValue",
                "unrelatedTag"      : "unrelatedValue",
        ]

        def vm = Stub(VirtualMachine) {
            name() >> "myvm"
            osProfile() >> new OSProfile().withComputerName("myhost").withAdminUsername("adminuser")
            getPrimaryPublicIPAddress() >> null
            storageProfile() >> new StorageProfile()
            size() >> VirtualMachineSizeTypes.STANDARD_D2_V3
            tags() >> vmTags
            id() >> "/subscriptions/x/resourceGroups/rg/vm/myvm"
            vmId() >> "vmid-123"
            region() >> Region.US_EAST
            resourceGroupName() >> "rg"
            powerState() >> PowerState.RUNNING
            plan() >> null
            getPrimaryNetworkInterface() >> null
            instanceView() >> instanceView
        }

        when:
        AzureNode node = new AzureNode(vm, size, false)

        then:
        node.tags == "tag1,tag2"
        node.azureAttributes.customAttr == "customValue"
        !node.azureAttributes.containsKey("unrelatedTag")
        !node.azureAttributes.containsKey("Rundeck-Tags")
        node.azureAttributes."size:name" == "Standard_D2_v3"
        node.azureAttributes."size:numberOfCores" == 2
        node.azureTags == null
    }

    def "collects raw azure tags (minus Rundeck-Tags) when useAzureTags is true"() {
        given:
        def instanceView = Stub(VirtualMachineInstanceView) {
            vmAgent() >> null
        }
        def size = Stub(VirtualMachineSize)

        Map<String, String> vmTags = [
                "Rundeck-Tags": "tag1",
                "env"         : "prod",
        ]

        def vm = Stub(VirtualMachine) {
            name() >> "myvm"
            osProfile() >> new OSProfile()
            getPrimaryPublicIPAddress() >> null
            storageProfile() >> new StorageProfile()
            size() >> VirtualMachineSizeTypes.STANDARD_D2_V3
            tags() >> vmTags
            id() >> "id"
            vmId() >> "vmid"
            region() >> Region.US_EAST
            resourceGroupName() >> "rg"
            powerState() >> PowerState.RUNNING
            plan() >> null
            getPrimaryNetworkInterface() >> null
            instanceView() >> instanceView
        }

        when:
        AzureNode node = new AzureNode(vm, size, true)

        then:
        node.azureTags == [env: "prod"]
        !node.azureTags.containsKey("Rundeck-Tags")
    }
}
