package com.rundeck.plugins.azure.plugin

import com.dtolabs.rundeck.core.dispatcher.DataContextUtils
import com.dtolabs.rundeck.core.execution.workflow.steps.StepException
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.plugins.configuration.Describable
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.core.plugins.configuration.PropertyUtil
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.step.PluginStepContext
import com.dtolabs.rundeck.plugins.step.StepPlugin
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder
import com.microsoft.azure.management.compute.VirtualMachine
import com.microsoft.azure.management.resources.fluentcore.arm.Region
import com.rundeck.plugins.azure.azure.AzureImage
import com.rundeck.plugins.azure.azure.AzureManager
import com.rundeck.plugins.azure.azure.AzureManagerBuilder
import com.rundeck.plugins.azure.azure.AzureVMSizeType
import com.rundeck.plugins.azure.azure.AzureVm
import com.rundeck.plugins.azure.azure.AzureVmImageType
import com.rundeck.plugins.azure.util.AzurePluginUtil

/**
 * Created by luistoledo on 11/16/17.
 */
@Plugin(name = AzureVMCreatePlugin.PROVIDER_NAME, service = ServiceNameConstants.WorkflowStep)
class AzureVMCreatePlugin  implements StepPlugin, Describable {
    public static final String PROVIDER_NAME = "azure-vm-create-step";
    public static final String PROVIDER_TITLE = "Azure / VM / Create"
    public static final String PROVIDER_DESCRIPTION ="Create an Azure Virtual Machine"

    //https://github.com/Azure/azure-sdk-for-java/blob/master/AUTH.md#using-applicationtokencredentials
    public static final String CLIENT = "client"
    public static final String TENANT = "tenant"
    public static final String SUBSCRIPTION_ID = "subscriptionId"
    public static final String KEY = "key"
    public static final String PFX_CERTIFICATE_PATH = "pfxCertificatePath"
    public static final String PFX_CERTIFICATE_PASSWORD = "pfxCertificatePassword"

    public static final String VM_REGION = "vmRegion"
    public static final String VM_NAME = "vmName"
    public static final String VM_USERNAME = "vmUsername"
    public static final String VM_PASSWORD = "vmPassword"
    public static final String VM_RESOURCE_GROUP = "vmResourceGroup"
    public static final String VM_CREATE_RESOURCE_GROUP = "vmCreateResourceGroup"
    public static final String VM_TYPE = "vmType"
    public static final String VM_IMAGE_TYPE = "vmImageType"
    public static final String VM_SIZE_TYPE = "vmSizeType"
    public static final String VM_NET_PRIMARY = "vmNetPrimary"
    public static final String VM_NET_TYPE = "vmNetType"

    public static final String VM_IMAGE_PUBLISHER = "vmImagePublisher"
    public static final String VM_IMAGE_OFFER = "vmImageOffer"
    public static final String VM_IMAGE_SKU = "vmImageSku"
    public static final String VM_IMAGE_VERSION = "vmImageVersion"
    public static final String VM_IMAGE_STORED = "vmImageStored"



    public static final List<String> LIST_VM_TYPE =[AzureVm.VmType.Linux, AzureVm.VmType.Windows]
    public static final List<String> LIST_NET_TYPE =[AzureVm.NetType.Public, AzureVm.NetType.Private]

    final static Map<String, Object> renderingOptionsAuthentication = AzurePluginUtil.getRenderOpt("Credentials",false)
    final static Map<String, Object> renderingOptionsAuthenticationStorage = AzurePluginUtil.getRenderOpt("Credentials",false, false, true)
    final static Map<String, Object> renderingOptionsConfig = AzurePluginUtil.getRenderOpt("VM Properties",false)
    final static Map<String, Object> renderingOptionsNetwork = AzurePluginUtil.getRenderOpt("Network Options",false)

    final static Map<String, Object> renderingImageConfig = AzurePluginUtil.getRenderOpt("Select Image: Select the image form a \"Known Image\" List",false)
    final static Map<String, Object> renderingSpecificImageConfig = AzurePluginUtil.getRenderOpt("Select Image: Specific Image using publisher, offer, sku and version",true)
    final static Map<String, Object> renderingSpecificStoredImageConfig = AzurePluginUtil.getRenderOpt("Select Image: Specific Stored Image from a URI",true)


