package com.rundeck.plugins.azure.plugin

import com.dtolabs.rundeck.core.dispatcher.DataContextUtils
import com.dtolabs.rundeck.core.logging.ExecutionFileStorageException
import com.dtolabs.rundeck.core.logging.ExecutionMultiFileStorage
import com.dtolabs.rundeck.core.logging.MultiFileStorageRequest
import com.dtolabs.rundeck.core.logging.MultiFileStorageRequestErrors
import com.dtolabs.rundeck.core.logging.StorageFile
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.descriptions.SelectValues
import com.dtolabs.rundeck.plugins.logging.ExecutionFileStoragePlugin
import com.microsoft.azure.management.resources.fluentcore.arm.models.HasManager
import com.microsoft.azure.storage.CloudStorageAccount
import com.microsoft.azure.storage.blob.CloudBlobClient
import com.microsoft.azure.storage.blob.CloudBlobContainer
import com.microsoft.azure.storage.blob.CloudBlockBlob
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by luistoledo on 11/14/17.
 */

/**
 * {@link ExecutionFileStoragePlugin} that stores files to Azure Storage.
 */
@Plugin(service = ServiceNameConstants.ExecutionFileStorage, name = AzureFileStoragePlugin.PROVIDER_NAME)
@PluginDescription(title = AzureFileStoragePlugin.PROVIDER_TITLE, description = AzureFileStoragePlugin.PROVIDER_DESCRIPTION)
class AzureFileStoragePlugin implements ExecutionFileStoragePlugin, ExecutionMultiFileStorage{

    static final Logger logger = Logger.getLogger(AzureFileStoragePlugin.class.getName());

    public static final String PROVIDER_NAME = "azure-storage";
    public static final String PROVIDER_TITLE = "Azure / Execution Log Storage"
    public static final String PROVIDER_DESCRIPTION ="Stores log files into an Azure Storage"

    public static final String META_EXECID = "execid";
    public static final String META_PROJECT = "project";
    public static final String _PREFIX_META = "rundeck.";
    public static final String META_USERNAME = "username";
    public static final String META_URL = "url";
    public static final String META_SERVERURL = "serverUrl";
    public static final String META_SERVER_UUID = "serverUUID"



    @PluginProperty(title = "Storage Account", description = "Azure Storage Account")
    private String storageAccount;

    @PluginProperty(title = "Access Key", description = "Azure Storage Access Key")
    private String accessKey;

    @PluginProperty(title = "Endpoint Protocol", description = "Default Endpoint Protocol: http or https ", defaultValue = "http")
    private String defaultEndpointProtocol

    @PluginProperty(title = "Extra connection string settings", description = "Extra connection settings, see https://docs.microsoft.com/en-us/azure/storage/common/storage-configure-connection-string#store-a-connection-string")
    private String extraConnectionSettings

    @PluginProperty(
            title = "Path",
            required = true,
            description = "The path in the bucket to store a log file. \n\nYou can use these expansion variables: \n\n * `\${job.execid}` = execution ID\n * `\${job.project}` = project name\n  * `\${job.id}` = job UUID (or blank).\n * `\${job.group}` = job group (or blank).\n * `\${job.name}` = job name (or blank).\n",
            defaultValue = "project/\${job.project}/\${job.execid}"
    )
    private String path

    @PluginProperty(
        title = "Container Name",
        description = "(Optional) Define the container name where the logs will be saved. If set, the whole expanded `path` is used as the blob name inside this container. If not set, the first segment of `path` is used as the container name and the rest as the blob name. Eg: If the path=\"project/\${job.project}/\${job.execid}\" then the container will be \"project\" and the blob \"<project>/<execid>.<ext>\""
    )
    protected String containerName = null

    Map<String, ?> context;
    CloudBlobClient serviceClient
    CloudBlobContainer container
    String expandedPath;
    /**
     * Expanded path of the log files inside the container, without the file type extension
     */
    String blobPath;

    AzureFileStoragePlugin() {
        super()
    }

