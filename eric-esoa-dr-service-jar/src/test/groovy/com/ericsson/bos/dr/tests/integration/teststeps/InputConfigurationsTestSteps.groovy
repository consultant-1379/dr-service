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
import com.ericsson.bos.dr.tests.integration.utils.YamlUtils
import com.ericsson.bos.dr.web.v1.api.model.ConfigurationListDto
import com.ericsson.bos.dr.web.v1.api.model.FeaturePackDto
import com.ericsson.bos.dr.web.v1.api.model.InputConfigurationDto
import com.ericsson.bos.dr.web.v1.api.model.InputConfigurationDtoAllOfInputs
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions

import java.util.concurrent.atomic.AtomicReference

import static com.ericsson.bos.dr.tests.integration.utils.IOUtils.readClasspathResource
import static org.springframework.http.MediaType.APPLICATION_JSON
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@Component
class InputConfigurationsTestSteps {

    private static final String INPUTS_URL = "/discovery-and-reconciliation/v1/feature-packs/%s/input-configurations"
    private static final String INPUT_URL = "/discovery-and-reconciliation/v1/feature-packs/%s/input-configurations/%s"

    @Autowired
    private MockMvc mockMvc


    ConfigurationListDto getInputConfigs(String featurePackId) {
        final AtomicReference<FeaturePackDto> response = new AtomicReference()
        getInputConfigResult(featurePackId)
                .andExpect(status().isOk())
                .andDo(result -> response.set(JsonUtils.read(result.getResponse().getContentAsString(), ConfigurationListDto.class)))
        return response.get()
    }

    InputConfigurationDto getInputConfig(String configurationId, String featurePackId) {
        final AtomicReference<FeaturePackDto> response = new AtomicReference()
        getInputConfigResult(featurePackId, configurationId)
                .andExpect(status().isOk())
                .andDo(result ->
                        response.set(JsonUtils.read(result.getResponse().getContentAsString(), InputConfigurationDto.class)))
        return response.get()
    }

    InputConfigurationDto evaluateInputConfigFunctions(String configurationId, String featurePackId) {
        final AtomicReference<FeaturePackDto> response = new AtomicReference()
        evaluateInputConfigFunctionsResult(featurePackId, configurationId)
                .andExpect(status().isOk())
                .andDo(result ->
                        response.set(JsonUtils.read(result.getResponse().getContentAsString(), InputConfigurationDto.class)))
        return response.get()
    }

    void deleteInputConfig(String configurationId, String featurePackId) {
        deleteInputConfigResult(featurePackId, configurationId)
                .andExpect(status().isNoContent())
    }

    String createInputConfig(InputConfigurationDto inputConfigurationDto, String featurePackId) {
        createInputConfigResult(inputConfigurationDto, featurePackId)
                .andExpect(status().isCreated())
        return getInputConfigs(featurePackId).items.collect { it.id }.sort().last()
    }

    String updateInputConfig(InputConfigurationDto inputConfigurationDto, String configId, String featurePackId) {
        updateInputConfigResult(featurePackId, inputConfigurationDto, configId)
                .andExpect(status().isCreated())
        return getInputConfigs(featurePackId).items.collect { it.id}.sort().last()
    }

    InputConfigurationDto createInputConfigRequest(String name) {
        InputConfigurationDto configurationDto = new InputConfigurationDto()
                .name(name)
                .description("new input config ${name}")
                .inputs([new InputConfigurationDtoAllOfInputs()
                                 .name("input1")
                                 .value("value1")])
        return configurationDto
    }

    InputConfigurationDto createInputConfigRequest(String path, String regex, String replacement) {
        def file = new File(getClass().getResource(path).toURI())
        def content = file.getText("UTF-8").replaceAll(regex, replacement)
        return YamlUtils.read(content.getBytes(), InputConfigurationDto.class)
    }


    ResultActions createInputConfigResult(InputConfigurationDto inputConfigurationDto, String featurePackId) {
        return mockMvc.perform(post(String.format(INPUTS_URL, featurePackId))
                .contentType(APPLICATION_JSON)
                .content(JsonUtils.toJsonString(inputConfigurationDto))
                .header(HttpHeaders.AUTHORIZATION, readClasspathResource("/tokens/admin_token.json")))
    }

    ResultActions getInputConfigResult(String featurePackId) {
        return mockMvc.perform(get(String.format(INPUTS_URL, featurePackId)))
    }

    ResultActions getInputConfigResult(String featurePackId, String configurationId) {
        return mockMvc.perform(get(String.format(INPUT_URL, featurePackId, configurationId)))
    }

    ResultActions evaluateInputConfigFunctionsResult(String featurePackId, String configurationId) {
        return mockMvc.perform(get(String.format(INPUT_URL, featurePackId, configurationId))
                .queryParam("evaluateFunctions", [true] as String[]))
    }

    ResultActions deleteInputConfigResult(String featurePackId, String configurationId) {
        return mockMvc.perform(delete(String.format(INPUT_URL, featurePackId, configurationId))
                .header(HttpHeaders.AUTHORIZATION, readClasspathResource("/tokens/admin_token.json")))
    }

    ResultActions updateInputConfigResult(String featurePackId, InputConfigurationDto inputConfigurationDto, String configId) {
        return mockMvc.perform(put(String.format(INPUT_URL, featurePackId, configId))
                .contentType(APPLICATION_JSON)
                .content(JsonUtils.toJsonString(inputConfigurationDto))
                .header(HttpHeaders.AUTHORIZATION, readClasspathResource("/tokens/admin_token.json")))
    }
}