    static Description DESCRIPTION = DescriptionBuilder.builder()
            .name(PROVIDER_NAME)
            .title(PROVIDER_TITLE)
            .description(PROVIDER_DESCRIPTION)
            .property(PropertyUtil.string(CLIENT, "Client ID", "Azure Client ID.", false,
            null,null,null, renderingOptionsAuthentication))
            .property(PropertyUtil.string(TENANT, "Tenant ID", "Azure Tenant ID.", false,
            null,null,null, renderingOptionsAuthentication))
            .property(PropertyUtil.string(SUBSCRIPTION_ID, "Subscription ID", "Azure Subscription ID.", false,
            null,null,null, renderingOptionsAuthentication))
            .property(PropertyUtil.string(KEY, "Key", "Azure Access Key.", false,
            null,null,null, renderingOptionsAuthenticationStorage))
            .property(PropertyUtil.string(PFX_CERTIFICATE_PATH, "Certificate Path", "Azure certificate file path.", false,
            null,null,null, renderingOptionsAuthentication))
            .property(PropertyUtil.string(PFX_CERTIFICATE_PASSWORD, "Certificate Password", "Azure certificate Password.", false,
            null,null,null, renderingOptionsAuthentication))


            .property(PropertyUtil.string(VM_REGION, "Region", "Azure Region.", true,
            null,null,null, renderingOptionsConfig))
            .property(PropertyUtil.string(VM_NAME, "Name", "Name of the new VM.", true,
            null,null,null, renderingOptionsConfig))
            .property(PropertyUtil.string(VM_USERNAME, "UserName", "UserName of the new VM.", true,
            null,null,null, renderingOptionsConfig))
            .property(PropertyUtil.string(VM_PASSWORD, "Password", "Password of the new VM.", true,
            null,null,null, renderingOptionsConfig))
            .property(PropertyUtil.string(VM_RESOURCE_GROUP, "Resource Group", "Resource Group of the new VM.", true,
            null,null,null, renderingOptionsConfig))
            .property(PropertyUtil.bool(VM_CREATE_RESOURCE_GROUP,"Create Resource Group?","Should create a new Resource Group?",
            false,"false",null,renderingOptionsConfig))
            .property(PropertyUtil.freeSelect(VM_SIZE_TYPE,"VM Size","VM Size.",
            true, "", AzureVMSizeType.VM_SIZE_TYPE,null,null,renderingOptionsConfig))

            .property(PropertyUtil.freeSelect(VM_TYPE,"OS Type","OS Type (Linux or Windows).",
            true, "",LIST_VM_TYPE,null,null,renderingOptionsConfig))

            .property(PropertyUtil.string(VM_NET_PRIMARY, "Primary Network", "Primary Network IP.", false,
            "10.0.0.0/28",null,null, renderingOptionsNetwork))
            .property(PropertyUtil.freeSelect(VM_NET_TYPE,"Network Type","Network Type (Public or Private)",
            true, "", LIST_NET_TYPE,null,null,renderingOptionsNetwork))

            .property(PropertyUtil.freeSelect(VM_IMAGE_TYPE,"Known Image","Known Image to use.",
            false, "", AzureVmImageType.VM_IMAGE_TYPE,new AzureVmImageType.ValidateImage(),null,renderingImageConfig))

            .property(PropertyUtil.string(VM_IMAGE_PUBLISHER, "Image Publisher", "Publisher of the Image.", false,
            null,null,null, renderingSpecificImageConfig))
            .property(PropertyUtil.string(VM_IMAGE_OFFER, "Image Offer", "Offer of the Image.", false,
            null,null,null, renderingSpecificImageConfig))
            .property(PropertyUtil.string(VM_IMAGE_SKU, "Image SKU", "SKU of the Image.", false,
            null,null,null, renderingSpecificImageConfig))
            .property(PropertyUtil.string(VM_IMAGE_VERSION, "Image Version", "Version of the Image.", false,
            "latest",null,null, renderingSpecificImageConfig))


            .property(PropertyUtil.string(VM_IMAGE_STORED, "Image URL", "Stored Image URL.", false,
            null,null,null, renderingSpecificStoredImageConfig))
            .build()

