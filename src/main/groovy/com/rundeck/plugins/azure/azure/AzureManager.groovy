package com.rundeck.plugins.azure.azure

import com.microsoft.azure.AzureEnvironment
import com.microsoft.azure.credentials.ApplicationTokenCredentials
import com.microsoft.azure.management.Azure
import com.microsoft.azure.management.compute.VirtualMachineSize
import com.microsoft.azure.management.resources.fluentcore.arm.Region
import com.microsoft.azure.management.resources.fluentcore.utils.SdkContext
import com.rundeck.plugins.azure.util.AzurePluginUtil
/**
 * Created by luistoledo on 11/6/17.
 */
class AzureManager {

    String clientId
    String tenantId
    String subscriptionId
    String key
    String pfxCertificatePath
    String pfxCertificatePassword

    String resourceGroup
    String tagName
    String tagValue
    Region region
    boolean onlyRunningInstances
    boolean debug

    Azure azure

    AzureManager() {
    }

    //for test only
    void setAzure(Azure azure) {
        this.azure = azure
    }

    Azure connect(){
        ApplicationTokenCredentials credentials

        if(this.key!=null){
            credentials = new ApplicationTokenCredentials(this.clientId, this.tenantId, this.key, AzureEnvironment.AZURE);
            azure = Azure.authenticate(credentials).withSubscription(this.subscriptionId);
        }

        if(this.pfxCertificatePath!=null && this.pfxCertificatePassword!=null){
            credentials = new ApplicationTokenCredentials(
                    this.clientId, this.tenantId, this.pfxCertificatePath, this.pfxCertificatePassword, AzureEnvironment.AZURE);
            azure = Azure.authenticate(credentials).withSubscription(subscriptionId);
        }

    }

    List<AzureNode> listVms(){

        this.connect()

        def vms = azure.virtualMachines()
        def list

        if(resourceGroup!=null){
            list = vms.listByResourceGroup(resourceGroup)
        }else{
            list = vms.list()
        }

        if(onlyRunningInstances){
            list = list.findAll({p-> p.powerState().toString().contains("running")})
        }

        if(region!=null){
            list = list.findAll({p-> p.region()==region})
        }

        if(tagName!=null && tagValue != null){
            list = list.findAll({p->
                p.tags().find { t -> t.getKey() == tagName && tagValue == t.getValue() } != null
            })
        }

        List<AzureNode> listNodes = new ArrayList<>()

        list.each { virtualMachine->

            if(debug){
                println ("--------- VM input ---------------")
                println(AzurePluginUtil.printVm(virtualMachine))
            }

            VirtualMachineSize size = azure.virtualMachines().sizes().listByRegion(virtualMachine.region()).find{ size-> size.name().equals(virtualMachine.size().toString())}


            AzureNode azureNode = new AzureNode(virtualMachine,size)

            if(debug){
                println ("--------- VM Mapping result ---------------")
                println(azureNode)
            }

            listNodes.add(azureNode)
        }

        return listNodes
    }

    void startVm(String name, boolean async){
        this.connect()

        def vms = azure.virtualMachines()

        if(async){
            vms.startAsync(resourceGroup,name).await()
        }else{
            vms.start(resourceGroup,name)
        }

    }

    void stopVm(String name, boolean async){
        this.connect()

        def vms = azure.virtualMachines()

        if(async) {
            vms.powerOffAsync(resourceGroup, name).await()

        }else{
            vms.powerOff(resourceGroup, name)
        }
    }



    boolean createVirtualMachine(AzureVm vm){
        this.connect()

        Date t1 = new Date();

        def create = azure.virtualMachines()
                .define(vm.getName())
                .withRegion(vm.getRegion())

        def rgDefinition
        if(vm.getCreateResourceGroup()){
            rgDefinition = create.withNewResourceGroup(vm.getResourceGroup())
        }else{
            rgDefinition = create.withExistingResourceGroup(vm.getResourceGroup())
        }

        final String publicIPAddressLeafDNS1 = SdkContext.randomResourceName("pip1", 24)

        def azureVm

        if(vm.getNetworkType() == AzureVm.NetType.Public){
            azureVm = rgDefinition.withNewPrimaryNetwork(vm.getPrimaryNetworkIp())
                                  .withPrimaryPrivateIPAddressDynamic()
                                  .withNewPrimaryPublicIPAddress(publicIPAddressLeafDNS1)

        }

        if(vm.getNetworkType() == AzureVm.NetType.Private){
            azureVm = rgDefinition.withNewPrimaryNetwork(vm.getPrimaryNetworkIp())
                    .withPrimaryPrivateIPAddressDynamic()
                    .withoutPrimaryPublicIPAddress()

        }

        def machine

        if(vm.getType() == AzureVm.VmType.Linux) {

            if (vm.getKnownImage() != null) {
                AzureVmImageType.LinuxType imageType = vm.getKnownImage() as AzureVmImageType.LinuxType
                machine = azureVm.withPopularLinuxImage(imageType.getImageValue())
            }

            if(vm.getSpecificImage()!=null){
                machine = azureVm.withSpecificLinuxImageVersion(vm.getSpecificImage())
            }

            if(vm.getStoredURLImage()!=null){
                machine = azureVm.withStoredLinuxImage(vm.getStoredURLImage())
            }
        }
        if(vm.getType()== AzureVm.VmType.Windows){
            if(vm.getKnownImage()!=null && !vm.getKnownImage().isEmpty()){
                AzureVmImageType.WindowsType knownImage = vm.getKnownImage() as AzureVmImageType.WindowsType
                machine= azureVm.withPopularWindowsImage(knownImage.getImageValue())
            }

            if(vm.getSpecificImage()!=null){
                machine= azureVm.withSpecificWindowsImageVersion(vm.getSpecificImage().getImageReference())
            }

            if(vm.getStoredURLImage()!=null && !vm.getStoredURLImage().isEmpty()){
                machine = azureVm.withStoredWindowsImage(vm.getStoredURLImage())
            }

        }

        def newVm = machine.withRootUsername(vm.getUsername())
                .withRootPassword(vm.getPassword())
                .withSize(vm.getSizeType().getImageSize()).create()


        Date t2 = new Date();
        println("Created VM: (took " + ((t2.getTime() - t1.getTime()) / 1000) + " seconds) " + newVm.id()+"\n" +
                "\t");

        AzurePluginUtil.printVm(newVm)

    }

    static def getVmSizes(){

    }


}
