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

import com.ericsson.bos.dr.jpa.model.JobScheduleEntity
import com.ericsson.bos.dr.jpa.model.JobSpecificationEntity
import com.ericsson.bos.dr.tests.integration.utils.JsonUtils
import com.ericsson.bos.dr.web.v1.api.model.CreateJobScheduleDto
import com.ericsson.bos.dr.web.v1.api.model.ExecuteJobDto
import com.ericsson.bos.dr.web.v1.api.model.ExecuteJobDtoExecutionOptions
import com.ericsson.bos.dr.web.v1.api.model.FeaturePackDto
import com.ericsson.bos.dr.web.v1.api.model.JobScheduleDto
import com.ericsson.bos.dr.web.v1.api.model.JobScheduleListDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions

import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@Component
class JobScheduleTestSteps {

    private static final String JOB_SCHEDULES_URL = "/discovery-and-reconciliation/v1/job-schedules"

    @Autowired
    private MockMvc mockMvc

    JobScheduleDto createJobSchedule(CreateJobScheduleDto createJobScheduleDto) {
        final AtomicReference<JobScheduleDto> response = new AtomicReference()
       createJobScheduleResult(createJobScheduleDto)
               .andExpect(status().isCreated())
               .andDo(result -> response.set(JsonUtils.read(result.getResponse().getContentAsString(), JobScheduleDto.class)))
        return response.get()
    }

    ResultActions createJobScheduleResult(CreateJobScheduleDto createJobScheduleDto) {
        return mockMvc.perform(post("${JOB_SCHEDULES_URL}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJsonString(createJobScheduleDto)))
    }

    ResultActions createJobScheduleResult(String json) {
        return mockMvc.perform(post("${JOB_SCHEDULES_URL}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
    }

    JobScheduleListDto getJobSchedules() {
        final AtomicReference<FeaturePackDto> response = new AtomicReference()
        mockMvc.perform(get(JOB_SCHEDULES_URL))
                .andExpect(status().isOk())
                .andDo(result -> response.set(JsonUtils.read(result.getResponse().getContentAsString(), JobScheduleListDto.class)))
        return response.get()
    }

    JobScheduleListDto getJobSchedulesWithPagination(String pageRequest) {
        final AtomicReference<FeaturePackDto> response = new AtomicReference()
        mockMvc.perform(get(JOB_SCHEDULES_URL + pageRequest))
                .andExpect(status().isOk())
                .andDo(result -> response.set(JsonUtils.read(result.getResponse().getContentAsString(), JobScheduleListDto.class)))
        return response.get()
    }

    ResultActions getJobSchedulesWithPaginationResult(String pageRequest) {
        return mockMvc.perform(get("${JOB_SCHEDULES_URL}${pageRequest}"))
    }

    JobScheduleDto getJobSchedule(String id) {
        final AtomicReference<FeaturePackDto> response = new AtomicReference()
        mockMvc.perform(get(JOB_SCHEDULES_URL + "/${id}"))
                .andExpect(status().isOk())
                .andDo(result -> response.set(JsonUtils.read(result.getResponse().getContentAsString(), JobScheduleDto.class)))
        return response.get()
    }

    void deleteJobSchedule(String id) {
        mockMvc.perform(delete(JOB_SCHEDULES_URL + "/${id}"))
                .andExpect(status().isNoContent())
    }

    ResultActions getJobScheduleResult(String id) {
        return mockMvc.perform(get("${JOB_SCHEDULES_URL}/${id}"))
    }

    ResultActions deleteJobScheduleResult(String id) {
        return mockMvc.perform(delete("${JOB_SCHEDULES_URL}/${id}"))
    }

    void enableJobSchedule(String id, boolean enabled) {
        mockMvc.perform(patch(JOB_SCHEDULES_URL + "/${id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\": \"${enabled}\"}"))
                .andExpect(status().isNoContent())
    }

    ResultActions enableJobScheduleResult(String id, boolean enabled) {
       return  mockMvc.perform(patch(JOB_SCHEDULES_URL + "/${id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\": \"${enabled}\"}"))
    }

    ResultActions enableJobScheduleResult(String id, String content) {
        return  mockMvc.perform(patch(JOB_SCHEDULES_URL + "/${id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(content))
    }

    JobScheduleEntity createJobScheduleEntity(String name) {
        JobSpecificationEntity jobSpecificationEntity = new JobSpecificationEntity(name: "job-1", description: "my job", applicationId: 1,
                applicationName: "app_1", featurePackName: "fp-1", featurePackId: 1, applicationJobName: "job-1", inputs: [input1: 1])
        return new JobScheduleEntity(name: name, description: "test job schedule ${name}",
                expression: "0 0/15 * * * *", "enabled": true, version: 1,
                jobSpecification: jobSpecificationEntity, creationDate: Date.from(Instant.now()))
    }
}