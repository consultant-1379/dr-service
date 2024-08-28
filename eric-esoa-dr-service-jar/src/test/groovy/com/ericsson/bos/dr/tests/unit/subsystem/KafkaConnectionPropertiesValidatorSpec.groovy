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
package com.ericsson.bos.dr.tests.unit.subsystem

import com.ericsson.bos.dr.service.exceptions.DRServiceException
import com.ericsson.bos.dr.service.subsystem.ConnectedSystem
import com.ericsson.bos.dr.service.subsystem.KafkaConnectionProperties
import com.ericsson.bos.dr.service.subsystem.KafkaConnectionPropertiesValidator
import com.ericsson.bos.dr.service.subsystem.SubsystemType

import spock.lang.Specification

class KafkaConnectionPropertiesValidatorSpec extends Specification{

    KafkaConnectionPropertiesValidator kafkaConnectionPropertiesValidator = new KafkaConnectionPropertiesValidator()

    def "Should throw exception when bootstrapServer is not specified" () {

        setup: "Create connected system with properties"
        ConnectedSystem<KafkaConnectionProperties> connectedSystem = createConnectedSystem(bootstrapServer, "", "", "", "","")

        when: "Calling the validator"
        kafkaConnectionPropertiesValidator.accept(connectedSystem)

        then: "DRServiceException is thrown"
        DRServiceException e = thrown()
        e.message.contains("bootstrapServer must be set")

        where:
        bootstrapServer | _
        null | _
        "" | _
    }

    def "Should throw exception when ssl enabled and missing truststore secret name or truststore password" () {

        setup: "Create connected system with properties"
        ConnectedSystem<KafkaConnectionProperties> connectedSystem = createConnectedSystem("localhost:8080", truststoreSecretName, trustStorePassword, "", "","")

        when: "Calling the validator"
        kafkaConnectionPropertiesValidator.accept(connectedSystem)

        then: "DRServiceException is thrown"
        DRServiceException e = thrown()
        e.message.contains("trustStoreSecretName and trustStorePassword must be set when ssl is enabled")

        where:
        truststoreSecretName | trustStorePassword
        "" | ""
        "truststore_secret" | ""
        "truststore_secret" | null
        "" | "ts_pwd"
        null | "ts_pwd"
    }

    def "Should throw exception when ssl enabled with keystore secret name specified and missing keystore password or keyPassword" () {

        setup: "Create connected system with properties"
        ConnectedSystem<KafkaConnectionProperties> connectedSystem = createConnectedSystem("localhost:8080", "truststore_secret",
                "ts_pwd", "keystore_secret", keyStorePassword, keyPassword)

        when: "Calling the validator"
        kafkaConnectionPropertiesValidator.accept(connectedSystem)

        then: "DRServiceException is thrown"
        DRServiceException e = thrown()
        e.message.contains("keyStorePassword and keyPassword must be set when keystoreSecretName is provided")

        where:
        keyStorePassword | keyPassword
        "" | ""
        null | null
        "" | "ks_pwd"
        "k_pwd" | ""
    }

    ConnectedSystem<KafkaConnectionProperties> createConnectedSystem(String bootstrapServer, String trustStoreSecretName, String trustStorePassword, String keyStoreSecretName, String keyStorePassword, String keyPassword) {
        return new ConnectedSystem<KafkaConnectionProperties>(name: "kafka",
                subsystemType: new SubsystemType(id: 1, type: "KAFKA"),
                connectionProperties: [new KafkaConnectionProperties(bootstrapServer: bootstrapServer,
                        sslEnabled: true,
                        trustStoreSecretName: trustStoreSecretName,
                        trustStorePassword: trustStorePassword,
                        keyStoreSecretName: keyStoreSecretName,
                        keyStorePassword: keyStorePassword,
                        keyPassword: keyPassword)])
    }
}
