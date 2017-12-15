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
import com.microsoft.azure.storage.CloudStorageAccount
import com.microsoft.azure.storage.StorageException
import com.microsoft.azure.storage.blob.CloudBlobClient
import com.microsoft.azure.storage.blob.CloudBlobContainer
import com.microsoft.azure.storage.blob.CloudBlockBlob
import com.rundeck.plugins.azure.util.AzurePluginUtil

/**
 * Created by luistoledo on 11/15/17.
 */
@Plugin(name = AzureStorageDeleteStepPlugin.PROVIDER_NAME, service = ServiceNameConstants.WorkflowStep)
class AzureStorageDeleteStepPlugin  implements StepPlugin, Describable {
    public static final String PROVIDER_NAME = "azure-local-delete-step";
    public static final String PROVIDER_TITLE = "Azure / Storage / Delete"
    public static final String PROVIDER_DESCRIPTION ="Delete blob form Azure Storage Container"

    public static final String STORAGE_NAME = "storage"
    public static final String ACCESS_KEY = "key"
    public static final String CONTAINER_NAME = "containerName"
    public static final String BLOB_PATH = "path"

    final static Map<String, Object> renderingOptionsAuthentication = AzurePluginUtil.getRenderOpt("Credentials",false)
    final static Map<String, Object> renderingOptionsAuthenticationStorage = AzurePluginUtil.getRenderOpt("Credentials",false, false, true)
    final static Map<String, Object> renderingOptionsConfig = AzurePluginUtil.getRenderOpt("Configuration",false)

    static Description DESCRIPTION = DescriptionBuilder.builder()
            .name(PROVIDER_NAME)
            .title(PROVIDER_TITLE)
            .description(PROVIDER_DESCRIPTION)
            .property(PropertyUtil.string(STORAGE_NAME, "Storage Account", "Azure Storage Account", true,
            null,null,null, renderingOptionsAuthentication))
            .property(PropertyUtil.string(ACCESS_KEY, "Access Key", "Azure Storage Access Key", true,
            null,null,null, renderingOptionsAuthenticationStorage))
            .property(PropertyUtil.string(CONTAINER_NAME, "Container Name", "Container Name form the Azure Storage", false,
            null,null,null, renderingOptionsConfig))
            .property(PropertyUtil.string(BLOB_PATH, "Blob Path", "Blob Path. If the blob is on a subfolder, add the full path like `path/file.ext`", false,
            null,null,null, renderingOptionsConfig))
            .build()

    @Override
    Description getDescription() {
        return DESCRIPTION
    }

    @Override
    void executeStep(PluginStepContext context, Map<String, Object> configuration) throws StepException {
        String storageName=configuration.get(AzureStorageDeleteStepPlugin.STORAGE_NAME)
        String accessKeyStoragePath=configuration.get(AzureStorageDeleteStepPlugin.ACCESS_KEY)
        String containerName=configuration.get(AzureStorageDeleteStepPlugin.CONTAINER_NAME)
        String path=configuration.get(AzureStorageDeleteStepPlugin.BLOB_PATH)


        String accessKey = AzurePluginUtil.getPasswordFromKeyStorage(accessKeyStoragePath,context);

        String storageConnectionString = "DefaultEndpointsProtocol=http;AccountName=" + storageName + ";AccountKey=" + accessKey;

        CloudStorageAccount account = CloudStorageAccount.parse(storageConnectionString);
        CloudBlobClient serviceClient = account.createCloudBlobClient();
        CloudBlobContainer container = null
        try{
            container = serviceClient.getContainerReference(containerName)
        }catch(URISyntaxException| StorageException e){
            throw new IllegalArgumentException("Error getting the container Name");
        }

        CloudBlockBlob blob=null
        try{
            blob = container.getBlockBlobReference(path);
        }catch(URISyntaxException| StorageException e){
            throw new IllegalArgumentException("Error getting the blob");
        }

        blob.delete()

        println "Blob ${blob.getName()} deleted successfully"
    }
}
