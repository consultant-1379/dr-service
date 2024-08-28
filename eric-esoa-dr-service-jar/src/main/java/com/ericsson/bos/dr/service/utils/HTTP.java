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
package com.ericsson.bos.dr.service.utils;

import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * HTTP utilities.
 */
public class HTTP {

    /**
     * Returns true if content type is application json.
     *
     * @param headers
     *         http headers
     * @return true if content type is application json, false if not application json or headers is null
     */
    public static boolean isContentTypeApplicationJson(final HttpHeaders headers) {
        return Optional.ofNullable(headers)
                .map(HttpHeaders::getContentType)
                .map(MediaType.APPLICATION_JSON::equals)
                .orElse(false);
    }
}
