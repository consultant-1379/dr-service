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

import spock.lang.Specification
import com.ericsson.bos.dr.service.utils.JSON
import spock.lang.Unroll

class JSONSpec extends Specification {

    @Unroll
    def "Should remove whitespace and newlines from json string"() {

        expect: "Whitespace and newlines are removed from json string"
        JSON.compact(uncompactedJson) == expectedResult

        where:
        uncompactedJson                                                       | expectedResult
        "{\"name\": \"John\",\n\n\"age\": 30,\n\"city\": \"New York\"\n\n\n}" | '{"name":"John","age":30,"city":"New York"}'
        "{\"name\": \"John\",\r\n\"age\": 30,\r\n\"city\": \"New York\"}"     | '{"name":"John","age":30,"city":"New York"}'
        "{\"name  \": \"John\",\"age\": 30,\"  city \": \"New York\"}"        | '{"name  ":"John","age":30,"  city ":"New York"}'
        "\n[\n{\"name\": \"John\"}\n, {\"age\": \"30\"}]"                     | "[{\"name\":\"John\"},{\"age\":\"30\"}]"
    }

    def "Should return original json string when bad data"() {

        expect: "Call method with non json string"
        "i am not a json string" == JSON.compact("i am not a json string")
    }
}
