package com.rundeck.plugins.azure.plugin.files.endpoints

import com.azure.storage.blob.BlobClient
import com.azure.storage.blob.BlobContainerClient
import com.rundeck.plugins.azure.azure.AzureBlobStorageClientFactory
import com.rundeck.plugins.azure.plugin.files.EndpointHandler
import com.rundeck.plugins.azure.plugin.files.URIParser

/**
 * Created by luistoledo on 11/14/17.
 */
class AzureEndpoint {
    public static EndpointHandler createEndpointHandler(final URIParser url, String storageName, String accessKey) throws IOException {

        String containerName = url.getHost()

        BlobContainerClient container = AzureBlobStorageClientFactory.buildContainerClient(storageName, accessKey, containerName, "http")
        container.createIfNotExists()

        OutputStream outputStream=null
        File tempFile=null
        String destinationPath=null

        boolean doUploading=false


        return new EndpointHandler() {
            @Override
            List<String> listFiles(String path) throws IOException {
                List<String> list = new ArrayList<>()
                container.listBlobs().each {blob->
                    list.add(new URI(container.getBlobClient(blob.getName()).getBlobUrl()).getPath())
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
                BlobClient blob = container.getBlobClient(fileName);
                return Boolean.TRUE.equals(blob.exists())
            }

            boolean upload() throws IOException {

                String fileName = destinationPath.substring(1,destinationPath.length())

                tempFile=new File(tempFile.getAbsolutePath())

                BlobClient blob = container.getBlobClient(fileName);
                new FileInputStream(tempFile).withCloseable { fis ->
                    blob.upload(fis, tempFile.length());
                }

                tempFile.delete()

                return true
            }

            InputStream download(String path) throws IOException {
                String fileName = path.substring(1,path.length())

                BlobClient blob = container.getBlobClient(fileName);

                tempFile = File.createTempFile("azure-transfer", "tmp", null);
                new FileOutputStream(tempFile).withCloseable { fos ->
                    blob.downloadStream(fos)
                }

                InputStream result = new BufferedInputStream(new FileInputStream(tempFile.getAbsolutePath()))

                tempFile.delete()

                return result;
            }
        }

    }
}
