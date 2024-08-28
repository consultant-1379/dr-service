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

import com.ericsson.bos.dr.jpa.model.DiscoveryObjectEntity
import com.ericsson.bos.dr.jpa.model.JobEntity
import com.ericsson.bos.dr.jpa.model.JobScheduleEntity
import com.ericsson.bos.dr.tests.integration.utils.JsonUtils
import com.ericsson.bos.dr.web.v1.api.model.DeleteJobsResponseDto
import com.ericsson.bos.dr.web.v1.api.model.DiscoveredObjectListDto
import com.ericsson.bos.dr.web.v1.api.model.ExecuteJobDto
import com.ericsson.bos.dr.web.v1.api.model.FeaturePackDto
import com.ericsson.bos.dr.web.v1.api.model.JobDto
import com.ericsson.bos.dr.web.v1.api.model.JobListDto
import com.ericsson.bos.dr.web.v1.api.model.JobSummaryDto
import org.springframework.test.web.servlet.ResultActions
import spock.lang.Unroll

import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.stream.Collectors

import static com.ericsson.bos.dr.service.exceptions.ErrorCode.APP_NOT_FOUND
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.CANNOT_FORCE_DELETE_INPROGRESS_SCHEDULED_JOBS
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.CANNOT_DELETE_SCHEDULED_JOB
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.FP_NOT_FOUND
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.ID_OR_NAME_NOT_PROVIDED
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.INVALID_FILTER_PARAM
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.INVALID_SORTING_PARAM
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.JOB_NOT_FOUND
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.OPERATION_ONGOING
import static com.ericsson.bos.dr.web.v1.api.model.DiscoveredObjectDto.StatusEnum
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class JobServiceSpec extends BaseSpec {

    def "Create job successfully with at least one id or name for featurePack and application"() {
        setup: "Create executeJobDto"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-1", "fp-1")
        ExecuteJobDto executeJobDto = new ExecuteJobDto(applicationJobName: "job")
        executeJobDto.setFeaturePackId(setFpId ? featurePackDto.id : null)
        executeJobDto.setFeaturePackName(setFpName ? featurePackDto.name : null)
        executeJobDto.setApplicationId(setAppId ? featurePackDto.applications[0].id : null)
        executeJobDto.setApplicationName(setAppName ? featurePackDto.applications[0].name : null)
        executeJobDto.setApplicationJobName("job1")
        executeJobDto.setInputs(["input1": "dummy"])

        when: "Create job"
        JobEntity jobEntity = jobTestSteps.createJob(executeJobDto)

        then: "JobEntity object is returned"
        assert jobEntity.getFeaturePackId() == featurePackDto.id as Long
        assert jobEntity.getApplicationId() == featurePackDto.applications[0].id as Long
        assert jobEntity.getFeaturePackName()== featurePackDto.name
        assert jobEntity.getApplicationName()== featurePackDto.applications[0].name

        and: "Job name is auto-generated"
        assert jobEntity.name.contains('job1_20')

        and: "Job is in state DISCOVERED"
        assertJobInState(String.valueOf(jobEntity.getId()), JobSummaryDto.StatusEnum.DISCOVERED)

        where:
        setFpId | setFpName| setAppId | setAppName
        true    | true     | true     | true
        true    | false    | true     | false
        false   | true     | false    | true
        true    | false    | false    | true
        false   | true     | true     | false
    }

    def "Create job should fail if id and name are not provided for feature pack or application"() {
        setup: "Create executeJobDto"
        ExecuteJobDto executeJobDto = new ExecuteJobDto(featurePackName: featurePackName, featurePackId: featurePackId,
                applicationName: applicationName, applicationId: applicationId, applicationJobName: "job1")

        when: "Create job"
        ResultActions result = jobTestSteps.createJobResult(executeJobDto)

        then: "Throw exception"
        result.andExpect(status().is(400))
                .andExpect(jsonPath("\$.errorCode").value(ID_OR_NAME_NOT_PROVIDED.errorCode))

        where:
        featurePackId | featurePackName | applicationId | applicationName
        "1"           | "fp_1"          | null          | null
        null          | null            |"1"            | "app_1"
    }

    def "Create job should fail if feature pack with id or name does not exist"() {
        setup: "Create executeJobDto"
        ExecuteJobDto executeJobDto = new ExecuteJobDto(featurePackName: featurePackName, featurePackId: featurePackId,
                applicationName: "", applicationId: "1", applicationJobName: "job1")

        when: "Create job"
        ResultActions result = jobTestSteps.createJobResult(executeJobDto)

        then: "Throw exception"
        result.andExpect(status().is(404))
                .andExpect(jsonPath("\$.errorCode").value(FP_NOT_FOUND.errorCode))

        where:
        featurePackId | featurePackName
        "1000"        | null
        null          | "unknownName"
    }

    def "Create job should fail fail if application with id or name does not exist"() {
        setup: "Create executeJobDto"
        featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-1", "fp-1")
        ExecuteJobDto executeJobDto = new ExecuteJobDto(featurePackName: "fp-1", applicationName: applicationName,
                applicationId: applicationId, applicationJobName: "job1")

        when: "create job"
        ResultActions result = jobTestSteps.createJobResult(executeJobDto)

        then: "Throw exception"
        result.andExpect(status().is(404))
                .andExpect(jsonPath("\$.errorCode").value(APP_NOT_FOUND.errorCode))

        where:
        applicationId | applicationName
        "1000"        | null
        null          | "unknownName"
    }

    def "Delete job with invalid id returns 404"() {

        when: "Delete job by id which does not exist"
        ResultActions result = jobTestSteps.deleteJobResult("1000")

        then:
        result.andExpect(status().is(404))
                .andExpect(jsonPath("\$.errorCode").value(JOB_NOT_FOUND.errorCode))
    }

    @Unroll
    def "Delete old jobs with status not inprogress"() {

        setup: "Jobs"
        LocalDateTime creationDate = LocalDateTime.now().minus(daysOld, ChronoUnit.DAYS);
        JobEntity jobEntity = new JobEntity(name: "j1", applicationJobName: "j1", applicationName: "a1",
                featurePackName: "fp1", featurePackId: 1, applicationId: 1,  jobStatus: status)
        jobEntity = jobRepository.save(jobEntity)
        jobEntity.setStartDate((Date) Timestamp.valueOf(creationDate))
        jobRepository.save(jobEntity)

        when: "Delete jobs older than one day"
        jobService.deleteOldJobs(ChronoUnit.DAYS, 1)

        then: "Jobs with status not inprogress, and older than one day are deleted"
        assert deleted == (jobRepository.findAll().size() == 0)

        where:
        daysOld | status                 | deleted
        2       | "DISCOVERED"           | true
        0       | "DISCOVERED"           | false
        2       | "DISCOVERY_FAILED"     | true
        0       | "DISCOVERY_FAILED"     | false
        2       | "PARTIALLY_RECONCILED" | true
        0       | "PARTIALLY_RECONCILED" | false
        2       | "COMPLETED"            | true
        0       | "COMPLETED"            | false
        2       | "DISCOVERY_INPROGRESS" | false
        2       | "RECONCILE_INPROGRESS" | false
    }

    def "Get job with invalid id returns 404"() {
        when: "Get job by id which does not exist"
        ResultActions result = jobTestSteps.getJobResult("1000")

        then: "Response is not found"
        result.andExpect(status().is(404))
                .andExpect(jsonPath("\$.errorCode").value(JOB_NOT_FOUND.errorCode))
    }

    def "Get job returns JobDto"() {
        setup: "Create job associated with a job schedule"
        JobScheduleEntity jobScheduleEntity1 = jobScheduleTestSteps.createJobScheduleEntity("schedule1")
        JobScheduleEntity savedJobScheduleEntity = jobScheduleRepository.save(jobScheduleEntity1)
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-1", "fp-1")
        JobEntity jobEntity = jobTestSteps.createSampleJobEntity(featurePackDto, "DISCOVERED")
        jobEntity.setJobScheduleId(savedJobScheduleEntity.id)
        JobEntity saved = jobRepository.save(jobEntity)

        when: "Get job by id"
        JobDto jobDto = jobTestSteps.getJob(saved.id.toString())

        then: "JobDto object is returned"
        assertJobProperties(jobDto, saved)
    }

    def "Get job returns JobDto after associated job schedule has been deleted"() {
        setup: "Create job associated with a job schedule"
        JobScheduleEntity jobScheduleEntity1 = jobScheduleTestSteps.createJobScheduleEntity("schedule1")
        JobScheduleEntity savedJobScheduleEntity = jobScheduleRepository.save(jobScheduleEntity1)
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-1", "fp-1")
        JobEntity jobEntity = jobTestSteps.createSampleJobEntity(featurePackDto, "DISCOVERED")
        jobEntity.setJobScheduleId(savedJobScheduleEntity.id)
        JobEntity saved = jobRepository.save(jobEntity)

        when: "Delete the Job schedule"
        jobScheduleRepository.deleteById(savedJobScheduleEntity.id)

        and: "Get job by id"
        JobDto jobDto = jobTestSteps.getJob(saved.id.toString())

        then: "JobDto object is returned"
        assertJobProperties(jobDto, saved)
    }

    def "Get jobs returns all jobs"() {
        setup: "Create 2 jobs, with 1 job associated with a job schedule"
        JobScheduleEntity jobScheduleEntity1 = jobScheduleTestSteps.createJobScheduleEntity("schedule1")
        JobScheduleEntity savedJobScheduleEntity = jobScheduleRepository.save(jobScheduleEntity1)
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-1", "fp-1")
        JobEntity jobEntity1 = jobTestSteps.createSampleJobEntity(featurePackDto, "DISCOVERED")
        JobEntity saved1 = jobRepository.save(jobEntity1)
        JobEntity jobEntity2 = jobTestSteps.createSampleJobEntity(featurePackDto, "DISCOVERED")
        jobEntity2.setJobScheduleId(savedJobScheduleEntity.id)
        JobEntity saved2 = jobRepository.save(jobEntity2)

        when: "Get all jobs"
        JobListDto jobListDto = jobTestSteps.getJobs()

        then: "All jobs returned successfully"
        jobListDto.totalCount == 2
        assertJobSummaryProperties(jobListDto.items[0], saved1)
        assertJobSummaryProperties(jobListDto.items[1], saved2)
    }

    def "Get jobs does not return jobs in state SCHEDULED"() {
        setup: "Create 3 jobs, 1 job in state SCHEDULED"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-1", "fp-1")
        JobEntity jobEntity1 = jobTestSteps.createSampleJobEntity(featurePackDto, "DISCOVERED")
        jobRepository.save(jobEntity1)
        JobEntity jobEntity2 = jobTestSteps.createSampleJobEntity(featurePackDto, "COMPLETED")
        jobRepository.save(jobEntity2)
        JobEntity jobEntity3 = jobTestSteps.createSampleJobEntity(featurePackDto, "SCHEDULED")
        jobRepository.save(jobEntity3)

        when: "Get all jobs"
        JobListDto jobListDto = jobTestSteps.getJobs()

        then: "2 Jobs returned, SCHEDULED job is not included"
        jobListDto.totalCount == 2
        jobListDto.items.stream().allMatch( {it.status.toString() != "SCHEDULED" })

        when: "Get jobs for page offset 0 and limit 1"
        jobListDto = jobTestSteps.getJobsWithPagination("?offset=0&limit=1")

        then: "First Job is returned, not in state SCHEDULED"
        jobListDto.totalCount == 2
        jobListDto.items.size() == 1

        when: "Get jobs for page offset 1"
        jobListDto = jobTestSteps.getJobsWithPagination("?offset=1&limit=1")

        then: "Second Job is returned, not in state SCHEDULED"
        jobListDto.totalCount == 2
        jobListDto.items.size() == 1

        when: "Get jobs for page offset 2"
        jobListDto = jobTestSteps.getJobsWithPagination("?offset=2&limit=1")

        then: "No jobs returned"
        jobListDto.items.size() == 0
        jobListDto.totalCount == 2
    }

    @Unroll
    def "Get jobs with pagination is successful"() {

        setup: "Create jobs"
        JobEntity jobEntity1 = new JobEntity( name: "job-name", jobStatus: "DISCOVERED", jobScheduleId: 1,
                featurePackName: "fp1", featurePackId: 1l, applicationId:  1l, applicationName: "app1", applicationJobName: "appJob1")
        jobRepository.save(jobEntity1)
        JobEntity jobEntity2 = new JobEntity(name: "job-name2", jobStatus: "DISCOVERY_INPROGRESS",
                featurePackName: "fp2", featurePackId: 2l, applicationId:  2l, applicationName: "app2", applicationJobName: "appJob2")
        jobRepository.save(jobEntity2)

        when: "Get all jobs"
        JobListDto jobListDto = jobTestSteps.getJobsWithPagination(pageRequest)

        and: "Get jobs with pageRequest"
        List<JobSummaryDto> jobs = jobListDto.getItems()
        List<String> jobNames = jobs.stream().map(JobSummaryDto::getName).collect(Collectors.toList())

        then: "Response is as expected"
        jobListDto.totalCount == 2
        jobListDto.items.size() == expectedResults.size()
        jobNames as Set == expectedResults as Set

        where:
        pageRequest                          | expectedResults
        ""                                   | ["job-name", "job-name2"]
        "?sort="                             | ["job-name", "job-name2"]
        "?sort=+name"                        | ["job-name", "job-name2"]
        "?sort=name"                         | ["job-name", "job-name2"]
        "?sort= name"                        | ["job-name", "job-name2"]
        "?sort=-name"                        | ["job-name2", "job-name"]
        "?sort=+id"                          | ["job-name", "job-name2"]
        "?sort=-id"                          | ["job-name2", "job-name"]
        "?sort=-startDate"                   | ["job-name2", "job-name"]
        "?sort=+status"                      | ["job-name", "job-name2"]
        "?sort=-status"                      | ["job-name2", "job-name"]
        "?sort=+featurePackId"               | ["job-name", "job-name2"]
        "?sort=+featurePackName"               | ["job-name", "job-name2"]
        "?sort=-featurePackId"               | ["job-name2", "job-name"]
        "?sort=-featurePackName"               | ["job-name2", "job-name"]
        "?sort=+applicationId"               | ["job-name", "job-name2"]
        "?sort=+applicationName"               | ["job-name", "job-name2"]
        "?sort=-applicationId"               | ["job-name2", "job-name"]
        "?sort=-applicationName"               | ["job-name2", "job-name"]
        "?sort=+applicationJobName"          | ["job-name", "job-name2"]
        "?sort=-applicationJobName"          | ["job-name2", "job-name"]
        "?sort=+jobScheduleId"               | ["job-name", "job-name2"]
        "?sort=-jobScheduleId"          |    ["job-name2", "job-name"]
        "?offset=0&limit=1"                  | ["job-name"]
        "?offset=0&limit=2"                  | ["job-name", "job-name2"]
        "?offset=1&limit=1"                  | ["job-name2"]
        "?offset=0"                          | ["job-name", "job-name2"]
        "?offset=1"                          | ["job-name2"]
        "?offset=2"                          | []
        "?offset=invalidValue&limit=100"     | ["job-name", "job-name2"]
        "?offset=0&limit=invalidValue"       | ["job-name", "job-name2"]
    }

    @Unroll
    def "Get jobs with invalid pagination sorting parameters should return 400"() {

        setup: "Create job"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-1", "fp-1")
        JobEntity jobEntity = jobTestSteps.createSampleJobEntity(featurePackDto, "DISCOVERED")
        jobRepository.save(jobEntity)

        when: "Get jobs"
        ResultActions result = jobTestSteps.getJobsWithPaginationResult(pageRequest)

        then: "Response is as expected"
        result.andExpect(status().is(400))
                .andExpect(jsonPath("\$.errorCode").value(INVALID_SORTING_PARAM.errorCode))

        where:
        pageRequest          | _
        "?sort=+unknownName" | _
        "?sort=%name"        | _
    }

    def "Delete job removes job and associated discovered objects"() {
        setup: "Create job"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-1", "fp-1")
        JobEntity jobEntity = jobTestSteps.createSampleJobEntity(featurePackDto, "DISCOVERED")
        JobEntity saved = jobRepository.save(jobEntity)

        and: "Add discovered objects"
        DiscoveryObjectEntity discoveryObjectEntity = new DiscoveryObjectEntity(
                jobId: saved.id, sourceProperties: [:], status: StatusEnum.DISCOVERED)
        discoveryObjectEntity = discoveryObjectRepository.save(discoveryObjectEntity)

        when: "delete job"
        jobTestSteps.deleteJob(saved.id.toString())

        then: "Job is deleted"
        jobTestSteps.getJobResult(saved.id.toString())
                .andExpect(status().is(404))

        and: "Associated discovered objects are also deleted"
        !discoveryObjectRepository.findById(discoveryObjectEntity.id).isPresent()

        and: "Associated job specification is also deleted"
        jobSpecificationRepository.findById(saved.jobSpecification.id).isPresent() == false
    }

    def "Force delete job removes job and associated discovered objects when job is in-progress"() {
        setup: "Create job"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-1", "fp-1")
        JobEntity jobEntity = jobTestSteps.createSampleJobEntity(featurePackDto, jobStatus)
        JobEntity saved = jobRepository.save(jobEntity)

        and: "Add discovered objects"
        DiscoveryObjectEntity discoveryObjectEntity = new DiscoveryObjectEntity(
                jobId: saved.id, sourceProperties: [:], status: StatusEnum.DISCOVERED)
        discoveryObjectEntity = discoveryObjectRepository.save(discoveryObjectEntity)

        when: "delete job with force header"
        jobTestSteps.forceDeleteJob(saved.id.toString())

        then: "Job is deleted"
        jobTestSteps.getJobResult(saved.id.toString())
                .andExpect(status().is(404))

        and: "Associated discovered objects are also deleted"
        !discoveryObjectRepository.findById(discoveryObjectEntity.id).isPresent()

        where:
        jobStatus | _
        "DISCOVERY_INPROGRESS" | _
        "RECONCILE_INPROGRESS" | _
    }

    def "Check delete rejected when job discovery in progress"() {
        setup: "Create job in state InProgress"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-1", "fp-1")
        JobEntity jobEntity = jobTestSteps.createSampleJobEntity(featurePackDto, "DISCOVERY_INPROGRESS")
        JobEntity saved = jobRepository.save(jobEntity)

        when: "Delete job whose status is discovery in progress"
        ResultActions result = jobTestSteps.deleteJobResult(saved.id.toString())

        then: "Response is conflict"
        result.andExpect(status().is(409))
                .andExpect(jsonPath("\$.errorCode").value(OPERATION_ONGOING.errorCode))
    }

    def "Delete is rejected if job is in state SCHEDULED"() {
        setup: "Create job in state SCHEDULED"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-1", "fp-1")
        JobEntity jobEntity = jobTestSteps.createSampleJobEntity(featurePackDto, "SCHEDULED")
        JobEntity saved = jobRepository.save(jobEntity)

        when: "force delete job"
        ResultActions result = jobTestSteps.forceDeleteJobResult(saved.id.toString())

        then: "Response is conflict"
        result.andExpect(status().is(409))
                .andExpect(jsonPath("\$.errorCode").value(CANNOT_DELETE_SCHEDULED_JOB.errorCode))
    }

    @Unroll
    def "Delete scheduled job with and without force flag"() {

        setup: "Create and save job schedule entity"
        JobScheduleEntity jobScheduleEntity = jobScheduleTestSteps.createJobScheduleEntity("schedule1")
        jobScheduleEntity.setEnabled(scheduleEnabled)
        JobScheduleEntity savedJobScheduleEntity = jobScheduleRepository.save(jobScheduleEntity)

        and: "Create job associated with the schedule"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-1", "fp-1")
        JobEntity jobEntity = jobTestSteps.createSampleJobEntity(featurePackDto, jobStatus)
        jobEntity.setJobScheduleId(savedJobScheduleEntity.id)
        JobEntity saved = jobRepository.save(jobEntity)

        when: "delete job"
        ResultActions result = forceDelete ? jobTestSteps.forceDeleteJobResult(saved.id.toString()) :
                jobTestSteps.deleteJobResult(saved.id.toString())

        then: "Response is as expected"
        result.andExpect(status().is(expectedResponseStatus))

        where:
        forceDelete | jobStatus              | scheduleEnabled | expectedResponseStatus
        false       | "DISCOVERY_INPROGRESS" | true            | 409
        true        | "DISCOVERY_INPROGRESS" | true            | 409
        true        | "DISCOVERY_INPROGRESS" | false           | 204
        true        | "RECONCILE_INPROGRESS" | true            | 409
        true        | "RECONCILE_INPROGRESS" | false           | 204
        false       | "DISCOVERED"           | true            | 204
        false       | "PARTIALLY_RECONCILED" | true            | 204
        false       | "COMPLETED"            | true            | 204
        false       | "DISCOVERY_FAILED"     | true            | 204
        false       | "RECONCILE_FAILED"     | true            | 204
    }

    def "Delete last scheduled job also deletes the job specification when job schedule no longer exists"() {

        setup: "Create job associated with schedule which no longer exists"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-1", "fp-1")
        JobEntity jobEntity = jobTestSteps.createSampleJobEntity(featurePackDto, "COMPLETED")
        jobEntity.setJobScheduleId(10)
        JobEntity saved = jobRepository.save(jobEntity)

        when: "delete job"
        ResultActions result = jobTestSteps.deleteJobResult(saved.id.toString())

        then: "Both the job and job specification are deleted"
        result.andExpect(status().is(204))
        jobSpecificationRepository.findById(saved.jobSpecification.id).isPresent() == false
    }

    @Unroll
    def "Get paged and sorted discovered objects"() {

        setup: "create jobs"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-1", "fp-1")
        JobEntity jobEntity1 = jobTestSteps.createSampleJobEntity(featurePackDto, "DISCOVERED")
        JobEntity jobEntity2 = jobTestSteps.createSampleJobEntity(featurePackDto, "DISCOVERED")
        JobEntity saved1 = jobRepository.save(jobEntity1)
        JobEntity saved2 = jobRepository.save(jobEntity2)

        and: "create discovered objects"
        DiscoveryObjectEntity discoveryObjectEntity1_1 = new DiscoveryObjectEntity(jobId: saved1.id, sourceProperties: [id:1], status: StatusEnum.DISCOVERED)
        DiscoveryObjectEntity discoveryObjectEntity1_2 = new DiscoveryObjectEntity(jobId: saved1.id, sourceProperties: [id:2], status: StatusEnum.RECONCILE_FAILED)
        DiscoveryObjectEntity discoveryObjectEntity1_3 = new DiscoveryObjectEntity(jobId: saved1.id, sourceProperties: [id:3], status: StatusEnum.RECONCILING)
        DiscoveryObjectEntity discoveryObjectEntity2 = new DiscoveryObjectEntity(jobId: saved2.id, sourceProperties: [id:4], status: StatusEnum.RECONCILED)
        discoveryObjectRepository.saveAll([discoveryObjectEntity1_1, discoveryObjectEntity1_2, discoveryObjectEntity1_3, discoveryObjectEntity2])

        when: "get paged discovered objects for job1"
        String discoveredObjectsJson = discoveryTestSteps.getDiscoveredObjects(saved1.id.toString(), query)
        DiscoveredObjectListDto discoveredObjects = JsonUtils.read(discoveredObjectsJson, DiscoveredObjectListDto)
        List ids = discoveredObjects.items.collect { it.properties['id'] }

        then: "expected discovered objects are returned"
        ids == expectedIds

        where:
        query | expectedIds
        "" | [1, 2, 3]
        "sort=+objectId" | [1, 2, 3]
        "sort=-objectId" | [3, 2, 1]
        "sort=+status" | [1, 2, 3]
        "sort=-status" | [3, 2, 1]
        "offset=0&limit=1" | [1]
        "offset=0&limit=2" | [1, 2]
        "offset=0&limit=3" | [1, 2, 3]
        "offset=1&limit=1" | [2]
        "offset=1&limit=2" | [2, 3]
        "offset=0" | [1, 2, 3]
        "offset=1" | [2, 3]
        "offset=2" | [3]
        "offset=0&sort=-objectId" | [3, 2, 1]
    }

    @Unroll
    def "Get discovery objects with invalid sorting parameters should return 400"() {

        when: "Get discovery objects"
        ResultActions result = discoveryTestSteps.getDiscoveredObjectsResult("1", pageRequest)

        then: "Response is as expected"
        result.andExpect(status().is(400))
                .andExpect(jsonPath("\$.errorCode").value(INVALID_SORTING_PARAM.errorCode))

        where:
        pageRequest          | _
        "sort=+unknownName" | _
        "sort=%name"        | _
    }

    @Unroll
    def "Filter jobs is successful"() {

        setup: "Create jobs"
        JobEntity jobEntity1 = new JobEntity(name: "job-1", jobStatus: "DISCOVERED", description: "job-1",
                featurePackName: "fp1", featurePackId: 1l, applicationId:  1l, applicationName: "app1", applicationJobName: "appJob1")
        jobRepository.save(jobEntity1)
        JobEntity jobEntity2 = new JobEntity(name: "job-2", jobStatus: "DISCOVERY_INPROGRESS",  description: "job-2",
                featurePackName: "fp2", featurePackId: 2l, applicationId:  2l, applicationName: "app2", applicationJobName: "appJob2")
        jobRepository.save(jobEntity2)
        JobEntity jobEntity3 = new JobEntity(name: "job-3", jobStatus: "COMPLETED", description: "job-3",
                featurePackName: "fp3", featurePackId: 3l, applicationId:  3l, applicationName: "app3", applicationJobName: "appJob3")
        jobRepository.save(jobEntity3)

        when: "Get all jobs with filter"
        JobListDto jobListDto = jobTestSteps.getJobsWithPagination(filters)
        List jobNames = jobListDto.items.collect({ it.name })

        then: "Response is as expected"
        jobNames == expectedResults

        where:
        filters                                                           | expectedResults
        "?filters=name==job-1"                                            | ["job-1"]
        "?filters=name==job*"                                             | ["job-1", "job-2", "job-3"]
        "?filters=name==*-1"                                              | ["job-1"]
        "?filters=name==*b-*"                                             | ["job-1", "job-2", "job-3"]
        "?filters=name==unknown"                                          | []
        "?filters=description==job-1"                                     | ["job-1"]
        "?filters=description==job*"                                      | ["job-1", "job-2", "job-3"]
        "?filters=status==COMPLETED"                                      | ["job-3"]
        "?filters=featurePackId==3"                                       | ["job-3"]
        "?filters=featurePackName==fp3"                                   | ["job-3"]
        "?filters=applicationId==3"                                       | ["job-3"]
        "?filters=applicationName==app3"                                  | ["job-3"]
        "?filters=applicationJobName==appJob3"                            | ["job-3"]
        "?filters=name==job-1,status==COMPLETED"                          | ["job-1", "job-3"]
        "?filters=name==job-1,((name==job-2;status==DIS*),(name==job-3))" | ["job-1", "job-2", "job-3"]
    }

    def "Filter jobs by jobScheduleId is successful"() {

        setup: "Create job schedules"
        JobScheduleEntity jobScheduleEntity1 = jobScheduleTestSteps.createJobScheduleEntity("schedule1")
        JobScheduleEntity jobScheduleEntity2 = jobScheduleTestSteps.createJobScheduleEntity("schedule2")
        JobScheduleEntity jobScheduleEntity3 = jobScheduleTestSteps.createJobScheduleEntity("schedule3")
        List<JobScheduleEntity> savedJobScheduleEntities = jobScheduleRepository.saveAll([jobScheduleEntity1, jobScheduleEntity2, jobScheduleEntity3])

        and: "Create jobs associated with the job schedules"
        JobEntity jobEntity1 = new JobEntity(name: "job-1", jobStatus: "DISCOVERED", description: "job-1",
                featurePackName: "fp1", featurePackId: 1l, applicationId:  1l, applicationName: "app1", applicationJobName: "appJob1",
                jobScheduleId: savedJobScheduleEntities[0].id)
        jobRepository.save(jobEntity1)
        JobEntity jobEntity2 = new JobEntity(name: "job-2", jobStatus: "DISCOVERY_INPROGRESS",  description: "job-2",
                featurePackName: "fp2", featurePackId: 2l, applicationId:  2l, applicationName: "app2", applicationJobName: "appJob2",
                jobScheduleId: savedJobScheduleEntities[1].id)
        jobRepository.save(jobEntity2)

        when: "Get all jobs for schedule1"
        JobListDto jobListDto = jobTestSteps.getJobsWithPagination("?filters=jobScheduleId==${savedJobScheduleEntities[0].id}")
        List jobNames = jobListDto.items.collect({ it.name })

        then: "Jobs associated with schedule1 are returned"
        jobNames == ["${jobEntity1.name}"]

        when: "Get all jobs for schedule2"
        jobListDto = jobTestSteps.getJobsWithPagination("?filters=jobScheduleId==${savedJobScheduleEntities[1].id}")
        jobNames = jobListDto.items.collect({ it.name })

        then: "Jobs associated with schedule2 are returned"
        jobNames == ["${jobEntity2.name}"]
    }

    @Unroll
    def "Filter jobs with invalid filter should return 400"() {

        when: "Get feature packs"
        ResultActions result = jobTestSteps.getJobsWithPaginationResult(filters)

        then: "Response is as expected"
        result.andExpect(status().is(400))
                .andExpect(jsonPath("\$.errorCode").value(INVALID_FILTER_PARAM.errorCode))

        where:
        filters              | _
        "?filters=invalid==job-1" | _
        "?filters=name!=job-1" | _
        "?filters=name=job-1" | _
    }

    @Unroll
    def "Filter discovered objects is successful"() {

        setup: "create jobs"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-1", "fp-1")
        JobEntity jobEntity1 = jobTestSteps.createSampleJobEntity(featurePackDto, "DISCOVERED")
        JobEntity jobEntity2 = jobTestSteps.createSampleJobEntity(featurePackDto, "DISCOVERED")
        JobEntity saved1 = jobRepository.save(jobEntity1)
        JobEntity saved2 = jobRepository.save(jobEntity2)

        and: "create discovered objects"
        DiscoveryObjectEntity discoveryObjectEntity1_1 = 
                new DiscoveryObjectEntity(jobId: saved1.id,
                        sourceProperties: [id: 1, src_prop_1: "value_1", nested: ["nested_prop_1": "nested_value_1"]],
                        status: StatusEnum.DISCOVERED)
        DiscoveryObjectEntity discoveryObjectEntity1_2 = 
                new DiscoveryObjectEntity(jobId: saved1.id, sourceProperties: [id: 2], status: StatusEnum.RECONCILE_FAILED)
        DiscoveryObjectEntity discoveryObjectEntity1_3 =
                new DiscoveryObjectEntity(jobId: saved1.id,
                        sourceProperties: [id: 3, src_prop_2: 1],
                        targetProperties: [tgt_prop_1: "value_2",tgt_prop_2: false],
                        status: StatusEnum.RECONCILING)
        DiscoveryObjectEntity discoveryObjectEntity2 =
                new DiscoveryObjectEntity(jobId: saved2.id, sourceProperties: [id: 4], status: StatusEnum.RECONCILED)
        List objectIds = discoveryObjectRepository.saveAll([discoveryObjectEntity1_1, discoveryObjectEntity1_2, discoveryObjectEntity1_3, discoveryObjectEntity2])
                .collect { it.id }

        when: "get filtered discovered objects for job1"
        String replacedFilters = filters.replace("{id1}", objectIds[0].toString())
                .replace("{id2}", objectIds[1].toString())
                .replace("{id3}", objectIds[2].toString());
        String discoveredObjectsJson = discoveryTestSteps.getDiscoveredObjects(saved1.id.toString(), replacedFilters)
        DiscoveredObjectListDto discoveredObjects = JsonUtils.read(discoveredObjectsJson, DiscoveredObjectListDto)
        List ids = discoveredObjects.items.collect { it.properties['id'] }

        then: "expected discovered objects are returned"
        ids == expectedIds

        where:
        filters                                                                 | expectedIds
        "filters=status==RECONCILE_FAILED"                                      | [2]
        "filters=status==RECONCILE_FAILED,status==DISCOVERED,status==COMPLETED" | [1, 2]
        "filters=objectId=={id1}"                                               | [1]
        "filters=objectId=={id1},objectId=={id3}"                               | [1, 3]
        "filters=objectId=={id1};status==COMPLETED"                             | []
        "filters=objectId=={id1},status==DISCOVERED"                            | [1]
        "filters=(objectId=={id1}),(status==DISCOVERED)"                        | [1]
        "filters=properties.src_prop_1==value_1"                                | [1]
        "filters=properties.tgt_prop_1==value_2"                                | [3]
        "filters=properties.src_prop_1==value_1,properties.tgt_prop_1==value_2" | [1, 3]
        "filters=properties.src_prop_2==1;properties.tgt_prop_2==false"         | [3]
        "filters=properties.src_prop_2==no_match,properties.tgt_prop_2==false"  | [3]
        "filters=properties.src_prop_1==value_1;status==DISCOVERED"             | [1]
        "filters=properties.src_prop_1==value_1,status==DISCOVERED"             | [1]
        "filters=properties.src_prop_1==value*"                                 | [1]
        "filters=properties.tgt_prop_1==*_2"                                    | [3]
        "filters=properties.nested.nested_prop_1==nested_value_1"               | [1]
        "filters=properties.nested.nested_prop_1==nested*"                      | [1]
        "filters=properties.source.src_prop_1==value_1"                         | [1]
        "filters=properties.target.tgt_prop_1==value_2"                         | [3]
        "filters=properties.target.src_prop_1==value_1"                         | []
        "filters=properties.source.tgt_prop_1==value_2"                         | []
    }

    @Unroll
    def "Get discovered objects with invalid filter should return 400"() {

        when: "Get discovered objects"
        ResultActions result = discoveryTestSteps.getDiscoveredObjectsResult("1", filters)

        then: "Response is as expected"
        result.andExpect(status().is(400))
                .andExpect(jsonPath("\$.errorCode").value(INVALID_FILTER_PARAM.errorCode))

        where:
        filters              | _
        "filters=invalid==abc" | _
        "filters=status!=DISCOVERED" | _
        "filters=status=RECONCILED" | _
    }

    @Unroll
    def "Delete jobs using filter and force flag is successful"() {

        setup: "Create jobs"
        JobEntity jobEntity1_1 = new JobEntity(name: "job-1_1", jobStatus: "DISCOVERY_FAILED", description: "job-1",
                featurePackName: "fp1", featurePackId: 1l, applicationId:  1l, applicationName: "app1", applicationJobName: "appJob1")
        jobRepository.save(jobEntity1_1)
        JobEntity jobEntity1_2 = new JobEntity(name: "job-1_2", jobStatus: "COMPLETED", description: "job-1",
                featurePackName: "fp1", featurePackId: 1l, applicationId:  1l, applicationName: "app1", applicationJobName: "appJob1")
        jobRepository.save(jobEntity1_2)
        JobEntity jobEntity1_3 = new JobEntity(name: "job-1_3", jobStatus: "DISCOVERED", description: "job-1",
                featurePackName: "fp1", featurePackId: 1l, applicationId:  1l, applicationName: "app1", applicationJobName: "appJob1")
        jobRepository.save(jobEntity1_3)
        JobEntity jobEntity1_4 = new JobEntity(name: "job-1_4", jobStatus: "DISCOVERY_INPROGRESS", description: "job-1",
                featurePackName: "fp1", featurePackId: 1l, applicationId:  1l, applicationName: "app1", applicationJobName: "appJob1")
        jobRepository.save(jobEntity1_4)
        JobEntity jobEntity1_5 = new JobEntity(name: "job-1_5", jobStatus: "RECONCILE_INPROGRESS", description: "job-1",
                featurePackName: "fp1", featurePackId: 1l, applicationId:  1l, applicationName: "app1", applicationJobName: "appJob1")
        jobRepository.save(jobEntity1_5)
        JobEntity jobEntity2_1 = new JobEntity(name: "job-2_1", jobStatus: "COMPLETED", description: "job-3",
                featurePackName: "fp2", featurePackId: 2l, applicationId:  2l, applicationName: "app2", applicationJobName: "appJob2")
        jobRepository.save(jobEntity2_1)

        when: "Delete jobs using filter"
        DeleteJobsResponseDto result = jobTestSteps.deleteJobs(filters, force)

        then: "Jobs are deleted"
        result.deleted == deletedCount
        JobListDto jobListDto = jobTestSteps.getJobs()
        jobListDto.items.collect({ it.name }) == remainingJobNames

        where:
        filters                                              | force | deletedCount | remainingJobNames
        "?filters=featurePackId==1"                          | false | 3            | ["job-1_4", "job-1_5", "job-2_1"]
        "?filters=featurePackId==1"                          | true  | 5            | ["job-2_1"]
        "?filters=featurePackId==1,featurePackId==2"         | false | 4            | ["job-1_4", "job-1_5"]
        "?filters=featurePackId==1,featurePackId==2"         | true  | 6            | []
        "?filters=featurePackName==fp1"                      | false | 3            | ["job-1_4", "job-1_5", "job-2_1"]
        "?filters=featurePackName==fp1"                      | true  | 5            | ["job-2_1"]
        "?filters=featurePackName==fp1,featurePackName==fp2" | false | 4            | ["job-1_4", "job-1_5"]
        "?filters=featurePackName==fp1,featurePackName==fp2" | true  | 6            | []
        "?filters=status==COMPLETED"                         | false | 2            | ["job-1_1", "job-1_3", "job-1_4", "job-1_5"]
        "?filters=status==COMPLETED;featurePackId==1"        | false | 1            | ["job-1_1", "job-1_3", "job-1_4", "job-1_5", "job-2_1"]
        "?filters=status==DISCOVERY_INPROGRESS"              | false | 0            | ["job-1_1", "job-1_2", "job-1_3", "job-1_4", "job-1_5", "job-2_1"]
        "?filters=status==DISCOVERY_INPROGRESS"              | true  | 1            | ["job-1_1", "job-1_2", "job-1_3", "job-1_5", "job-2_1"]
        "?filters=status==RECONCILE_INPROGRESS"              | true  | 1            | ["job-1_1", "job-1_2", "job-1_3", "job-1_4", "job-2_1"]
        "?filters=name==job*"                                | false | 4            | ["job-1_4", "job-1_5"]
        "?filters=name==job*"                                | true  | 6            | []
    }


    @Unroll
    def "Delete scheduled jobs using filter is successful"() {

        setup: "Create scheduled jobs"
        JobEntity jobEntity1_1 = new JobEntity(name: "job-1_1", jobStatus: "DISCOVERY_FAILED", description: "job-1", jobScheduleId: 1,
                featurePackName: "fp1", featurePackId: 1l, applicationId:  1l, applicationName: "app1", applicationJobName: "appJob1")
        jobRepository.save(jobEntity1_1)
        JobEntity jobEntity1_2 = new JobEntity(name: "job-1_2", jobStatus: "COMPLETED", description: "job-1", jobScheduleId: 1,
                featurePackName: "fp1", featurePackId: 1l, applicationId:  1l, applicationName: "app1", applicationJobName: "appJob1")
        jobRepository.save(jobEntity1_2)
        JobEntity jobEntity1_3 = new JobEntity(name: "job-1_3", jobStatus: "DISCOVERED", description: "job-1", jobScheduleId: 1,
                featurePackName: "fp1", featurePackId: 1l, applicationId:  1l, applicationName: "app1", applicationJobName: "appJob1")
        jobRepository.save(jobEntity1_3)
        JobEntity jobEntity1_4 = new JobEntity(name: "job-1_4", jobStatus: "DISCOVERY_INPROGRESS", description: "job-1", jobScheduleId: 1,
                featurePackName: "fp1", featurePackId: 1l, applicationId:  1l, applicationName: "app1", applicationJobName: "appJob1")
        jobRepository.save(jobEntity1_4)
        JobEntity jobEntity2_1 = new JobEntity(name: "job-2_1", jobStatus: "RECONCILE_INPROGRESS", description: "job-2", jobScheduleId: 2,
                featurePackName: "fp2", featurePackId: 2l, applicationId:  2l, applicationName: "app2", applicationJobName: "appJob2")
        jobRepository.save(jobEntity2_1)
        JobEntity jobEntity3_1 = new JobEntity(name: "job-3_1", jobStatus: "SCHEDULED", description: "job-3", jobScheduleId: 3,
                featurePackName: "fp3", featurePackId: 3l, applicationId:  3l, applicationName: "app3", applicationJobName: "appJob3")
        jobRepository.save(jobEntity3_1)

        when: "Delete scheduled jobs using filter"
        DeleteJobsResponseDto result = jobTestSteps.deleteJobs(filters, false)

        then: "Jobs are deleted"
        result.deleted == deletedCount
        JobListDto jobListDto = jobTestSteps.getJobs()
        jobListDto.items.collect({ it.name }) == remainingJobNames

        where:
        filters                                                       | deletedCount | remainingJobNames
        "?filters=jobScheduleId==1"                                   | 3            | ["job-1_4", "job-2_1"]
        "?filters=jobScheduleId==2"                                   | 0            | ["job-1_1", "job-1_2", "job-1_3", "job-1_4", "job-2_1"]
        "?filters=jobScheduleId==3"                                   | 0            | ["job-1_1", "job-1_2", "job-1_3", "job-1_4", "job-2_1"]
        "?filters=jobScheduleId==1,jobScheduleId==2,jobScheduleId==3" | 3            | ["job-1_4", "job-2_1"]
        "?filters=jobScheduleId==999"                                 | 0            | ["job-1_1", "job-1_2", "job-1_3", "job-1_4", "job-2_1"]
    }

    def "Delete jobs by ids is successful"() {

        setup: "Create 10 jobs"
        List ids = []
        (1..10).each {
            JobEntity jobEntity1_1 = new JobEntity(name: "job-1_1", jobStatus: "COMPLETED", description: "job-1",
                    featurePackName: "fp1", featurePackId: 1l, applicationId: 1l, applicationName: "app1", applicationJobName: "appJob1")
            ids.add(jobRepository.save(jobEntity1_1).id)
        }

        when: "Delete all jobs using ids in filter"
        String filters = "?filters=${ids.stream().map(id -> "id==${id}").collect(Collectors.joining(","))}"
        DeleteJobsResponseDto result = jobTestSteps.deleteJobs(filters, false)

        then: "10 jobs are deleted"
        result.deleted == 10
    }

    def "Delete jobs with force flag fails when filter matches an inprogress scheduled job"() {

        setup: "Create 3 scheduled jobs, 2 of which are InProgress"
        JobEntity jobEntity1_1 = new JobEntity(name: "job-1_1", jobStatus: "DISCOVERY_FAILED", description: "job-1", jobScheduleId: 1,
                featurePackName: "fp1", featurePackId: 1l, applicationId:  1l, applicationName: "app1", applicationJobName: "appJob1")
        jobRepository.save(jobEntity1_1)
        JobEntity jobEntity1_2 = new JobEntity(name: "job-1_2", jobStatus: "DISCOVERY_INPROGRESS", description: "job-1", jobScheduleId: 1,
                featurePackName: "fp1", featurePackId: 1l, applicationId:  1l, applicationName: "app1", applicationJobName: "appJob1")
        jobRepository.save(jobEntity1_2)
        JobEntity jobEntity2_1 = new JobEntity(name: "job-2_1", jobStatus: "RECONCILE_INPROGRESS", description: "job-2", jobScheduleId: 2,
                featurePackName: "fp2", featurePackId: 2l, applicationId:  2l, applicationName: "app2", applicationJobName: "appJob2")
        jobRepository.save(jobEntity2_1)

        when: "Delete scheduled jobs using filter which includes all jobs"
        ResultActions result = jobTestSteps.deleteJobsResult(filters, true)

        then: "Error"
        result.andExpect(status().is(409))
                .andExpect(jsonPath("\$.errorCode").value(CANNOT_FORCE_DELETE_INPROGRESS_SCHEDULED_JOBS.errorCode))

        where:
        filters | _
        "?filters=name==job*" | _
        "?filters=status==RECONCILE_INPROGRESS" | _
        "?filters=status==DISCOVERY_INPROGRESS" | _
    }

    @Unroll
    def "Delete jobs using invalid filters returns 400"() {

        when: "Delete jobs using invalid filter"
        ResultActions result = jobTestSteps.deleteJobsResult(filters, false)

        then: "Response is 400"
        result.andExpect(status().is(400))
                .andExpect(jsonPath("\$.errorCode").value(INVALID_FILTER_PARAM.errorCode))

        where:
        filters | _
        "" | _
        "?filters=" | _
        "?filters=prop1==1" | _
    }

    void assertJobProperties(JobDto jobDto, JobEntity jobEntity) {
        assert jobDto.getId() == jobEntity.id.toString()
        assert jobDto.getName() == jobEntity.name
        assert jobDto.getDescription() == jobEntity.description
        assert jobDto.getFeaturePackId() == jobEntity.featurePackId.toString()
        assert jobDto.getFeaturePackName() == jobEntity.featurePackName
        assert jobDto.getApplicationId() == jobEntity.applicationId.toString()
        assert jobDto.getApplicationName() == jobEntity.applicationName
        assert jobDto.getStartDate() == jobEntity.startDate.toInstant().toString()
        assert jobDto.getStatus().toString() == jobEntity.jobStatus.toString()
        assert jobDto.getInputs() == jobEntity.inputs
        assert jobDto.getJobScheduleId() == jobEntity.getJobScheduleId().toString()
    }

    void assertJobSummaryProperties(JobSummaryDto jobSummaryDto, JobEntity jobEntity) {
        assert jobSummaryDto.getId() == jobEntity.id.toString()
        assert jobSummaryDto.getName() == jobEntity.name
        assert jobSummaryDto.getDescription() == jobEntity.description
        assert jobSummaryDto.getFeaturePackId() == jobEntity.featurePackId.toString()
        assert jobSummaryDto.getFeaturePackName() == jobEntity.featurePackName
        assert jobSummaryDto.getApplicationId() == jobEntity.applicationId.toString()
        assert jobSummaryDto.getApplicationName() == jobEntity.applicationName
        assert jobSummaryDto.getStartDate() == jobEntity.startDate.toInstant().toString()
        assert jobSummaryDto.getStatus().toString() == jobEntity.jobStatus.toString()
        Optional.ofNullable(jobEntity.getJobScheduleId()).ifPresent(id -> { assert jobSummaryDto.getJobScheduleId() == id.toString() })
    }
}
