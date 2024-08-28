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

import com.ericsson.bos.dr.tests.integration.utils.WiremockUtil
import com.ericsson.bos.dr.service.alarms.FaultName
import com.ericsson.bos.dr.web.v1.api.model.CreateJobScheduleDto
import com.ericsson.bos.dr.web.v1.api.model.ExecuteJobDto
import com.ericsson.bos.dr.web.v1.api.model.ExecuteJobDtoExecutionOptions
import com.ericsson.bos.dr.web.v1.api.model.FeaturePackDto
import com.ericsson.bos.dr.web.v1.api.model.JobListDto
import com.ericsson.bos.dr.web.v1.api.model.JobScheduleDto
import com.ericsson.bos.dr.web.v1.api.model.JobSummaryDto
import com.ericsson.bos.dr.web.v1.api.model.JobSummaryDto.StatusEnum
import com.ericsson.bos.so.alarm.model.Alarm
import org.mockito.ArgumentCaptor
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.any
import org.mockito.Mockito
import spock.lang.Unroll

import static com.ericsson.bos.dr.web.v1.api.model.JobSummaryDto.StatusEnum.COMPLETED
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class JobScheduleExecutionSpec extends BaseSpec {

    def "Multiple scheduled jobs for same feature pack execute successfully"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-7", "fp-7-${System.currentTimeMillis()}")
        String appId = featurePackDto.applications.first().id

        when: "Schedule 2 jobs to run every 1 second"
        CreateJobScheduleDto createJobScheduleDto = new CreateJobScheduleDto(name: "schedule-1", expression: "*/1 * * * * *", jobSpecification:
                new ExecuteJobDto(name: "job1", featurePackName: "fp-7", applicationName: appId, applicationJobName: "job1", applicationId: appId,
                        featurePackId: featurePackDto.id, inputs: [:], executionOptions: new ExecuteJobDtoExecutionOptions().autoReconcile(true)))
        JobScheduleDto jobScheduleDto = jobScheduleTestSteps.createJobSchedule(createJobScheduleDto)
        CreateJobScheduleDto createJobScheduleDto2 = new CreateJobScheduleDto(name: "schedule-2", expression: "*/1 * * * * *", jobSpecification:
                new ExecuteJobDto(name: "job1", featurePackName: "fp-7", applicationName: appId, applicationJobName: "job1", applicationId: appId,
                        featurePackId: featurePackDto.id, inputs: [:], executionOptions: new ExecuteJobDtoExecutionOptions().autoReconcile(true)))
        JobScheduleDto jobScheduleDto2 = jobScheduleTestSteps.createJobSchedule(createJobScheduleDto2)

        then: "Schedule jobs executes at least 2 times for the 2 schedules, with jobs in state COMPLETED"
        assertScheduledJobExecutedAtLeastTwice(jobScheduleDto.id)
        assertScheduledJobExecutedAtLeastTwice(jobScheduleDto2.id)

        when: "Delete feature pack"
        featurePackTestSteps.deleteFeaturePack(featurePackDto.id)

        then: "Job schedules are deleted"
        jobScheduleTestSteps.getJobScheduleResult(jobScheduleDto.id).andExpect(status().is(404))
        jobScheduleTestSteps.getJobScheduleResult(jobScheduleDto2.id).andExpect(status().is(404))

        and: "Scheduled jobs are deleted"
        jobRepository.findByJobScheduleIdAndJobStatus(jobScheduleDto.id.toLong(), "SCHEDULED").size() == 0
        jobRepository.findByJobScheduleIdAndJobStatus(jobScheduleDto2.id.toLong(), "SCHEDULED").size() == 0

    }

    @Unroll
    def "Next scheduled job is not executed after schedule is deleted or disabled while job is running"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-7", "fp-7-${System.currentTimeMillis()}")
        String appId = featurePackDto.applications.first().id

        when: "Schedule job to run every 5 second"
        CreateJobScheduleDto createJobScheduleDto = new CreateJobScheduleDto(name: "schedule-1", expression: "*/6 * * * * *", jobSpecification:
                new ExecuteJobDto(name: "job1", featurePackName: "fp-7", applicationName: appId, applicationJobName: "job1", applicationId: appId,
                        featurePackId: featurePackDto.id, inputs: [:], executionOptions: new ExecuteJobDtoExecutionOptions().autoReconcile(true)))
        JobScheduleDto jobScheduleDto = jobScheduleTestSteps.createJobSchedule(createJobScheduleDto)

        and: "Delete or disable the job Schedule when first job is started"
        pollingConditions.within(10, {
            JobListDto executedJobs = jobTestSteps.getScheduledJobs(jobScheduleDto.id)
            executedJobs.totalCount == 1
            executedJobs.items[0].status != JobSummaryDto.StatusEnum.COMPLETED
            if ("delete".equals(operation)) {
                jobScheduleTestSteps.deleteJobSchedule(jobScheduleDto.id)
            } else {
                jobScheduleTestSteps.enableJobSchedule(jobScheduleDto.id, false)
            }
        })

        then: "First job completes"
        assertFirstScheduledJobInState(jobScheduleDto.id, StatusEnum.COMPLETED)

        and: "No further jobs scheduled or executed"
        jobRepository.countByJobScheduleId(jobScheduleDto.id.toLong()) == 1
        jobTestSteps.getScheduledJobs(jobScheduleDto.id).totalCount == 1

        where:
        operation | _
        "delete" | _
        "disable" | _
    }

    def "Next scheduled job executes after error executing previous job"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-2", "fp-2-${System.currentTimeMillis()}")
        String appId = featurePackDto.applications.first().id

        and: "Stub http executor wiremock requests to return error during discovery"
        WiremockUtil.stubForPost("/rest-service/v1/run/dummyEnm/dummyResourceConfig/dummyResource", "/feature-packs/fp-2/responses/sources.json")
        WiremockUtil.stubForGet("/fp-2/enrich/[0-9]+", "/feature-packs/fp-2/responses/enrich.json")
        WiremockUtil.stubForGet("/fp-2/targets", 500, "Error!")

        when: "Schedule job to run every 1 second"
        Map inputs = [sourcesUrl: "${wireMock.baseUrl()}/fp-2/sources".toString(),
                      targetsUrl: "${wireMock.baseUrl()}/fp-2/targets".toString(),
                      enrichUrl : "${wireMock.baseUrl()}/fp-2/enrich".toString()]
        CreateJobScheduleDto createJobScheduleDto = new CreateJobScheduleDto(name: "schedule-1", expression: "*/1 * * * * *", jobSpecification:
                new ExecuteJobDto(name: "job1", featurePackName: "fp-2", applicationName: appId, applicationJobName: "job1", applicationId: appId,
                        featurePackId: featurePackDto.id, inputs: inputs))
        JobScheduleDto jobScheduleDto = jobScheduleTestSteps.createJobSchedule(createJobScheduleDto)

        and: "Scheduled job executes at least once, with jobs in state DISCOVERY_FAILED"
        assertFirstScheduledJobInState(jobScheduleDto.id, StatusEnum.DISCOVERY_FAILED)

        and: "Stub http executor wiremock requests for successful discovery"
        WiremockUtil.stubForGet("/fp-2/targets", "/feature-packs/fp-2/responses/targets.json")

        then: "Scheduled job executes at least once, with job in state DISCOVERED"
        pollingConditions.within(10, {
            JobListDto executedJobs = jobTestSteps.getScheduledJobs(jobScheduleDto.id)
            executedJobs.totalCount >= 2
            executedJobs.items.status.any { it == JobSummaryDto.StatusEnum.DISCOVERED }
        })
    }

    def "Job specification is deleted after schedule and all associated jobs are deleted"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-7", "fp-7-${System.currentTimeMillis()}")
        String appId = featurePackDto.applications.first().id

        when: "Schedule job to run every 1 second"
        CreateJobScheduleDto createJobScheduleDto = new CreateJobScheduleDto(name: "schedule-1", expression: "*/1 * * * * *", jobSpecification:
                new ExecuteJobDto(name: "job1", featurePackName: "fp-7", applicationName: appId, applicationJobName: "job1", applicationId: appId,
                        featurePackId: featurePackDto.id, inputs: [:], executionOptions: new ExecuteJobDtoExecutionOptions().autoReconcile(false)))
        JobScheduleDto jobScheduleDto = jobScheduleTestSteps.createJobSchedule(createJobScheduleDto)

        and: "Wait until at least 1 job is completed"
        assertFirstScheduledJobInState(jobScheduleDto.id, StatusEnum.DISCOVERED)

        and: "Delete the schedule and force delete the jobs"
        jobScheduleTestSteps.deleteJobSchedule(jobScheduleDto.id)
        jobTestSteps.getScheduledJobs(jobScheduleDto.id).items.each { jobTestSteps.forceDeleteJob(it.id) }

        then: "JobSpecification is also deleted"
        jobSpecificationRepository.count() == 0
    }

    def "Next Scheduled job is executed when schedule is enabled after being disabled"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-7", "fp-7-${System.currentTimeMillis()}")
        String appId = featurePackDto.applications.first().id

        when: "Schedule job to run every 1 second"
        CreateJobScheduleDto createJobScheduleDto = new CreateJobScheduleDto(name: "schedule-1", expression: "*/1 * * * * *", jobSpecification:
                new ExecuteJobDto(name: "job1", featurePackName: "fp-7", applicationName: appId, applicationJobName: "job1", applicationId: appId,
                        featurePackId: featurePackDto.id, inputs: [:], executionOptions: new ExecuteJobDtoExecutionOptions().autoReconcile(true)))
        JobScheduleDto jobScheduleDto = jobScheduleTestSteps.createJobSchedule(createJobScheduleDto)

        and: "Disable the scheduled job"
        jobScheduleTestSteps.enableJobSchedule(jobScheduleDto.id, false)

        then: "No scheduled ,completed or running jobs after being disabled"
        jobRepository.findAll().count { it.jobScheduleId == jobScheduleDto.id} == 0

        when: "Re-enable the schedule"
        jobScheduleTestSteps.enableJobSchedule(jobScheduleDto.id, true)

        then: "Scheduled job executes at least 1 time, with job in state COMPLETED"
        assertFirstScheduledJobInState(jobScheduleDto.id, StatusEnum.COMPLETED)
    }

    def "Disable schedule and force delete running job before re-enabling a schedule"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-7", "fp-7-${System.currentTimeMillis()}")
        String appId = featurePackDto.applications.first().id

        when: "Schedule job to run every 1 second"
        CreateJobScheduleDto createJobScheduleDto = new CreateJobScheduleDto(name: "schedule-1", expression: "*/1 * * * * *", jobSpecification:
                new ExecuteJobDto(name: "job1", featurePackName: "fp-7", applicationName: appId, applicationJobName: "job1", applicationId: appId,
                        featurePackId: featurePackDto.id, inputs: [:],
                        executionOptions: new ExecuteJobDtoExecutionOptions().autoReconcile(true)))
        JobScheduleDto jobScheduleDto = jobScheduleTestSteps.createJobSchedule(createJobScheduleDto)

        and: "Disable schedule and force delete running job"
        pollingConditions.within(10, {
            JobListDto scheduledJobs = jobTestSteps.getScheduledJobs(jobScheduleDto.id)
            JobSummaryDto runningJob = scheduledJobs.items.find { it.status != COMPLETED }
            scheduledJobs.totalCount > 0 && runningJob != null
            jobScheduleTestSteps.enableJobSchedule(jobScheduleDto.id, false)
            jobTestSteps.forceDeleteJob(runningJob.id)
        })

        then: "All scheduled jobs are completed and there is no job in state SCHEDULED"
        JobListDto scheduledJobs = jobTestSteps.getScheduledJobs(jobScheduleDto.id)
        scheduledJobs.items.stream().allMatch( {it.status == JobSummaryDto.StatusEnum.COMPLETED })
        jobRepository.findAll().findAll { it.jobScheduleId == jobScheduleDto.id.toLong() && it.jobStatus == "SCHEDULED"}.isEmpty()

        when: "Re-enable the schedule"
        jobScheduleTestSteps.enableJobSchedule(jobScheduleDto.id, true)

        then: "Scheduled job exists"
        jobRepository.findAll().count( { it.jobScheduleId == jobScheduleDto.id.toLong() && it.jobStatus == "SCHEDULED"}) == 1
    }

    @Unroll
    def "Alarm raised when scheduled discovery or reconcile fails"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-24", "fp-24-${System.currentTimeMillis()}")
        String appId = featurePackDto.applications.first().id

        and: "Stub http executor wiremock requests. Will return error during either the discovery or reconcile"
        WiremockUtil.stubForGet("/fp-24/sources", get_sources_status, get_sources_body)
        WiremockUtil.stubForPost("/fp-24/reconcile/[0-9]+", reconcile_status, reconcile_body)

        when: "Schedule job to run every 1 second"
        Map inputs = [sourcesUrl: "${wireMock.baseUrl()}/fp-24/sources".toString(),
                      reconcileUrl: "${wireMock.baseUrl()}/fp-24/reconcile".toString()]
        CreateJobScheduleDto createJobScheduleDto = new CreateJobScheduleDto(name: "schedule-1", expression: "*/1 * * * * *", jobSpecification:
                new ExecuteJobDto(name: "job1", featurePackName: "fp-24", applicationName: appId, applicationJobName: "job1", applicationId: appId,
                        featurePackId: featurePackDto.id, inputs: inputs, executionOptions:  new ExecuteJobDtoExecutionOptions(autoReconcile: true)))
        JobScheduleDto jobScheduleDto = jobScheduleTestSteps.createJobSchedule(createJobScheduleDto)

        and: "Scheduled job executes at least once, with jobs in state DISCOVERY_FAILED | RECONCILE_FAILED"
        assertFirstScheduledJobInState(jobScheduleDto.id, expected_job_state)

        then: "Alarm raised with expected values"
        ArgumentCaptor<Alarm> alarmCaptor = ArgumentCaptor.forClass(Alarm.class)
        verify(alarmSenderMock, Mockito.timeout(10000).atLeastOnce()).postAlarm(alarmCaptor.capture(), any())
        with(alarmCaptor.getValue()) {
            assert serviceName == "eric-esoa-dr-service"
            assert faultName == FaultName.JOB_FAILED.getName()
            assert faultyUrlResource.contains("/discovery-and-reconciliation/v1/jobs/")
            assert description == "A scheduled D&R job is in a failed state. Please refer to the job resource in the D&R UI or NBI for details on " +
                    "the specific reason for the failure."
            assert expiration == 600
            assert eventTime != null
        }

        where: 'Set up discovery and reconcile success or failure'
        get_sources_status | get_sources_body                             | reconcile_status | reconcile_body     | expected_job_state
        500                | "Discovery Error!"                           | 0                | "N/A"              | StatusEnum.DISCOVERY_FAILED
        200                | "/feature-packs/fp-24/responses/source.json" | 500              | "Reconcile Error!" | StatusEnum.RECONCILE_FAILED
    }

    void assertScheduledJobExecutedAtLeastTwice(String jobScheduleId) {
        pollingConditions.within(10, {
            JobListDto executedJobs = jobTestSteps.getScheduledJobs(jobScheduleId)
            executedJobs.totalCount >= 2
            executedJobs.items[0].status == JobSummaryDto.StatusEnum.COMPLETED
            executedJobs.items[1].status == JobSummaryDto.StatusEnum.COMPLETED
        })
    }

    void assertFirstScheduledJobInState(String jobScheduleId, JobSummaryDto.StatusEnum status) {
        pollingConditions.within(5, {
            JobListDto executedJobs = jobTestSteps.getScheduledJobs(jobScheduleId)
            executedJobs.totalCount >= 1
            executedJobs.items[0].status == status
        })
    }
}