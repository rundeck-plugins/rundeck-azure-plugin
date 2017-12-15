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
import com.rundeck.plugins.azure.plugin.files.EndpointHandler
import com.rundeck.plugins.azure.plugin.files.URIParser
import com.rundeck.plugins.azure.plugin.files.endpoints.AzureEndpoint
import com.rundeck.plugins.azure.plugin.files.endpoints.FileEndpoint
import com.rundeck.plugins.azure.util.AzurePluginUtil

import java.util.logging.Logger
import org.apache.commons.net.io.Util;


/**
 * Created by luistoledo on 11/14/17.
 */
@Plugin(name = AzureStorageCopyStepPlugin.PROVIDER_NAME, service = ServiceNameConstants.WorkflowStep)
class AzureStorageCopyStepPlugin implements StepPlugin, Describable {

    static final Logger logger = Logger.getLogger(AzureStorageCopyStepPlugin.getName());

    public static final String PROVIDER_NAME = "azure-local-copy-step";
    public static final String PROVIDER_TITLE = "Azure / Storage / Copy"
    public static final String PROVIDER_DESCRIPTION ="Copy or get objects form Azure Storage to/from Rundeck Server"

    public static final String STORAGE_NAME = "storage"
    public static final String ACCESS_KEY = "key"
    public static final String SOURCE = "source"
    public static final String DESTINATION = "destination"

    private static final int DEBUG_LEVEL = 4


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
            .property(PropertyUtil.string(SOURCE, "Source", "Azure URI or local path (azure://container/path/file.ext , file://some/path/file.ext)", true,
            null,null,null, renderingOptionsConfig))
            .property(PropertyUtil.string(DESTINATION, " Destination", "Azure URI or local path (azure://container/path/file.ext , file://some/path/file.ext)", true,
            null,null,null, renderingOptionsConfig))
            .build()


    @Override
    Description getDescription() {
        return DESCRIPTION
    }

    @Override
    void executeStep(PluginStepContext context, Map<String, Object> configuration) throws StepException {


        EndpointHandler sourceEndpoint=null;
        EndpointHandler destEndpoint=null;

        String storageName=configuration.get(AzureStorageCopyStepPlugin.STORAGE_NAME)
        String accessKeyStoragePath=configuration.get(AzureStorageCopyStepPlugin.ACCESS_KEY)
        String sourcePath=configuration.get(AzureStorageCopyStepPlugin.SOURCE)
        String destinationPath=configuration.get(AzureStorageCopyStepPlugin.DESTINATION)

        URIParser sourceURL = new URIParser(sourcePath)
        URIParser destURL = new URIParser(destinationPath)

        println sourceURL.getFile()

        // Check SOURCE is not a directory
        if (AzurePluginUtil.isDirectory(sourceURL.getFile()))
            throw new IllegalArgumentException("Source must not be a directory");

        // Check dest path does not have wildcards.
        if (AzurePluginUtil.hasWildcards(sourceURL.getFile()))
            throw new IllegalArgumentException("Wildcards are not allowed on source URL");


        // Check dest path does not have wildcards.
        if (AzurePluginUtil.hasWildcards(destURL.getFile()))
            throw new IllegalArgumentException("Wildcards are not allowed on destination URL");

        String accessKey = AzurePluginUtil.getPasswordFromKeyStorage(accessKeyStoragePath,context);

        boolean debug=context.getExecutionContext().getLoglevel()==DEBUG_LEVEL?true:false;
        AzurePluginUtil.printMessage(" Source => " + sourceURL.toString(), debug, "info");
        AzurePluginUtil.printMessage(" Destination => " + destURL.toString(), debug, "info");
        AzurePluginUtil.printMessage("---------------------------------------------------", debug, "info");

        sourceEndpoint = createEndpointHandler(sourceURL, storageName, accessKey)
        destEndpoint = createEndpointHandler(destURL, storageName, accessKey)


        if(!sourceEndpoint.fileExists(sourceURL.getFile())){
            throw new IllegalArgumentException("Source File doesn't exists");

        }

        String destinationFile = destURL.getFile()
        // Check SOURCE is not a directory
        if (!AzurePluginUtil.isDirectory(destURL.getFile())){
            destinationFile = destURL.getPath() +"/"+ sourceURL.getFileName()
        }

        processCopyFile(sourceEndpoint,destEndpoint,sourceURL.getFile(),destinationFile);


    }

    private void processCopyFile(EndpointHandler sourceEndpoint, EndpointHandler destEndpoint, String sourcePath, String destinationPath) throws IOException {

        OutputStream destOutputStream = destEndpoint.newTransferOutputStream(destinationPath);
        InputStream sourceInputStream= sourceEndpoint.newTransferInputStream(sourcePath);


        Util.copyStream(sourceInputStream, destOutputStream);

        sourceInputStream.close();
        destOutputStream.flush();
        destOutputStream.close();

        sourceEndpoint.finishTransferTransaction();
        destEndpoint.finishTransferTransaction();


    }

    private EndpointHandler createEndpointHandler(URIParser url, String storageName, String accessKey) throws IOException {

        switch (url.getProtocol().toLowerCase()) {
            case "file":
                return FileEndpoint.createEndpointHandler(url);

            case "azure":
                return AzureEndpoint.createEndpointHandler(url, storageName, accessKey);


            default:
                throw new IllegalArgumentException("Invalid protocol specified in source URL: " + url.getProtocol());
        }
    }


}
