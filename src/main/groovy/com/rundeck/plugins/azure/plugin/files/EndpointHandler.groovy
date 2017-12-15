package com.rundeck.plugins.azure.plugin.files

/**
 * Created by luistoledo on 11/14/17.
 */
interface EndpointHandler {

    /**
     * Get a list of files (only regular files) from the specified directory.
     *
     * @param path Path of directory for listing.
     * @return List with the full pathnames of files listed.
     */
    List<String> listFiles(String path) throws IOException;

    /**
     * Set up an InputStream for reading a file from this endpoint. The inputstream must me ready for reading.
     * If possible, closing the InputStream should end the transfer transaction (not the session). However,
     * if the underlying protocol does not permit it, you should implement {@link EndpointHandler#finishTransferTransaction()}
     * which will be called on this endpoint after closing the stream.
     *
     * @param path Path of the remote file to read.
     * @return A newly set-up InputStream
     * @throws IOException On Any error.
     */
    InputStream newTransferInputStream(String path) throws IOException;

    /**
     * Set up an OutputStream for writing a file to this endpoint. The OutputStream must me ready for writing.
     * If possible, closing the OutputStream should end the transfer transaction (not the session). However,
     * if the underlying protocol does not permit it, you should implement {@link EndpointHandler#finishTransferTransaction()}
     * which will be called on this endpoint after closing the stream.
     *
     * @param path Path of the remote file to read.
     * @return A newly set-up InputStream
     * @throws IOException On Any error.
     */
    OutputStream newTransferOutputStream(String path) throws IOException;

    /**
     * Signals the end of a file transfer transaction. Use this method to implement any operation needed
     * to be performed after closing a stream created by this endpoint, in order to get the endpoint ready
     * to create a new transfer transaction and a new stream.
     * @return
     * @throws IOException
     */
    boolean finishTransferTransaction() throws IOException;

    /**
     * Delete the file after copyed on demand.
     * @return
     * @throws IOException
     */
    boolean deleteFile(String path) throws IOException;

    /**
     * Terminates the session on this endpoint.
     * @throws IOException
     */
    void disconnect() throws IOException;

    /**
     * Check if the file exists
     *
     * @param path Path of directory for listing.
     * @return Boolean.
     */
    boolean fileExists(String path) throws IOException;



}