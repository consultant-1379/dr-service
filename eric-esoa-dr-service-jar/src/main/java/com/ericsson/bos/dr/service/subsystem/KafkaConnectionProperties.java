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
package com.ericsson.bos.dr.service.subsystem;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Connection properties for connected system of type kafka
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class KafkaConnectionProperties {

    @JsonProperty("bootstrapServer")
    private String bootstrapServer;
    @JsonProperty("ssl.enabled")
    @JsonAlias("ssl_enabled")
    private boolean sslEnabled;
    @JsonProperty("ssl.trustStoreSecretName")
    @JsonAlias("ssl_trustStoreSecretName")
    private String trustStoreSecretName;
    @JsonProperty("ssl.trustStorePassword")
    @JsonAlias("ssl_trustStorePassword")
    private String trustStorePassword;
    @JsonProperty("ssl.keyStoreSecretName")
    @JsonAlias("ssl_keyStoreSecretName")
    private String keyStoreSecretName;
    @JsonProperty("ssl.keyStorePassword")
    @JsonAlias("ssl_keyStorePassword")
    private String keyStorePassword;
    @JsonProperty("ssl.keyPassword")
    @JsonAlias("ssl_keyPassword")
    private String keyPassword;

    public String getBootstrapServer() {
        return bootstrapServer;
    }

    public void setBootstrapServer(final String bootstrapServer) {
        this.bootstrapServer = bootstrapServer;
    }

    public boolean isSslEnabled() {
        return sslEnabled;
    }

    public void setSslEnabled(final boolean sslEnabled) {
        this.sslEnabled = sslEnabled;
    }

    public String getTrustStoreSecretName() {
        return trustStoreSecretName;
    }

    public void setTrustStoreSecretName(final String trustStoreSecretName) {
        this.trustStoreSecretName = trustStoreSecretName;
    }

    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    public void setTrustStorePassword(final String trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
    }

    public String getKeyStoreSecretName() {
        return keyStoreSecretName;
    }

    public void setKeyStoreSecretName(final String keyStoreSecretName) {
        this.keyStoreSecretName = keyStoreSecretName;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public void setKeyStorePassword(final String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    public String getKeyPassword() {
        return keyPassword;
    }

    public void setKeyPassword(final String keyPassword) {
        this.keyPassword = keyPassword;
    }

    @Override
    public String toString() {
        return "KafkaConnectionProperties{" +
               "bootstrapServer='" + bootstrapServer + '\'' +
               ", sslEnabled=" + sslEnabled +
               ", trustStoreSecretName='" + trustStoreSecretName + '\'' +
               ", trustStorePassword='" + trustStorePassword + '\'' +
               ", keyStoreSecretName='" + keyStoreSecretName + '\'' +
               ", keyStorePassword='" + keyStorePassword + '\'' +
               ", keyPassword='" + keyPassword + '\'' +
               '}';
    }
}
