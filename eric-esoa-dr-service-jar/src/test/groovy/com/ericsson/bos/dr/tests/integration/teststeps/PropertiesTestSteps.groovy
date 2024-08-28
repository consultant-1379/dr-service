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
package com.ericsson.bos.dr.tests.integration.teststeps

import com.ericsson.bos.dr.jpa.PropertiesRepository
import com.ericsson.bos.dr.jpa.model.PropertiesEntity
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class PropertiesTestSteps {

    @Autowired
    private PropertiesRepository propertiesRepository

    Optional<PropertiesEntity> getProperties(String featurePackId) {
        return propertiesRepository.findByFeaturePackId(Long.valueOf(featurePackId))
    }
}
