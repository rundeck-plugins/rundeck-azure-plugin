package com.rundeck.plugins.azure.plugin.files.endpoints

import com.rundeck.plugins.azure.plugin.files.EndpointHandler
import com.rundeck.plugins.azure.plugin.files.URIParser

/**
 * Created by luistoledo on 11/14/17.
 */
class FileEndpoint {
    public static EndpointHandler createEndpointHandler(final URIParser url) throws IOException {

        return new EndpointHandler() {

            @Override
            public List<String> listFiles(String path) throws IOException {
                File dir = new File(path);
                File[] list = dir.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        return pathname.isFile();
                    }
                });

                List<String> fileList =  new ArrayList<>();
                for(File file: list) {
                    fileList.add(file.getAbsolutePath());
                }

                return fileList;
            }

            @Override
            public InputStream newTransferInputStream(String path) throws IOException {
                return new BufferedInputStream(new FileInputStream(path));
            }

            @Override
            public OutputStream newTransferOutputStream(String path) throws IOException {
                return new BufferedOutputStream(new FileOutputStream(path));
            }


            @Override
            public boolean finishTransferTransaction() throws IOException {
                // nothing to do.
                return true;
            }

            @Override
            public void disconnect() throws IOException {
                // nothing to do.
            }

            @Override
            public boolean deleteFile(String path) throws IOException {
                File file = new File(path);
                return file.delete();

            }

            @Override
            public boolean fileExists(String path) throws IOException {

                InputStream is;
                try
                {
                    is = new BufferedInputStream(new FileInputStream(path));
                    if (is != null) {
                        is.close();
                        is = null;
                        return true;
                    }
                }
                catch (Exception e)
                {
                    return false;
                }
                return false;
            }


        };

    }

}
