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
import com.ericsson.bos.dr.web.v1.api.model.ExecuteJobResponseDto
import com.ericsson.bos.dr.web.v1.api.model.ExecuteReconcileDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions

import java.util.concurrent.atomic.AtomicReference

import static org.springframework.http.MediaType.APPLICATION_JSON
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@Component
class ReconcileTestSteps {

    private static final String RECONCILE_URL = "/discovery-and-reconciliation/v1/jobs/%s/reconciliations"

    @Autowired
    private MockMvc mockMvc

    void startReconcile(String jobId, Map inputs, List filters) {
        startReconcile(jobId, inputs, filters, Collections.emptyList())
    }

    ResultActions startReconcileResult(String jobId) {
        mockMvc.perform(post(String.format(RECONCILE_URL, jobId))
                .contentType(APPLICATION_JSON)
                .content("{}"))
    }

    void startReconcile(String jobId, Map inputs, List filters, List objects) {
        ExecuteReconcileDto executeReconcileDto = new ExecuteReconcileDto()
                .inputs(inputs)
                .filters(filters)
                .objects(objects)
        final AtomicReference<ExecuteJobResponseDto> response = new AtomicReference()
        mockMvc.perform(post(String.format(RECONCILE_URL, jobId))
                .contentType(APPLICATION_JSON)
                .content(JsonUtils.toJsonString(executeReconcileDto)))
                .andExpect(status().isAccepted())
    }
}