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

import com.ericsson.bos.dr.service.compare.filters.PropertiesEqualsPredicate
import spock.lang.Specification

class PropertiesEqualsPredicateSpec extends Specification {

    def "Properties predicate returns the expected result"() {

        when: "test"
        boolean result = new PropertiesEqualsPredicate(sourceProperties).test(
                ["sourceProp", "targetProp"] as String[], targetProperties)

        then: "Result is as expected"
        result == expectedResult

        where:
        sourceProperties                               | targetProperties                         | expectedResult
        [sourceProp: "1"]                              | [targetProp: "1"]                        | true
        [sourceProp: "1"]                              | [targetProp: "2"]                        | false
        [sourceProp: 1]                                | [targetProp: 1]                          | true
        [sourceProp: 1]                                | [targetProp: "1"]                        | true
        [sourceProp: false]                            | [targetProp: false]                      | true
        [sourceProp: false]                            | [targetProp: true]                       | false
        [sourceProp: "false"]                          | [targetProp: false]                      | true
        [sourceProp: ["1", "2"]]                       | [targetProp: ["1", "2"]]                 | true
        [sourceProp: ["1", "2"]]                       | [targetProp: [1, 2]]                     | true
        [sourceProp: ["1", "2"]]                       | [targetProp: [1]]                        | false
        [sourceProp: [k1: "v1", "k2": "v2"]]           | [targetProp: [k1: "v1", "k2": "v2"]]     | true
        [sourceProp: [k1: "v1", "k2": "v2"]]           | [targetProp: ["k2": "v2", "k1": "v1"]]   | true
        [sourceProp: [k1: "v1", "k2": "v2"]]           | [targetProp: ["k1": "v1"]]               | false
        [sourceProp: [k1: "1", "k2": "2", k3: "true"]] | [targetProp: [k1: 1, "k2": 2, k3: true]] | true
    }
}