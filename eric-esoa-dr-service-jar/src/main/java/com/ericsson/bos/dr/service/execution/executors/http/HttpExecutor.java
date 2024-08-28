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
package com.ericsson.bos.dr.service.execution.executors.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.ericsson.bos.dr.service.exceptions.DRServiceException;
import com.ericsson.bos.dr.service.execution.ExecutionContext;
import com.ericsson.bos.dr.service.execution.executors.CommandExecutor;
import com.ericsson.bos.dr.service.execution.executors.CommandExecutorException;
import com.ericsson.bos.dr.service.execution.executors.CommandResponse;
import com.ericsson.bos.dr.service.http.HttpClient;
import com.ericsson.bos.dr.service.http.HttpRequest;

/**
 * Executes http requests based on the action properties from the application configuration.
 */
@Component
public class HttpExecutor implements CommandExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpExecutor.class);

    @Autowired
    private HttpPropertiesParser httpPropertiesParser;

    @Autowired
    private HttpClient httpClient;

    @Override
    public CommandResponse execute(final ExecutionContext executionContext) {
        final HttpRequest properties = httpPropertiesParser.parse(executionContext.getActionDto(),
            executionContext.getSubstitutionCtx(), executionContext.getFeaturePackId());
        LOGGER.debug("Executing http request: {}, with substitution ctx: {}", properties, executionContext.getSubstitutionCtx());

        try {
            final ResponseEntity<String> response = httpClient.executeRequest(properties);
            return new CommandResponse(properties.toString(), response.getBody());

        } catch (final WebClientRequestException requestException) {
            throw new CommandExecutorException(properties.toString(),
                String.format("Failed to reach external service. Cause: %s.", requestException.getCause().getMessage()));
        } catch (final WebClientResponseException responseException) {
            final String body = responseException.getResponseBodyAsString();
            throw new CommandExecutorException(properties.toString(), body);
        } catch (final DRServiceException drServiceException) {
            throw new CommandExecutorException(properties.toString(), drServiceException.getMessage());
        }
    }

    @Override
    public boolean canExecute(final String type) {
        return "rest".equalsIgnoreCase(type);
    }
}
