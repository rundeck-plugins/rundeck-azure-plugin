## Rundeck Azure Plugin
Azure Plugin integrates Rundeck with Azure Virtual Machines and Azure Storage. The plugin contains a Resource Model plugin,  an Execution Log Storage plugin, and others plugin steps like Create/Start/Stop Azure VMs. 


## Install

Build with `./gradlew build` and copy the `build/lib/azure-plugin-X.X.X.jar` to `$RDECK_BASE/libext` folder


## Resource Model Plugin

The resource model plugin provides the Azure VMs as nodes on a Rundeck Server.

### Credentials Settings
Settings related to the Azure connection

* **Client ID**: Azure Client ID.
* **Tenant ID**: Azure Tenant ID.
* **Subscription ID**: Azure Subscription ID.
* **Azure Access Key**: Azure Access Key.
* **Certificate Path**: (Optional) Azure certificate file path (if the access key is not defined). 
* **Certificate Password**: (Optional) Azure certificate Password (if the access key is not defined).


###  Other Settings:
Mapping and filter settings

* **Mapping Params**: Custom mapping settings. Property mapping definitions. Specify multiple mappings in the form "attributeName.selector=selector" or "attributeName.default=value", separated by ";"
* **Resource Group**:  Filter using resource group
* **Only Running Instances**: Filter for the "Running" instances. If false, all instances will be returned.

### Mapping

Map the Azure VM properties to Rundeck Node definition

#### Default Mapping
```
nodename.selector                   =    name
hostname.selector                   =    hostname
description.selector                =    short_description
osName.selector                     =    osName
osVersion.selector                  =    osVersion
osFamily.selector                   =    osFamily
username.selector                   =    username
region.selector                     =    azure_region
resourceGroup.selector              =    azure_resourceGroup
status.selector                     =    azure_status
id.selector                         =    azure_id
node-executor.selector              =    node_executor
file-copier.selector                =    file_copier
vmId.selector                       =    azure_vmId
tags.selector                       =    tags

image:type.selector                 =    azure_image_type
image:offer.selector                =    azure_image_offer
image:sku.selector                  =    azure_image_sku
image:version.selector              =    azure_image_version
osDisk:osType.selector              =    azure_osDisk_osType
osDisk:name.selector                =    azure_osDisk_name
osDisk:createOption.selector        =    azure_osDisk_createOption
osDisk:diskSizeGB.selector          =    azure_osDisk_diskSizeGB

netInterface:privateIp.selector     =    azure_netInterface_privateIp

plan:name.selector                  =    azure_plan_name
plan:product.selector               =    azure_plan_product
plan:publisher.selector             =    azure_plan_publisher

size:name.selector                              =    azure_size_name
size:numberOfCores.selector                     =    azure_size_numberOfCores
size:memoryInMB.selector                        =    azure_size_memoryInMB
size:maxDataDiskCount.selector                  =    azure_size_maxDataDiskCount
size:azure_size_resourceDiskSizeInMB.selector   =    azure_size_azure_size_resourceDiskSizeInMB

provisioningState:code.selector                 =    azure_provisioningState_code
provisioningState:displayStatus.selector        =    azure_provisioningState_displayStatus
provisioningState:message.selector              =    azure_provisioningState_message
provisioningState:time.selector                 =    azure_provisioningState_time

```
### Adding Tags from Azure VM Tags

You can add Rundeck's node tags using Azure VM tags.

For example, create an Azure VM tags like:

* Rundeck-Tags=sometag1,sometag2

`sometag1` and `sometag2` will be added as tags on Rundeck nodes


### Adding custom tags from Azure VM files

You can add extra tags using the azure fields available (right column on the default mapping). 

For example, adding extra tags based on  the VM resource group and status:

```
tags.selector=azure_resourceGroup,azure_status;
```

### Adding custom attribute based on Azure VM Tags

Also, you can add extra nodes attributes using Azure VM tags.

