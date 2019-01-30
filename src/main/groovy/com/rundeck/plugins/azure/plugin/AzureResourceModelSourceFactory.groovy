package com.rundeck.plugins.azure.plugin

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException
import com.dtolabs.rundeck.core.plugins.configuration.Describable
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.core.plugins.configuration.PropertyUtil
import com.dtolabs.rundeck.core.resources.ResourceModelSource
import com.dtolabs.rundeck.core.resources.ResourceModelSourceFactory
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder
import com.rundeck.plugins.azure.util.AzurePluginUtil
/**
 * Created by luistoledo on 11/3/17.
 */
@Plugin(name = AzureResourceModelSourceFactory.PROVIDER_NAME, service = ServiceNameConstants.ResourceModelSource)
@PluginDescription(title = AzureResourceModelSourceFactory.PROVIDER_TITLE, description = AzureResourceModelSourceFactory.PROVIDER_DESCRIPTION)
class AzureResourceModelSourceFactory implements ResourceModelSourceFactory,Describable {


    private Framework framework;

    public static final String PROVIDER_NAME = "azure-resource-model";
    public static final String PROVIDER_TITLE = "Azure / Resource Model"
    public static final String PROVIDER_DESCRIPTION ="Get nodes from Azure Virtual Machines. Add custom tags on Azuere VMS using the `Rundeck-Tags` (comma separated). Add custom attributes adding the tag `Rundeck-customattribute` on the VM   "

    //https://github.com/Azure/azure-sdk-for-java/blob/master/AUTH.md#using-applicationtokencredentials
    public static final String CLIENT = "client"
    public static final String TENANT = "tenant"
    public static final String SUBSCRIPTION_ID = "subscriptionId"
    public static final String KEY = "key"
    public static final String PFX_CERTIFICATE_PATH = "pfxCertificatePath"
    public static final String PFX_CERTIFICATE_PASSWORD = "pfxCertificatePassword"

    //mapping
    public static final String EXTRA_MAPPING = "extraMapping"

    //filters
    public static final String RESOURCE_GROUP = "resourceGroup"
    public static final String TAG_NAME = "tagName"
    public static final String TAG_VALUE = "tagValue"
    public static final String RUNNING_ONLY = "onlyRunningInstances"

    public static final String DEBUG = "debugVm"

    final static Map<String, Object> renderingOptionsAuthentication = AzurePluginUtil.getRenderOpt("Credentials",false)
    final static Map<String, Object> renderingOptionsAuthenticationPassword = AzurePluginUtil.getRenderOpt("Credentials",false, true)
    final static Map<String, Object> renderingOptionsConfig = AzurePluginUtil.getRenderOpt("Configuration",false)

    AzureResourceModelSourceFactory(Framework framework) {
        this.framework = framework
    }



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
            null,null,null, renderingOptionsAuthenticationPassword))
            .property(PropertyUtil.string(PFX_CERTIFICATE_PATH, "Certificate Path", "Azure certificate file path.", false,
            null,null,null, renderingOptionsAuthentication))
            .property(PropertyUtil.string(PFX_CERTIFICATE_PASSWORD, "Certificate Password", "Azure certificate Password.", false,
            null,null,null, renderingOptionsAuthentication))
            .property(PropertyUtil.string(EXTRA_MAPPING, "Mapping Params", "Property mapping definitions. Specify multiple mappings in the form \"attributeName.selector=selector\" or \"attributeName.default=value\", separated by \";\"", false,
            "tags.selector=azure_status",null,null, renderingOptionsConfig))
            .property(PropertyUtil.string(RESOURCE_GROUP, "Resource Group", "Filter using resource group", false,
            null,null,null, renderingOptionsConfig))
            .property(PropertyUtil.string(TAG_NAME, "Tag Name", "Filter using tag name (this value will be ignored if either Tag Name or Tag Value is empty)", false,
            null,null,null, renderingOptionsConfig))
            .property(PropertyUtil.string(TAG_VALUE, "Tag Value", "Filter using tag value (this value will be ignored if either Tag Name or Tag Value is empty)", false,
            null,null,null, renderingOptionsConfig))
            .property(PropertyUtil.bool(RUNNING_ONLY, "Only Running Instances",
            "Include Running state instances only. If false, all instances will be returned that match your " +
                    "filters.",
            false, "false", null,renderingOptionsConfig))

            .property(PropertyUtil.bool(DEBUG, "Debug VM info",
            "Get the VM data on rundeck's log",
            false, "false", null,renderingOptionsConfig))
            .build()

    @Override
    Description getDescription() {
        return DESCRIPTION
    }

    @Override
    ResourceModelSource createResourceModelSource(Properties configuration) throws ConfigurationException {
        final AzureResourceModelSource resource = new AzureResourceModelSource(configuration)

        return resource
    }
}
