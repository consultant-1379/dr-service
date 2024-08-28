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
package com.ericsson.bos.dr.tests.unit.http

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
import com.ericsson.bos.dr.service.exceptions.DRServiceException
import com.ericsson.bos.dr.service.http.HttpRequest
import com.ericsson.bos.dr.service.http.HttpClient
import com.github.tomakehurst.wiremock.WireMockServer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.WebClientResponseException
import spock.lang.Specification
import spock.lang.Unroll

@ContextConfiguration(classes = HttpServiceTestConfig.class)
@TestPropertySource(properties = ["service.http-client.retry.max-attempts=1",
        "service.http-client.retry.delay=1",
        "spring.codec.max-in-memory-size-kb=100000",
        "netty.http-client.connection-pool.maxConnections=50",
        "netty.http-client.connection-pool.maxIdleTime=60s",
        "netty.http-client.connection-pool.maxLifeTime=300s",
        "netty.http-client.connection-pool.evictInterval=120s",
        "netty.http-client.connection-pool.disposeInterval=300s",
        "netty.http-client.connection-pool.poolInactivityTime=300s"])
class HttpClientSpec extends Specification{

    @Autowired
    HttpClient httpClient

    static String wiremockPort

    static WireMockServer wireMockServer = new WireMockServer(options().dynamicPort())

    def setupSpec() {
        wireMockServer.start()
        configureFor(wireMockServer.port())
        wiremockPort = Integer.toString(wireMockServer.port())
    }

    def cleanupSpec() {
        wireMockServer.stop()
    }

    def cleanup() {
        wireMockServer.resetAll()
    }