    @Override
    void initialize(Map<String, ?> context) {

        this.context = context;

        if ( null == this.storageAccount) {
            throw new IllegalArgumentException("Azure Storage Account must be configured.");
        }

        if ( null == this.accessKey) {
            throw new IllegalArgumentException("Azure Storage Access Key must be configured.");
        }

        if (null == this.path || "".equals(this.path.trim())) {
            throw new IllegalArgumentException("path was not set");
        }
        if (!this.path.contains("\${job.execid}") && !this.path.endsWith("/")) {
            throw new IllegalArgumentException("path must contain \${job.execid} or end with /");
        }

        String configPath= this.path;
        if (!configPath.contains("\${job.execid}") && configPath.endsWith("/")) {
            configPath = path + "/\${job.execid}";
        }

        expandedPath = expandPath(configPath, context);
        if (null == expandedPath || "".equals(expandedPath.trim())) {
            throw new IllegalArgumentException("expanded value of path was empty");
        }
        if (expandedPath.endsWith("/")) {
            throw new IllegalArgumentException("expanded value of path must not end with /");
        }

        String storageConnectionString = "DefaultEndpointsProtocol="+defaultEndpointProtocol+";AccountName=" + this.storageAccount+ ";AccountKey=" + this.accessKey;

        if(extraConnectionSettings){
            storageConnectionString = storageConnectionString + ";" + extraConnectionSettings
        }

        CloudStorageAccount account = CloudStorageAccount.parse(storageConnectionString);
        serviceClient = account.createCloudBlobClient();

        resolveContainerAndBlobPath()

        container = serviceClient.getContainerReference(containerName)
        container.createIfNotExists()
    }

    /**
     * Resolves the container name and the blob path from the expanded path.
     * If containerName is configured, the whole expanded path is the blob path,
     * otherwise the first segment of the expanded path is the container name and the rest is the blob path.
     */
    protected void resolveContainerAndBlobPath() {
        if (this.containerName) {
            blobPath = expandedPath
        } else {
            int separator = expandedPath.indexOf("/")
            this.containerName = expandedPath.substring(0, separator)
            blobPath = expandedPath.substring(separator + 1)
        }
        // Container name must be lower case.
        this.containerName = this.containerName.toLowerCase()
    }

    /**
     * @param context execution context
     * @return true if the given context has the expanded path of the log files in the `outputfilepath` variable
     */
    protected static boolean isImportedExecution(Map<String, ?> context){
        return context != null && context.get("isRemoteFilePath") != null && context.get("isRemoteFilePath") == "true"
    }

    @Override
    boolean isAvailable(String filetype) throws ExecutionFileStorageException {
        try {
            return findStoredBlobFile(filetype) != null
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage());
            logger.log(Level.FINE, e.getMessage(), e);
            throw new ExecutionFileStorageException(e.getMessage(), e);
        }

