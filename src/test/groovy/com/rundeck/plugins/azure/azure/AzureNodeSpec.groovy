package com.rundeck.plugins.azure.azure

import com.microsoft.azure.management.compute.OperatingSystemTypes
import spock.lang.Specification
import spock.lang.Unroll

/**
 * RUN-4820: osFamily must be normalized to Rundeck's canonical unix/windows
 * values instead of the raw Azure SDK OperatingSystemTypes string.
 */
class AzureNodeSpec extends Specification {

    @Unroll
    def "mapOsFamily normalizes #osType to #expected"() {
        expect:
        AzureNode.mapOsFamily(osType) == expected

        where:
        osType                          | expected
        OperatingSystemTypes.LINUX      | "unix"
        OperatingSystemTypes.WINDOWS    | "windows"
        null                            | null
    }
}
