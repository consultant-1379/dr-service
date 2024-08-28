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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification
import spock.lang.Unroll

@ContextConfiguration(classes = [NumberAndBooleanStringifier.class])
class NumberAndBooleanStringifierSpec extends Specification {

    @Autowired
    NumberAndBooleanStringifier numberAndBooleanStringifier

    @Unroll
    def "Numbers and booleans in map are converted to string"() {

        given: 'Map'
        Map map = ["key1": value]

        when: 'To string'
        Map result = numberAndBooleanStringifier.toString(map)

        then: 'Numbers and booleans are converted to string'
        result.get("key1") == expected_result

        where:
        value               | expected_result
        true                | "true"
        false               | "false"
        1                   | "1" // int
        999L                | "999" // long
        999D                | "999.0" // double
        3.4F                | "3.4" // floating point
        1.1                 | "1.1" // big decimal
        3.445e2             | "344.5" // big decimal
        3.445e20            | "3.445E+20" // big decimal
        0x2F                | "47" // hexadecimal
        0b10010             | "18" // binary
        027                 | "23" // octal
        [-1, true, "3"]     | ["-1", "true", "3"] // map contains list
        [1, ["key2": 3]]    | ["1", ["key2": "3"]] // map contains list that contains a map
        ["key2": 5]         | ["key2": "5"] // map contains a map
        ["key2": [1, 2, 3]] | ["key2": ["1", "2", "3"]] // map contains a map that contains a list
    }

    @Unroll
    def "Numbers and booleans in list are converted to string"() {

        given: 'List'
        List list = items

        when: 'To string'
        List result = numberAndBooleanStringifier.toString(list)

        then: 'Numbers and booleans are converted to string'
        result == expected_result

        where:
        items                                                             | expected_result
        [true, false]                                                     | ["true", "false"]
        [1, 999l, 999D, 3.4F, 1.1, 3.445e2, 3.445e20, 0X2F, 0b10010, 027] | ["1", "999", "999.0", "3.4", "1.1", "344.5", "3.445E+20", "47", "18", "23"]
        [-1, ["key1": -1, "key2": true]]                                  | ["-1", ["key1": "-1", "key2": "true"]]  // list contains a map
        [2, ["key1": [1, 2, 3, false]]]                                   | ["2", ["key1": ["1", "2", "3", "false"]]] // list contains a map that
        [true, [1, false, 3.5]]                                           | ["true", ["1", "false", "3.5"]] // list contains a list
    }

    @Unroll
    def "Numbers and booleans are converted to string"() {

        given: 'Object'
        Object object = numberOrBoolean

        when: 'To string'
        Object result = numberAndBooleanStringifier.toString(numberOrBoolean)

        then: 'Numbers and booleans are converted to string'
        result == expected_result

        where:
        numberOrBoolean | expected_result
        true            | "true"
        false           | "false"
        1               | "1" // int
        -1              | "-1"
        999L            | "999" // long
        999D            | "999.0" // double
        3.4F            | "3.4" // floating point
        1.1             | "1.1" // big decimal
        3.445e2         | "344.5" // big decimal
        3.445e20        | "3.445E+20" // big decimal
        0x2F            | "47" // hexadecimal
        0b10010         | "18" // binary
        027             | "23" // octal
    }
}
