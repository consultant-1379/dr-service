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
package com.ericsson.bos.dr.tests.integration

import com.ericsson.bos.dr.jpa.PropertiesRepository
import com.ericsson.bos.dr.web.v1.api.model.FeaturePackDto
import org.springframework.boot.test.mock.mockito.MockBean
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.times

class PropertiesServiceSpec extends BaseSpec {

    @MockBean
    PropertiesRepository propertiesRepository

    def "Feature pack properties are read from cache after initial fetch"() {
        setup: "Upload feature pack with properties"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-1", "fp-1-${System.currentTimeMillis()}")

        when: "Get properties multiple times"
        propertiesService.getProperties(featurePackDto.getId().toLong())
        propertiesService.getProperties(featurePackDto.getId().toLong())
        propertiesService.getProperties(featurePackDto.getId().toLong())

        then: "Properties are only fetched from repository once"
        def v = verify(propertiesRepository, times(1)).findByFeaturePackId(featurePackDto.getId().toLong());
    }
}
