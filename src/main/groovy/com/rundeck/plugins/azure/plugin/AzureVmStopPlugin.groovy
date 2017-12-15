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
import com.rundeck.plugins.azure.azure.AzureManager
import com.rundeck.plugins.azure.azure.AzureManagerBuilder
import com.rundeck.plugins.azure.util.AzurePluginUtil
import groovy.json.JsonOutput

/**
 * Created by luistoledo on 11/21/17.
 */
@Plugin(name = AzureVmStopPlugin.PROVIDER_NAME, service = ServiceNameConstants.WorkflowStep)
class AzureVmStopPlugin  implements StepPlugin, Describable {
    public static final String PROVIDER_NAME = "azure-vm-stop-step";
    public static final String PROVIDER_TITLE = "Azure / VM / Stop"
    public static final String PROVIDER_DESCRIPTION ="Stop an Azure Virtual Machine"

    //https://github.com/Azure/azure-sdk-for-java/blob/master/AUTH.md#using-applicationtokencredentials
    public static final String CLIENT = "client"
    public static final String TENANT = "tenant"
    public static final String SUBSCRIPTION_ID = "subscriptionId"
    public static final String KEY = "key"
    public static final String PFX_CERTIFICATE_PATH = "pfxCertificatePath"
    public static final String PFX_CERTIFICATE_PASSWORD = "pfxCertificatePassword"

    public static final String VM_RESOURCE_GROUP = "vmResourceGroup"
    public static final String VM_NAME = "VmName"

    public static final String VM_STOP_ASYNC="StopAsync"

    final static Map<String, Object> renderingOptionsAuthentication = AzurePluginUtil.getRenderOpt("Credentials",false)
    final static Map<String, Object> renderingOptionsAuthenticationStorage = AzurePluginUtil.getRenderOpt("Credentials",false, false, true)
    final static Map<String, Object> renderingOptionsConfig = AzurePluginUtil.getRenderOpt("VM Properties",false)

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
            .property(PropertyUtil.string(PFX_CERTIFICATE_PASSWORD, "Certificate Password", "Azure certificate Password.", false,
            null,null,null, renderingOptionsAuthentication))
            .property(PropertyUtil.string(VM_RESOURCE_GROUP, "Resource Group", "Azure Resource Group.", true,
            null,null,null, renderingOptionsConfig))
            .property(PropertyUtil.string(VM_NAME, "Name", "Azure VM Name.", true,
            null,null,null, renderingOptionsConfig))
            .property(PropertyUtil.bool(VM_STOP_ASYNC,"Stop asynchronously ?","Will stop the VM asynchronously ",
            false,"false",null,renderingOptionsConfig))
            .build()

    @Override
    Description getDescription() {
        return DESCRIPTION
    }

    @Override
    void executeStep(PluginStepContext context, Map<String, Object> configuration) throws StepException {

        String clientId=configuration.get(AzureVmStopPlugin.CLIENT)
        String tenantId=configuration.get(AzureVmStopPlugin.TENANT)
        String subscriptionId=configuration.get(AzureVmStopPlugin.SUBSCRIPTION_ID)
        String keyPath=configuration.get(AzureVmStopPlugin.KEY)
        String pfxCertificatePath=configuration.get(AzureVmStopPlugin.PFX_CERTIFICATE_PATH)
        String pfxCertificatePassword=configuration.get(AzureVmStopPlugin.PFX_CERTIFICATE_PASSWORD)

        if(keyPath == null && pfxCertificatePath == null){
            throw new IllegalArgumentException("You must set the key or the certificate path in order to authenticate");
        }

        String name=configuration.get(AzureVmStopPlugin.VM_NAME)
        String resourceGroup=configuration.get(AzureVmStopPlugin.VM_RESOURCE_GROUP)

        String key = AzurePluginUtil.getPasswordFromKeyStorage(keyPath,context);

        Boolean async = Boolean.valueOf(configuration.get(AzureVmStopPlugin.VM_STOP_ASYNC))


        AzureManager manager = AzureManagerBuilder.builder()
                .clientId(clientId)
                .tenantId(tenantId)
                .subscriptionId(subscriptionId)
                .key(key)
                .pfxCertificatePath(pfxCertificatePath)
                .pfxCertificatePassword(pfxCertificatePassword)
                .resourceGroup(resourceGroup)
                .build()

        Map<String, String> meta = new HashMap<>();
        meta.put("content-data-type", "application/json");

        Date t1 = new Date();

        def log = new ArrayList()
        log.add(["date":t1.format("yyyy/MM/dd HH:mm"),"info":"Stopping VM"])

        context.getExecutionContext()
                .getExecutionListener()
                .log(2,JsonOutput.toJson(log),meta);


        manager.stopVm(name,async)

        Date t2 = new Date();

        log.clear()
        log.add(["date":t2.format("yyyy/MM/dd HH:mm"),"info":"VM stopped after ${((t2.getTime() - t1.getTime()) / 1000)} seconds"])

        context.getExecutionContext()
                .getExecutionListener()
                .log(2,JsonOutput.toJson(log),meta);

    }
}
