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
import com.ericsson.bos.dr.model.mappers.JobMapper
import com.ericsson.bos.dr.model.mappers.JobSummaryMapper
import com.ericsson.bos.dr.web.v1.api.model.JobDto
import com.ericsson.bos.dr.web.v1.api.model.JobSummaryDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

import java.time.Instant

@ContextConfiguration(classes = [JobSummaryMapper.class, JobMapper.class])
class JobEntityMappersSpec extends Specification {

    @Autowired
    JobMapper jobMapper

    @Autowired
    JobSummaryMapper jobSummaryMapper

    def "JobEntityMapper maps to JobDto instance successfully"() {

        setup: "Create JobEntity"
        JobEntity jobEntity = createJobEntity(1)

        when: "Map JobEntity to JobDto"
        JobDto jobDto = jobMapper.apply(jobEntity)

        then: "Attributes converted as expected"
        jobDto.getId() == jobEntity.getId().toString()
        jobDto.getFeaturePackId() == jobEntity.getFeaturePackId().toString()
        jobDto.getFeaturePackName() == jobEntity.getFeaturePackName()
        jobDto.getInputs() == jobEntity.getInputs()
        jobDto.getApplicationId() == jobEntity.getApplicationId().toString()
        jobDto.getApplicationName() == jobEntity.getApplicationName()
        jobDto.getApplicationJobName() == jobEntity.getApplicationJobName()
        jobDto.getStatus().value == jobEntity.getJobStatus()
        jobDto.getStartDate() == jobEntity.getStartDate().toInstant().toString()
        jobDto.getCompletedDate() == jobEntity.getCompletedDate().toInstant().toString()
        jobDto.getName() == jobEntity.getName()
        jobDto.getDescription() == jobEntity.getDescription()
        jobDto.getDiscoveredObjectsCount() == jobEntity.getDiscoveredObjectsCount()
        jobDto.getReconciledObjectsCount() == jobEntity.getReconciledObjectsCount()
        jobDto.getReconciledObjectsErrorCount() == jobEntity.getReconciledObjectsErrorCount()
        jobDto.getErrorMessage() == jobEntity.getErrorMessage()

    }

    def "JobEntityMapper maps to JobSummaryDto instance successfully"() {

        setup: "Create JobEntity"
        JobEntity jobEntity = createJobEntity(1)

        when: "Map JobEntity to JobSummaryDto"
        JobSummaryDto jobSummaryDto = jobSummaryMapper.apply(jobEntity)

        then: "Attributes converted as expected"
        jobSummaryDto.getId() == jobEntity.getId().toString()
        jobSummaryDto.getFeaturePackId() == jobEntity.getFeaturePackId().toString()
        jobSummaryDto.getFeaturePackName() == jobEntity.getFeaturePackName()
        jobSummaryDto.getApplicationId() == jobEntity.getApplicationId().toString()
        jobSummaryDto.getApplicationName() == jobEntity.getApplicationName()
        jobSummaryDto.getApplicationJobName() == jobEntity.getApplicationJobName()
        jobSummaryDto.getStatus().value == jobEntity.getJobStatus()
        jobSummaryDto.getStartDate() == jobEntity.getStartDate().toInstant().toString()
        jobSummaryDto.getCompletedDate() == jobEntity.getCompletedDate().toInstant().toString()
        jobSummaryDto.getName() == jobEntity.getName()
        jobSummaryDto.getDescription() == jobEntity.getDescription()
    }

    JobEntity createJobEntity(Long jobId) {
        JobEntity jobEntity = new JobEntity()
        jobEntity.setId(jobId)
        jobEntity.setReconciledObjectsErrorCount(0)
        jobEntity.setReconciledObjectsCount(0)
        jobEntity.setDiscoveredObjectsCount(0)
        jobEntity.setDescription("a description")
        jobEntity.setFeaturePackId(1)
        jobEntity.setFeaturePackName("fp1")
        jobEntity.setApplicationJobName("applicationjobname")
        jobEntity.setApplicationId(1)
        jobEntity.setApplicationName("app1")
        jobEntity.setName("name")
        jobEntity.setStartDate(Date.from(Instant.now()))
        jobEntity.setCompletedDate(Date.from(Instant.now().plusSeconds(86400)))
        jobEntity.setJobStatus("DISCOVERY_INPROGRESS")
        jobEntity.setInputs(createInputs())
        jobEntity.setErrorMessage("testvalue")
        return jobEntity
    }

    Map<String,Object> createInputs() {
        Map<String,Object> testing = new HashMap<>();
        testing.put("testkey", "testvalue");
        return testing;
    }

}