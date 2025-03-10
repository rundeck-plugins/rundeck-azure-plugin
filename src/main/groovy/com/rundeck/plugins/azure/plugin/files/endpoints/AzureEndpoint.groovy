package com.rundeck.plugins.azure.plugin.files.endpoints

import com.microsoft.azure.storage.CloudStorageAccount
import com.microsoft.azure.storage.blob.CloudBlobClient
import com.microsoft.azure.storage.blob.CloudBlobContainer
import com.microsoft.azure.storage.blob.CloudBlockBlob
import com.rundeck.plugins.azure.plugin.files.EndpointHandler
import com.rundeck.plugins.azure.plugin.files.URIParser

/**
 * Created by luistoledo on 11/14/17.
 */
class AzureEndpoint {
    public static EndpointHandler createEndpointHandler(final URIParser url, String storageName, String accessKey, String defaultEndpointsProtocol) throws IOException {

        String storageConnectionString = "DefaultEndpointsProtocol=" + defaultEndpointsProtocol + ";AccountName=" + storageName+ ";AccountKey=" + accessKey;

        String containerName = url.getHost()

        CloudStorageAccount account = CloudStorageAccount.parse(storageConnectionString);
        CloudBlobClient serviceClient = account.createCloudBlobClient();

        CloudBlobContainer container = serviceClient.getContainerReference(containerName)
        container.createIfNotExists()

        OutputStream outputStream=null
        File tempFile=null
        String destinationPath=null

        boolean doUploading=false


        return new EndpointHandler() {
            @Override
            List<String> listFiles(String path) throws IOException {
                List<String> list = new ArrayList<>()
                container.listBlobs().each {blog->
                    list.add(blog.getUri().path)
                }
                return list
            }

            @Override
            InputStream newTransferInputStream(String path) throws IOException {
                return download(path)
            }

            @Override
            OutputStream newTransferOutputStream(String path) throws IOException {
                destinationPath=path
                tempFile = File.createTempFile("azure-transfer", "tmp", null);
                outputStream = new BufferedOutputStream(new FileOutputStream(tempFile.getAbsolutePath()));

                doUploading=true

                return outputStream
            }

            @Override
            boolean finishTransferTransaction() throws IOException {
                if(doUploading){
                    upload()
                }
                return false
            }

            @Override
            boolean deleteFile(String path) throws IOException {
                return false
            }

            @Override
            void disconnect() throws IOException {

            }

            @Override
            boolean fileExists(String path) throws IOException {
                String fileName = path.substring(1,path.length())
                CloudBlockBlob blob = container.getBlockBlobReference(fileName);
                return blob.exists()
            }

            boolean upload() throws IOException {

                String fileName = destinationPath.substring(1,destinationPath.length())

                tempFile=new File(tempFile.getAbsolutePath())

                CloudBlockBlob blob = container.getBlockBlobReference(fileName);
                blob.upload(new FileInputStream(tempFile), tempFile.length());

                tempFile.delete()

                return true
            }

            InputStream download(String path) throws IOException {
                String fileName = path.substring(1,path.length())

                CloudBlockBlob blob = container.getBlockBlobReference(fileName);

                tempFile = File.createTempFile("azure-transfer", "tmp", null);
                blob.download(new FileOutputStream(tempFile))

                InputStream result = new BufferedInputStream(new FileInputStream(tempFile.getAbsolutePath()))

                tempFile.delete()

                return result;
            }
        }

    }
}
