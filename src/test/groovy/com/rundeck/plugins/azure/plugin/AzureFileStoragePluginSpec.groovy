package com.rundeck.plugins.azure.plugin

import com.microsoft.azure.storage.blob.CloudBlockBlob
import spock.lang.Specification

/**
 * Created by luistoledo on 12/14/17.
 */
class AzureFileStoragePluginSpec  extends Specification{

    private static String ACCOUNT_NAME_TEST="accountNameTest"
    private static String ACCESS_KEY_TEST="m1ttXHzuy4LR8kkLsdrzGPkY7B7I5uz7IPI6OSBj6Bo="

    def "log storage null parameters"(){
        println "<log storage null parameters>"
        given:
        AzureFileStoragePlugin storage = new AzureFileStoragePlugin()

        when:
        storage.initialize(testContext())

        then:
        thrown IllegalArgumentException
    }

    def "log storage invalid key"(){
        println "<log storage invalid key>"
        given:
        AzureFileStoragePlugin storage = new AzureFileStoragePlugin()
        storage.setAccessKey("accesssKeyTest")
        storage.setStorageAccount(ACCOUNT_NAME_TEST)
        storage.setPath("project/\${job.project}/\${job.execid}")

        when:
        storage.initialize(testContext())

        then:
        thrown java.security.InvalidKeyException
    }

    def "storage wrong path"(){
        println "<storage wrong path>"
        given:
        AzureFileStoragePlugin storage = new AzureFileStoragePlugin()
        storage.setAccessKey(ACCESS_KEY_TEST)
        storage.setStorageAccount(ACCOUNT_NAME_TEST)
        storage.setPath("testbadpath")

        when:
        storage.initialize(testContext())

        then:
        thrown IllegalArgumentException
    }

    def "return true if it was initialized with an imported execution"(){
        when:
        boolean isImportedExecution = AzureFileStoragePlugin.isImportedExecution(context)

        then:
        isImportedExecution == expected

        where:
        context                      | expected
        ['isRemoteFilePath': 'asd']  | false
        ['isRemoteFilePath': 'true'] | true
        [:]                          | false
        null                         | false
    }


    private HashMap<String, Object> testContext() {
        HashMap<String, Object> stringHashMap = new HashMap<String, Object>();
        stringHashMap.put("execid", "testexecid");
        stringHashMap.put("project", "testproject");
        stringHashMap.put("url", "http://rundeck:4440/execution/5/show");
        stringHashMap.put("serverUrl", "http://rundeck:4440");
        stringHashMap.put("serverUUID", "123");
        return stringHashMap;
    }


}
