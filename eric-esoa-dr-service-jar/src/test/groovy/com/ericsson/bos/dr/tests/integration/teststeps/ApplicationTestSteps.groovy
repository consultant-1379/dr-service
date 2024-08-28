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
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationDto
import com.ericsson.bos.dr.web.v1.api.model.ConfigurationListDto
import com.ericsson.bos.dr.web.v1.api.model.FeaturePackDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions

import java.util.concurrent.atomic.AtomicReference

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@Component
class ApplicationTestSteps {

    private static final String APPS_URL = "/discovery-and-reconciliation/v1/feature-packs/%s/applications"
    private static final String APP_URL = "/discovery-and-reconciliation/v1/feature-packs/%s/applications/%s"

    @Autowired
    private MockMvc mockMvc

    ApplicationConfigurationDto getApplication(String appId, String featurePackId) {
        final AtomicReference<FeaturePackDto> response = new AtomicReference()
        getApplicationResult(appId, featurePackId)
                .andExpect(status().isOk())
                .andDo(result -> response.set(JsonUtils.read(result.getResponse().getContentAsString(), ApplicationConfigurationDto.class)))
        return response.get()
    }

    ResultActions getApplicationResult(String appId, String featurePackId) {
        return mockMvc.perform(get(String.format(APP_URL, featurePackId, appId)))
    }

    ConfigurationListDto getApplications(String featurePackId) {
        final AtomicReference<FeaturePackDto> response = new AtomicReference()
        mockMvc.perform(get(String.format(APPS_URL, featurePackId)))
                .andExpect(status().isOk())
                .andDo(result -> response.set(JsonUtils.read(result.getResponse().getContentAsString(), ConfigurationListDto.class)))
        return response.get()
    }
}