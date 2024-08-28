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

package com.ericsson.bos.dr.tests.unit.mappers

import com.ericsson.bos.dr.model.mappers.ApiPropertiesFilter
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationJobDtoApiPropertiesInner
import spock.lang.Specification

class ApiPropertiesFilterSpec extends Specification {

    def "Return all source properties with and without source prefix"() {

        setup: "api properties"
        List<String> apiPropertiesName = ["prop1", "source.prop2", "target.prop1"]

        and: "discovered object source properties"
        Map<String, Object> sourceProperties = [prop1: "v1", prop2: "v2", prop3: "v3", prop4: "v4"]

        when: "parse source properties"
        Map<String, Object> result = ApiPropertiesFilter.filterSourceProperties(apiPropertiesName, sourceProperties)

        then: "expected api properties are returned"
        result ==  [prop1: "v1", "source.prop2": "v2"]
    }

    def "Return all target properties with and without target prefix"() {

        setup: "api properties"
        List<String> apiPropertiesName = ["prop1", "source.prop2", "target.prop3"]

        and: "discovered object target properties"
        Map<String, Object> targetProperties = [prop1: "v1", prop2: "v2", prop3: "v3", prop4: "v4"]

        when: "parse target properties"
        Map<String, Object> result = ApiPropertiesFilter.filterTargetProperties(apiPropertiesName, targetProperties)

        then: "expected api properties are returned"
        result ==  [prop1: "v1", "target.prop3": "v3"]
    }
}