package com.rundeck.plugins.azure.plugin

import com.dtolabs.rundeck.plugins.step.PluginStepContext
import spock.lang.Specification

/**
 * Created by luistoledo on 12/15/17.
 */
class AzureStorageCopyStepPluginSpec extends Specification{

    private static String ACCOUNT_NAME_TEST="accountNameTest"
    private static String ACCESS_KEY_TEST="m1ttXHzuy4LR8kkLsdrzGPkY7B7I5uz7IPI6OSBj6Bo="

    def "check source is not directory"(){
        given:
        AzureStorageCopyStepPlugin storage = new AzureStorageCopyStepPlugin()
        def context = Mock(PluginStepContext)
        def configuration = [storage:ACCOUNT_NAME_TEST,key:ACCESS_KEY_TEST,source:"../resources/",destination:""]

        when:
        storage.executeStep(context,configuration)

        then:
        thrown IllegalArgumentException
    }

    def "check source has a wildcard"(){
        given:
        AzureStorageCopyStepPlugin storage = new AzureStorageCopyStepPlugin()
        def context = Mock(PluginStepContext)
        def configuration = [storage:ACCOUNT_NAME_TEST,key:ACCESS_KEY_TEST,source:"../resources/.*",destination:""]

        when:
        storage.executeStep(context,configuration)

        then:
        thrown IllegalArgumentException
    }

    def "check destination has a wildcard"(){
        given:
        AzureStorageCopyStepPlugin storage = new AzureStorageCopyStepPlugin()
        def context = Mock(PluginStepContext)
        def configuration = [storage:ACCOUNT_NAME_TEST,key:ACCESS_KEY_TEST,source:"../resources",destination:"destination/.*"]

        when:
        storage.executeStep(context,configuration)

        then:
        thrown IllegalArgumentException
    }

}