        return false
    }

    @Override
    boolean store(String filetype, InputStream stream, long length, Date lastModified) throws IOException, ExecutionFileStorageException {

        try {
            CloudBlockBlob blob = getBlobFile(filetype)
            blob.setMetadata(createObjectMetadata())
            blob.upload(stream, length);
            return true

        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage());
            logger.log(Level.FINE, e.getMessage(), e);
            throw new ExecutionFileStorageException(e.getMessage(), e);
        }

        return false
    }

    @Override
    boolean retrieve(String filetype, OutputStream stream) throws IOException, ExecutionFileStorageException {
        try {
            CloudBlockBlob blob = findStoredBlobFile(filetype)
            if (blob == null) {
                blob = getBlobFile(filetype)
            }
            blob.download(stream)
            return true
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage());
            logger.log(Level.FINE, e.getMessage(), e);
            throw new ExecutionFileStorageException(e.getMessage(), e);
        }

        return false
    }

    @Override
    void storeMultiple(MultiFileStorageRequest files) throws IOException, ExecutionFileStorageException {
        Set<String> availableFiletypes = files.getAvailableFiletypes();
        logger.log(
                Level.FINE,
                "Storing multiple files to Azure bucket {0} filetypes: {1}",
                this.storageAccount,
                availableFiletypes
        );
        availableFiletypes.each {fileType ->
            StorageFile storageFile = files.getStorageFile(fileType);
            boolean success;
            try {
                success = store(
                        fileType,
                        storageFile.getInputStream(),
                        storageFile.getLength(),
                        storageFile.getLastModified()
                );

                files.storageResultForFiletype(fileType, success);
            } catch (ExecutionFileStorageException e) {
                if (files instanceof MultiFileStorageRequestErrors) {
                    MultiFileStorageRequestErrors errors = (MultiFileStorageRequestErrors) files;
                    errors.storageFailureForFiletype(fileType, e.getMessage());
                } else {
                    logger.log(Level.SEVERE, e.getMessage());
                    logger.log(Level.FINE, e.getMessage(), e);
                    files.storageResultForFiletype(fileType, false);
                }
            }
        }
    }

    /**
     * Expands the path format using the context data
     *
     * @param pathFormat format
     * @param context context data
     *
     * @return expanded path
     */
    static String expandPath(String pathFormat, Map<String, ?> context) {
        String result = pathFormat.replaceAll("^/+", "");

        if(isImportedExecution(context))
            result = String.valueOf(context.get("outputfilepath").toString())
        else if (null != context) {
            result = DataContextUtils.replaceDataReferences(
                    result,
                    DataContextUtils.addContext("job", stringMap(context), new HashMap<>()),
                    null,
                    false,
                    true
            );
        }
        result = result.replaceAll("/+", "/");

        return result;
    }


    private static Map<String, String> stringMap(final Map<String, ?> context) {
        HashMap<String, String> result = new HashMap<>();
        for (String s : context.keySet()) {
            Object o = context.get(s);
            if (o != null) {
                result.put(s, o.toString());
            }
        }
        return result;
    }

    /**
     * Blob name for the file type. For imported executions the expanded path is the stored
     * `outputfilepath`, so this resolves to the same blob the original execution was written to.
     *
     * @param fileType file type extension
     * @return blob name
     */
    String getFileName(String fileType){
        return blobPath + ".${fileType}"
    }

    /**
     * Blob name used by previous versions of the plugin, which ignored the configured path.
     * Kept so log files stored before the path was honored can still be retrieved.
     *
     * @param fileType file type extension
     * @return legacy blob name: project/execid.fileType
     */
    String getLegacyFileName(String fileType){
        String executionId=context.get(META_EXECID)
        String project=context.get(META_PROJECT)
        return "${project}/${executionId}.${fileType}"
    }

    /**
     * Blob name previous versions of the plugin used to read imported executions: the stored
     * `outputfilepath` without its first segment, lower case.
     * Kept so imported executions whose logs were stored by a previous version can still be retrieved.
     *
     * @param fileType file type extension
     * @return legacy blob name for an imported execution
     */
    String getLegacyImportedFileName(String fileType){
        return expandedPath.substring(expandedPath.indexOf("/")).toLowerCase() + ".${fileType}"
    }



    /**
     * Metadata keys from the Execution context that will be stored as User Metadata in the S3 Object
     */
    private static final String[] STORED_META = [META_EXECID, META_USERNAME,
        META_PROJECT, META_URL, META_SERVERURL,
        META_SERVER_UUID]

    def createObjectMetadata ={
        HashMap<String,String> map = new HashMap<>()
        STORED_META.each { s->
            Object v = context.get(s)
            if (null != v) {
                map.put(s,v.toString())
            }
        }
        return map;
    }

    void setStorageAccount(String storageAccount) {
        this.storageAccount = storageAccount
    }

    void setAccessKey(String accessKey) {
        this.accessKey = accessKey
    }

    void setPath(String path) {
        this.path = path
    }

    CloudBlockBlob getBlobFile(String fileType){
        String fileName=getFileName(fileType)
        CloudBlockBlob blob = container.getBlockBlobReference(fileName);

        return blob
    }

    /**
     * Finds an existing blob for the file type, at the configured path or at the location used by
     * previous versions of the plugin: the legacy project/execid location, or for imported executions
     * the previous imported lookup.
     *
     * @param fileType file type extension
     * @return the existing blob, or null if it isn't stored in either location
     */
    CloudBlockBlob findStoredBlobFile(String fileType){
        CloudBlockBlob blob = getBlobFile(fileType)
        if (blob.exists()) {
            return blob
        }
        String legacyFileName = isImportedExecution(this.context) ?
                getLegacyImportedFileName(fileType) :
                getLegacyFileName(fileType)
        if (legacyFileName == getFileName(fileType)) {
            return null
        }
        CloudBlockBlob legacyBlob = container.getBlockBlobReference(legacyFileName)
        return legacyBlob.exists() ? legacyBlob : null
    }

    @Override
    public String getConfiguredPathTemplate(){
        return this.path;
    }
}
