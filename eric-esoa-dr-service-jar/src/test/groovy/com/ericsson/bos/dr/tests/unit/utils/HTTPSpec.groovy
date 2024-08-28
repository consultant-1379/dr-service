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
package com.ericsson.bos.dr.tests.unit.utils

import com.ericsson.bos.dr.service.utils.HTTP
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType;
import spock.lang.Specification
import spock.lang.Unroll

class HTTPSpec extends Specification {

    @Unroll
    def "Should check content type is application json"() {

        setup: "Set content type"
        HttpHeaders httpHeaders = new HttpHeaders()
        httpHeaders.setContentType(mediaType);

        expect: "Check content type is application json"
        HTTP.isContentTypeApplicationJson(httpHeaders) == expectedResult

        where:
        mediaType                          | expectedResult
        MediaType.APPLICATION_JSON         | true
        MediaType.APPLICATION_OCTET_STREAM | false
        null                               | false
    }
}
