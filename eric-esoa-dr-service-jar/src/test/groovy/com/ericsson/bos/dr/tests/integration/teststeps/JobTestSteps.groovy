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

import com.ericsson.bos.dr.jpa.model.JobEntity
import com.ericsson.bos.dr.service.JobService
import com.ericsson.bos.dr.tests.integration.utils.JsonUtils
import com.ericsson.bos.dr.web.v1.api.model.DeleteJobsResponseDto
import com.ericsson.bos.dr.web.v1.api.model.ExecuteJobDto
import com.ericsson.bos.dr.web.v1.api.model.FeaturePackDto
import com.ericsson.bos.dr.web.v1.api.model.JobDto
import com.ericsson.bos.dr.web.v1.api.model.JobListDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions

import java.util.concurrent.atomic.AtomicReference

import static org.springframework.http.MediaType.APPLICATION_JSON
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@Component
class JobTestSteps {

    private static final String JOBS_URL = "/discovery-and-reconciliation/v1/jobs"

    @Autowired
    private MockMvc mockMvc

    @Autowired
    private JobService jobService;

    void deleteJob(String id) {
        deleteJobResult(id).andExpect(status().isNoContent())
    }

    ResultActions deleteJobResult(String id) {
        return mockMvc.perform(delete("${JOBS_URL}/${id}"))
    }

    // retry to cater for OptimisticLockException which can occur during test when
    // attempting to delete a running job and the state is updated at the same time.
    // for example the job completes at the time of deletion.
    @Retryable(retryFor = Throwable)
    void forceDeleteJob(String id) {
        forceDeleteJobResult(id).andExpect(status().isNoContent())
    }

    ResultActions forceDeleteJobResult(String id) {
        return mockMvc.perform(delete("${JOBS_URL}/${id}").header("force", true))
    }

    ResultActions getJobResult(String id) {
        return mockMvc.perform(get("${JOBS_URL}/${id}"))
    }

    JobDto getJob(String id) {
        final AtomicReference<JobDto> response = new AtomicReference()
        getJobResult(id)
                .andExpect(status().isOk())
                .andDo(result -> response.set(JsonUtils.read(result.getResponse().getContentAsString(), JobDto.class)))
        return response.get()
    }

    ResultActions getJobsResult() {
        return mockMvc.perform(get("${JOBS_URL}"))
    }

    JobListDto getJobs() {
        final AtomicReference<JobListDto> response = new AtomicReference()
        getJobsResult()
                .andExpect(status().isOk())
                .andDo(result -> response.set(JsonUtils.read(result.getResponse().getContentAsString(), JobListDto.class)))
        return response.get()
    }

    JobListDto getScheduledJobs(final String jobScheduleId) {
        final AtomicReference<JobListDto> response = new AtomicReference()
        mockMvc.perform(get("${JOBS_URL}?filters=jobScheduleId==${jobScheduleId}"))
                .andExpect(status().isOk())
                .andDo(result -> response.set(JsonUtils.read(result.getResponse().getContentAsString(), JobListDto.class)))
        return response.get()
    }

    ResultActions getJobsWithPaginationResult(String pageRequest) {
        return mockMvc.perform(get("${JOBS_URL}${pageRequest}"))
    }

    JobListDto getJobsWithPagination(String pageRequest) {
        final AtomicReference<JobListDto> response = new AtomicReference()
        getJobsWithPaginationResult(pageRequest)
                .andExpect(status().isOk())
                .andDo(result -> response.set(JsonUtils.read(result.getResponse().getContentAsString(), JobListDto.class)))
        return response.get()
    }

    DeleteJobsResponseDto deleteJobs(String filter, boolean force) {
        final AtomicReference<JobListDto> response = new AtomicReference()
        mockMvc.perform(delete("${JOBS_URL}${filter}").header("force", force))
                .andExpect(status().isOk())
                .andDo(result -> response.set(JsonUtils.read(result.getResponse().getContentAsString(), DeleteJobsResponseDto.class)))
        return response.get()
    }

    ResultActions deleteJobsResult(String filter, boolean force) {
        return mockMvc.perform(delete("${JOBS_URL}${filter}").header("force", force))
    }


    JobEntity createSampleJobEntity(FeaturePackDto featurePackDto, String state) {
        JobEntity jobEntity = new JobEntity()
        jobEntity.setJobStatus(state)
        jobEntity.setDescription("job description")
        jobEntity.setApplicationId(featurePackDto.applications[0].id.toLong())
        jobEntity.setApplicationName(featurePackDto.applications[0].name)
        jobEntity.setFeaturePackId(featurePackDto.id.toLong())
        jobEntity.setFeaturePackName(featurePackDto.getName())
        jobEntity.setName("job-${UUID.randomUUID().toString()}")
        jobEntity.setErrorMessage("error")
        jobEntity.setStartDate(Date.newInstance())
        jobEntity.setApplicationJobName("job1")
        jobEntity.setInputs(["input1": "value1"])
        return jobEntity
    }

    ResultActions createJobResult(final ExecuteJobDto executeJobDto) {
        return mockMvc.perform(post("${JOBS_URL}")
                .contentType(APPLICATION_JSON)
                .content(JsonUtils.toJsonString(executeJobDto)))
    }

    JobEntity createJob(final ExecuteJobDto executeJobDto) {
        return jobService.createJob(executeJobDto)
    }
}