    @Override
    Description getDescription() {
        return DESCRIPTION
    }

    @Override
    void executeStep(PluginStepContext context, Map<String, Object> configuration) throws StepException {


        String clientId=configuration.get(AzureVMCreatePlugin.CLIENT)
        String tenantId=configuration.get(AzureVMCreatePlugin.TENANT)
        String subscriptionId=configuration.get(AzureVMCreatePlugin.SUBSCRIPTION_ID)
        String keyPath=configuration.get(AzureVMCreatePlugin.KEY)
        String pfxCertificatePath=configuration.get(AzureVMCreatePlugin.PFX_CERTIFICATE_PATH)
        String pfxCertificatePassword=configuration.get(AzureVMCreatePlugin.PFX_CERTIFICATE_PASSWORD)

        if(keyPath == null && pfxCertificatePath == null){
            throw new IllegalArgumentException("You must set the key or the certificate path in order to authenticate");
        }

        String regionNameLabel=configuration.get(AzureVMCreatePlugin.VM_REGION)

        String name=configuration.get(AzureVMCreatePlugin.VM_NAME)
        String username=configuration.get(AzureVMCreatePlugin.VM_USERNAME)
        String password=configuration.get(AzureVMCreatePlugin.VM_PASSWORD)
        String resourceGroup=configuration.get(AzureVMCreatePlugin.VM_RESOURCE_GROUP)
        boolean createResourceGroup = Boolean.valueOf(configuration.get(AzureVMCreatePlugin.VM_CREATE_RESOURCE_GROUP))
        String type=configuration.get(AzureVMCreatePlugin.VM_TYPE)
        String knownImage=configuration.get(AzureVMCreatePlugin.VM_IMAGE_TYPE)
        String sizeType=configuration.get(AzureVMCreatePlugin.VM_SIZE_TYPE)
        String primaryNetworkIp=configuration.get(AzureVMCreatePlugin.VM_NET_PRIMARY)
        String netType=configuration.get(AzureVMCreatePlugin.VM_NET_TYPE)

        String imagePublisher = configuration.get(AzureVMCreatePlugin.VM_IMAGE_PUBLISHER)
        String imageOffer = configuration.get(AzureVMCreatePlugin.VM_IMAGE_OFFER)
        String imageSKU = configuration.get(AzureVMCreatePlugin.VM_IMAGE_SKU)
        String imageVersion = configuration.get(AzureVMCreatePlugin.VM_IMAGE_VERSION)

        String imageStored = configuration.get(AzureVMCreatePlugin.VM_IMAGE_STORED)

        if(knownImage==null && imagePublisher==null && imageOffer==null && imageSKU==null && imageStored==null){
            throw new IllegalArgumentException("You must select one image source");
        }

        String key = AzurePluginUtil.getPasswordFromKeyStorage(keyPath,context);
        Region region = Region.findByLabelOrName(regionNameLabel)

        if(region==null){
            throw new IllegalArgumentException("Region not found");
        }

        AzureManager manager = AzureManagerBuilder.builder()
                .clientId(clientId)
                .tenantId(tenantId)
                .subscriptionId(subscriptionId)
                .key(key)
                .pfxCertificatePath(pfxCertificatePath)
                .pfxCertificatePassword(pfxCertificatePassword)
                .build()

        AzureVm vm = new AzureVm()
        vm.setRegion(region)
        vm.setName(name)
        vm.setUsername(username)
        vm.setPassword(password)
        vm.setResourceGroup(resourceGroup)
        vm.setCreateResourceGroup(createResourceGroup)
        vm.setType(type as AzureVm.VmType )
        vm.setKnownImage(knownImage)
        vm.setSizeType(new AzureVMSizeType(sizeType))
        vm.setStoredURLImage(imageStored)
        vm.setPrimaryNetworkIp(primaryNetworkIp)
        vm.setNetworkType(netType as AzureVm.NetType)

        if(imagePublisher!=null && imageOffer!=null&&imageSKU!=null){
            AzureImage image = new AzureImage(imagePublisher,imageOffer,imageSKU,imageVersion)
            vm.setSpecificImage(image)
        }

        manager.createVirtualMachine(vm)

    }


}
