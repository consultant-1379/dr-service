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

import com.ericsson.bos.dr.tests.integration.utils.JsonUtils
import com.ericsson.bos.dr.web.v1.api.model.DiscoveredObjectDto
import com.ericsson.bos.dr.web.v1.api.model.DiscoveredObjectListDto
import com.ericsson.bos.dr.web.v1.api.model.ExecuteJobDto
import com.ericsson.bos.dr.web.v1.api.model.ExecuteJobDtoExecutionOptions
import com.ericsson.bos.dr.web.v1.api.model.ExecuteJobResponseDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions

import java.util.concurrent.atomic.AtomicReference

import static org.springframework.http.MediaType.APPLICATION_JSON
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@Component
class DiscoveryTestSteps {

    private static final String DISCOVERY_JOB_URL = "/discovery-and-reconciliation/v1/jobs"
    private static final String DUPLICATE_DISCOVERY_JOB_URL = "/discovery-and-reconciliation/v1/jobs/%s/duplicate"
    private static final String DISCOVERY_OBJECTS_URL = "/discovery-and-reconciliation/v1/jobs/%s/discovered-objects"

    @Autowired
    private MockMvc mockMvc

    String startDiscovery(String featurePackId, String appId, String jobName, Map inputs) {
        final AtomicReference<ExecuteJobResponseDto> response = new AtomicReference()
        startDiscoveryResult(featurePackId, appId, jobName, inputs)
                .andExpect(status().isAccepted())
                .andDo(result -> response.set(JsonUtils.read(result.getResponse().getContentAsString(), ExecuteJobResponseDto.class)))
        return response.get().id
    }

    String startDuplicateDiscovery(String originalJobId) {
        final AtomicReference<ExecuteJobResponseDto> response = new AtomicReference()
        startDuplicateDiscoveryResult(originalJobId)
                .andExpect(status().isAccepted())
                .andDo(result -> response.set(JsonUtils.read(result.getResponse().getContentAsString(), ExecuteJobResponseDto.class)))
        return response.get().id
    }

    String startDiscoveryAndReconcile(String featurePackId, String appId, String jobName, Map inputs) {
        final AtomicReference<ExecuteJobResponseDto> response = new AtomicReference()
        startDiscoveryAndReconcileResult(featurePackId, appId, jobName, inputs)
                .andExpect(status().isAccepted())
                .andDo(result -> response.set(JsonUtils.read(result.getResponse().getContentAsString(), ExecuteJobResponseDto.class)))
        return response.get().id
    }

    ResultActions startDiscoveryResult(String featurePackId, String appId, String jobName, Map inputs) {
        ExecuteJobDto discoveryJob = new ExecuteJobDto().name("test")
                .featurePackId(featurePackId)
                .applicationId(appId)
                .applicationJobName(jobName)
                .inputs(inputs)
        return mockMvc.perform(post(DISCOVERY_JOB_URL)
                .contentType(APPLICATION_JSON)
                .content(JsonUtils.toJsonString(discoveryJob)))
    }

    ResultActions startDuplicateDiscoveryResult(String originalJobId) {
        return mockMvc.perform(post(String.format(DUPLICATE_DISCOVERY_JOB_URL, originalJobId)))
    }

    ResultActions startDiscoveryAndReconcileResult(String featurePackId, String appId, String jobName, Map inputs) {
        ExecuteJobDto discoveryJob = new ExecuteJobDto().name("test")
                .featurePackId(featurePackId)
                .applicationId(appId)
                .applicationJobName(jobName)
                .inputs(inputs)
                .executionOptions(new ExecuteJobDtoExecutionOptions(autoReconcile: true))
        return mockMvc.perform(post(DISCOVERY_JOB_URL)
                .contentType(APPLICATION_JSON)
                .content(JsonUtils.toJsonString(discoveryJob)))
    }

    String getDiscoveredObjects(String jobId) {
        final AtomicReference<String> response = new AtomicReference()
        mockMvc.perform(get(String.format(DISCOVERY_OBJECTS_URL, jobId)))
                .andExpect(status().isOk())
                .andDo(result -> response.set(result.getResponse().getContentAsString()))
        return response.get()
    }

    String getDiscoveredObjects(String jobId, String queryParams) {
        final AtomicReference<String> response = new AtomicReference()
        getDiscoveredObjectsResult(jobId, queryParams)
                .andExpect(status().isOk())
                .andDo(result -> response.set(result.getResponse().getContentAsString()))
        return response.get()
    }

    ResultActions getDiscoveredObjectsResult(String jobId, String queryParams) {
        return mockMvc.perform(get(String.format(DISCOVERY_OBJECTS_URL, jobId) + "?${queryParams}"))
    }

    List<DiscoveredObjectDto> getDiscoveredObjectItems(String jobId) {
        return JsonUtils.read(getDiscoveredObjects(jobId), DiscoveredObjectListDto).items
    }

    DiscoveredObjectDto getDiscoveredObject(String jobId, String objectId) {
        DiscoveredObjectListDto discoveredObjects = JsonUtils.read(getDiscoveredObjects(jobId), DiscoveredObjectListDto)
        return discoveredObjects.items.find { it.objectId == objectId }
    }
}