    def "Execute post request with body containing a json string"() {

        setup: "create http request properties, specifying a post request"
        HttpRequest properties = new HttpRequest(
                url    : "http://localhost:${wiremockPort}/rest-api/some-endpoint".toString(),
                method : "POST",
                body   : "{\"prop_1\":\"value_1\"}",
                headers: ["Accept": ["text/html"], "Content-Type": ["text/html"]])

        and: "Configure expected rest call from the http executor"
        stubFor(post(urlEqualTo("/rest-api/some-endpoint"))
                .withHeader("Accept", matching("text/html"))
                .withHeader("Content-Type", matching("text/html"))
                .withRequestBody(containing("{\"prop_1\":\"value_1\"}"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("some response")))

        when: "Execute http request"
        ResponseEntity<String> responseEntity = httpClient.executeRequest(properties)

        then: 'Response as expected'
        assert responseEntity.body == "some response"
    }

    def "Execute post request with body containing a json object"() {

        setup: "create http request properties, specifying a post request"
        HttpRequest properties = new HttpRequest(
                url    : "http://localhost:${wiremockPort}/rest-api/some-endpoint".toString(),
                method : "POST",
                body   : ["prop_1": 1],
                headers: ["Accept": ["application/json"], "Content-Type": ["application/json"]])

        and: "Configure expected rest call"
        stubFor(post(urlEqualTo("/rest-api/some-endpoint"))
                .withHeader("Accept", matching("application/json"))
                .withHeader("Content-Type", matching("application/json"))
                .withRequestBody(matchingJsonPath("\$.[?(@.prop_1 == 1)]"))
                .willReturn(okJson("{ \"res_1\": \"res_1_value\" }")))

        when: "Execute http request"
        ResponseEntity<String> responseEntity = httpClient.executeRequest(properties)

        then: 'Response as expected'
        assert responseEntity.body == "{ \"res_1\": \"res_1_value\" }"
    }

    @Unroll
    def "Execute get request" () {

        setup: "create http request properties, specifying a get request"
        HttpRequest properties = new HttpRequest(
                url : "http://localhost:${wiremockPort}/rest-api/some-endpoint${originalURL}".toString(),
                method : "GET",
                headers : ["Accept": ["application/json"]])

        and: "Configure expected rest call. Note url is encoded"
        stubFor(get(urlEqualTo("/rest-api/some-endpoint${urlEncodedURL}"))
                .withHeader("Accept", matching("application/json"))
                .willReturn(okJson("{ \"res_1\": \"res_1_value\" }")))

        when: "Execute http request"
        ResponseEntity<String> responseEntity = httpClient.executeRequest(properties)

        then: "Response as expected"
        assert responseEntity.body == "{ \"res_1\": \"res_1_value\" }"

        where:
        originalURL                                                                    | urlEncodedURL
        "/1?\$filter=isVimAssign=true 'and' projectName=theProject"                    | "/1?\$filter=isVimAssign%3Dtrue%20'and'%20projectName%3DtheProject"
        "?filters=%7B\"subsystemType\":%7B\"type\":\"KAFKA\"%7D,\"name\":\"kafka\"%7D" | "?filters=%257B%22subsystemType%22:%257B%22type%22:%22KAFKA%22%257D,%22name%22:%22kafka%22%257D"
    }

    def "Execute put request"() {

        setup: "create http request properties, specifying a put request"
        HttpRequest properties = new HttpRequest(
                url    : "http://localhost:${wiremockPort}/rest-api/some-endpoint/1".toString(),
                method : "PUT",
                body   : ["prop_1": 1],
                headers: ["Accept": ["application/json"], "Content-Type": ["application/json"]])

        and: "Configure expected rest call"
        stubFor(put(urlEqualTo("/rest-api/some-endpoint/1"))
                .withHeader("Accept", matching("application/json"))
                .withHeader("Content-Type", matching("application/json"))
                .withRequestBody(matchingJsonPath("\$.[?(@.prop_1 == 1)]"))
                .willReturn(okJson("{ \"res_1\": \"res_1_value\" }")))

        when: "Execute http request"
        ResponseEntity<String> responseEntity = httpClient.executeRequest(properties)

        then: 'Response as expected'
        assert responseEntity.body == "{ \"res_1\": \"res_1_value\" }"
    }

    def "Execute patch request"() {

        setup: "create http request properties, specifying a patch request"
        HttpRequest properties = new HttpRequest(
                url    : "http://localhost:${wiremockPort}/rest-api/some-endpoint/1".toString(),
                method : "PATCH",
                body   : ["prop_1": 1],
                headers: ["Accept": ["application/json"], "Content-Type": ["application/json"]])

        and: "Configure expected rest call"
        stubFor(patch(urlEqualTo("/rest-api/some-endpoint/1"))
                .withHeader("Accept", matching("application/json"))
                .withHeader("Content-Type", matching("application/json"))
                .withRequestBody(matchingJsonPath("\$.[?(@.prop_1 == 1)]"))
                .willReturn(okJson("{ \"res_1\": \"res_1_value\" }")))

        when: "Execute http request"
        ResponseEntity<String> responseEntity = httpClient.executeRequest(properties)

        then: 'Response as expected'
        assert responseEntity.body == "{ \"res_1\": \"res_1_value\" }"
    }

    def "Execute delete request"() {

        setup: "create http request properties, specifying a delete request"
        HttpRequest properties = new HttpRequest(
                url: "http://localhost:${wiremockPort}/rest-api/some-endpoint/1".toString(),
                method : "DELETE")

        and: "Configure expected rest call"
        stubFor(delete(urlEqualTo("/rest-api/some-endpoint/1"))
                .willReturn(aResponse()
                        .withStatus(200)))

        when: "Execute http request"
        ResponseEntity<String> responseEntity = httpClient.executeRequest(properties)

        then: "Response as expected"
        assert responseEntity.body == null
    }

    @Unroll
    def "Exception when http status error response"() {

        setup: "create http request properties, specifying a delete request"
        HttpRequest properties = new HttpRequest(
                url: "http://localhost:${wiremockPort}/rest-api/some-endpoint/1".toString(),
                method : "DELETE")

        and: "Configure error response"
        stubFor(delete(urlEqualTo("/rest-api/some-endpoint/1"))
                .willReturn(aResponse()
                        .withStatus(httpError)
                        .withBody("some error response")))

        when: "Execute http request"
        ResponseEntity<String> responseEntity = httpClient.executeRequest(properties)

        then: "Verify no retries attempted"
        verify(exactly(1), deleteRequestedFor(urlEqualTo("/rest-api/some-endpoint/1")))

        and: "WebClientResponseException thrown"
        WebClientResponseException e = thrown()
        assert e.responseBodyAsString.contains("some error response")

        where: "Client and server errors"
        httpError << [500, 400]
    }

    def "Exception and retry when read timeout exceeded"() {

        setup: "create http request properties, specifying a read timeout"
        HttpRequest properties = new HttpRequest(
                url    : "http://localhost:${wiremockPort}/rest-api/some-endpoint/1".toString(),
                method : "GET",
                headers: ["Accept": ["application/json"]],
                readTimeoutSeconds: 1)

        and: "Configure expected rest call from the http executor, adding a response delay"
        stubFor(get(urlEqualTo("/rest-api/some-endpoint/1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(3000)))

        when: "Execute http request"
        ResponseEntity<String> responseEntity = httpClient.executeRequest(properties)

        then: "Verify multiple retries attempted"
        verify(exactly(2), getRequestedFor(urlEqualTo("/rest-api/some-endpoint/1")))

        and: "DRServiceException thrown"
        DRServiceException e = thrown()
        assert e.message =~ /Failed.*after 1 retry/
        assert e.message.contains("Read timeout exceeded")
    }

    def "Exception and no retry on unknown host"() {

        setup: "create http request properties, specifying a URL with an unknown hostname"
        HttpRequest properties = new HttpRequest(
                url    : "http://jake.fake:${wiremockPort}/rest-api/some-endpoint/1".toString(),
                method : "GET",
                headers: ["Accept": ["application/json"]])

        when: "Execute http request"
        ResponseEntity<String> responseEntity = httpClient.executeRequest(properties)

        then: "WebClientRequestException thrown"
        WebClientRequestException e = thrown()
        assert e.message.contains("Failed to resolve 'jake.fake'")
    }

    def "Exception and retry when connect timeout exceeded"() {

        setup: "create http request properties, adding a connect timeout and a URL with an IP address calculated to cause a connection timeout"
        HttpRequest properties = new HttpRequest(
                url    : "http://192.168.128.128:${wiremockPort}/rest-api/some-endpoint/1".toString(),
                method : "GET",
                headers: ["Accept": ["application/json"]],
                connectTimeoutSeconds: 1)

        when: "Execute http request"
        ResponseEntity<String> responseEntity = httpClient.executeRequest(properties)

        then: "DRServiceException thrown"
        DRServiceException e = thrown()
        assert e.message =~ /Failed.*after 1 retry/
        assert e.message =~ /connection timed out/
    }

    def "Exception and retry when connection refused"() {

        setup: "create http request properties, specifying a localhost URL with an unusual port"
        HttpRequest properties = new HttpRequest(
                url    : "http://localhost:55555/rest-api/some-endpoint/1",
                method : "GET",
                headers: ["Accept": ["application/json"]])

        when: "Execute http request"
        ResponseEntity<String> responseEntity = httpClient.executeRequest(properties)

        then: "DRServiceException thrown"
        DRServiceException e = thrown()
        assert e.message =~ /Failed.*after 1 retry/
        assert e.message =~ /(?i)connection refused/
    }

    @TestConfiguration
    @EnableConfigurationProperties
    @ComponentScan(basePackages = ["com.ericsson.bos.dr.service.http"])
    static class HttpServiceTestConfig {

        @Bean
        public WebClient.Builder getWebClientBuilder() {
            return WebClient.builder()
        }
    }
}
