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
package com.ericsson.bos.dr.tests.integration.http


import com.ericsson.bos.dr.service.http.HttpRequest
import com.ericsson.bos.dr.service.http.HttpClient
import com.ericsson.bos.so.security.mtls.MtlsConfigurationReloader
import com.ericsson.bos.so.security.mtls.MtlsConfigurationReloadersRegister
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.Options
import com.google.common.io.Resources
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientRequestException
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

import static com.github.tomakehurst.wiremock.client.WireMock.*
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options

/**
 * NOTE: The keystore.jks, truststore.jks and server.jks for this test where populated with the server.p12, client.p12 and ca.p12 are self-signed certs.
 *
 * <p>
 *     keytool -importkeystore -srckeystore source.p12 -srcstoretype PKCS12 -destkeystore target.jks -deststoretype JKS -srcstorepass password -deststorepass password -destkeypass password -noprompt
 * </p>
 *
 */
@ContextConfiguration(classes = HttpServiceTestConfig.class)
@TestPropertySource(properties = [
        "security.tls.enabled=true",
        "service.http-client.retry.max-attempts=1",
        "service.http-client.retry.delay=1",
        "service.substitution.fail-on-unknown-tokens=true",
        "netty.http-client.connection-pool.maxConnections=50",
        "netty.http-client.connection-pool.maxIdleTime=60s",
        "netty.http-client.connection-pool.maxLifeTime=300s",
        "netty.http-client.connection-pool.evictInterval=120s",
        "netty.http-client.connection-pool.disposeInterval=300s",
        "netty.http-client.connection-pool.poolInactivityTime=300s"], locations = "classpath:application-test-containers.properties")
class InternalServiceHttpClientTlsSpec extends Specification {

    private static final WireMockServer wireMockServer = new WireMockServer(options()
            .dynamicHttpsPort().preserveHostHeader(true)
            .needClientAuth(true)
            .trustStoreType("JKS")
            .trustStorePath((new File(Resources.getResource("security/certs/ca.jks").getPath())).getAbsolutePath())
            .trustStorePassword("password")
            .keystoreType("JKS")
            .keystorePath((new File(Resources.getResource("security/certs/server.jks").getPath())).getAbsolutePath())
            .keystorePassword("password")
            .keyManagerPassword("password")
            .useChunkedTransferEncoding(Options.ChunkedEncodingPolicy.BODY_FILE)
            .gzipDisabled(true))

    private static int wiremockPort

    @Autowired
    private HttpClient httpClient

    def setupSpec() {
        wireMockServer.start()
        wiremockPort = wireMockServer.httpsPort()
        setupTrustStore("security/certs/truststore.jks")
        setupKeyStore("security/certs/keystore.jks")
    }

    def cleanupSpec() {
        wireMockServer.stop()
    }

    def cleanup() {
        wireMockServer.resetAll()
    }

    def "Execute http request is successful with MTLS configured"() {

        setup:
        HttpRequest properties = new HttpRequest(
                url    : "https://localhost:${wiremockPort}/rest-api/some-endpoint".toString(),
                method : "GET",
                headers: ["Accept": ["application/json"]])

        and: "Configure expected rest call"
        wireMockServer.stubFor(get(urlEqualTo("/rest-api/some-endpoint"))
                .withHeader("Accept", matching("application/json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("some response")));

        when: "Execute http request"
        ResponseEntity<String> responseEntity = httpClient.executeRequest(properties)

        then: "Response as expected"
        assert responseEntity.body == "some response"
    }

    def "CommandExecutorException thrown when MTLS configured and invalid client key"() {

        setup:
        HttpRequest properties = new HttpRequest(
                url    : "https://localhost:${wiremockPort}/rest-api/some-endpoint".toString(),
                method : "GET",
                headers: ["Accept": ["application/json"]])

        and: "Setup keystore with invalid client key"
        setupKeyStore("security/certs/invalid_keystore.jks")

        and: "Reload sslContext"
        MtlsConfigurationReloadersRegister.getInstance().getMtlsConfigurationReloaders().forEach( { it.reload() })

        when: "Execute http request"
        ResponseEntity<String> responseEntity = httpClient.executeRequest(properties)

        then: "WebClientRequestException thrown"
        WebClientRequestException exception = thrown()
        exception.message.contains("bad_certificate")
    }

    def setupKeyStore(String keyStoreResource) {
        Path keyStorePath = Paths.get((new File(Resources.getResource(keyStoreResource).getPath())).getAbsolutePath())
        Files.copy(keyStorePath, Paths.get(System.getProperty("java.io.tmpdir"),"keystore.jks"), StandardCopyOption.REPLACE_EXISTING)
    }

    def setupTrustStore(String trustStoreResource) {
        Path trustStorePath = Paths.get((new File(Resources.getResource(trustStoreResource).getPath())).getAbsolutePath())
        Files.copy(trustStorePath, Paths.get(System.getProperty("java.io.tmpdir"),"truststore.jks"), StandardCopyOption.REPLACE_EXISTING)
    }

    @TestConfiguration
    @EnableConfigurationProperties
    @ComponentScan(basePackages = ["com.ericsson.bos.dr.service.http"])
    static class HttpServiceTestConfig {

        @Bean
        public WebClient.Builder getWebClientBuilder() {
            return WebClient.builder();
        }
    }
}
