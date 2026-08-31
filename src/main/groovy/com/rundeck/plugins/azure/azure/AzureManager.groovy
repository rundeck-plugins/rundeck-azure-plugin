package com.rundeck.plugins.azure.azure

import com.dtolabs.rundeck.core.resources.ResourceModelSourceException
import com.microsoft.azure.AzureEnvironment
import com.microsoft.azure.CloudException
import com.microsoft.azure.credentials.ApplicationTokenCredentials
import com.microsoft.azure.management.Azure
import com.microsoft.azure.management.compute.VirtualMachine
import com.microsoft.azure.management.compute.VirtualMachineSize
import com.microsoft.azure.management.resources.fluentcore.arm.Region
import com.microsoft.azure.management.resources.fluentcore.utils.SdkContext
import com.rundeck.plugins.azure.util.AzurePluginUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
/**
 * Created by luistoledo on 11/6/17.
 */
class AzureManager {

    private static final Logger logger = LoggerFactory.getLogger(AzureManager.class)

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
    boolean queryNodeInstancesInParallel

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
                    this.clientId, this.tenantId, this.pfxCertificatePath as byte[], this.pfxCertificatePassword, AzureEnvironment.AZURE);
            azure = Azure.authenticate(credentials).withSubscription(subscriptionId);
        }

    }

    List<AzureNode> listVms(){

        this.connect()

        def vms = azure.virtualMachines()
        List<VirtualMachine> list = new LinkedList<>()

        if(resourceGroups.isEmpty()){
            list.addAll(new ArrayList<>(vms.list()))
        }else{
            StringBuilder errorMsgs = new StringBuilder()
            for(String rg : resourceGroups)
                try{
                    list.addAll(new ArrayList<>(vms.listByResourceGroup(rg)))
                }catch(CloudException requestError){
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

        if(queryNodeInstancesInParallel && !list.isEmpty()){
            ExecutorService executor = Executors.newFixedThreadPool(list.size())
            try {
                List<Callable<AzureNode>> tasks = list.collect { VirtualMachine virtualMachine ->
                    { -> mapVirtualMachineToNode(virtualMachine) } as Callable<AzureNode>
                }
                logger.info("Querying {} virtual machines in parallel", list.size())
                List<Future<AzureNode>> futures = executor.invokeAll(tasks)
                for (Future<AzureNode> future : futures) {
                    listNodes.add(future.get())
                }
                logger.info("Finished querying {} virtual machines in parallel", list.size())
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt()
                throw new ResourceModelSourceException("Interrupted while querying virtual machines in parallel", e)
            } catch (ExecutionException e) {
                throw new ResourceModelSourceException("Failed to query virtual machines in parallel", e.getCause() ?: e)
            } finally {
                executor.shutdown()
                try {
                    if (!executor.awaitTermination(90, TimeUnit.SECONDS)) {
                        logger.warn("Forcing shutdown of thread pool after waiting 90 seconds")
                        executor.shutdownNow()
                    }
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt()
                    executor.shutdownNow()
                }
            }
        }else{
            list.each { VirtualMachine virtualMachine ->
                listNodes.add(mapVirtualMachineToNode(virtualMachine))
            }
        }

        return listNodes
    }

    private AzureNode mapVirtualMachineToNode(VirtualMachine virtualMachine){
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

        return azureNode
    }

    void startVm(String name, boolean async){
        this.connect()

        def vms = azure.virtualMachines()

        if(async){
            vms.startAsync(resourceGroups[0],name).await()
        }else{
            vms.start(resourceGroups[0],name)
        }

    }

    void stopVm(String name, boolean async){
        this.connect()

        def vms = azure.virtualMachines()

        if(async) {
            vms.powerOffAsync(resourceGroups[0], name).await()

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
