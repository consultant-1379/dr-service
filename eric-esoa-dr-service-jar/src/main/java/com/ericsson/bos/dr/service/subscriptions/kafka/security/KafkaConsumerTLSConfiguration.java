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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.kafka.common.config.SslConfigs;
import org.springframework.stereotype.Component;

/**
 * Returns the (m)TLS configuration for a kafka consumer.
 */
@Component
public class KafkaConsumerTLSConfiguration {

    /**
     * Returns the (m)TLS configuration for a kafka consumer.
     *
     * @param jksStore
     *         JKS store details
     * @return (m)TLS configuration
     */
    public Map<String, Object> getConfig(final KafkaJKSManager.JKSSpec jksStore) {
        final Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put("security.protocol", "SSL");
        Optional.ofNullable(jksStore.getTrustStorePath()).ifPresent(path -> consumerProps.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, path));
        Optional.ofNullable(jksStore.getTrustStorePwd()).ifPresent(pwd -> consumerProps.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, pwd));
        Optional.ofNullable(jksStore.getKeyStorePath()).ifPresent(path -> consumerProps.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, path));
        Optional.ofNullable(jksStore.getKeyStorePwd()).ifPresent(pwd -> consumerProps.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, pwd));
        Optional.ofNullable(jksStore.getKeyPwd()).ifPresent(pwd -> consumerProps.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, pwd));
        return consumerProps;
    }

}
