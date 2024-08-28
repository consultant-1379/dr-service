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

import com.ericsson.bos.dr.service.subsystem.ConnectedSystem
import com.ericsson.bos.dr.service.subsystem.ConnectedSystemClient
import com.ericsson.bos.dr.service.exceptions.DRServiceException
import com.ericsson.bos.dr.service.http.HttpClient
import com.ericsson.bos.dr.service.subsystem.KafkaConnectionPropertiesValidator
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import spock.lang.Specification

class ConnectedSystemClientSpec extends Specification {

    HttpClient httpClientMock = Mock(HttpClient)
    KafkaConnectionPropertiesValidator kafkaConnectionPropertiesValidator = Spy(KafkaConnectionPropertiesValidator)
    ConnectedSystemClient connectedSystemClient = new ConnectedSystemClient(httpClient: httpClientMock,
            kafkaConnectionPropertiesValidator: kafkaConnectionPropertiesValidator,
            subsystemUrl: "http://localhost:8081", subsystemsPath: "/subsystem-manager/v2/subsystems")

    def "fetch connected system is successful" () {

        setup: "Mock HttpClient to return responseEntity containing kafka connected system"
        ResponseEntity<String> responseEntity = new ResponseEntity(body, HttpStatusCode.valueOf(200))

        when: "fetch connected system"
        ConnectedSystem connectedSystem = connectedSystemClient.fetchKafkaConnectedSystem("kafka")

        then: "call to http client is made with correctly encoded filters query param"
        1 * httpClientMock.executeRequest(_) >> {arguments -> assert arguments[0].url ==
                "http://localhost:8081/subsystem-manager/v2/subsystems?filters={\"subsystemType\":{\"type\":\"KAFKA\"},\"name\":\"kafka\"}";
            return responseEntity}

        and: "correct connected system retrieved"
        assert connectedSystem.name == "kafka"
        assert connectedSystem.url == null
        assert connectedSystem.subsystemType.id == 1
        assert connectedSystem.subsystemType.type == "KAFKA"
        with(connectedSystem.connectionProperties[0]) {
            assert bootstrapServer == "kafka-server-reference"
            assert sslEnabled
            assert trustStoreSecretName == "ts_1"
            assert trustStorePassword == "pwd1"
            assert keyStoreSecretName == "ks_1"
            assert keyStorePassword == "pwd2"
            assert keyPassword == "pwd3"
        }

        where: "Both dot and period notation supported"
        body                                    | _
        '[{"name":"kafka","url":null,"subsystemType":{"id":1,"type":"KAFKA"},' +
                '"connectionProperties":[{' +
                '"bootstrapServer":"kafka-server-reference", ' +
                '"ssl.enabled": true, ' +
                '"ssl.trustStoreSecretName": "ts_1",' +
                '"ssl.trustStorePassword": "pwd1", ' +
                '"ssl.keyStoreSecretName": "ks_1", ' +
                '"ssl.keyStorePassword": "pwd2", ' +
                '"ssl.keyPassword": "pwd3"}]}]' | _
        '[{"name":"kafka","url":null,"subsystemType":{"id":1,"type":"KAFKA"},' +
                '"connectionProperties":[{' +
                '"bootstrapServer":"kafka-server-reference", ' +
                '"ssl_enabled": true, ' +
                '"ssl_trustStoreSecretName": "ts_1",' +
                '"ssl_trustStorePassword": "pwd1", ' +
                '"ssl_keyStoreSecretName": "ks_1", ' +
                '"ssl_keyStorePassword": "pwd2", ' +
                '"ssl_keyPassword": "pwd3"}]}]' | _
    }

    def "Exception CONNECTED_SYSTEM_NOT_FOUND when connected system with given name is not found" () {

        setup: "Mock HttpClient to return responseEntity with empty body"
        String body = '[]'
        ResponseEntity<String> responseEntity = new ResponseEntity(body, HttpStatusCode.valueOf(200))
        httpClientMock.executeRequest(_) >> responseEntity

        when: "fetch connected system"
        ConnectedSystem connectedSystem = connectedSystemClient.fetchKafkaConnectedSystem("kafka")

        then: "DRServiceException is thrown"
        DRServiceException e = thrown()
        e.message.contains("could not be retrieved from subsystem manager")
    }

    def "Exception CONNECTED_PROPERTIES_NOT_FOUND when connected system with given name has no connection properties" () {

        setup: "Mock HttpClient to return responseEntity containing kafka connected system with empty connection properties"
        String body = '[{"name":"kafka","url":null,"subsystemType":{"id":1,"type":"KAFKA"},"connectionProperties":[]}]'
        ResponseEntity<String> responseEntity = new ResponseEntity(body, HttpStatusCode.valueOf(200))
        httpClientMock.executeRequest(_) >> responseEntity

        when: "fetch connected system"
        ConnectedSystem connectedSystem = connectedSystemClient.fetchKafkaConnectedSystem("kafka")

        then: "DRServiceException is thrown"
        DRServiceException e = thrown()
        e.message.contains("does not have connection properties")
    }
}
