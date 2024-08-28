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

import com.ericsson.bos.dr.jpa.model.JobEntity
import com.ericsson.bos.dr.jpa.model.JobSpecificationEntity
import com.ericsson.bos.dr.web.v1.api.model.ExecuteJobDtoExecutionOptions
import com.ericsson.bos.dr.web.v1.api.model.ExecuteReconcileDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate

import java.time.Instant
import java.time.LocalDateTime

class JobRepositorySpec extends BaseSpec {

    @Autowired
    PlatformTransactionManager platformTransactionManager

    JobSpecificationEntity jobSpecificationEntity = new JobSpecificationEntity(name: "job-1", description: "my job", applicationId: 1,
            applicationName: "app_1", featurePackName: "fp-1", featurePackId: 1, executionOptions: new ExecuteJobDtoExecutionOptions().autoReconcile (
            true), inputs: ["input1": 1], applicationJobName: "job-1")

    def "Job entity is successfully saved"() {

        setup: "Create Job entity"
        JobEntity jobEntity = new JobEntity(jobSpecification: jobSpecificationEntity, startDate: Date.from(Instant.now()), completedDate:
                Date.from (Instant.now()), discoveredObjectsCount: 10, reconciledObjectsCount: 9, reconciledObjectsErrorCount: 1, jobStatus:
                "PARTIALLY_RECONCILED", applicationJobName: "job-1", errorMessage: "None", reconcileRequest: new ExecuteReconcileDto().inputs([intput1: 1]))

        when: "Save Job entity"
        JobEntity savedJobEntity = jobRepository.save(jobEntity)

        then: "Properties are saved with expected values"
        savedJobEntity.id != null
        savedJobEntity.startDate != null
        savedJobEntity.completedDate != null
        savedJobEntity.jobStatus == jobEntity.jobStatus
        savedJobEntity.errorMessage == jobEntity.errorMessage
        savedJobEntity.reconciledObjectsCount == jobEntity.reconciledObjectsCount
        savedJobEntity.reconciledObjectsErrorCount == jobEntity.reconciledObjectsErrorCount
        savedJobEntity.discoveredObjectsCount == jobEntity.discoveredObjectsCount
        savedJobEntity.reconcileRequest == jobEntity.reconcileRequest
        savedJobEntity.jobSpecification != null
        savedJobEntity.name == jobSpecificationEntity.name
        savedJobEntity.description == jobSpecificationEntity.description
        savedJobEntity.applicationName == jobSpecificationEntity.applicationName
        savedJobEntity.applicationId == jobSpecificationEntity.applicationId
        savedJobEntity.featurePackName == jobSpecificationEntity.featurePackName
        savedJobEntity.featurePackId == jobSpecificationEntity.featurePackId
        savedJobEntity.inputs == jobSpecificationEntity.inputs
        savedJobEntity.executionOptions == jobSpecificationEntity.executionOptions
    }

    def "Delete JobSpecification after associated Job is deleted"() {
        setup: "Create and save Job entity with jobSpecification"
        JobEntity jobEntity = new JobEntity(jobSpecification: jobSpecificationEntity, startDate: Date.from(Instant.now()), completedDate:
                Date.from (Instant.now()), discoveredObjectsCount: 10, reconciledObjectsCount: 9, reconciledObjectsErrorCount: 1, jobStatus:
                "PARTIALLY_RECONCILED", errorMessage: "None", reconcileRequest: new ExecuteReconcileDto().inputs([intput1: 1]))
        JobEntity savedJobEntity = jobRepository.save(jobEntity)

        when: "Delete Job entity"
        jobRepository.deleteById(savedJobEntity.id)

        then: "JobSpecification still exists"
        jobSpecificationRepository.findById(savedJobEntity.jobSpecification.id).isPresent()

        when: "Delete JobSpecification"
        jobSpecificationRepository.deleteById(savedJobEntity.jobSpecification.id)

        then: "JobSpecification is  deleted"
        jobSpecificationRepository.findById(savedJobEntity.jobSpecification.id).isPresent() == false
    }


    def "Error deleting JobSpecification when it is associated with a Job"() {

        setup: "Create and save Job entity and associated jobSpecification"
        JobEntity jobEntity = new JobEntity(jobSpecification: jobSpecificationEntity, startDate: Date.from(Instant.now()), completedDate:
                Date.from (Instant.now()), discoveredObjectsCount: 10, reconciledObjectsCount: 9, reconciledObjectsErrorCount: 1, jobStatus:
                "PARTIALLY_RECONCILED", errorMessage: "None", reconcileRequest: new ExecuteReconcileDto().inputs([intput1: 1]))
        JobEntity savedJobEntity = jobRepository.save(jobEntity)

        when: "Delete the JobSpecification"
        jobSpecificationRepository.deleteById(savedJobEntity.jobSpecification.id)

        then: "Cannot delete JobSpecification"
        thrown(DataIntegrityViolationException)
    }

    def "Save Job entity fails when missing mandatory jobSpecification properties"() {

        setup: "Create Job entity without setting job specification properties"
        JobEntity jobEntity = new JobEntity(startDate: Date.from(Instant.now()), completedDate:
                Date.from (Instant.now()), discoveredObjectsCount: 10, reconciledObjectsCount: 9, reconciledObjectsErrorCount: 1, jobStatus:
                "PARTIALLY_RECONCILED", errorMessage: "None", reconcileRequest: new ExecuteReconcileDto().inputs([intput1: 1]))

        when: "Save Job entity"
        jobRepository.save(jobEntity)

        then: "Error saving entity"
        DataIntegrityViolationException ex = thrown(DataIntegrityViolationException)
        ex.message.contains("ERROR: null value in column \"name\" of relation \"job_specification\"")
    }

