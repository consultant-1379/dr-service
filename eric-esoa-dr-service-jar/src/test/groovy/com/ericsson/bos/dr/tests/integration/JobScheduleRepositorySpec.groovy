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

import com.ericsson.bos.dr.jpa.JobScheduleRepository
import com.ericsson.bos.dr.jpa.JobSpecificationRepository
import com.ericsson.bos.dr.jpa.model.JobEntity
import com.ericsson.bos.dr.jpa.model.JobScheduleEntity
import com.ericsson.bos.dr.jpa.model.JobSpecificationEntity
import com.ericsson.bos.dr.web.v1.api.model.ExecuteJobDtoExecutionOptions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.annotation.Transactional

import java.time.Instant

class JobScheduleRepositorySpec extends BaseSpec {

    @Autowired
    JobScheduleRepository jobScheduleRepository

    @Autowired
    JobSpecificationRepository jobSpecificationRepository

    JobSpecificationEntity jobSpecificationEntity = new JobSpecificationEntity(name: "job-1", description: "my job", applicationId: 1,
            applicationName: "app_1", featurePackName: "fp-1", featurePackId: 1, executionOptions: new ExecuteJobDtoExecutionOptions().autoReconcile (
            true), inputs: ["input1": 1], applicationJobName: "job-1")

    def"JobSchedule entity is successfully saved"() {

        setup: "Create JobSchedule entity"
        JobScheduleEntity jobScheduleEntity = new JobScheduleEntity(name: "schedule-1", description: "my schedule",
                expression: "0 0/15 * 1/1 * ? *", "enabled": true, version: 1,
                jobSpecification: jobSpecificationEntity, creationDate: Date.from(Instant.now()))

        when: "Save JobSchedule entity"
        JobScheduleEntity savedJobScheduleEntity = jobScheduleRepository.save(jobScheduleEntity)

        then: "Properties are saved with expected values"
        savedJobScheduleEntity.id != null
        savedJobScheduleEntity.creationDate != null
        savedJobScheduleEntity.name == jobScheduleEntity.name
        savedJobScheduleEntity.description == jobScheduleEntity.description
        savedJobScheduleEntity.enabled == jobScheduleEntity.enabled
        savedJobScheduleEntity.jobSpecification != null
        savedJobScheduleEntity.jobSpecification.id != null
        savedJobScheduleEntity.jobSpecification.name == jobSpecificationEntity.name
        savedJobScheduleEntity.jobSpecification.description == jobSpecificationEntity.description
        savedJobScheduleEntity.jobSpecification.applicationName == jobSpecificationEntity.applicationName
        savedJobScheduleEntity.jobSpecification.applicationId == jobSpecificationEntity.applicationId
        savedJobScheduleEntity.jobSpecification.featurePackName == jobSpecificationEntity.featurePackName
        savedJobScheduleEntity.jobSpecification.featurePackId == jobSpecificationEntity.featurePackId
        savedJobScheduleEntity.jobSpecification.inputs == jobSpecificationEntity.inputs
        savedJobScheduleEntity.jobSpecification.executionOptions == jobSpecificationEntity.executionOptions
    }

    def "Delete JobSpecification after all associated JobSchedules are deleted"() {

        setup: "Create and save 3 JobSchedule entities associated with the same jobSpecification"
        JobScheduleEntity jobScheduleEntity = new JobScheduleEntity(name: "schedule-1", description: "my schedule",
                expression: "0 0/15 * 1/1 * ? *", "enabled": true, version: 1,
                jobSpecification: jobSpecificationEntity, creationDate: Date.from(Instant.now()))
        JobScheduleEntity savedJobScheduleEntity = jobScheduleRepository.save(jobScheduleEntity)
        JobScheduleEntity jobScheduleEntity2 = new JobScheduleEntity(name: "schedule-2", description: "my schedule",
                expression: "0 0/15 * 1/1 * ? *", "enabled": true, version: 1,
                jobSpecification: savedJobScheduleEntity.jobSpecification, creationDate: Date.from(Instant.now()))
        JobScheduleEntity savedJobScheduleEntity2 = jobScheduleRepository.save(jobScheduleEntity2)
        JobScheduleEntity jobScheduleEntity3 = new JobScheduleEntity(name: "schedule-3", description: "my schedule",
                expression: "0 0/15 * 1/1 * ? *", "enabled": true, version: 1,
                jobSpecification: savedJobScheduleEntity2.jobSpecification, creationDate: Date.from(Instant.now()))
        JobScheduleEntity savedJobScheduleEntity3 = jobScheduleRepository.save(jobScheduleEntity3)

        when: "Delete the 3 JobSchedule entities"
        jobScheduleRepository.deleteById(savedJobScheduleEntity.id)
        jobScheduleRepository.deleteById(savedJobScheduleEntity2.id)
        jobScheduleRepository.deleteById(savedJobScheduleEntity3.id)

        then: "JobSpecification still exists"
        jobSpecificationRepository.findById(savedJobScheduleEntity.jobSpecification.id).isPresent()

        and: "Delete the JobSpecification"
        jobSpecificationRepository.deleteById(savedJobScheduleEntity.jobSpecification.id)

        then: "JobSpecification is deleted"
        jobSpecificationRepository.findById(savedJobScheduleEntity.jobSpecification.id).isPresent() == false
    }

