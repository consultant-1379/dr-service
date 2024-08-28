/*******************************************************************************
 * COPYRIGHT Ericsson 2023
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 ******************************************************************************/
package com.ericsson.bos.dr.tests.unit.execution.executors.http

import org.springframework.boot.context.properties.EnableConfigurationProperties

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor
import static com.github.tomakehurst.wiremock.client.WireMock.containing
import static com.github.tomakehurst.wiremock.client.WireMock.delete
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor
import static com.github.tomakehurst.wiremock.client.WireMock.exactly
import static com.github.tomakehurst.wiremock.client.WireMock.get
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import static com.github.tomakehurst.wiremock.client.WireMock.matching
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath
import static com.github.tomakehurst.wiremock.client.WireMock.okJson
import static com.github.tomakehurst.wiremock.client.WireMock.patch
import static com.github.tomakehurst.wiremock.client.WireMock.post
import static com.github.tomakehurst.wiremock.client.WireMock.put
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import static com.github.tomakehurst.wiremock.client.WireMock.verify
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.web.reactive.function.client.WebClient

import com.ericsson.bos.dr.service.PropertiesService
import com.ericsson.bos.dr.service.execution.ExecutionContext
import com.ericsson.bos.dr.service.execution.executors.CommandExecutorException
import com.ericsson.bos.dr.service.execution.executors.CommandResponse
import com.ericsson.bos.dr.service.execution.executors.http.HttpExecutor
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationActionDto
import com.github.tomakehurst.wiremock.WireMockServer

import spock.lang.Specification
import spock.lang.Unroll

@ContextConfiguration(classes = HttpExecutorTestConfig.class)
@TestPropertySource(properties = ["service.http-client.retry.max-attempts=1",
    "service.http-client.retry.delay=1",
    "service.substitution.fail-on-unknown-tokens=true",
    "service.rest-service.runUrlPath=/rest-service/v1/run",
    "spring.codec.max-in-memory-size-kb=100000",
    "netty.http-client.connection-pool.maxConnections=50",
    "netty.http-client.connection-pool.maxIdleTime=60s",
    "netty.http-client.connection-pool.maxLifeTime=300s",
    "netty.http-client.connection-pool.evictInterval=120s",
    "netty.http-client.connection-pool.disposeInterval=300s",
    "netty.http-client.connection-pool.poolInactivityTime=300s"])
class HttpExecutorSpec extends Specification {

    @Autowired
    HttpExecutor httpExecutor

    @Value("\${service.rest-service.runUrlPath}")
    def restActionRunPath

    static String wiremockPort
    static String restServiceBaseUrl

    static WireMockServer wireMockServer = new WireMockServer(options().dynamicPort())

    def setupSpec() {
        wireMockServer.start()
        configureFor(wireMockServer.port())
        wiremockPort = Integer.toString(wireMockServer.port())
        restServiceBaseUrl = "http://localhost:${wiremockPort}"
        System.setProperty("service.rest-service.baseUrl", restServiceBaseUrl)
    }

    def cleanupSpec() {
        wireMockServer.stop()
    }

    def cleanup() {
        wireMockServer.resetAll()
    }

    def "Default to post request to rest-service when url not specified"() {

        setup: "Create actionDto without specifying the URL"
        def subsystemName = "dummyEnmSubsystem", resourceConfig = "dummyResourceConfig", resourceName = "dummyResourceInConfiguration"
        def actionProperties =
                [
                    "subsystemName" : subsystemName,
                    "resource"      : [
                        "resourceConfigurationName" : resourceConfig,
                        "resourceName"              : resourceName
                    ],
                    "body"          : actionBody
                ]
        def actionDto = new ApplicationConfigurationActionDto()
                .properties(actionProperties)

        and: "Configure expected rest call from the http executor"
        def expectedCommandResponse = "{\"res_1\":\"res_1_value\"}"
        stubFor(post(urlEqualTo("/rest-service/v1/run/${subsystemName}/${resourceConfig}/${resourceName}"))
                .withHeader("Accept", matching("\\*/\\*"))
                .withHeader("Content-Type", matching("application/json"))
                .withRequestBody(matchingJsonPath("\$.[?(@.prop_1 == 1)]"))
                .willReturn(okJson(expectedCommandResponse)))

        when: "Execute http request"
        CommandResponse response = httpExecutor.execute(new ExecutionContext(1l, actionDto, [:]))

        then: 'Response as expected'
        assert response.command.contains("Method: POST")
        assert response.command.contains("URL: ${restServiceBaseUrl}${restActionRunPath}/${subsystemName}/${resourceConfig}/${resourceName}")
        assert response.command.contains("Body: {\"prop_1\":1}")

        def expectedCommandHeaders = [
            "Content-Type:\\\"application\\/json\\\"",
            "Accept:\\\"\\*/\\*\\\""
        ]
        expectedCommandHeaders.each { eachExpectedHeader ->
            assert response.command.matches("(?s).*Headers.*: \\[.*?${eachExpectedHeader}.*?\\]")
        }
        assert response.response == expectedCommandResponse

        where:
        actionBody << [
            ["prop_1": 1],
            "{\"prop_1\":1}"
        ]
    }

