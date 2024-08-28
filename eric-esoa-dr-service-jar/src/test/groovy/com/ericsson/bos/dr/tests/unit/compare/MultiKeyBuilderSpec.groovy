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

package com.ericsson.bos.dr.tests.unit.compare

import com.ericsson.bos.dr.service.compare.filters.NumberAndBooleanStringifier
import org.apache.commons.collections4.keyvalue.MultiKey
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification
import com.ericsson.bos.dr.service.compare.filters.MultiKeyBuilder
import spock.lang.Unroll

@ContextConfiguration(classes = [MultiKeyBuilder.class, NumberAndBooleanStringifier.class])
class MultiKeyBuilderSpec extends Specification {

    @Autowired
    MultiKeyBuilder multiKeyBuilder

    @Unroll
    def "Build multi key"() {

        when: 'Build multi key'
        MultiKey result = multiKeyBuilder.build(properties, args).get()

        then: 'Multi key contains expected keys, and numbers/booleans are converted to strings'
        result.getKeys() == expected_keys

        where:
        properties                                        | args             | expected_keys
        ["key1": "value1", "key2": 2, "key3": "value3"]   | ["key1", "key2"] | ["value1", "2"]
        ["key1": 1.1, "key2": 2]                          | ["key1", "key2"] | ["1.1", "2"]
        ["key1": false, "key2": true]                     | ["key1", "key2"] | ["false", "true"]
        ["key1": [1, 2, 3]]                               | ["key1"]         | [["1", "2", "3"]]
        ["key1": [1, ["key2": false]]]                    | ["key1"]         | [["1", ["key2": "false"]]]
        ["key1": ["key1": 1, "key2": true]]               | ["key1"]         | [["key1": "1", "key2": "true"]]
        ["key1": ["key1": 1, "key2": [true, false, 1.1]]] | ["key1"]         | [["key1": "1", "key2": ["true", "false", "1.1"]]]
    }

    def "Returns empty optional if property value is null"() {

        when: 'Build multi key with null value'
        Optional<MultiKey> result = multiKeyBuilder.build(["key1": null], ["key1"])

        then: 'Empty optional returned'
        result == Optional.empty()
    }
}

