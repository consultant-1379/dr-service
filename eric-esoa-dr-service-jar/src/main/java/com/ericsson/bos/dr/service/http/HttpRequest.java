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
package com.ericsson.bos.dr.service.http;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.ericsson.bos.dr.service.utils.HTTP;
import com.ericsson.bos.dr.service.utils.JSON;

/**
 * Http Request Properties
 */
public class HttpRequest {

    private String url;
    private String method;
    private Object body;
    private HttpHeaders headers;
    private Integer readTimeoutSeconds;
    private Integer writeTimeoutSeconds;
    private Integer connectTimeoutSeconds;
    private boolean encodeUrl = true;

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(final String method) {
        this.method = method;
    }

    public Optional<Object> getBody() {
        return Optional.ofNullable(body);
    }

    /**
     * Set body. If the body is a json string whitespace and newlines will be removed.
     *
     * @param body
     *         body
     */
    public void setBody(final Object body) {
        // We compact the body for more efficient storage in the db
        // and to make the logs more readable.
        Optional.ofNullable(body).ifPresent(b -> {
            if (isCompactibleBody(b)) {
                this.body = JSON.compact((String) b);
            } else {
                this.body = b;
            }
        });
    }

    public Optional<HttpHeaders> getHeaders() {
        return Optional.ofNullable(headers);
    }

    /**
     * Set headers.
     *
     * @param headers
     *         headers
     */
    public void setHeaders(final Map<String, List<String>> headers) {
        if (headers != null) {
            final MultiValueMap<String, String> multiValueHeadersMap = new LinkedMultiValueMap<>();
            multiValueHeadersMap.putAll(headers);
            this.headers = new HttpHeaders(multiValueHeadersMap);
        }
    }

    public Optional<Integer> getConnectTimeoutSeconds() {
        return Optional.ofNullable(connectTimeoutSeconds);
    }

    public Optional<Integer> getReadTimeoutSeconds() {

        return Optional.ofNullable(readTimeoutSeconds);
    }

    public void setReadTimeoutSeconds(final Integer readTimeoutSeconds) {
        this.readTimeoutSeconds = readTimeoutSeconds;
    }

    public Optional<Integer> getWriteTimeoutSeconds() {
        return Optional.ofNullable(writeTimeoutSeconds);
    }

    public void setWriteTimeoutSeconds(final Integer writeTimeoutSeconds) {
        this.writeTimeoutSeconds = writeTimeoutSeconds;
    }

    public void setConnectTimeoutSeconds(final Integer connectTimeoutSeconds) {
        this.connectTimeoutSeconds = connectTimeoutSeconds;
    }

    public boolean isEncodeUrl() {
        return encodeUrl;
    }

    public void setEncodeUrl(boolean encodeUrl) {
        this.encodeUrl = encodeUrl;
    }

    private boolean isCompactibleBody(final Object body) {
        return (body instanceof String) &&
               (HTTP.isContentTypeApplicationJson(headers) || JSON.isJsonStr((String) body));
    }

    @Override
    public String toString() {
        final var sb = new StringJoiner("\n");
        sb.add("Method: " + getMethod());
        sb.add("URL: " + getUrl());
        getBody().ifPresent(b -> sb.add("Body: " + b));
        getHeaders().ifPresent(h -> sb.add("Headers: " + h));
        getConnectTimeoutSeconds().ifPresent(t -> sb.add("Connection timeout: " + t));
        getReadTimeoutSeconds().ifPresent(t -> sb.add("Read timeout: " + t));
        getWriteTimeoutSeconds().ifPresent(t -> sb.add("Write timeout: " + t));
        return sb.toString();
    }

    /**
     * Factory method to return a GET Http request
     * @param url url for the get method
     * @param enocodeUrl encode the url
     * @return Http request
     */
    public static HttpRequest get(String url, boolean enocodeUrl) {
        final var httpRequest = new HttpRequest();

        final Map<String, List<String>> headers = new HashMap<>();
        headers.put("content-type", List.of("application/json"));

        httpRequest.setUrl(url);
        httpRequest.setMethod("GET");
        httpRequest.setBody(null);
        httpRequest.setEncodeUrl(enocodeUrl);
        httpRequest.setHeaders(headers);
        httpRequest.setConnectTimeoutSeconds(10);
        httpRequest.setReadTimeoutSeconds(60);
        httpRequest.setWriteTimeoutSeconds(60);
        return httpRequest;
    }
}
