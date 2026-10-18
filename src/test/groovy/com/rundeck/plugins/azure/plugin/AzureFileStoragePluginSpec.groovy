package com.rundeck.plugins.azure.plugin

import com.microsoft.azure.storage.blob.CloudBlobContainer
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

    def "blob name follows the configured path"(){
        given:
        Map<String, Object> context = testContext()
        context.put("name", "MyJob")
        AzureFileStoragePlugin storage = pluginFor(path, containerName, context)

        expect:
        storage.containerName == expectedContainer
        storage.getFileName("rdlog") == expectedFileName

        where:
        path                                                | containerName  | expectedContainer | expectedFileName
        'project/${job.project}/${job.execid}'              | null           | "project"         | "testproject/testexecid.rdlog"
        'logs/${job.name}-${job.project}/${job.execid}.log' | null           | "logs"            | "MyJob-testproject/testexecid.log.rdlog"
        'Logs/${job.project}/'                              | null           | "logs"            | "testproject/testexecid.rdlog"
        'project/${job.project}/${job.execid}'              | "Rundeck-Logs" | "rundeck-logs"    | "project/testproject/testexecid.rdlog"
        '${job.name}/${job.execid}'                         | "rundeck-logs" | "rundeck-logs"    | "MyJob/testexecid.rdlog"
    }

    def "stored blob is found at the configured path"(){
        given:
        AzureFileStoragePlugin storage = pluginFor('logs/${job.project}/${job.execid}.log', null, testContext())
        CloudBlockBlob blob = GroovyMock(CloudBlockBlob)
        storage.container = GroovyMock(CloudBlobContainer){
            getBlockBlobReference("testproject/testexecid.log.rdlog") >> blob
        }

        when:
        CloudBlockBlob result = storage.findStoredBlobFile("rdlog")

        then:
        1 * blob.exists() >> true
        result == blob
    }

    def "stored blob falls back to the legacy location"(){
        given:
        AzureFileStoragePlugin storage = pluginFor('logs/${job.project}/${job.execid}.log', null, testContext())
        CloudBlockBlob blob = GroovyMock(CloudBlockBlob){
            exists() >> false
            getName() >> "testproject/testexecid.log.rdlog"
        }
        CloudBlockBlob legacyBlob = GroovyMock(CloudBlockBlob){
            exists() >> legacyExists
        }
        storage.container = GroovyMock(CloudBlobContainer){
            getBlockBlobReference("testproject/testexecid.log.rdlog") >> blob
            getBlockBlobReference("testproject/testexecid.rdlog") >> legacyBlob
        }

        when:
        CloudBlockBlob result = storage.findStoredBlobFile("rdlog")

        then:
        result == (legacyExists ? legacyBlob : null)
        storage.isAvailable("rdlog") == legacyExists

        where:
        legacyExists << [true, false]
    }

    def "retrieve reads from the legacy location when the log was stored by a previous version"(){
        given:
        AzureFileStoragePlugin storage = pluginFor('logs/${job.project}/${job.execid}.log', null, testContext())
        CloudBlockBlob blob = GroovyMock(CloudBlockBlob){
            exists() >> false
            getName() >> "testproject/testexecid.log.rdlog"
        }
        CloudBlockBlob legacyBlob = GroovyMock(CloudBlockBlob){
            exists() >> true
        }
        storage.container = GroovyMock(CloudBlobContainer){
            getBlockBlobReference("testproject/testexecid.log.rdlog") >> blob
            getBlockBlobReference("testproject/testexecid.rdlog") >> legacyBlob
        }
        OutputStream stream = new ByteArrayOutputStream()

        when:
        boolean result = storage.retrieve("rdlog", stream)

        then:
        result
        1 * legacyBlob.download(stream)
        0 * blob.download(_)
    }

    def "store writes to the configured path"(){
        given:
        AzureFileStoragePlugin storage = pluginFor('logs/${job.project}/${job.execid}.log', null, testContext())
        CloudBlockBlob blob = GroovyMock(CloudBlockBlob)
        CloudBlobContainer container = GroovyMock(CloudBlobContainer)
        storage.container = container
        InputStream stream = new ByteArrayInputStream("log".bytes)

        when:
        boolean result = storage.store("rdlog", stream, 3, new Date())

        then:
        result
        1 * container.getBlockBlobReference("testproject/testexecid.log.rdlog") >> blob
        1 * blob.upload(stream, 3)
    }

    def "imported executions fall back to the previous imported lookup, not the legacy location"(){
        given:
        Map<String, Object> context = testContext()
        context.put("isRemoteFilePath", "true")
        context.put("outputfilepath", "project/otherproject/123")
        AzureFileStoragePlugin storage = pluginFor('project/${job.project}/${job.execid}', null, context)
        CloudBlockBlob blob = GroovyMock(CloudBlockBlob){
            exists() >> false
        }
        CloudBlockBlob previousBlob = GroovyMock(CloudBlockBlob){
            exists() >> false
        }
        CloudBlobContainer container = GroovyMock(CloudBlobContainer)
        storage.container = container

        when:
        CloudBlockBlob result = storage.findStoredBlobFile("rdlog")

        then:
        result == null
        1 * container.getBlockBlobReference("otherproject/123.rdlog") >> blob
        1 * container.getBlockBlobReference("/otherproject/123.rdlog") >> previousBlob
        0 * container.getBlockBlobReference("testproject/testexecid.rdlog")
    }

    def "log written by an execution is found after the execution is imported"(){
        given: "an in-memory container shared by the original and the importing server"
        Set<String> stored = []
        CloudBlobContainer container = fakeContainer(stored)
        Map<String, Object> context = testContext()
        context.put("name", "MyJob")

        and: "the original execution stores its log"
        AzureFileStoragePlugin writer = pluginFor(path, containerName, context)
        writer.container = container
        writer.store("rdlog", new ByteArrayInputStream("log".bytes), 3, new Date())

        and: "the execution is exported (outputfilepath = expanded path template) and imported"
        Map<String, Object> importContext = testContext()
        importContext.put("execid", "999")
        importContext.put("isRemoteFilePath", "true")
        importContext.put("outputfilepath", AzureFileStoragePlugin.expandPath(path, context))
        AzureFileStoragePlugin reader = pluginFor(path, containerName, importContext)
        reader.container = container

        expect:
        reader.containerName == writer.containerName
        reader.findStoredBlobFile("rdlog")?.getName() == writer.getFileName("rdlog")

        where:
        path                                    | containerName
        'project/${job.project}/${job.execid}'  | "rundeck-logs"
        'project/${job.project}/${job.execid}'  | null
        'logs/${job.name}/${job.execid}'        | null
        '${job.name}/${job.execid}'             | "rundeck-logs"
    }

    def "imported execution still finds a log stored by a previous version"(){
        given: "a previous version stored the log at project/execid in the configured container"
        Set<String> stored = ["/testproject/123.rdlog"]
        CloudBlobContainer container = fakeContainer(stored)
        Map<String, Object> importContext = testContext()
        importContext.put("isRemoteFilePath", "true")
        importContext.put("outputfilepath", "project/testproject/123")
        AzureFileStoragePlugin reader = pluginFor('project/${job.project}/${job.execid}', "rundeck-logs", importContext)
        reader.container = container

        expect:
        reader.findStoredBlobFile("rdlog")?.getName() == "/testproject/123.rdlog"
    }

    /**
     * In-memory container: blobs exist once uploaded or when listed in stored.
     */
    private CloudBlobContainer fakeContainer(Set<String> stored) {
        GroovyMock(CloudBlobContainer) {
            getBlockBlobReference(_ as String) >> { String name ->
                GroovyMock(CloudBlockBlob) {
                    getName() >> name
                    exists() >> { stored.contains(name) }
                    upload(_, _) >> { stored.add(name) }
                }
            }
        }
    }

    private static AzureFileStoragePlugin pluginFor(String path, String containerName, Map<String, Object> context) {
        AzureFileStoragePlugin storage = new AzureFileStoragePlugin()
        storage.setPath(path)
        storage.containerName = containerName
        storage.context = context
        String configPath = path.contains('${job.execid}') ? path : path + '${job.execid}'
        storage.expandedPath = AzureFileStoragePlugin.expandPath(configPath, context)
        storage.resolveContainerAndBlobPath()
        return storage
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
