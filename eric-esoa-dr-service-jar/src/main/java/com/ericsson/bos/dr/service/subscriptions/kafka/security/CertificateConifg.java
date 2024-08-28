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

package com.ericsson.bos.dr.service.subscriptions.kafka.security;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Certificate Config.
 */
@Configuration
@ConfigurationProperties(prefix = "service.message-subscriptions.kafka.tls.certificates")
public class CertificateConifg {

    private List<SecretConfig> secrets;

    /**
     * Return truststore configuration.
     *
     * @return SecretConfig
     */
    public SecretConfig getTrustStoreConfig() {
        return getConfig("truststore");
    }

    /**
     * Return keystore configuration.
     *
     * @return SecretConfig
     */
    public SecretConfig getKeyStoreConfig() {
        return getConfig("keystore");
    }

    private SecretConfig getConfig(final String store) {
        return secrets.stream().
                filter(secret -> store.equalsIgnoreCase(secret.getType())).
                findFirst().orElse(null);
    }

    public void setSecrets(List<SecretConfig> secrets) {
        this.secrets = secrets;
    }

    /**
     * Secret configuration.
     */
    public static class SecretConfig {
        private String type;
        private String dataFieldKey;

        public String getType() {
            return type;
        }

        public void setType(final String type) {
            this.type = type;
        }

        public String getDataFieldKey() {
            return dataFieldKey;
        }

        public void setDataFieldKey(final String dataFieldKey) {
            this.dataFieldKey = dataFieldKey;
        }
    }
}
