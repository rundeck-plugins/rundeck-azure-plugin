package com.rundeck.plugins.azure.plugin

import com.dtolabs.rundeck.core.common.IFramework
import com.dtolabs.rundeck.core.common.INodeSet
import com.dtolabs.rundeck.core.resources.ResourceModelSourceException
import com.dtolabs.rundeck.core.resources.format.ResourceFormatParser
import com.dtolabs.rundeck.core.resources.format.ResourceFormatParserService
import com.dtolabs.rundeck.core.storage.ResourceMeta
import com.dtolabs.rundeck.core.storage.keys.KeyStorageTree
import com.microsoft.azure.management.Azure
import com.rundeck.plugins.azure.azure.AzureManager
import com.rundeck.plugins.azure.azure.AzureNode
import org.rundeck.app.spi.Services
import org.rundeck.storage.api.Resource
import spock.lang.Specification

/**
 * Created by luistoledo on 12/14/17.
 */
class AzureResourceModelSourceSpec extends Specification{

    def "retrieve resource success"(){
        given:

        def azureManager = Mock(AzureManager)
        def azure = GroovyMock(Azure)
        azureManager.setAzure(azure)

        Properties configuration = [client:"client123",tenant:"tenant123",key:"key123",subscriptionId:"subscriptionId123"]
        Services services = getServices()

        def azureResource = new AzureResourceModelSource(configuration, services)
        azureResource.setAzureManager(azureManager)

        AzureNode test = new AzureNode()
        test.name="test"

        AzureNode test2 = new AzureNode()
        test2.name="test2"
        def vmList = [test,test2]

        when:
        def result = azureResource.getNodes()

        then:
        1 * azureManager.listVms() >> vmList
        result.size()==vmList.size()

    }

    def "empty node name"(){
        given:

        def azureManager = Mock(AzureManager)
        def azure = GroovyMock(Azure)
        azureManager.setAzure(azure)

        Properties configuration = [client:"1234"]
        Services services = getServices()

        def azureResource = new AzureResourceModelSource(configuration, services)
        azureResource.setAzureManager(azureManager)

        AzureNode test = new AzureNode()
        def vmList = [test]

        when:
        azureResource.getNodes()

        then:
        azureManager.listVms() >> vmList
        thrown java.lang.IllegalArgumentException

    }

    def "fail authentication"(){
        given:

        def azureManager = Mock(AzureManager)
        def azure = GroovyMock(Azure)
        azureManager.setAzure(azure)
        Services services = getServices()

        Properties configuration = [client:"client123",tenant:"tenant123",key:"key123",subscriptionId:"subscriptionId123"]

        def azureResource = new AzureResourceModelSource(configuration, services)

        when:
        azureResource.getNodes()

        then:
        thrown java.lang.RuntimeException

    }

    def "bad authentication parameters"(){
        given:

        def azureManager = Mock(AzureManager)
        def azure = GroovyMock(Azure)
        azureManager.setAzure(azure)
        Services services = getServices()

        Properties configuration = [client:"client123",tenant:"tenant123",subscriptionId:"subscriptionId123"]

        def azureResource = new AzureResourceModelSource(configuration, services)
        azureResource.setAzureManager(azureManager)

        when:
        azureResource.getNodes()

        then:
        thrown java.lang.IllegalArgumentException

    }

    def "retrieve resource success using key storage"(){
        given:

        def azureManager = Mock(AzureManager)
        def azure = GroovyMock(Azure)
        azureManager.setAzure(azure)

        Services services = getServices()

        Properties configuration = [client:"client123",tenant:"tenant123",keyStoragePath:"keys/azure.key",subscriptionId:"subscriptionId123"]

        def azureResource = new AzureResourceModelSource(configuration, services)
        azureResource.setAzureManager(azureManager)

        AzureNode test = new AzureNode()
        test.name="test"

        AzureNode test2 = new AzureNode()
        test2.name="test2"
        def vmList = [test,test2]

        when:
        def result = azureResource.getNodes()

        then:
        1 * azureManager.listVms() >> vmList
        result.size()==vmList.size()

    }


    Services getServices(){
        def storageTree = Mock(KeyStorageTree)
        storageTree.getResource(_) >> Mock(Resource) {
            getContents() >> Mock(ResourceMeta) {
                writeContent(_) >> { args ->
                    args[0].write('password'.bytes)
                    return 6L
                }
            }
        }

        Services services = Mock(Services){
            getService(KeyStorageTree.class) >> storageTree
        }
        return services
    }



}