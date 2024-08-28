
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
package com.ericsson.bos.dr.tests.integration.utils

import com.ericsson.bos.dr.service.utils.MapUtils
import spock.lang.Specification

class MapUtilsSpec extends Specification {

    def "Merge maps"() {

        given: "From map"
        def fromMap = ["input2": null, "input3": false, "input4": "value4"]

        and: "To map"
        def toMap = ["input1": "value1", "input2": 2, "input3": true]

        when: "Merge"
        def result = MapUtils.merge(fromMap, toMap)

        then: "Maps are merged. From map will override to map. Will not override to map with null values."
        result == ["input1": "value1", "input2": 2, "input3": false, "input4": "value4"]
    }

}
