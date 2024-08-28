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

import com.ericsson.bos.dr.service.utils.JQ
import com.ericsson.bos.dr.tests.integration.utils.JsonUtils
import com.fasterxml.jackson.databind.JsonNode
import spock.lang.Specification

class JQSpec extends Specification {

    def "JQ query response is converted to expected Object"() {

        when: "Perform JQ operation"
        JsonNode jsonNode = JsonUtils.read(json, JsonNode.class)
        JQ.JQResult result = JQ.query(query, jsonNode)

        then: "result is as expected"
        result.getObject() == expectedResult

        where:
        json                            | query             | expectedResult
        '{"id": 1}'                     | ".name"           | null
        '{"id": 1}'                     | ".id"             | 1
        '{"id": "1"}'                   | ".id"             | "1"
        '{"value": true}'               | ".value"          | true
        '{"value": "true"}'             | ".value"          | "true"
        '{"value": {"id": 1}}'          | ".value.id"       | 1
        '{"value": {"items": [1,2,3]}}' | ".value.items[0]" | 1
        '[{"id": 1}, {"id": 2}]'        | ".[] | .id"       | [1, 2]
        '[{"id": 1}, {"id": 2}]'        | ".[] | {id}"      | [[id: 1], [id: 2]]
    }

    def "queryEach returns result for each key in map"() {
        setup:
        JsonNode jsonNode = JsonUtils.read('{"id": 1, "name": "object1"}', JsonNode.class)
        Map jqExpressions = [id: ".id", name: ".name", other: ".other"]

        when: "Perform JQ operations on value in map"
        Map result = JQ.queryEach(jqExpressions, jsonNode)

        then: "result is as expected"
        result['id'] == 1
        result['name'] == "object1"
        result['other'] == null
    }
}