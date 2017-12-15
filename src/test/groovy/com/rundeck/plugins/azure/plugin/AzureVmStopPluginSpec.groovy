package com.rundeck.plugins.azure.plugin

import com.dtolabs.rundeck.plugins.step.PluginStepContext
import spock.lang.Specification

/**
 * Created by luistoledo on 12/15/17.
 */
class AzureVmStopPluginSpec extends Specification{

    def "check authentication parameters key or cert"(){

        given:

        def vmList = new AzureVmStopPlugin()
        def context = Mock(PluginStepContext)
        def configuration = [client:"client123",tenant:"tenant123",subscriptionId:"subscriptionId123"]

        when:
        vmList.executeStep(context,configuration)

        then:
        thrown IllegalArgumentException
    }
}
