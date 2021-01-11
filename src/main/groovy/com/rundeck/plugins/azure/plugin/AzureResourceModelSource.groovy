package com.rundeck.plugins.azure.plugin

import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.common.INodeSet
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.common.NodeSetImpl
import com.dtolabs.rundeck.core.resources.ResourceModelSource
import com.dtolabs.rundeck.core.resources.ResourceModelSourceException
import com.rundeck.plugins.azure.azure.AzureManager
import com.rundeck.plugins.azure.azure.AzureManagerBuilder
import com.rundeck.plugins.azure.azure.AzureNode
import com.rundeck.plugins.azure.azure.AzureNodeMapper

/**
 * Created by luistoledo on 11/6/17.
 */
class AzureResourceModelSource  implements ResourceModelSource {

    private Properties configuration;
    private AzureManager manager;

    AzureResourceModelSource(Properties configuration) {
        this.configuration = configuration
    }

    void setAzureManager(AzureManager manager){
        this.manager=manager
    }

    @Override
    INodeSet getNodes() throws ResourceModelSourceException {

        String clientId=configuration.getProperty(AzureResourceModelSourceFactory.CLIENT)
        String tenantId=configuration.getProperty(AzureResourceModelSourceFactory.TENANT)
        String subscriptionId=configuration.getProperty(AzureResourceModelSourceFactory.SUBSCRIPTION_ID)
        String key=configuration.getProperty(AzureResourceModelSourceFactory.KEY)
        String pfxCertificatePath=configuration.getProperty(AzureResourceModelSourceFactory.PFX_CERTIFICATE_PATH)
        String pfxCertificatePassword=configuration.getProperty(AzureResourceModelSourceFactory.PFX_CERTIFICATE_PASSWORD)
        String resourceGroup=configuration.getProperty(AzureResourceModelSourceFactory.RESOURCE_GROUP)
        boolean onlyRunningInstances=Boolean.parseBoolean(configuration.getProperty(AzureResourceModelSourceFactory.RUNNING_ONLY))
        String tagName=configuration.getProperty(AzureResourceModelSourceFactory.TAG_NAME)
        String tagValue=configuration.getProperty(AzureResourceModelSourceFactory.TAG_VALUE)
        String extraMapping=configuration.getProperty(AzureResourceModelSourceFactory.EXTRA_MAPPING)
        boolean useAzureTags=Boolean.parseBoolean(configuration.getProperty(AzureResourceModelSourceFactory.USE_AZURE_TAGS))

        boolean debug=Boolean.parseBoolean(configuration.getProperty(AzureResourceModelSourceFactory.DEBUG))

        if(key == null && pfxCertificatePath == null){
            throw new IllegalArgumentException("You must set the key or the certificate path in order to authenticate");
        }

        if(manager==null) {
            manager = AzureManagerBuilder.builder()
                    .clientId(clientId)
                    .tenantId(tenantId)
                    .subscriptionId(subscriptionId)
                    .key(key)
                    .pfxCertificatePath(pfxCertificatePath)
                    .pfxCertificatePassword(pfxCertificatePassword)
                    .resourceGroup(resourceGroup)
                    .onlyRunningInstances(onlyRunningInstances)
                    .tagName(tagName)
                    .tagValue(tagValue)
                    .debug(debug)
                    .useAzureTags(useAzureTags)
                    .build()
        }

        List<AzureNode> nodes = manager.listVms()

        final NodeSetImpl nodeSet = new NodeSetImpl();

        nodes.each{ azureNode ->
            final INodeEntry node = new NodeEntryImpl();

            AzureNodeMapper azureMapper= new AzureNodeMapper(azureNode,extraMapping)

            node.setAttributes(azureMapper.getAttributes())
            node.setTags(azureMapper.getTags())

            if (null != node) {
                nodeSet.putNode(node);
            }

        }


        return nodeSet
    }

}
