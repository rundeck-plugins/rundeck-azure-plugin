package com.rundeck.plugins.azure.plugin

import com.dtolabs.rundeck.core.execution.workflow.steps.StepException
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.plugins.configuration.Describable
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.core.plugins.configuration.PropertyUtil
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.step.PluginStepContext
import com.dtolabs.rundeck.plugins.step.StepPlugin
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder
import com.microsoft.azure.management.resources.fluentcore.arm.Region
import com.rundeck.plugins.azure.azure.AzureManager
import com.rundeck.plugins.azure.azure.AzureManagerBuilder
import com.rundeck.plugins.azure.util.AzurePluginUtil
import groovy.json.JsonOutput
import org.apache.commons.collections.map.HashedMap

/**
 * Created by luistoledo on 11/21/17.
 */
@Plugin(name = AzureVmListPlugin.PROVIDER_NAME, service = ServiceNameConstants.WorkflowStep)
class AzureVmListPlugin implements StepPlugin, Describable {
    public static final String PROVIDER_NAME = "azure-vm-list-step";
    public static final String PROVIDER_TITLE = "Azure / VM / List"
    public static final String PROVIDER_DESCRIPTION ="List Azure Virtual Machines"

    //https://github.com/Azure/azure-sdk-for-java/blob/master/AUTH.md#using-applicationtokencredentials
    public static final String CLIENT = "client"
    public static final String TENANT = "tenant"
    public static final String SUBSCRIPTION_ID = "subscriptionId"
    public static final String KEY = "key"
    public static final String PFX_CERTIFICATE_PATH = "pfxCertificatePath"
    public static final String PFX_CERTIFICATE_PASSWORD = "pfxCertificatePassword"

    public static final String VM_RESOURCE_GROUP = "vmResourceGroup"
    public static final String VM_REGION = "vmRegion"
    public static final String RUNNING_ONLY = "onlyRunningInstances"

    final static Map<String, Object> renderingOptionsAuthentication = AzurePluginUtil.getRenderOpt("Credentials",false)
    final static Map<String, Object> renderingOptionsAuthenticationStorage = AzurePluginUtil.getRenderOpt("Credentials",false, false, true)
    final static Map<String, Object> renderingOptionsConfig = AzurePluginUtil.getRenderOpt("VM Properties",false)

    private AzureManager manager;

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
            .property(PropertyUtil.string(VM_RESOURCE_GROUP, "Resource Group", "Azure Resource Group.", true,
            null,null,null, renderingOptionsConfig))
            .property(PropertyUtil.bool(RUNNING_ONLY, "Only Running Instances",
            "Include Running state instances only. If false, all instances will be returned that match your " +
                    "filters.",
            false, "false", null,renderingOptionsConfig))
            .build()

    void setAzureManager(AzureManager manager){
        this.manager=manager
    }


    @Override
    Description getDescription() {
        return DESCRIPTION
    }

    @Override
    void executeStep(PluginStepContext context, Map<String, Object> configuration) throws StepException {

        String clientId=configuration.get(AzureVmListPlugin.CLIENT)
        String tenantId=configuration.get(AzureVmListPlugin.TENANT)
        String subscriptionId=configuration.get(AzureVmListPlugin.SUBSCRIPTION_ID)
        String keyPath=configuration.get(AzureVmListPlugin.KEY)
        String pfxCertificatePath=configuration.get(AzureVmListPlugin.PFX_CERTIFICATE_PATH)
        String pfxCertificatePassword=configuration.get(AzureVmListPlugin.PFX_CERTIFICATE_PASSWORD)

        if(keyPath == null && pfxCertificatePath == null){
            throw new IllegalArgumentException("You must set the key or the certificate path in order to authenticate");
        }

        String regionNameLabel=configuration.get(AzureVmListPlugin.VM_REGION)
        String resourceGroup=configuration.get(AzureVmListPlugin.VM_RESOURCE_GROUP)
        boolean runningOnly=Boolean.valueOf(configuration.get(AzureVmListPlugin.RUNNING_ONLY))


        Region region = Region.findByLabelOrName(regionNameLabel)

        if(region==null){
            throw new IllegalArgumentException("Region not found");
        }


        if(manager==null) {
            String key = AzurePluginUtil.getPasswordFromKeyStorage(keyPath,context);

            manager = AzureManagerBuilder.builder()
                    .clientId(clientId)
                    .tenantId(tenantId)
                    .subscriptionId(subscriptionId)
                    .key(key)
                    .pfxCertificatePath(pfxCertificatePath)
                    .pfxCertificatePassword(pfxCertificatePassword)
                    .resourceGroup(resourceGroup)
                    .onlyRunningInstances(runningOnly)
                    .region(region)
                    .build()
        }

        List list = new ArrayList<>()

        manager.listVms().each{vm->
            list.add(vm.toMap())
        }

        def json = JsonOutput.toJson(list)

        Map<String, String> meta = new HashMap<>();
        meta.put("content-data-type", "application/json");
        context.getExecutionContext().getExecutionListener().log(2, json, meta);

    }
}