For example, creating the following tags on the Azure VM, you can map those tags to a rundeck node attribute:

* Rundeck-node-executor=winrm-exe
* Rundeck-file-copier=winrm-filecopier
* Rundeck-winrm-password-storage-path=keys/node/windows.password

As you see, the Azure VM tags must start with **Rundeck-**

Then to map those tags to nodes attribute use:

```
node-executor.selector=node-executor;
file-copier.selector=node-executor;
winrm-password-storage-path.selector=winrm-password-storage-path
```



                                 
## Execution Log Storage

The Execution Log Storage plugin uses Azure Storage to store execution log files, for backup or for a cluster environment behavior.

### Enable the plugin

Enable the ExecutionFileStorage provider named azure-storage in your `rundeck-config.properties` file:

`rundeck.execution.logs.fileStoragePlugin=azure-storage`

### Configuration
To configure the Azure Storage Account credentials you can set these property values:

* **storageAccount**: Azure Storage Account
* **accessKey**: Azure Storage Access Key
* **path**: The path in the bucket to store a log file. 

    You can use these expansion variables: 
    
     * `${job.execid}` = execution ID
     * `${job.project}` = project name
     * `${job.id}` = job UUID (or blank).
     * `${job.group}` = job group (or blank).
     * `${job.name}` = job name (or blank)
     
* **defaultEndpointProtocol**: Endpoint Protocol. Default Endpoint Protocol: http (default) or https
* **extraConnectionSettings**: Extra connection settings, see https://docs.microsoft.com/en-us/azure/storage/common/storage-configure-connection-string#store-a-connection-string

You can define the configuration values in `framework.properties` by prefixing the property name with the stem: `framework.plugin.ExecutionFileStorage.azure-storage`. Or in a project's `project.properties` file with the stem `project.plugin.ExecutionFileStorage.azure-storage`.

For example:

```
#storage.storageAccount and storage.accessKey
framework.plugin.ExecutionFileStorage.azure-storage.storageAccount=<ACCOUNT-NAME>
framework.plugin.ExecutionFileStorage.azure-storage.accessKey=<ACCESS-KEY>
framework.plugin.ExecutionFileStorage.azure-storage.defaultEndpointProtocol=https

#path to store the logs
framework.plugin.ExecutionFileStorage.azure-storage.path=logs/${job.project}/${job.execid}.log

```


## Managing Azure VM

This plugin contains basic operation for Azure VMs like list, create, start and stop Azure VMs. All the following workflow steps needs an authentication account like the one explained on the Resource Model Plugin


### List Azure VM
List the Azure VM on a Resource Group and/or Region

### Create an Azure VM
Create a new Azure VM. For selecting the image that the VM will be based on, you have the following options:

* Select the image form a *Known List*
* Find an image selecting the attributes: Publisher, Offer, SKU, version (for example, take a look at the following docs: [https://docs.microsoft.com/en-us/azure/virtual-machines/windows/cli-ps-findimage](https://docs.microsoft.com/en-us/azure/virtual-machines/windows/cli-ps-findimage)) 
* Passing the Image URL

### Start an Azure VM

Start an Azure VM using the VM name and Resource Group

### Stop an Azure VM
Stop an Azure VM using the VM name and Resource Group


## Azure Storage Operations

This plugin contains basic operation for Azure Storage, like list the blob files located on a container (within an Azure Storage Account), and copy/remove blobs files from/to the Rundeck Server.

### List blobs of a container
List the blobs files of a container inside an Azure Account

### Copy blobs from Azure Container to Rundek Server (or vice-versa)
Copy files from the Rundeck Server to an Azure Storage Container or from an Azure Storage Container to the Rundeck server.

To define if the source or destination is the local server or Azure, you must define a URI pattern like:

* file://path/file for local files
* Azure://container/path/file for blob files on Azure

### Removing blobs for a container

Delete blob files located on an Azure Storage Account Container



