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

import com.ericsson.bos.dr.service.exceptions.DRServiceException;
import com.ericsson.bos.dr.service.exceptions.ErrorCode;

import java.util.function.Consumer;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * Validates the kafka connection properties in the connected system.
 * <ol>
 *     <il>Validate that the bootstrapServer property is specified.</il>
 *     <il>Validate that when the ssl.enabled property is set to true ---
 *         1. At a minimum the ssl.trustStoreSecretName and ssl.trustStorePassword properties are specified.
 *         2. The ssl.keyStorePassword and ssl.keyPassword properties are specified if keystore secret name is also specified.</il>
 * </ol>
 */
@Component
public class KafkaConnectionPropertiesValidator implements Consumer<ConnectedSystem<KafkaConnectionProperties>> {

    @Override
    public void accept(ConnectedSystem<KafkaConnectionProperties> connectedSystem) {
        final KafkaConnectionProperties kafkaConnectionProperties = connectedSystem.getConnectionProperties().get(0);
        final String subsystemName = connectedSystem.getName();
        if (StringUtils.isBlank(kafkaConnectionProperties.getBootstrapServer())) {
            throw new DRServiceException(ErrorCode.KAFKA_PROPERTIES_VALIDATION_ERROR, subsystemName, "bootstrapServer must be set");
        }
        if (kafkaConnectionProperties.isSslEnabled()) {
            checkSslProperties(kafkaConnectionProperties, subsystemName);
        }
    }

    private void checkSslProperties(KafkaConnectionProperties connectionProperties, String subsystemName) {
        if (StringUtils.isBlank(connectionProperties.getTrustStoreSecretName()) ||
                StringUtils.isBlank(connectionProperties.getTrustStorePassword())) {
            throw new DRServiceException(ErrorCode.KAFKA_PROPERTIES_VALIDATION_ERROR, subsystemName,
                    "trustStoreSecretName and trustStorePassword must be set when ssl is enabled");
        }

        if (!StringUtils.isBlank(connectionProperties.getKeyStoreSecretName()) &&
                (StringUtils.isBlank(connectionProperties.getKeyStorePassword()) || StringUtils.isBlank(connectionProperties.getKeyPassword()))) {
            throw new DRServiceException(ErrorCode.KAFKA_PROPERTIES_VALIDATION_ERROR, subsystemName,
                    "keyStorePassword and keyPassword must be set when keystoreSecretName is provided");
        }
    }
}