    def "Substitution performed for url, body and headers"() {

        setup: "Create actionDto, specifying a post request with substitution markers for the url, body and header"
        def actionProperties =
                ["url"    : "http://localhost:${wiremockPort}/rest-api/some-endpoint/{{ substitute_url }}".toString(),
                    "method" : "POST",
                    "body"   : "{\"prop_1\": \"{{ substitute_body }}\"}",
                    "headers": ["Accept": ["{{ substitute_header }}"], "Content-Type": ["text/html"]]]
        def actionDto = new ApplicationConfigurationActionDto()
                .properties(actionProperties)

        and: "Configure expected rest call from the http executor, with substitutions"
        stubFor(post(urlEqualTo("/rest-api/some-endpoint/substituted-url"))
                .withHeader("Accept", matching("substituted-header"))
                .withHeader("Content-Type", matching("text/html"))
                .withRequestBody(containing("{\"prop_1\":\"substituted-body\"}"))
                .willReturn(aResponse()
                .withStatus(200)))

        when: "Execute http request, with substitutions"
        def substitutions =
                ["substitute_url": "substituted-url", "substitute_header": "substituted-header", "substitute_body": "substituted-body"]
        CommandResponse response = httpExecutor.execute(new ExecutionContext(1l, actionDto, substitutions))

        then: 'Response as expected, with substitutions'
        assert response.command.contains("Method: " + actionProperties.get("method"))
        assert response.command.contains("URL: http://localhost:${wiremockPort}/rest-api/some-endpoint/substituted-url".toString())
        assert response.command.contains("Body: {\"prop_1\":\"substituted-body\"}")
        assert response.command.contains("Headers: [Accept:\"substituted-header\", Content-Type:\"text/html\"]")
    }

    def "Http property names are case insensitive"() {

        setup: "Create actionDto, specifying a post request with property keys in mix of upper and lower case"
        def actionProperties =
                ["uRL"    : "http://localhost:${wiremockPort}/rest-api/some-endpoint".toString(),
                 "meTHod" : "POST",
                 "BodY"   : "{\"prop_1\":\"value_1\"}",
                 "HEADERs": ["Accept": ["text/html"], "Content-Type": ["text/html"]]]
        def actionDto = new ApplicationConfigurationActionDto()
                .properties(actionProperties)

        and: "Configure expected rest call from the http executor"
        stubFor(post(urlEqualTo("/rest-api/some-endpoint"))
                .withHeader("Accept", matching("text/html"))
                .withHeader("Content-Type", matching("text/html"))
                .withRequestBody(containing("{\"prop_1\":\"value_1\"}"))
                .willReturn(aResponse()
                .withStatus(200)
                .withBody("some response")))

        when: "Execute http request"
        CommandResponse response = httpExecutor.execute(new ExecutionContext(1l, actionDto, [:]))

        then: 'Response as expected'
        assert response.command.contains("Method: POST")
        assert response.command.contains("URL: http://localhost:${wiremockPort}/rest-api/some-endpoint".toString())
        assert response.command.contains("Body: {\"prop_1\":\"value_1\"}")
        assert response.command.contains("Headers: [Accept:\"text/html\", Content-Type:\"text/html\"]")
        assert response.response == "some response"
    }

    @Unroll
    def "Exception when http status error response"() {

        setup: "Create actionDto, specifying a delete request"
        def actionProperties =
                ["url": "http://localhost:${wiremockPort}/rest-api/some-endpoint/1".toString(),
                    "method" : "DELETE"]
        def actionDto = new ApplicationConfigurationActionDto()
                .properties(actionProperties)

        and: "Configure error response"
        stubFor(delete(urlEqualTo("/rest-api/some-endpoint/1"))
                .willReturn(aResponse()
                .withStatus(httpError)
                .withBody("some error response")))

        when: "Execute http request"
        httpExecutor.execute(new ExecutionContext(1l, actionDto, [:]))

        then: "Verify no retries attempted"
        verify(exactly(1), deleteRequestedFor(urlEqualTo("/rest-api/some-endpoint/1")))

        and: "CommandExecutorException thrown in reaction to error response"
        CommandExecutorException e = thrown()
        assert e.message.contains("some error response")

        where: "Client and server errors"
        httpError << [500, 400]
    }

