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

import static com.ericsson.bos.dr.service.exceptions.ErrorCode.CONNECTED_PROPERTIES_NOT_FOUND;
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.CONNECTED_SYSTEM_NOT_FOUND;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.ericsson.bos.dr.service.exceptions.DRServiceException;
import com.ericsson.bos.dr.service.http.HttpRequest;
import com.ericsson.bos.dr.service.http.HttpClient;
import com.ericsson.bos.dr.service.utils.JSON;
import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Retrieves the connected system from subsystem manager
 */
@Component
public class ConnectedSystemClient {

    private static final Logger LOGGER = LoggerFactory.getLogger((ConnectedSystemClient.class));

    @Value("${service.connected-system.baseUrl}")
    private String subsystemUrl;

    @Value("${service.connected-system.subsystems-path}")
    private String subsystemsPath;

    @Autowired
    private HttpClient httpClient;

    @Autowired
    private KafkaConnectionPropertiesValidator kafkaConnectionPropertiesValidator;

    /**
     * Fetches kafka connected system with given name from subsystem manager
     *
     * @param subsystemName name of the connected system
     * @return connected system
     */
    public ConnectedSystem<KafkaConnectionProperties> fetchKafkaConnectedSystem(final String subsystemName) {
        LOGGER.debug("Fetch connected system: {}", subsystemName);

        final String queryParams = String.format("?filters={\"subsystemType\":{\"type\":\"KAFKA\"},\"name\":\"%s\"}", subsystemName);
        final var url = StringUtils.join(subsystemUrl, subsystemsPath, queryParams);
        final var httpRequest = HttpRequest.get(url, false);
        final var responseEntity = httpClient.executeRequest(httpRequest);

        final List<ConnectedSystem<KafkaConnectionProperties>> connectedSystems =
            JSON.read(responseEntity.getBody(), new TypeReference<List<ConnectedSystem<KafkaConnectionProperties>>>(){});
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Subsystems: {}", connectedSystems);
        }
        if (connectedSystems.isEmpty()){
            throw new DRServiceException(CONNECTED_SYSTEM_NOT_FOUND, subsystemName);
        }
        final ConnectedSystem<KafkaConnectionProperties> connectedSystem = connectedSystems.iterator().next();
        if (CollectionUtils.isEmpty(connectedSystems.get(0).getConnectionProperties())) {
            throw new DRServiceException(CONNECTED_PROPERTIES_NOT_FOUND, subsystemName);
        }
        kafkaConnectionPropertiesValidator.accept(connectedSystem);
        return connectedSystem;
    }
}
