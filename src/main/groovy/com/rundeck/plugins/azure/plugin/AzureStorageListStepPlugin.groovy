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
import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.models.BlobItem
import com.azure.storage.blob.models.ListBlobsOptions
import com.rundeck.plugins.azure.azure.AzureBlobStorageClientFactory
import com.rundeck.plugins.azure.util.AzurePluginUtil
import groovy.json.JsonOutput

import java.time.format.DateTimeFormatter

/**
 * Created by luistoledo on 11/15/17.
 */
@Plugin(name = AzureStorageListStepPlugin.PROVIDER_NAME, service = ServiceNameConstants.WorkflowStep)
class AzureStorageListStepPlugin implements StepPlugin, Describable {
    public static final String PROVIDER_NAME = "azure-local-list-step";
    public static final String PROVIDER_TITLE = "Azure / Storage / List"
    public static final String PROVIDER_DESCRIPTION ="List blobs form Azure Storage Container"

    public static final String STORAGE_NAME = "storage"
    public static final String ACCESS_KEY = "key"
    public static final String CONTAINER_NAME = "containerName"
    public static final String RECURSIVE = "recursive"

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
            .property(PropertyUtil.bool(RECURSIVE, "Recursive", "Show content of subfolders", false,
            "false",null, renderingOptionsConfig))
            .build()


    @Override
    Description getDescription() {
        return DESCRIPTION
    }

    @Override
    void executeStep(PluginStepContext context, Map<String, Object> configuration) throws StepException {

        String storageName=configuration.get(AzureStorageListStepPlugin.STORAGE_NAME)
        String accessKeyStoragePath=configuration.get(AzureStorageListStepPlugin.ACCESS_KEY)
        String containerName=configuration.get(AzureStorageListStepPlugin.CONTAINER_NAME)
        boolean recursive=Boolean.valueOf(configuration.get(AzureStorageListStepPlugin.RECURSIVE))

        String accessKey = AzurePluginUtil.getPasswordFromKeyStorage(accessKeyStoragePath,context);

        BlobContainerClient container
        try{
            container = AzureBlobStorageClientFactory.buildContainerClient(storageName, accessKey, containerName, "http")
        }catch(IllegalArgumentException e){
            throw new IllegalArgumentException("Error getting the container '${containerName}' for storage account '${storageName}': ${e.message}", e);
        }

        List list = new ArrayList()

        container.listBlobsByHierarchy("/").each { BlobItem object ->
            if(Boolean.TRUE.equals(object.isPrefix())){
                list.add([name:object.getName(),
                          container:container.getBlobContainerName(),
                          uri:"",
                          lastModified:"",
                          length:"",
                          type:"FOLDER",
                          contentType:""])

                if(recursive){
                    list.addAll(listBlobs(container, object.getName()))
                }

            }else{
                list.add(printBlob(container, object))
            }

        }

        def json = JsonOutput.toJson(list)

        Map<String, String> meta = new HashMap<>();
        meta.put("content-data-type", "application/json");
        context.getExecutionContext().getExecutionListener().log(2, json, meta);
    }


    def listBlobs = { BlobContainerClient container, String prefix ->

        List list = new ArrayList()
        container.listBlobsByHierarchy("/", new ListBlobsOptions().setPrefix(prefix), null).each { BlobItem item ->
            if(Boolean.TRUE.equals(item.isPrefix())){
                list.add([name:item.getName(),
                          container:container.getBlobContainerName(),
                          uri:"",
                          lastModified:"",
                          length:"",
                          type:"FOLDER",
                          contentType:""])
                list.addAll(listBlobs(container, item.getName()))
            }else{
                list.add(printBlob(container, item))
            }
        }

        return list
    }

    def printBlob = { BlobContainerClient container, BlobItem blob ->
        def properties = blob.getProperties()

        return [name:blob.getName(),
                container:container.getBlobContainerName(),
                uri:container.getBlobClient(blob.getName()).getBlobUrl(),
                lastModified:properties.getLastModified()?.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")),
                length:properties.getContentLength(),
                type:properties.getBlobType(),
                contentType:properties.getContentType()]

    }
    def printMetadata ={meta->
        StringBuffer buffer = new StringBuffer()

        meta.each{key,value->
            buffer.append("${key}: ${value}")
        }

        return buffer.toString()
    }

}