    def "Exception and retry when read timeout exceeded"() {

        setup: "Create actionDto, specifying a read timeout"
        def actionProperties =
                ["url"    : "http://localhost:${wiremockPort}/rest-api/some-endpoint/1".toString(),
                    "method" : "GET",
                    "headers": ["Accept": ["application/json"]],
                    "readTimeoutSeconds": 1]
        def actionDto = new ApplicationConfigurationActionDto()
                .properties(actionProperties)

        and: "Configure expected rest call from the http executor, adding a response delay"
        stubFor(get(urlEqualTo("/rest-api/some-endpoint/1"))
                .willReturn(aResponse()
                .withStatus(200)
                .withFixedDelay(3000)))

        when: "Execute http request"
        httpExecutor.execute(new ExecutionContext(1l, actionDto, [:]))

        then: "Verify multiple retries attempted"
        verify(exactly(2), getRequestedFor(urlEqualTo("/rest-api/some-endpoint/1")))

        and: "CommandExecutorException thrown in reaction to ReadTimeoutException"
        CommandExecutorException e = thrown()
        assert e.message =~ /Failed.*after 1 retry/
        assert e.message.contains("Read timeout exceeded")
    }

    def "Exception and no retry on unknown host"() {

        setup: "Create actionDto, specifying a URL with an unknown hostname"
        def actionProperties =
                ["url"    : "http://jake.fake:${wiremockPort}/rest-api/some-endpoint/1".toString(),
                    "method" : "GET",
                    "headers": ["Accept": ["application/json"]]]
        def actionDto = new ApplicationConfigurationActionDto()
                .properties(actionProperties)

        when: "Execute http request"
        httpExecutor.execute(new ExecutionContext(1l, actionDto, [:]))

        then: "CommandExecutorException thrown in reaction to UnknownHostException"
        CommandExecutorException e = thrown()
        assert e.message.contains("Failed to resolve 'jake.fake'")
    }

    def "Exception and retry when connect timeout exceeded"() {

        setup: "Create actionDto, adding a connect timeout and a URL with an IP address calculated to cause a connection timeout"
        def actionProperties =
                ["url"    : "http://192.168.128.128:${wiremockPort}/rest-api/some-endpoint/1".toString(),
                    "method" : "GET",
                    "headers": ["Accept": ["application/json"]],
                    "connectTimeoutSeconds": 1]
        def actionDto = new ApplicationConfigurationActionDto()
                .properties(actionProperties)

        when: "Execute http request"
        httpExecutor.execute(new ExecutionContext(1l, actionDto, [:]))

        then: "CommandExecutorException thrown in reaction to ConnectTimeoutException"
        CommandExecutorException e = thrown()
        assert e.message =~ /Failed.*after 1 retry/
        assert e.message =~ /connection timed out/
    }

    def "Exception and retry when connection refused"() {

        setup: "Create actionDto, specifying a localhost URL with an unusual port"
        def actionProperties =
                ["url"    : "http://localhost:55555/rest-api/some-endpoint/1",
                    "method" : "GET",
                    "headers": ["Accept": ["application/json"]]]

        def actionDto = new ApplicationConfigurationActionDto()
                .properties(actionProperties)

        when: "Execute http request"
        httpExecutor.execute(new ExecutionContext(1l, actionDto, [:]))

        then: "CommandExecutorException thrown in reaction to ConnectException caused by 'connection refused'"
        CommandExecutorException e = thrown()
        assert e.message =~ /Failed.*after 1 retry/
        assert e.message =~ /(?i)connection refused/
    }

    @TestConfiguration
    @EnableConfigurationProperties
    @ComponentScan(basePackages = ["com.ericsson.bos.dr.service.execution.executors.http", "com.ericsson.bos.dr.service.substitution",
            "com.ericsson.bos.dr.service.http"])
    static class HttpExecutorTestConfig {

        @MockBean
        PropertiesService propertiesServiceMock

        @Bean
        public WebClient.Builder getWebClientBuilder() {
            return WebClient.builder()
        }
    }
}
