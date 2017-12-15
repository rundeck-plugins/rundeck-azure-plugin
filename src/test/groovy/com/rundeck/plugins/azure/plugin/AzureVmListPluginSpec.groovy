package com.rundeck.plugins.azure.plugin

import com.dtolabs.rundeck.core.execution.ExecutionContext
import com.dtolabs.rundeck.core.execution.ExecutionListener
import com.dtolabs.rundeck.core.execution.ExecutionLogger
import com.dtolabs.rundeck.plugins.step.PluginStepContext
import com.microsoft.azure.management.Azure
import com.rundeck.plugins.azure.azure.AzureManager
import com.rundeck.plugins.azure.azure.AzureNode
import spock.lang.Specification

/**
 * Created by luistoledo on 12/15/17.
 */
class AzureVmListPluginSpec extends Specification{

    def "check authentication parameters key or cert"(){

        given:

        def vmList = new AzureVmListPlugin()
        def context = Mock(PluginStepContext)
        def configuration = [client:"client123",tenant:"tenant123",subscriptionId:"subscriptionId123"]

        when:
        vmList.executeStep(context,configuration)

        then:
        thrown IllegalArgumentException
    }

    def "retrieve resource success"(){
        given:

        def azureManager = Mock(AzureManager)
        def azure = GroovyMock(Azure)
        azureManager.setAzure(azure)

        def context = Mock(PluginStepContext)
        def executionContext = Mock(ExecutionContext)
        def executionListener = Mock(ExecutionListener)
        def configuration = [client:"client123",tenant:"tenant123",subscriptionId:"subscriptionId123","key":"12345","vmRegion":"centralus"]

        def plugin = new AzureVmListPlugin()
        plugin.setAzureManager(azureManager)

        AzureNode test = new AzureNode()
        test.name="test"
        test.hostname="test"
        test.azureAttributes=[]

        def vmList = [test]

        String json='[{"name":"test","ip":"test","username":null,"osFamily":null,"osName":null,"osVersion":null,"description":null,"tags":null}]'

        when:
        plugin.executeStep(context,configuration)

        then:
        1 * azureManager.listVms() >> vmList
        1 * context.getExecutionContext() >> executionContext
        executionContext.getExecutionListener() >>executionListener

        1 * executionListener.log(2,json,_)

    }
}
