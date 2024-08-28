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
import com.ericsson.bos.dr.jpa.model.JobScheduleEntity
import com.ericsson.bos.dr.jpa.model.JobSpecificationEntity
import com.ericsson.bos.dr.web.v1.api.model.CreateJobScheduleDto
import com.ericsson.bos.dr.web.v1.api.model.ExecuteJobDto
import com.ericsson.bos.dr.web.v1.api.model.ExecuteJobDtoExecutionOptions
import com.ericsson.bos.dr.web.v1.api.model.FeaturePackDto
import com.ericsson.bos.dr.web.v1.api.model.JobScheduleDto
import com.ericsson.bos.dr.web.v1.api.model.JobScheduleListDto
import com.ericsson.bos.dr.web.v1.api.model.JobScheduleSummaryDto
import org.hamcrest.Matchers
import org.springframework.test.web.servlet.ResultActions
import org.springframework.transaction.annotation.Transactional
import spock.lang.Unroll

import java.nio.charset.Charset
import java.time.Instant
import java.time.LocalDateTime

import static com.ericsson.bos.dr.service.exceptions.ErrorCode.APP_NOT_FOUND
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.BAD_REQUEST_PARAM
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.CANNOT_ENABLE_JOB_SCHEDULE
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.FP_NOT_FOUND
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.ID_OR_NAME_NOT_PROVIDED
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.INVALID_CRON_EXPRESSION
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.INVALID_FILTER_PARAM
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.INVALID_SORTING_PARAM
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.JOB_NOT_FOUND
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.JOB_SCHEDULE_EXISTS
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.JOB_SCHEDULE_NOT_FOUND
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.MISSING_INPUTS
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class JobScheduleServiceSpec extends BaseSpec {

    JobSpecificationEntity jobSpecificationEntity = new JobSpecificationEntity(name: "job-1", description: "my job", applicationId: 1,
            applicationName: "app_1", featurePackName: "fp-1", featurePackId: 1, executionOptions: new ExecuteJobDtoExecutionOptions().autoReconcile (
            true), inputs: ["input1": 1], applicationJobName: "job-1")

    def "Create Job schedule returns 201 when schedule is successfully created"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-2", "fp-2-${System.currentTimeMillis()}")
        String appId = featurePackDto.applications.first().id

        when: "Create job schedule using feature pack and application id"
        ExecuteJobDto executeJobDto = new ExecuteJobDto().featurePackId(featurePackDto.id).applicationId(appId).applicationJobName("job1")
                .inputs([sourcesUrl:"sourcesUrl",targetsUrl:"targetsUrl",reconcileUrl:"reconcileUrl"])
                .executionOptions(new ExecuteJobDtoExecutionOptions().autoReconcile(true))
                .description("test job")
        CreateJobScheduleDto createJobScheduleDto = new CreateJobScheduleDto().jobSpecification(executeJobDto).name("schedule-1")
                .description("test schedule").expression("* * * * * *")
        JobScheduleDto jobScheduleDto = jobScheduleTestSteps.createJobSchedule(createJobScheduleDto)

        then: "JobScheduleDto properties set as expected"
        assertJobScheduleSummaryProperties(jobScheduleDto, jobScheduleRepository.findById(jobScheduleDto.id).orElse(null))

        and: "JobSpecification properties set as expected"
        JobSpecificationEntity jobSpecificationEntity = jobScheduleRepository.findById(jobScheduleDto.getId()).get().getJobSpecification()
        jobSpecificationEntity.apiPropertyNames.containsAll(["name", "id"])

        when: "Create job schedule using feature pack and application name"
        executeJobDto = new ExecuteJobDto().featurePackName(featurePackDto.name).applicationName("application_2").applicationJobName("job1")
                .inputs([sourcesUrl:"sourcesUrl",targetsUrl:"targetsUrl",reconcileUrl:"reconcileUrl"])
                .executionOptions(new ExecuteJobDtoExecutionOptions().autoReconcile(true))
                .description("test job")
        createJobScheduleDto = new CreateJobScheduleDto().jobSpecification(executeJobDto).name("schedule-2")
                .description("test schedule").expression("* * * * * *")
        jobScheduleDto = jobScheduleTestSteps.createJobSchedule(createJobScheduleDto)

        then: "JobScheduleDto properties set as expected"
        assertJobScheduleSummaryProperties(jobScheduleDto, jobScheduleRepository.findById(jobScheduleDto.id).orElse(null))
    }

    def "Create Job schedule returns 400 when missing or invalid parameters"() {

        when: "Create job schedule"
        ResultActions result = jobScheduleTestSteps.createJobScheduleResult(request)
        result.andDo(handler -> System.out.println(handler.getResponse().getContentAsString(Charset.defaultCharset())))

        then: "Response is Bad Parameter"
        result.andExpect(status().is(400))
                .andExpect(jsonPath("\$.errorCode").value(errorCode))
                .andExpect(jsonPath("\$.errorMessage").value(Matchers.containsString(expectedErrorMsg)))

        where:
        request | errorCode | expectedErrorMsg
        '{ "expression": "15 * * * * *","jobSpecification": {}}' | BAD_REQUEST_PARAM.errorCode | "name must not be null"
        '{ "name":"", "expression": "15 * * * * *","jobSpecification": {}}' | BAD_REQUEST_PARAM.errorCode | "name must not be blank"
        '{"name": "s1","jobSpecification": {}}' | BAD_REQUEST_PARAM.errorCode  | "expression must not be null"
        '{ "name":"s1", "expression": "","jobSpecification": {}}' | BAD_REQUEST_PARAM.errorCode | "expression must not be blank"
        '{"name": "s1", "expression": "15 * * * * *"}' | BAD_REQUEST_PARAM.errorCode  | "jobSpecification must not be null"
        '{"name": "s1", "expression": "15 * * * * *","jobSpecification": {"applicationJobName":"job1"}}'
                | ID_OR_NAME_NOT_PROVIDED.errorCode  | "ID or name must be provided for feature pack and application."
        '{"name": "s1", "expression": "15 * * * * *","jobSpecification": {"applicationJobName":"job1","featurePackId":"","applicationId":""}}'
                | ID_OR_NAME_NOT_PROVIDED.errorCode  | "ID or name must be provided for feature pack and application."
        '{"name": "s1", "expression": "15 * * * * *","jobSpecification": {"applicationJobName":"job1","featurePackId":"1"}}'
                | ID_OR_NAME_NOT_PROVIDED.errorCode  | "ID or name must be provided for feature pack and application."
        '{"name": "s1", "expression": "15 * * * * *","jobSpecification": {"applicationJobName":"job1", "applicationId":"1"}}'
                | ID_OR_NAME_NOT_PROVIDED.errorCode  | "ID or name must be provided for feature pack and application."
        '{"name": "s1", "expression": "15 * * * * *","jobSpecification": {"featurePackId":"1", "applicationId":"1"}}'
                | BAD_REQUEST_PARAM.errorCode  | "jobSpecification.applicationJobName must not be null"
        '{"name": "s1", "expression": "%%%%%","jobSpecification": {"featurePackId":"1", "applicationId":"1", "applicationJobName":"1"}}'
                | INVALID_CRON_EXPRESSION.errorCode  | "Cron expression '%%%%%' is not valid"
    }

    def "Create Job schedule returns 409 when schedule with same name already exists"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-2", "fp-2-${System.currentTimeMillis()}")
        String appId = featurePackDto.applications.first().id

        when: "Create job schedule"
        ExecuteJobDto executeJobDto  = new ExecuteJobDto().featurePackId(featurePackDto.id).applicationId(appId).applicationJobName("job1")
        CreateJobScheduleDto createJobScheduleDto = new CreateJobScheduleDto().jobSpecification(executeJobDto).name("schedule-1")
                .expression("* * * * * *")
        ResultActions result = jobScheduleTestSteps.createJobScheduleResult(createJobScheduleDto)

        then: "Job Schedule is created"
        result.andExpect(status().is(201))

        when: "Attempt to Create job schedule with same name"
        result = jobScheduleTestSteps.createJobScheduleResult(createJobScheduleDto)

        then: "Response is Conflict"
        result.andExpect(status().is(409))
                .andExpect(jsonPath("\$.errorCode").value(JOB_SCHEDULE_EXISTS.errorCode))
    }

    def "Create Job schedule returns 404 when feature pack does not exist"() {

        when: "Create job schedule with invalid feature pack id"
        ExecuteJobDto executeJobDto  = new ExecuteJobDto().featurePackId("999").applicationId("999").applicationJobName("job1")
        CreateJobScheduleDto createJobScheduleDto = new CreateJobScheduleDto().jobSpecification(executeJobDto).name("schedule-1")
                .expression("* * * * * *")
        ResultActions result = jobScheduleTestSteps.createJobScheduleResult(createJobScheduleDto)

        then: "Response is Not Found"
        result.andExpect(status().is(404))
                .andExpect(jsonPath("\$.errorCode").value(FP_NOT_FOUND.errorCode))
    }

    def "Create Job schedule returns 404 when application or job do not exist"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-2", "fp-2-${System.currentTimeMillis()}")
        String appId = featurePackDto.applications.first().id

        when: "Create job schedule with invalid application id"
        ExecuteJobDto executeJobDto  = new ExecuteJobDto().featurePackId(featurePackDto.id).applicationId("999").applicationJobName("job1")
        CreateJobScheduleDto createJobScheduleDto = new CreateJobScheduleDto().jobSpecification(executeJobDto).name("schedule-1")
                .expression("* * * * * *")
        ResultActions result = jobScheduleTestSteps.createJobScheduleResult(createJobScheduleDto)

        then: "Response is Not Found"
        result.andExpect(status().is(404))
                .andExpect(jsonPath("\$.errorCode").value(APP_NOT_FOUND.errorCode))

        when: "Create job schedule with invalid application job name"
        executeJobDto  = new ExecuteJobDto().featurePackId(featurePackDto.id).applicationId(appId).applicationJobName("job999")
        createJobScheduleDto = new CreateJobScheduleDto().jobSpecification(executeJobDto).name("schedule-1")
                .expression("* * * * * *")
        result = jobScheduleTestSteps.createJobScheduleResult(createJobScheduleDto)

        then: "Response is Not Found"
        result.andExpect(status().is(404))
                .andExpect(jsonPath("\$.errorCode").value(JOB_NOT_FOUND.errorCode))

    }

    def "Create Job schedule returns 400 when missing mandatory discovery inputs"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-2", "fp-2-${System.currentTimeMillis()}")
        String appId = featurePackDto.applications.first().id

        when: "Create job schedule with missing discovery inputs"
        ExecuteJobDto executeJobDto = new ExecuteJobDto().featurePackId(featurePackDto.id).applicationId(appId).applicationJobName("job1")
                .inputs([reconcileUrl:"reconcileUrl"])
                .executionOptions(new ExecuteJobDtoExecutionOptions().autoReconcile(true))
                .description("test job")
        CreateJobScheduleDto createJobScheduleDto = new CreateJobScheduleDto().jobSpecification(executeJobDto).name("schedule-1")
                .expression("* * * * * *")
        ResultActions result = jobScheduleTestSteps.createJobScheduleResult(createJobScheduleDto)

        then: "Response is Bad Request"
        result.andExpect(status().is(400))
                .andExpect(jsonPath("\$.errorCode").value(MISSING_INPUTS.errorCode))
    }

    def "Create Job schedule returns 400 when missing mandatory reconcile input and autoReconcile is true"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-2", "fp-2-${System.currentTimeMillis()}")
        String appId = featurePackDto.applications.first().id

        when: "Create job schedule with missing reconcile input"
        ExecuteJobDto executeJobDto = new ExecuteJobDto().featurePackId(featurePackDto.id).applicationId(appId).applicationJobName("job1")
                .inputs([sourcesUrl:"sourcesUrl",targetsUrl:"targetsUrl"])
                .executionOptions(new ExecuteJobDtoExecutionOptions().autoReconcile(true))
                .description("test job")
        CreateJobScheduleDto createJobScheduleDto = new CreateJobScheduleDto().jobSpecification(executeJobDto).name("schedule-1")
                .expression("* * * * * *")
        ResultActions result = jobScheduleTestSteps.createJobScheduleResult(createJobScheduleDto)

        then: "Response is Bad Request"
        result.andExpect(status().is(400))
                .andExpect(jsonPath("\$.errorCode").value(MISSING_INPUTS.errorCode))
    }

    def "Create Job schedule is successful when missing mandatory reconcile input and autoReconcile is false"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-2", "fp-2-${System.currentTimeMillis()}")
        String appId = featurePackDto.applications.first().id

        when: "Create job schedule with missing reconcile input and autoReconcile false"
        ExecuteJobDto executeJobDto = new ExecuteJobDto().featurePackId(featurePackDto.id).applicationId(appId).applicationJobName("job1")
                .inputs([sourcesUrl:"sourcesUrl",targetsUrl:"targetsUrl"])
                .executionOptions(new ExecuteJobDtoExecutionOptions().autoReconcile(false))
                .description("test job")
        CreateJobScheduleDto createJobScheduleDto = new CreateJobScheduleDto().jobSpecification(executeJobDto).name("schedule-1")
                .expression("* * * * * *")
        ResultActions result = jobScheduleTestSteps.createJobScheduleResult(createJobScheduleDto)

        then: "Job schedule is created"
        result.andExpect(status().is(201))
    }

    def "Get job schedules is successful"() {

        setup: "Create and save 3 job schedules entities"
        JobScheduleEntity jobScheduleEntity1 = jobScheduleTestSteps.createJobScheduleEntity("schedule1")
        JobScheduleEntity jobScheduleEntity2 = jobScheduleTestSteps.createJobScheduleEntity("schedule2")
        JobScheduleEntity jobScheduleEntity3 = jobScheduleTestSteps.createJobScheduleEntity("schedule3")
        List jobScheduleEntities = [jobScheduleEntity1, jobScheduleEntity2, jobScheduleEntity3]
        jobScheduleRepository.saveAll(jobScheduleEntities)

        when: "Get job schedules"
        JobScheduleListDto jobScheduleListDto = jobScheduleTestSteps.getJobSchedules()

        then: "Response is as expected"
        jobScheduleListDto.totalCount == 3
        jobScheduleListDto.items.each { it ->
            assertJobScheduleSummaryProperties(it, jobScheduleEntities.find { it2 -> it2.name == it.name }) }
    }

    def "Get job schedules return empty list when none"() {

        when: "Get job schedules when none exist"
        JobScheduleListDto jobScheduleListDto = jobScheduleTestSteps.getJobSchedules()

        then: "Response count is 0"
        jobScheduleListDto.totalCount == 0
        jobScheduleListDto.items == []
    }

    @Unroll
    def "Get job schedules with pagination is successful"() {

        setup: "Create and save 3 job schedules entities"
        JobScheduleEntity jobScheduleEntity1 = jobScheduleTestSteps.createJobScheduleEntity("js-1")
        jobScheduleEntity1.getJobSpecification().setFeaturePackName("fp-1")
        jobScheduleEntity1.getJobSpecification().setFeaturePackId(1)
        JobScheduleEntity jobScheduleEntity2 = jobScheduleTestSteps.createJobScheduleEntity("js-2")
        jobScheduleEntity2.getJobSpecification().setFeaturePackName("fp-2")
        jobScheduleEntity2.getJobSpecification().setFeaturePackId(2)
        JobScheduleEntity jobScheduleEntity3 = jobScheduleTestSteps.createJobScheduleEntity("js-3")
        jobScheduleEntity3.getJobSpecification().setFeaturePackName("fp-3")
        jobScheduleEntity3.getJobSpecification().setFeaturePackId(3)
        List jobScheduleEntities = [jobScheduleEntity1, jobScheduleEntity2, jobScheduleEntity3]
        jobScheduleRepository.saveAll(jobScheduleEntities)

        when: "Get job schedules"
        JobScheduleListDto jobScheduleListDto = jobScheduleTestSteps.getJobSchedulesWithPagination(pageRequest)
        List jobScheduleNames = jobScheduleListDto.items.collect { it.name }

        then: "Response is as expected"
        jobScheduleListDto.getTotalCount() == 3
        jobScheduleNames == expectedResults

        where:
        pageRequest                      | expectedResults
        ""                               | ["js-1", "js-2", "js-3"]
        "?sort="                         | ["js-1", "js-2", "js-3"]
        "?sort=+name"                    | ["js-1", "js-2", "js-3"]
        "?sort=-name"                    | ["js-3", "js-2", "js-1"]
        "?sort=+id"                      | ["js-1", "js-2", "js-3"]
        "?sort=-id"                      | ["js-3", "js-2", "js-1"]
        "?sort=createdAt"                | ["js-1", "js-2", "js-3"]
        "?sort=name"                     | ["js-1", "js-2", "js-3"]
        "?sort= name"                    | ["js-1", "js-2", "js-3"]
        "?sort=-description"             | ["js-3", "js-2", "js-1"]
        "?sort=-featurePackName"         | ["js-3", "js-2", "js-1"]
        "?sort=-featurePackId"           | ["js-3", "js-2", "js-1"]
        "?offset=0"                      | ["js-1", "js-2", "js-3"]
        "?offset=1"                      | ["js-2", "js-3"]
        "?offset=2"                      | ["js-3"]
        "?offset=0&limit=1"              | ["js-1"]
        "?offset=0&limit=2"              | ["js-1", "js-2"]
        "?offset=0&limit=3"              | ["js-1", "js-2", "js-3"]
        "?offset=1&limit=1"              | ["js-2"]
        "?offset=1&limit=2"              | ["js-2", "js-3"]
        "?offset=2&limit=1"              | ["js-3"]
        "?offset=invalidValue&limit=100" | ["js-1", "js-2", "js-3"]
        "?offset=0&limit=invalidValue"   | ["js-1", "js-2", "js-3"]
    }

    @Unroll
    def "Get job schedules with invalid pagination or sorting parameters should return 400"() {

        when: "Get job schedules"
        ResultActions result= jobScheduleTestSteps.getJobSchedulesWithPaginationResult(pageRequest)

        then: "Response is as expected"
        result.andExpect(status().is(400))
                .andExpect(jsonPath("\$.errorCode").value(INVALID_SORTING_PARAM.errorCode))

        where:
        pageRequest          | _
        "?sort=+unknownName" | _
        "?sort=%name"        | _
    }

    @Unroll
    def "Filter job schedules is successful"() {

        setup: "Create and save 3 job schedules entities"
        JobScheduleEntity jobScheduleEntity1 = jobScheduleTestSteps.createJobScheduleEntity("js-1")
        jobScheduleEntity1.getJobSpecification().setFeaturePackName("fp-1")
        jobScheduleEntity1.getJobSpecification().setFeaturePackId(1)
        JobScheduleEntity jobScheduleEntity2 = jobScheduleTestSteps.createJobScheduleEntity("js-2")
        jobScheduleEntity2.getJobSpecification().setFeaturePackName("fp-2")
        jobScheduleEntity2.getJobSpecification().setFeaturePackId(2)
        JobScheduleEntity jobScheduleEntity3 = jobScheduleTestSteps.createJobScheduleEntity("js-3")
        jobScheduleEntity3.getJobSpecification().setFeaturePackName("fp-3")
        jobScheduleEntity3.getJobSpecification().setFeaturePackId(3)
        List jobScheduleEntities = [jobScheduleEntity1, jobScheduleEntity2, jobScheduleEntity3]
        jobScheduleRepository.saveAll(jobScheduleEntities)

        when: "Get job schedules"
        JobScheduleListDto jobScheduleListDto = jobScheduleTestSteps.getJobSchedulesWithPagination(filters)
        List jobScheduleNames = jobScheduleListDto.items.collect { it.name }

        then: "Response is as expected"
        jobScheduleNames == expectedResults

        where:
        filters                                                                    | expectedResults
        "?filters=name==js*"                                                       | ["js-1", "js-2", "js-3"]
        "?filters=name==*-1"                                                       | ["js-1"]
        "?filters=name==*js-*"                                                     | ["js-1", "js-2", "js-3"]
        "?filters=name==unknown"                                                   | []
        "?filters=description==*test*"                                             | ["js-1", "js-2", "js-3"]
        "?filters=description==unknown"                                            | []
        "?filters=featurePackName==fp-3"                                           | ["js-3"]
        "?filters=featurePackId==3"                                                | ["js-3"]
        "?filters=name==js-1,description==test job schedule*"                      | ["js-1", "js-2", "js-3"]
        "?filters=name==js-1;description==test job schedule*"                      | ["js-1"]
        "?filters=name==js-1,description==test*"                                   | ["js-1", "js-2", "js-3"]
        "?filters=(name==js-1;description==test*),(name==js-2;description==test*)" | ["js-1", "js-2"]
        "?filters=name==js-3,((name==js-1;description==test*),(name==js-2))"       | ["js-1", "js-2", "js-3"]
    }

    @Unroll
    def "Filter job schedules with invalid filter should return 400"() {

        when: "Get job schedules"
        ResultActions result = jobScheduleTestSteps.getJobSchedulesWithPaginationResult(filters)

        then: "Response is as expected"
        result.andExpect(status().is(400))
                .andExpect(jsonPath("\$.errorCode").value(INVALID_FILTER_PARAM.errorCode))

        where:
        filters              | _
        "?filters=invalid==js-1" | _
        "?filters=name!=js-1" | _
        "?filters=name=js-1" | _
    }

    def "Get job schedule is successful"() {

        setup: "Create and save job schedules entity"
        JobScheduleEntity jobScheduleEntity = jobScheduleTestSteps.createJobScheduleEntity("schedule1")
        JobScheduleEntity savedJobScheduleEntity = jobScheduleRepository.save(jobScheduleEntity)

        when: "Get job schedule"
        JobScheduleDto jobScheduleDto = jobScheduleTestSteps.getJobSchedule(savedJobScheduleEntity.id.toString())

        then: "Response is as expected"
        assertJobScheduleSummaryProperties(jobScheduleDto, jobScheduleEntity)
    }

    def "Get job schedule returns 404"() {

        when: "Get job schedule which does not exist"
        ResultActions result = jobScheduleTestSteps.getJobScheduleResult("1000")

        then: "Response is as expected"
        result.andExpect(status().is(404))
                .andExpect(jsonPath("\$.errorCode").value(JOB_SCHEDULE_NOT_FOUND.errorCode))
    }

    @Transactional
    def "Delete job schedule is successful"() {

        setup: "Create and save job schedule entity"
        JobScheduleEntity jobScheduleEntity = jobScheduleTestSteps.createJobScheduleEntity("schedule1")
        JobScheduleEntity savedJobScheduleEntity = jobScheduleRepository.save(jobScheduleEntity)

        and: "Create 2 jobs associated with the schedule, 1 job completed and the other scheduled"
        JobEntity jobEntity = new JobEntity(jobSpecification: jobSpecificationEntity, startDate:
                Date.from(Instant.now()), jobStatus: "COMPLETED", applicationJobName: "job-1", jobScheduleId: savedJobScheduleEntity.id)
        JobEntity savedJobEntity = jobRepository.save(jobEntity)
        JobEntity jobEntity2 = new JobEntity(jobSpecification: savedJobEntity.jobSpecification, startDate:
                Date.from(Instant.now()), dueDate: LocalDateTime.now().plusDays(1), jobStatus: "SCHEDULED", applicationJobName: "job-1",
                jobScheduleId: savedJobScheduleEntity.id)
        JobEntity savedJobEntity2 = jobRepository.save(jobEntity2)

        when: "Delete job schedule"
        jobScheduleTestSteps.deleteJobSchedule(savedJobScheduleEntity.id.toString())

        then: "Job schedule is deleted"
        jobScheduleTestSteps.getJobScheduleResult(savedJobScheduleEntity.id.toString())
                .andExpect(status().is(404))
                .andExpect(jsonPath("\$.errorCode").value(JOB_SCHEDULE_NOT_FOUND.errorCode))

        and: "Associated scheduled job is deleted"
        jobTestSteps.getJobResult(savedJobEntity2.id.toString()).andExpect(status().is(404))

        and: "Associated completed job remains"
        jobTestSteps.getJobResult(savedJobEntity.id.toString()).andExpect(status().is(200))
    }

    def "Delete job schedule also deletes the job specification when no scheduled jobs"() {

        setup: "Create and save job schedule entity"
        JobScheduleEntity jobScheduleEntity = jobScheduleTestSteps.createJobScheduleEntity("schedule1")
        JobScheduleEntity savedJobScheduleEntity = jobScheduleRepository.save(jobScheduleEntity)

        when: "Delete job schedule"
        ResultActions result= jobScheduleTestSteps.deleteJobScheduleResult(savedJobScheduleEntity.id.toString())

        then: "Job schedule is deleted"
        result.andExpect(status().is(204))

        and: "Job specification is deleted"
        jobSpecificationRepository.findById(savedJobScheduleEntity.jobSpecification.id).isPresent() == false
    }

    def "Delete job schedule return 404"() {

        when: "Delete job schedule which does not exist"
        ResultActions result = jobScheduleTestSteps.deleteJobScheduleResult("1000")

        then: "Response is as expected"
        result.andExpect(status().is(404))
                .andExpect(jsonPath("\$.errorCode").value(JOB_SCHEDULE_NOT_FOUND.errorCode))
    }

    @Unroll
    def "Enable and disable job schedule is successful"() {

        setup: "Create and save job schedule entity"
        JobScheduleEntity jobScheduleEntity = jobScheduleTestSteps.createJobScheduleEntity("schedule1")
        jobScheduleEntity.enabled = !enabled
        JobScheduleEntity savedJobScheduleEntity = jobScheduleRepository.save(jobScheduleEntity)

        when: "Enable/Disable job schedule"
        jobScheduleTestSteps.enableJobSchedule(savedJobScheduleEntity.id.toString(), enabled)
        JobScheduleDto jobScheduleDto = jobScheduleTestSteps.getJobSchedule(savedJobScheduleEntity.id.toString())

        then: "Job schedule enabled value is set"
        jobScheduleDto.enabled == enabled

        where:
        enabled | _
        false | _
        true | _
    }

    def "Enable/Disable job schedule return 404"() {

        when: "Enable job schedule which does not exist"
        ResultActions result = jobScheduleTestSteps.enableJobScheduleResult("1000", true)

        then: "Response is as expected"
        result.andExpect(status().is(404))
                .andExpect(jsonPath("\$.errorCode").value(JOB_SCHEDULE_NOT_FOUND.errorCode))
    }

    def "Enable/Disable job schedule return 400 if enabled is not set"() {

        when: "Enable job schedule which does not exist"
        ResultActions result = jobScheduleTestSteps.enableJobScheduleResult("1000", "{}")

        then: "Response is as expected"
        result.andExpect(status().is(400))
                .andExpect(jsonPath("\$.errorCode").value(BAD_REQUEST_PARAM.errorCode))
    }


    def "Enable job schedule return 409 if there is a job inprogress"() {

        setup: "Create job schedule which is disabled"
        JobScheduleEntity jobScheduleEntity = jobScheduleTestSteps.createJobScheduleEntity("schedule1")
        jobScheduleEntity.setEnabled(false)
        JobScheduleEntity savedJobScheduleEntity = jobScheduleRepository.save(jobScheduleEntity)

        and: "Create job associated with the job schedule which is InProgress"
        JobEntity jobEntity1 = new JobEntity(name: "job-1", jobStatus: "DISCOVERY_INPROGRESS", description: "job-1",
                featurePackName: "fp1", featurePackId: 1l, applicationId:  1l, applicationName: "app1", applicationJobName: "appJob1",
                jobScheduleId: savedJobScheduleEntity.id)
        jobRepository.save(jobEntity1)

        when: "Enable job schedule"
        ResultActions result = jobScheduleTestSteps.enableJobScheduleResult(savedJobScheduleEntity.id.toString(), true)

        then: "Response is Conflict"
        result.andExpect(status().is(409))
                .andExpect(jsonPath("\$.errorCode").value(CANNOT_ENABLE_JOB_SCHEDULE.errorCode))
    }

    def "Enable job schedule does not create new SCHEDULED job if already existing"() {

        setup: "Create job schedule which is disabled"
        JobScheduleEntity jobScheduleEntity = jobScheduleTestSteps.createJobScheduleEntity("schedule1")
        jobScheduleEntity.setEnabled(false)
        JobScheduleEntity savedJobScheduleEntity = jobScheduleRepository.save(jobScheduleEntity)

        and: "Create job associated with the job schedule which is SCHEDULED"
        JobEntity jobEntity1 = new JobEntity(name: "job-1", jobStatus: "SCHEDULED", description: "job-1",
                featurePackName: "fp1", featurePackId: 1l, applicationId:  1l, applicationName: "app1", applicationJobName: "appJob1",
                jobScheduleId: savedJobScheduleEntity.id)
        jobRepository.save(jobEntity1)

        when: "Enable job schedule"
        jobScheduleTestSteps.enableJobSchedule(savedJobScheduleEntity.id.toString(), true)

        then: "Only 1 SCHEDULED job associated with the schedule"
        jobRepository.findByJobScheduleIdAndJobStatus(savedJobScheduleEntity.id, "SCHEDULED").size() == 1
    }

    def "Enable job schedule creates new SCHEDULED job when none existing"() {

        setup: "Create job schedule which is disabled"
        JobScheduleEntity jobScheduleEntity = jobScheduleTestSteps.createJobScheduleEntity("schedule1")
        jobScheduleEntity.setEnabled(false)
        JobScheduleEntity savedJobScheduleEntity = jobScheduleRepository.save(jobScheduleEntity)

        when: "Enable job schedule"
        jobScheduleTestSteps.enableJobSchedule(savedJobScheduleEntity.id.toString(), true)

        then: "1 SCHEDULED job associated with the schedule"
        jobRepository.findByJobScheduleIdAndJobStatus(savedJobScheduleEntity.id, "SCHEDULED").size() == 1
    }

    def "Disable job schedule deletes SCHEDULED job"() {

        setup: "Create job schedule"
        JobScheduleEntity jobScheduleEntity = jobScheduleTestSteps.createJobScheduleEntity("schedule1")
        JobScheduleEntity savedJobScheduleEntity = jobScheduleRepository.save(jobScheduleEntity)

        and: "Create job associated with the job schedule which is SCHEDULED"
        JobEntity jobEntity1 = new JobEntity(name: "job-1", jobStatus: "SCHEDULED", description: "job-1",
                featurePackName: "fp1", featurePackId: 1l, applicationId:  1l, applicationName: "app1", applicationJobName: "appJob1",
                jobScheduleId: savedJobScheduleEntity.id)
        jobRepository.save(jobEntity1)

        when: "Disable job schedule"
        jobScheduleTestSteps.enableJobSchedule(savedJobScheduleEntity.id.toString(), false)

        then: "No SCHEDULED job associated with the schedule"
        jobRepository.findByJobScheduleIdAndJobStatus(savedJobScheduleEntity.id, "SCHEDULED").size() == 0
    }

    def "Enabled/Disabled Job schedules are deleted when the feature pack is deleted."() {
        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-2", "fp-2-${System.currentTimeMillis()}")
        String appId = featurePackDto.applications.first().id

        and: "Schedule 3 jobs associated with the feature pack"
        CreateJobScheduleDto createJobScheduleDto = new CreateJobScheduleDto(name: "schedule-1", expression: "0 0/15 * * * *", jobSpecification:
                new ExecuteJobDto(name: "job1", featurePackName: "fp-7", applicationName: appId, applicationJobName: "job1", applicationId: appId,
                        featurePackId: featurePackDto.id, inputs: [:], executionOptions: new ExecuteJobDtoExecutionOptions().autoReconcile(true)))
        jobScheduleTestSteps.createJobSchedule(createJobScheduleDto)
        CreateJobScheduleDto createJobScheduleDto2 = new CreateJobScheduleDto(name: "schedule-2", expression: "0 0/15 * * * *", jobSpecification:
                new ExecuteJobDto(name: "job1", featurePackName: "fp-7", applicationName: appId, applicationJobName: "job1", applicationId: appId,
                        featurePackId: featurePackDto.id, inputs: [:], executionOptions: new ExecuteJobDtoExecutionOptions().autoReconcile(true)))
        JobScheduleDto jobScheduleDto2 = jobScheduleTestSteps.createJobSchedule(createJobScheduleDto2)
        CreateJobScheduleDto createJobScheduleDto3 = new CreateJobScheduleDto(name: "schedule-3", expression: "0 0/15 * * * *", jobSpecification:
                new ExecuteJobDto(name: "job1", featurePackName: "fp-7", applicationName: appId, applicationJobName: "job1", applicationId: appId,
                        featurePackId: featurePackDto.id, inputs: [:], executionOptions: new ExecuteJobDtoExecutionOptions().autoReconcile(true)))
        jobScheduleTestSteps.createJobSchedule(createJobScheduleDto3)

        and: "Disable one of the job schedules"
        jobScheduleTestSteps.enableJobSchedule(jobScheduleDto2.id, false)

        when: "Delete the feature pack"
        featurePackTestSteps.deleteFeaturePack(featurePackDto.id)

        then: "Job schedules and specifications are  deleted"
        jobScheduleRepository.count() == 0
        jobSpecificationRepository.count() == 0

        and: "SCHEDULED jobs are deleted"
        jobRepository.count() == 0
    }

    @Transactional
    def "Job schedule with completed job is deleted when the feature pack is deleted."() {
        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-2", "fp-2-${System.currentTimeMillis()}")
        String appId = featurePackDto.applications.first().id

        and: "Schedule job associated with the feature pack"
        CreateJobScheduleDto createJobScheduleDto = new CreateJobScheduleDto(name: "schedule-1", expression: "0 0/15 * * * *", jobSpecification:
                new ExecuteJobDto(name: "job1", featurePackName: "fp-7", applicationName: appId, applicationJobName: "job1", applicationId: appId,
                        featurePackId: featurePackDto.id, inputs: [:], executionOptions: new ExecuteJobDtoExecutionOptions().autoReconcile(true)))
        JobScheduleDto jobScheduleDto = jobScheduleTestSteps.createJobSchedule(createJobScheduleDto)

        and: "Create Completed job associated with the schedule"
        JobSpecificationEntity jobSpecificationEntity1 = jobScheduleRepository.findById(jobScheduleDto.id.toLong()).get().jobSpecification
        JobEntity jobEntity = new JobEntity(jobSpecification: jobSpecificationEntity1, startDate:
                Date.from(Instant.now()), jobStatus: "COMPLETED", applicationJobName: "job-1", jobScheduleId: jobScheduleDto.id.toLong())
        jobRepository.save(jobEntity)

        when: "Delete the feature pack"
        featurePackTestSteps.deleteFeaturePack(featurePackDto.id)

        then: "Job schedule is  deleted"
        jobScheduleRepository.count() == 0

        and: "Job specification remains"
        jobSpecificationRepository.count() == 1

        and: "Completed job remains"
        jobRepository.count() == 1
    }

    def "Feature Pack is not deleted when error deleting job schedule"() {
        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-2", "fp-2-${System.currentTimeMillis()}")
        String appId = featurePackDto.applications.first().id

        and: "Schedule job associated with the feature pack"
        CreateJobScheduleDto createJobScheduleDto = new CreateJobScheduleDto(name: "schedule-1", expression: "0 0/15 * * * *", jobSpecification:
                new ExecuteJobDto(name: "job1", featurePackName: "fp-7", applicationName: appId, applicationJobName: "job1", applicationId: appId,
                        featurePackId: featurePackDto.id, inputs: [:], executionOptions: new ExecuteJobDtoExecutionOptions().autoReconcile(true)))
        jobScheduleTestSteps.createJobSchedule(createJobScheduleDto)

        and: "Mock jobScheduleService#deleteFeaturePackJobSchedules to throw exception"
        jobScheduleService.deleteFeaturePackJobSchedules(_) >> { throw new RuntimeException("Error!") }

        when: "Delete the feature pack"
        ResultActions result = featurePackTestSteps.deleteFeaturePackResult(featurePackDto.id)

        then: "500 Response due to error deleting the Job schedule"
        result.andExpect(status().is(500))

        and: "Feature pack still exists"
        featurePackTestSteps.getFeaturePackResult(featurePackDto.id).andExpect(status().is(200))
    }

    def "Job schedules are deleted for a feature pack when it is updated"() {
        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-2", "fp-2-${System.currentTimeMillis()}")
        String appId = featurePackDto.applications.first().id

        and: "Schedule job associated with the feature pack"
        CreateJobScheduleDto createJobScheduleDto = new CreateJobScheduleDto(name: "schedule-1", expression: "0 0/15 * * * *", jobSpecification:
                new ExecuteJobDto(name: "job1", featurePackName: "fp-7", applicationName: appId, applicationJobName: "job1", applicationId: appId,
                        featurePackId: featurePackDto.id, inputs: [:], executionOptions: new ExecuteJobDtoExecutionOptions().autoReconcile(true)))
        JobScheduleDto jobScheduleDto = jobScheduleTestSteps.createJobSchedule(createJobScheduleDto)
        createJobScheduleDto.setName("schedule-2")
        JobScheduleDto jobScheduleDto2 = jobScheduleTestSteps.createJobSchedule(createJobScheduleDto)

        when: "Replace the feature pack"
        featurePackTestSteps.replaceFeaturePack("/feature-packs/fp-2", featurePackDto.id)

        then: "Job schedules are  deleted"
        jobScheduleTestSteps.getJobScheduleResult(jobScheduleDto.id).andExpect(status().is(404))
        jobScheduleTestSteps.getJobScheduleResult(jobScheduleDto2.id).andExpect(status().is(404))
    }

    def "Job schedules are not deleted when error updating a feature pack"() {
        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-2", "fp-2-${System.currentTimeMillis()}")
        String appId = featurePackDto.applications.first().id

        and: "Schedule job associated with the feature pack"
        CreateJobScheduleDto createJobScheduleDto = new CreateJobScheduleDto(name: "schedule-1", expression: "0 0/15 * * * *", jobSpecification:
                new ExecuteJobDto(name: "job1", featurePackName: "fp-7", applicationName: appId, applicationJobName: "job1", applicationId: appId,
                        featurePackId: featurePackDto.id, inputs: [:], executionOptions: new ExecuteJobDtoExecutionOptions().autoReconcile(true)))
        JobScheduleDto jobScheduleDto = jobScheduleTestSteps.createJobSchedule(createJobScheduleDto)
        createJobScheduleDto.setName("schedule-2")
        JobScheduleDto jobScheduleDto2 = jobScheduleTestSteps.createJobSchedule(createJobScheduleDto)

        when: "Replace using invalid feature pack"
        ResultActions result = featurePackTestSteps.replaceFeaturePackResult("/feature-packs/invalid/fp-invalid-app-schema", featurePackDto.id)

        then: "Job schedules are not deleted"
        result.andExpect(status().is(400))
        jobScheduleTestSteps.getJobScheduleResult(jobScheduleDto.id).andExpect(status().is(200))
        jobScheduleTestSteps.getJobScheduleResult(jobScheduleDto2.id).andExpect(status().is(200))
    }

    void assertJobScheduleSummaryProperties(final JobScheduleSummaryDto jobScheduleSummaryDto, final JobScheduleEntity jobScheduleEntity) {
        assert jobScheduleSummaryDto.name != null
        assert jobScheduleSummaryDto.name == jobScheduleEntity.name
        assert jobScheduleSummaryDto.description == jobScheduleEntity.description
        assert jobScheduleSummaryDto.enabled
        assert jobScheduleSummaryDto.expression == jobScheduleEntity.expression
        assert jobScheduleSummaryDto.createdAt != null
        assert jobScheduleSummaryDto.jobSpecification.name == jobScheduleEntity.jobSpecification.name
        assert jobScheduleSummaryDto.jobSpecification.featurePackId == jobScheduleEntity.jobSpecification.featurePackId.toString()
        assert jobScheduleSummaryDto.jobSpecification.featurePackName == jobScheduleEntity.jobSpecification.featurePackName
        assert jobScheduleSummaryDto.jobSpecification.applicationId == jobScheduleEntity.jobSpecification.applicationId.toString()
        assert jobScheduleSummaryDto.jobSpecification.applicationName == jobScheduleEntity.jobSpecification.applicationName
        assert jobScheduleSummaryDto.jobSpecification.applicationJobName == jobScheduleEntity.jobSpecification.applicationJobName
        assert jobScheduleSummaryDto.jobSpecification.description == jobScheduleEntity.jobSpecification.description
        assert jobScheduleSummaryDto.jobSpecification.executionOptions == jobScheduleEntity.jobSpecification.executionOptions
        assert jobScheduleSummaryDto.jobSpecification.inputs == jobScheduleEntity.jobSpecification.inputs
    }
}