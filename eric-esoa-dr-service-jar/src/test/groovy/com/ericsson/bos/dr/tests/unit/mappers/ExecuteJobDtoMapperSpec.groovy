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

import com.ericsson.bos.dr.jpa.model.JobEntity
import com.ericsson.bos.dr.model.mappers.ExecuteJobDtoMapper
import com.ericsson.bos.dr.web.v1.api.model.ExecuteJobDto
import com.ericsson.bos.dr.web.v1.api.model.ExecuteJobDtoExecutionOptions
import spock.lang.Specification

class ExecuteJobDtoMapperSpec extends Specification {

    def "Should map ExecuteJobDto to JobEntity"() {

        setup: "JobEntity"
        JobEntity jobEntity = new JobEntity([name              : "entity_1",
                                             description       : "job description",
                                             featurePackId     : 1,
                                             featurePackName   : "FP-1",
                                             applicationId     : 2,
                                             applicationName   : "app_1",
                                             applicationJobName: 'job1',
                                             inputs            : ["prop_1": "value_1"],
                                             executionOptions  : new ExecuteJobDtoExecutionOptions([autoReconcile: true]),
                                             jobStatus         : "completed"])

        when: "Apply mapper"
        ExecuteJobDto actualExecuteJobDto = new ExecuteJobDtoMapper().apply(jobEntity)

        then: "ExecuteJobDto mapped to JobEntity"
        with(actualExecuteJobDto) {
            name == jobEntity.name
            description == jobEntity.description
            featurePackId == String.valueOf(jobEntity.featurePackId)
            featurePackName == jobEntity.featurePackName
            applicationId == String.valueOf(jobEntity.applicationId)
            applicationName == jobEntity.applicationName
            applicationJobName == jobEntity.applicationJobName
            inputs == jobEntity.inputs
            executionOptions.properties == jobEntity.executionOptions.properties
        }
    }
}