    def "Error deleting JobSpecification when it is associated with a JobSchedule"() {

        setup: "Create and save JobSchedule entity and associated jobSpecification"
        JobScheduleEntity jobScheduleEntity = new JobScheduleEntity(name: "schedule-1", description: "my schedule",
                expression: "0 0/15 * 1/1 * ? *", "enabled": true, version: 1,
                jobSpecification: jobSpecificationEntity, creationDate: Date.from(Instant.now()))
        JobScheduleEntity savedJobScheduleEntity = jobScheduleRepository.save(jobScheduleEntity)

        when: "Delete the JobSpecification"
        jobSpecificationRepository.deleteById(savedJobScheduleEntity.jobSpecification.id)

        then: "Cannot delete JobSpecification"
        thrown(DataIntegrityViolationException)
    }

    def "Save JobSchedule entity fails when missing mandatory name property"() {

        setup: "Create JobSchedule entity without setting name"
        JobScheduleEntity jobScheduleEntity = new JobScheduleEntity(description: "my schedule",
                expression: "0 0/15 * 1/1 * ? *", "enabled": true, version: 1,
                jobSpecification: jobSpecificationEntity, creationDate: Date.from(Instant.now()))

        when: "Save JobSchedule entity"
        jobScheduleRepository.save(jobScheduleEntity)

        then: "Error saving entity"
        DataIntegrityViolationException ex = thrown(DataIntegrityViolationException)
        ex.message.contains("ERROR: null value in column \"name\"")
    }

    def "Save JobSchedule entity fails when missing mandatory expression property"() {

        setup: "Create JobSchedule entity without setting expression"
        JobScheduleEntity jobScheduleEntity = new JobScheduleEntity(name: "schedule-1", description: "my schedule",
                "enabled": true, version: 1,
                jobSpecification: jobSpecificationEntity, creationDate: Date.from(Instant.now()))

        when: "Save JobSchedule entity"
        jobScheduleRepository.save(jobScheduleEntity)

        then: "Error saving entity"
        DataIntegrityViolationException ex = thrown(DataIntegrityViolationException)
        ex.message.contains("ERROR: null value in column \"expression\"")
    }

    def "Save JobSchedule entity fails when missing mandatory jobSpecification property"() {

        setup: "Create JobSchedule entity without setting job"
        JobScheduleEntity jobScheduleEntity = new JobScheduleEntity(name: "schedule-1", description: "my schedule",
                expression: "0 0/15 * 1/1 * ? *","enabled": true, version: 1,
                creationDate: Date.from(Instant.now()))

        when: "Save JobSchedule entity"
        jobScheduleRepository.save(jobScheduleEntity)

        then: "Error saving entity"
        DataIntegrityViolationException ex = thrown(DataIntegrityViolationException)
        ex.message.contains("ERROR: null value in column \"job_specification_id\"")
    }

    @Transactional
    def "Delete Job entity that references a Job schedule"() {
        when: "Create and save Job Schedule and associated Job entity"
        JobScheduleEntity jobScheduleEntity = new JobScheduleEntity(name: "schedule-1", description: "my schedule",
                expression: "0 0/15 * 1/1 * ? *", "enabled": true, version: 1,
                jobSpecification: jobSpecificationEntity, creationDate: Date.from(Instant.now()))
        JobScheduleEntity savedJobScheduleEntity = jobScheduleRepository.save(jobScheduleEntity)
        JobEntity jobEntity = new JobEntity(jobSpecification: savedJobScheduleEntity.jobSpecification, startDate:
                Date.from(Instant.now()), completedDate:
                Date.from (Instant.now()), jobStatus: "PARTIALLY_RECONCILED", applicationJobName: "job-1",
                jobScheduleId: savedJobScheduleEntity.id)
        JobEntity savedJobEntity = jobRepository.save(jobEntity)

        and: "Delete Job entity"
        jobRepository.deleteById(savedJobEntity.id)

        then: "Job entity is deleted"
        jobRepository.findById(savedJobEntity.id).isPresent() == false
    }

    @Transactional
    def "Delete Job schedule that is referenced by a job"() {
        when: "Create and save Job Schedule and associated Job entity"
        JobScheduleEntity jobScheduleEntity = new JobScheduleEntity(name: "schedule-1", description: "my schedule",
                expression: "0 0/15 * 1/1 * ? *", "enabled": true, version: 1,
                jobSpecification: jobSpecificationEntity, creationDate: Date.from(Instant.now()))
        JobScheduleEntity savedJobScheduleEntity = jobScheduleRepository.save(jobScheduleEntity)
        JobEntity jobEntity = new JobEntity(jobSpecification: savedJobScheduleEntity.jobSpecification, startDate:
                Date.from(Instant.now()), completedDate:
                Date.from (Instant.now()), jobStatus: "PARTIALLY_RECONCILED", applicationJobName: "job-1",
                jobScheduleId: savedJobScheduleEntity.id)
        jobRepository.save(jobEntity)

        and: "Delete Job schedule"
        jobScheduleRepository.deleteById(savedJobScheduleEntity.id)

        then: "Job schedule is deleted"
        jobScheduleRepository.findById(savedJobScheduleEntity.id).isPresent() == false
    }
}