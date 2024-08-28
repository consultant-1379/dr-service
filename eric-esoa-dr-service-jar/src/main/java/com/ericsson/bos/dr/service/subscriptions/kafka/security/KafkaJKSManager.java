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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.ericsson.bos.dr.jpa.model.ListenerMessageSubscriptionEntity;
import com.ericsson.bos.dr.service.exceptions.DRServiceException;
import com.ericsson.bos.dr.service.exceptions.ErrorCode;
import com.ericsson.bos.dr.service.subsystem.KafkaConnectionProperties;
import com.ericsson.bos.dr.service.utils.JKS;
import com.ericsson.bos.dr.service.utils.KUBE;

import io.fabric8.kubernetes.api.model.Secret;

/**
 * Builds the key and trust stores from k8s secrets. The stores are then written out to the file system.
 */
@Component
public class KafkaJKSManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaJKSManager.class);

    private static final String JKS_FILE_EXTENSION = "jks";

    @Value(value = "${service.message-subscriptions.kafka.tls.jks-dir}")
    private String jksBaseDir;

    @Autowired
    private CertificateConifg certificateConifg;

    /**
     * Builds the key and trust stores from the configuration in the subsystem connection properties.
     * The stores are then written out to the file system.
     *
     * @param connectionProperties
     *         subsystem kafka connection properties
     * @param listenerMsgSubscriptionEntity
     *         listener entity
     * @return JKSSpec
     */
    public JKSSpec createStores(final KafkaConnectionProperties connectionProperties,
            final ListenerMessageSubscriptionEntity listenerMsgSubscriptionEntity) {
        LOGGER.debug("Building JKS: Subscription name: {}, Properties: {}", connectionProperties, listenerMsgSubscriptionEntity.getName());
        try {
            final String pathToTrustStore = buildTrustStore(connectionProperties, listenerMsgSubscriptionEntity);
            final String pathToKeyStore = ismTLS(connectionProperties) ? buildKeyStore(connectionProperties, listenerMsgSubscriptionEntity) : null;
            return new JKSSpec(pathToTrustStore, connectionProperties.getTrustStorePassword(), pathToKeyStore,
                    connectionProperties.getKeyStorePassword(), connectionProperties.getKeyPassword());
        } catch (final DRServiceException e) {
            throw e;
        } catch (final Exception e) {
            throw new DRServiceException(e, ErrorCode.FAILED_TO_CREATE_JKS, listenerMsgSubscriptionEntity.getSubsystemName(), e.getMessage());
        }
    }

    private String buildTrustStore(final KafkaConnectionProperties connectionProperties,
            final ListenerMessageSubscriptionEntity listenerMsgSubscriptionEntity) {
        final String dataFieldKey = certificateConifg.getTrustStoreConfig().getDataFieldKey();
        final Path jksFilePath = createJKSFile("truststore", listenerMsgSubscriptionEntity);
        buildStore(jksFilePath, connectionProperties.getTrustStorePassword(), connectionProperties.getTrustStoreSecretName(), dataFieldKey);
        return jksFilePath.toString();
    }

    private String buildKeyStore(final KafkaConnectionProperties connectionProperties,
            final ListenerMessageSubscriptionEntity listenerMsgSubscriptionEntity) {
        final String dataFieldKey = certificateConifg.getKeyStoreConfig().getDataFieldKey();
        final Path jksFilePath = createJKSFile("keystore", listenerMsgSubscriptionEntity);
        buildStore(jksFilePath, connectionProperties.getKeyStorePassword(), connectionProperties.getKeyStoreSecretName(), dataFieldKey);
        return jksFilePath.toString();
    }

    private void buildStore(final Path jksFilePath, final String storePassword, final String secretName, final String dataFieldKey) {
        final byte[] storeData = readSecret(secretName, dataFieldKey);
        JKS.write(storeData, storePassword, jksFilePath);
    }

    private byte[] readSecret(final String secretName, final String dataFieldKey) {
        final Secret secret = KUBE.getSecret(secretName).orElseThrow(
                () -> new DRServiceException(ErrorCode.KAFKA_SECRET_NOT_FOUND, secretName));
        final String secretData = secret.getData().get(dataFieldKey);
        if (secretData == null) {
            throw  new DRServiceException(ErrorCode.KAFKA_SECRET_DATA_FIELD_NOT_FOUND, dataFieldKey, secretName);
        }
        LOGGER.debug("Secret name: {}, data: {}", secretName, secretData);
        return Base64.getDecoder().decode(secretData);
    }

    private Path createJKSFile(final String storeName, final ListenerMessageSubscriptionEntity listenerMsgSubscriptionEntity) {
        return Paths.get(jksBaseDir,
                getFeaturePackName(listenerMsgSubscriptionEntity),
                getListenerName(listenerMsgSubscriptionEntity),
                getSubscriptionName(listenerMsgSubscriptionEntity),
                storeName + "." + JKS_FILE_EXTENSION);
    }

    private String getFeaturePackName(final ListenerMessageSubscriptionEntity listenerMessageSubscriptionEntity) {
        return listenerMessageSubscriptionEntity.getListenerEntity().getFeaturePack().getName();
    }

    private String getSubscriptionName(final ListenerMessageSubscriptionEntity listenerMessageSubscriptionEntity) {
        return listenerMessageSubscriptionEntity.getName();
    }

    private String getListenerName(final ListenerMessageSubscriptionEntity listenerMessageSubscriptionEntity) {
        return listenerMessageSubscriptionEntity.getListenerEntity().getName();
    }

    private boolean ismTLS(final KafkaConnectionProperties connectionProperties) {
        return StringUtils.hasText(connectionProperties.getKeyStoreSecretName());
    }

    /**
     * Contains details of the created key and trust stores, including their location on the file system.
     */
    public static class JKSSpec {
        private final String trustStorePath;
        private final String trustStorePwd;
        private final String keyStorePath;
        private final String keyStorePwd;
        private final String keyPwd;

        /**
         * Constructor.
         *
         * @param trustStorePath
         *         the file system truststore path
         * @param trustStorePwd
         *         the truststore pwd
         * @param keyStorePath
         *         the file system keystore path
         * @param keyStorePwd
         *         the keystore pwd
         * @param keyPwd
         *         the key pwd
         */
        JKSSpec(final String trustStorePath, final String trustStorePwd, final String keyStorePath, final String keyStorePwd, String keyPwd) {
            this.trustStorePath = trustStorePath;
            this.trustStorePwd = trustStorePwd;
            this.keyStorePath = keyStorePath;
            this.keyStorePwd = keyStorePwd;
            this.keyPwd = keyPwd;
        }

        public String getTrustStorePath() {
            return trustStorePath;
        }

        public String getTrustStorePwd() {
            return trustStorePwd;
        }

        public String getKeyStorePath() {
            return keyStorePath;
        }

        public String getKeyStorePwd() {
            return keyStorePwd;
        }

        public String getKeyPwd() {
            return keyPwd;
        }
    }
}
