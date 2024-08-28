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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.hc.core5.http.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedCaseInsensitiveMap;

import com.ericsson.bos.dr.service.execution.executors.CommandExecutorException;
import com.ericsson.bos.dr.service.http.HttpRequest;
import com.ericsson.bos.dr.service.substitution.SubstitutionEngine;
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationActionDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Parses the http properties from an action configuration. The action configuration
 * is part of the application configuration.
 * Performs substitution on the url, headers and body.
 * Properties are stored in a <code>LinkedCaseInsensitiveMap</code> so that
 * the parser is not sensitive to the case of provided property names.
 */
@Component
class HttpPropertiesParser {

    @Autowired
    private SubstitutionEngine substitutionEngine;

    @Value("${service.rest-service.baseUrl}")
    private String restServiceBaseUrl;
    @Value("${service.rest-service.runUrlPath}")
    private String restServiceRunUrlPath;

    /**
     * Parse the http properties from the action configuration. Performs substitution.
     *
     * @param action the action configuration
     * @param substitutionCtx substitution context
     * @param featurePackId feature pack id
     *
     * @return substituted http properties wrapped in <code>HttpRequest</code>
     */
    HttpRequest parse(final ApplicationConfigurationActionDto action, final Map<String, Object> substitutionCtx,
                                           final long featurePackId) {
        final Map<String, Object> caseInsensitiveProperties = new LinkedCaseInsensitiveMap<>();
        caseInsensitiveProperties.putAll(action.getProperties());

        final var requestProperties = new HttpRequest();

        final var urlOptional = Optional.ofNullable((String) caseInsensitiveProperties.get("url"));
        if (urlOptional.isEmpty()) {
            initDefaultRestServiceProperties(requestProperties, substitutionCtx, featurePackId, caseInsensitiveProperties);
        } else {
            initCustomRequestProperties(requestProperties, urlOptional.get(), substitutionCtx, featurePackId, caseInsensitiveProperties);
        }

        requestProperties.setBody(getSubstitutedBody(
                caseInsensitiveProperties.get("body"), substitutionCtx, featurePackId));
        requestProperties.setConnectTimeoutSeconds((Integer)caseInsensitiveProperties.get("connectTimeoutSeconds"));
        requestProperties.setReadTimeoutSeconds((Integer)caseInsensitiveProperties.get("readTimeoutSeconds"));
        requestProperties.setWriteTimeoutSeconds((Integer)caseInsensitiveProperties.get("writeTimeoutSeconds"));
        return requestProperties;
    }

    private void initCustomRequestProperties(final HttpRequest requestProperties,
                                             final String url, final Map<String, Object> substitutionCtx,
                                             final long featurePackId, final Map<String, Object> actionProperties) {
        requestProperties.setUrl(getSubstitutedUrl(url, substitutionCtx, featurePackId));
        requestProperties.setMethod((String) actionProperties.get("method"));
        requestProperties.setHeaders(getSubstitutedHeaders(
                (Map<String, List<String>>) actionProperties.get("headers"), substitutionCtx, featurePackId));
    }

    private void initDefaultRestServiceProperties(final HttpRequest requestProperties,
                                                      final Map<String, Object> substitutionCtx, final long featurePackId,
                                                      final Map<String, Object> actionProperties) {

        final var defaultRestActionUrl = restServiceBaseUrl + restServiceRunUrlPath;

        final var subsystemName = (String) actionProperties.get("subsystemName");
        final var resourceProperties = (Map<?, ?>) actionProperties.get("resource");
        final var resourceConfigurationName = resourceProperties.get("resourceConfigurationName");
        final var resourceName = resourceProperties.get("resourceName");

        final var url = String.format("%s/%s/%s/%s", defaultRestActionUrl, subsystemName, resourceConfigurationName, resourceName);

        requestProperties.setUrl(getSubstitutedUrl(url, substitutionCtx, featurePackId));
        requestProperties.setMethod(HttpMethod.POST.name());

        final var applicationJson = List.of(ContentType.APPLICATION_JSON.getMimeType());
        final var wildcard = List.of(ContentType.WILDCARD.getMimeType());
        requestProperties.setHeaders(Map.of(
                "Content-Type", applicationJson,
                "Accept", wildcard)
        );
    }

    private String getSubstitutedUrl(final String url, final Map<String, Object> substitutionCtx, final long featurePackId) {
        return substitutionEngine.render(url, substitutionCtx, featurePackId);
    }

    private Object getSubstitutedBody(final Object body, final Map<String, Object> substitutionCtx, final long featurePackId) {
        Object substitutedBody = null;
        if (body != null) {
            if (body instanceof final String bodyString) {
                substitutedBody = substitutionEngine.render(bodyString, substitutionCtx, featurePackId);
            } else {
                try {
                    substitutedBody = substitutionEngine.render(
                            new ObjectMapper().writeValueAsString(body), substitutionCtx, featurePackId);
                } catch (final JsonProcessingException e) {
                    throw new CommandExecutorException(body.toString(), "Error converting http request body to string", e);
                }
            }
        }
        return substitutedBody;
    }

    private Map<String, List<String>> getSubstitutedHeaders(final Map<String, List<String>> headers,
            final Map<String, Object> substitutionCtx, final long featurePackId) {
        Map<String, List<String>> substitutedHeaders = null;
        if (headers != null) {
            for(final Map.Entry<String, List<String>> entry: headers.entrySet()) {
                final List<String> substitutedHeaderValues = entry.getValue().stream()
                        .map(headerValue -> substitutionEngine.render(headerValue, substitutionCtx, featurePackId))
                        .toList();
                headers.put(entry.getKey(), substitutedHeaderValues);
            }
            substitutedHeaders =  headers;
        }
        return substitutedHeaders;
    }
}