    @Transactional
    def "Find scheduled executable jobs with due date reached"() {

        setup: "Save 1 job entity in state SCHEDULED"
        JobEntity jobEntity1 = new JobEntity(jobStatus: "SCHEDULED", jobSpecification:  jobSpecificationEntity, dueDate: LocalDateTime.parse(dueDate))
        JobEntity jobEntity2 = new JobEntity(jobStatus: "COMPLETED", jobSpecification:  jobSpecificationEntity, dueDate: null)
        jobRepository.saveAll([jobEntity1, jobEntity2])

        when: "Find executable jobs"
        final var pageable = PageRequest.of(0, 10, Sort.by("modifiedDate").ascending());
        List<JobEntity> jobEntities = jobRepository.findExecutableJobs(pageable, LocalDateTime.parse(queryDate))

        then: "Scheduled job entities with due date found"
        jobEntities.size() == expectedResultSize

        where:
        dueDate               | queryDate             | expectedResultSize
        "2024-03-01T09:00:00" | "2024-03-01T09:00:00" | 1
        "2024-03-01T09:00:00" | "2024-03-01T09:01:00" | 1
        "2024-03-01T09:00:00" | "2024-03-01T08:59:59" | 0
    }

    @Transactional
    def "Find unscheduled executable jobs"() {

        setup: "Save 1 job entity in with configured state and locked values"
        JobEntity jobEntity1 = new JobEntity(jobStatus: jobStatus, jobSpecification:  jobSpecificationEntity, locked: locked)
        jobRepository.save(jobEntity1)

        when: "Find executable jobs"
        final var pageable = PageRequest.of(0, 10, Sort.by("modifiedDate").ascending());
        List<JobEntity> jobEntities = jobRepository.findExecutableJobs(pageable, null)

        then: "Scheduled job entities with due date found"
        jobEntities.size() == expectedResultSize

        where:
        jobStatus              | locked | expectedResultSize
        "NEW"                  | false  | 1
        "NEW"                  | true   | 0
        "RECONCILE_REQUESTED"  | false  | 1
        "RECONCILE_REQUESTED"  | true   | 0
        "COMPLETED"            | false  | 0
        "DISCOVERY_FAILED"     | false  | 0
        "PARTIALLY_RECONCILED" | false  | 0
        "RECONCILE_FAILED"     | false  | 0
    }

    @Transactional
    def "Find executable jobs returns paged results"() {

        setup: "Save 2 job entities"
        JobEntity jobEntity1 = new JobEntity(jobStatus: "NEW", jobSpecification:  jobSpecificationEntity, locked: false)
        JobSpecificationEntity jobSpecificationEntity2 = new JobSpecificationEntity(name: "job-2", description: "my job", applicationId: 1,
                applicationName: "app_1", featurePackName: "fp-1", featurePackId: 1, executionOptions: new ExecuteJobDtoExecutionOptions().autoReconcile (
                true), inputs: ["input1": 1], applicationJobName: "job-1")
        JobEntity jobEntity2 = new JobEntity(jobStatus: "NEW", jobSpecification:  jobSpecificationEntity2, locked: false)
        jobRepository.saveAll([jobEntity1, jobEntity2])

        when: "Find executable jobs, sorted by name and pageSize 1"
        final var pageable = PageRequest.of(0, 1, sortBy);
        List<JobEntity> jobEntities = jobRepository.findExecutableJobs(pageable, null)

        then: "Paged jobs are returned as expected"
        jobEntities.size() == 1
        jobEntities[0].name == expectedJobName

        where:
        sortBy | expectedJobName
        Sort.by("jobSpecification.name").ascending() | "job-1"
        Sort.by("jobSpecification.name").descending() | "job-2"
    }

    def "Concurrent threads do not find the same job due to use of SKIP_LOCKED"() {

        setup: "Save 1 job entity in state SCHEDULED, with due date set to next day"
        JobEntity jobEntity1 = new JobEntity(jobStatus: "SCHEDULED", jobSpecification:  jobSpecificationEntity, dueDate:
                LocalDateTime.now().plusDays(1))
        JobEntity savedJobEntity1 = jobRepository.save(jobEntity1)

        when: "Run 2 threads to find executable jobs in state SCHEDULED with limit 1 and wait for them to complete"
        List t1JobEntities = []
        Thread t1 = new Thread(() -> {
            t1JobEntities.addAll(findAndLockExecutableJobs())
        })
        List t2JobEntities = []
        Thread t2 = new Thread(() -> {
            t2JobEntities.addAll(findAndLockExecutableJobs())
        })

        t1.start()
        t2.start()
        t1.join()
        t2.join()

        then: "Only 1 of the threads finds the job"
        // without use of SKIP_LOCKED, both threads would find the same job
        ((t1JobEntities.size() == 1 && t1JobEntities.get(0).id == savedJobEntity1.id) && t2JobEntities.size() == 0) ||
                (t1JobEntities.size() == 0 && (t2JobEntities.size() == 1 && t2JobEntities.get(0).id == savedJobEntity1.id))
    }

    private List<JobEntity> findAndLockExecutableJobs() {
        return new TransactionTemplate(platformTransactionManager)
                .execute(status -> {
                    List<JobEntity> jobEntities = jobRepository.findExecutableJobs(PageRequest.of(0,1), LocalDateTime.now().plusDays(2))
                    jobEntities.each { it.setLocked(true) }
                    return jobEntities
                })
    }
}