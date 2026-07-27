package com.rundeck.plugins.azure.azure

import com.azure.core.credential.TokenCredential
import com.azure.core.management.AzureEnvironment
import com.azure.core.management.Region
import com.azure.core.management.exception.ManagementException
import com.azure.core.management.profile.AzureProfile
import com.azure.identity.ClientCertificateCredentialBuilder
import com.azure.identity.ClientSecretCredentialBuilder
import com.azure.resourcemanager.compute.ComputeManager
import com.azure.resourcemanager.compute.models.VirtualMachine
import com.azure.resourcemanager.compute.models.VirtualMachineSize
import com.dtolabs.rundeck.core.resources.ResourceModelSourceException
import com.rundeck.plugins.azure.util.AzurePluginUtil

import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Collectors
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

    List<String> resourceGroups
    String tagName
    String tagValue
    Region region
    boolean onlyRunningInstances
    boolean debug
    boolean useAzureTags

    ComputeManager azure

    AzureManager() {
    }

    //for test only
    void setAzure(ComputeManager azure) {
        this.azure = azure
    }

    ComputeManager connect(){
        TokenCredential credential
        AzureProfile profile = new AzureProfile(this.tenantId, this.subscriptionId, AzureEnvironment.AZURE)

        if(this.key!=null){
            credential = new ClientSecretCredentialBuilder()
                    .clientId(this.clientId)
                    .tenantId(this.tenantId)
                    .clientSecret(this.key)
                    .build()
            azure = ComputeManager.authenticate(credential, profile)
        }

        if(this.pfxCertificatePath!=null && this.pfxCertificatePassword!=null){
            byte[] pfxBytes = Files.readAllBytes(Paths.get(this.pfxCertificatePath))
            credential = new ClientCertificateCredentialBuilder()
                    .clientId(this.clientId)
                    .tenantId(this.tenantId)
                    .pfxCertificate(new ByteArrayInputStream(pfxBytes), this.pfxCertificatePassword)
                    .build()
            azure = ComputeManager.authenticate(credential, profile)
        }

        return azure
    }

    List<AzureNode> listVms(){

        this.connect()

        def vms = azure.virtualMachines()
        List<VirtualMachine> list = new LinkedList<>()

        if(resourceGroups.isEmpty()){
            list.addAll(vms.list().stream().collect(Collectors.toList()))
        }else{
            StringBuilder errorMsgs = new StringBuilder()
            for(String rg : resourceGroups)
                try{
                    list.addAll(vms.listByResourceGroup(rg).stream().collect(Collectors.toList()))
                }catch(ManagementException requestError){
                    errorMsgs.append("\n" + requestError.getLocalizedMessage())
                    if(debug){
                        println("Couldn't load machines for resource group '${rg}': " + requestError.getLocalizedMessage())
                    }
                }
            if (list.size() < 1 && errorMsgs.length() > 0)
                throw new ResourceModelSourceException("COULDN'T LOAD ANY VIRTUAL MACHINES. LISTING ERRORS: " + errorMsgs)
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

            AzureNode azureNode = new AzureNode(virtualMachine,size, useAzureTags)

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
            vms.startAsync(resourceGroups[0],name).block()
        }else{
            vms.start(resourceGroups[0],name)
        }

    }

    void stopVm(String name, boolean async){
        this.connect()

        def vms = azure.virtualMachines()

        if(async) {
            vms.powerOffAsync(resourceGroups[0], name).block()

        }else{
            vms.powerOff(resourceGroups[0], name)
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

        final String publicIPAddressLeafDNS1 = randomResourceName("pip1", 24)

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

    /**
     * Generates a short random resource name with the given prefix, bounded to maxLen characters.
     * Avoids depending on Azure SDK internal utility classes (e.g. ResourceManagerUtils.InternalRuntimeContext)
     * that aren't part of the public API contract and may change without notice.
     */
    private static String randomResourceName(String prefix, int maxLen) {
        String random = UUID.randomUUID().toString().replace("-", "")
        int available = Math.max(0, maxLen - prefix.length())
        return prefix + random.substring(0, Math.min(random.length(), available))
    }

}
