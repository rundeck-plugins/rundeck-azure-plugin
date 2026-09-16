package com.rundeck.plugins.azure.util

import spock.lang.Specification
import spock.lang.Unroll

class AzurePluginUtilSpec extends Specification {

    @Unroll
    def "normalizeEndpointProtocol defaults to http for missing value: #protocol"() {
        expect:
        AzurePluginUtil.normalizeEndpointProtocol(protocol) == "http"

        where:
        protocol << [null, ""]
    }

    @Unroll
    def "normalizeEndpointProtocol accepts and normalizes valid value: #protocol"() {
        expect:
        AzurePluginUtil.normalizeEndpointProtocol(protocol) == expected

        where:
        protocol  || expected
        "http"    || "http"
        "https"   || "https"
        "HTTPS"   || "https"
        " https " || "https"
    }

    @Unroll
    def "normalizeEndpointProtocol rejects invalid value: #protocol"() {
        when:
        AzurePluginUtil.normalizeEndpointProtocol(protocol)

        then:
        thrown IllegalArgumentException

        where:
        protocol << ["ftp", "http;AccountName=other", "https;"]
    }
}
