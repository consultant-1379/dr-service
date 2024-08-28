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

import static com.ericsson.bos.dr.service.exceptions.ErrorCode.BAD_REQUEST_PARAM
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.CONFIG_EXISTS
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.CONFIG_NOT_FOUND
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.FP_NOT_FOUND
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.READ_ONLY_ACCESS
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.CONFIG_NAME_MISMATCH
import static com.ericsson.bos.dr.tests.integration.asserts.JsonAssertComparators.ID_COMPARATOR
import static com.ericsson.bos.dr.tests.integration.asserts.JsonAssertComparators.ID_LIST_COMPARATOR
import static com.ericsson.bos.dr.tests.integration.utils.IOUtils.readClasspathResource
import static com.ericsson.bos.dr.tests.integration.utils.JsonUtils.toJsonString
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import com.ericsson.bos.dr.tests.integration.utils.WiremockUtil
import com.ericsson.bos.dr.web.v1.api.model.ConfigurationListDto
import com.ericsson.bos.dr.web.v1.api.model.FeaturePackDto
import com.ericsson.bos.dr.web.v1.api.model.InputConfigurationDto
import com.ericsson.bos.dr.web.v1.api.model.InputConfigurationDtoAllOfInputs
import org.springframework.test.web.servlet.ResultActions
import spock.lang.Unroll

class InputConfigurationsServiceSpec extends BaseSpec {

    def "Create input configuration is successful"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto =
                featurePackTestSteps.uploadFeaturePack("/feature-packs/fp-1", "fp-1")

        when: "Create input configuration"
        InputConfigurationDto expectedInputConfig = inputTestSteps.createInputConfigRequest("newInput")
        String newInputConfigId = inputTestSteps.createInputConfig(expectedInputConfig, featurePackDto.id)
        InputConfigurationDto actualInputConfig = inputTestSteps.getInputConfig(newInputConfigId, featurePackDto.id)

        then: "Input Configuration is persisted"
        actualInputConfig.id != null
        actualInputConfig.name == expectedInputConfig.name
        actualInputConfig.inputs == expectedInputConfig.inputs

        and: "Two input configurations exist"
        inputTestSteps.getInputConfigs(featurePackDto.id).totalCount == 2
    }

    @Unroll
    def "Create an input configuration returns 400"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto =
                featurePackTestSteps.uploadFeaturePack("/feature-packs/fp-1", fp_name)

        when: "Create input configuration with invalid name"
        ResultActions result = inputTestSteps.createInputConfigResult(
                inputTestSteps.createInputConfigRequest(name), featurePackDto.id)

        then: "Response is bad parameter"
        result.andExpect(status().is(400))
                .andExpect(jsonPath("\$.errorCode").value(BAD_REQUEST_PARAM.errorCode))

        where:
        name | fp_name
        null | "fp_1"
        ""   | "fp_2"
    }

    def "Create an input configuration returns 409"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto =
                featurePackTestSteps.uploadFeaturePack("/feature-packs/fp-1", "fp-1")

        when: "Create input configuration with existing name"
        ResultActions result = inputTestSteps.createInputConfigResult(
                inputTestSteps.createInputConfigRequest("inputs_1"), featurePackDto.id)

        then: "Response is conflict"
        result.andExpect(status().is(409))
                .andExpect(jsonPath("\$.errorCode").value(CONFIG_EXISTS.errorCode))
    }

    def "Delete an input configuration is successful"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto =
                featurePackTestSteps.uploadFeaturePack("/feature-packs/fp-1", "fp-1")

        and: "Create input configuration"
        String configId = inputTestSteps.createInputConfig(
                inputTestSteps.createInputConfigRequest("inputs_2"), featurePackDto.id)

        when: "Delete input configuration"
        inputTestSteps.deleteInputConfig(configId, featurePackDto.id)

        then: "Configuration is deleted"
        inputTestSteps.getInputConfigs(featurePackDto.id).items
                .collect { it.name } == ["inputs_1"]
    }

    def "Delete an input configuration uploaded in the FP returns 409"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto =
                featurePackTestSteps.uploadFeaturePack("/feature-packs/fp-1", "fp-1")
        String uploadedWithFPConfigId = featurePackDto.inputs[0].id

        when: "Delete input configuration"
        ResultActions result = inputTestSteps.deleteInputConfigResult(featurePackDto.id, uploadedWithFPConfigId)

        then: "Response is forbidden"
        result.andExpect(status().is(409))
                .andExpect(jsonPath("\$.errorCode").value(READ_ONLY_ACCESS.errorCode))
    }

    def "Delete an input configuration returns 404 when configuration does not exist in feature pack"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto =
                featurePackTestSteps.uploadFeaturePack("/feature-packs/fp-1", "fp-1")

        when: "Delete an input configuration which doesnt exist"
        ResultActions result = inputTestSteps.deleteInputConfigResult(featurePackDto.id, "1000")

        then: "Response is not found"
        result.andExpect(status().is(404))
                .andExpect(jsonPath("\$.errorCode").value(CONFIG_NOT_FOUND.errorCode))
    }

    def "Delete an input configuration returns 404 when feature pack does not exist"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto =
                featurePackTestSteps.uploadFeaturePack("/feature-packs/fp-1", "fp-1")
        String uploadedWithFPConfigId = featurePackDto.inputs[0].id

        when: "Delete an input configuration which doesnt exist"
        ResultActions result = inputTestSteps.deleteInputConfigResult("1000", uploadedWithFPConfigId)

        then: "Response is not found"
        result.andExpect(status().is(404))
                .andExpect(jsonPath("\$.errorCode").value(FP_NOT_FOUND.errorCode))
    }

    def "Replace an input configuration is successful"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto =
                featurePackTestSteps.uploadFeaturePack("/feature-packs/fp-1", "fp-1")
        String uploadedWithFPConfigId = featurePackDto.inputs[0].id

        and: "Create input configuration"
        String configId = inputTestSteps.createInputConfig(
                inputTestSteps.createInputConfigRequest("inputs_2"), featurePackDto.id)

        when: "Replace input configuration"
        String replacementConfigId = inputTestSteps.updateInputConfig(
                inputTestSteps.createInputConfigRequest("inputs_2"), configId, featurePackDto.id)

        then: "New input configuration is created and original id deleted"
        inputTestSteps.getInputConfigs(featurePackDto.id).items
                .collect { it.id } == [uploadedWithFPConfigId, replacementConfigId]
    }

    @Unroll
    def "Replace an input configuration returns 400"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto =
                featurePackTestSteps.uploadFeaturePack("/feature-packs/fp-1", "fp-1")
        String uploadedWithFPConfigId = featurePackDto.inputs[0].id

        and: "Create input configuration"
        String configId = inputTestSteps.createInputConfig(
                inputTestSteps.createInputConfigRequest("inputs_2"), featurePackDto.id)

        when: "Replace input configuration with invalid name"
        ResultActions result = inputTestSteps.updateInputConfigResult(
                featurePackDto.id, inputTestSteps.createInputConfigRequest(name), configId)

        then: "Response is bad parameter"
        result.andExpect(status().is(400))
                .andExpect(jsonPath("\$.errorCode").value(errorCode))

        where:
        name        | errorCode
        null        | BAD_REQUEST_PARAM.errorCode
        "inputs_22" | CONFIG_NAME_MISMATCH.errorCode
    }

    def "Replace an input configuration uploaded in the FP returns 409"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto =
                featurePackTestSteps.uploadFeaturePack("/feature-packs/fp-1", "fp-1")
        String uploadedWithFPConfigId = featurePackDto.inputs[0].id

        when: "Replace input configuration"
        ResultActions result = inputTestSteps.updateInputConfigResult(
                featurePackDto.id, inputTestSteps.createInputConfigRequest(featurePackDto.inputs[0].name), uploadedWithFPConfigId)

        then: "Response is forbidden"
        result.andExpect(status().is(409))
                .andExpect(jsonPath("\$.errorCode").value(READ_ONLY_ACCESS.errorCode))
    }

    def "Replace an input configuration returns 404 when input configuration does not exist"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto =
                featurePackTestSteps.uploadFeaturePack("/feature-packs/fp-1", "fp-1")

        when: "Replace input configuration"
        ResultActions result = inputTestSteps.updateInputConfigResult(
                featurePackDto.id, inputTestSteps.createInputConfigRequest("inputs_2"), "1000")

        then: "Response is not found"
        result.andExpect(status().is(404))
                .andExpect(jsonPath("\$.errorCode").value(CONFIG_NOT_FOUND.errorCode))
    }

    def "Replace an input configuration returns 404 when feature pack does not exist"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto =
                featurePackTestSteps.uploadFeaturePack("/feature-packs/fp-1", "fp-1")
        String uploadedWithFPConfigId = featurePackDto.inputs[0].id

        when: "Replace input configuration"
        ResultActions result = inputTestSteps.updateInputConfigResult(
                "1000", inputTestSteps.createInputConfigRequest("inputs_2"), uploadedWithFPConfigId)

        then: "Response is not found"
        result.andExpect(status().is(404))
                .andExpect(jsonPath("\$.errorCode").value(FP_NOT_FOUND.errorCode))
    }

    def "Get input configurations is successful"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto =
                featurePackTestSteps.uploadFeaturePack("/feature-packs/fp-1", "fp-1")
        String expectedResponse = readClasspathResource("/feature-packs/fp-1/responses/expected_input_configs_response.json")

        when: "Get input configurations"
        ConfigurationListDto configs = inputTestSteps.getInputConfigs(featurePackDto.id)

        then: "Response is as expected"
        assertEquals(expectedResponse, toJsonString(configs), ID_LIST_COMPARATOR)
    }

    def "Get input configurations returns an empty list"() {

        when: "Get input configurations"
        ConfigurationListDto configs = inputTestSteps.getInputConfigs("1000")

        then: "Response is empty"
        configs.items.empty
    }

    def "Get input configuration is successful"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto =
                featurePackTestSteps.uploadFeaturePack("/feature-packs/fp-1", "fp-1")
        String uploadedWithFPConfigId = featurePackDto.inputs[0].id
        String expectedResponse = readClasspathResource("/feature-packs/fp-1/responses/expected_input_config_response.json")

        when: "Get input configuration"
        InputConfigurationDto config = inputTestSteps.getInputConfig(uploadedWithFPConfigId, featurePackDto.id)

        then: "Response is as expected"
        assertEquals(expectedResponse, toJsonString(config), ID_COMPARATOR)
    }

    def "Get input function evaluation is successful"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto =
                featurePackTestSteps.uploadFeaturePack("/feature-packs/fp-12", "fp-12")
        String expectedResponse = readClasspathResource("/feature-packs/fp-12/responses/expected_input_config_response.json")

        and: "Upload input configuration"
        InputConfigurationDto inputConfigurationDto = inputTestSteps.
                createInputConfigRequest("/feature-packs/fp-12/inputs/dynamic_inputs_12.yml", "WIREMOCK_URL", wireMock.baseUrl())
        String newInputConfigId = inputTestSteps.createInputConfig(inputConfigurationDto, featurePackDto.id)

        and: "Stub http executor wiremock requests"
        WiremockUtil.stubForGet(
                "/subsystem-management-api/subsystem-manager/v2/subsystems", "/feature-packs/fp-12/responses/subsystems.json")

        when: "Get input configuration"
        InputConfigurationDto config = inputTestSteps.evaluateInputConfigFunctions(newInputConfigId, featurePackDto.id)

        then: "Response is as expected"
        assertEquals(expectedResponse, toJsonString(config), ID_COMPARATOR)

        and: "Configuration entity is unchanged in db"
        InputConfigurationDto currentInputConfigurationDto = inputConfigurationsRepository
                .findByFeaturePackId(Long.valueOf(featurePackDto.id))[0].getConfig()
        List<InputConfigurationDtoAllOfInputs> inputs = currentInputConfigurationDto.getInputs()
        def listOfDynamicPicklists = inputs.stream()
                .filter {input -> input.getPickList() != null && input.getPickList().contains("fn:execute(executionOption)")}.toList()
        listOfDynamicPicklists.size() == 2
    }

    def "Get input configuration returns 404 when feature pack does not exist"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto =
                featurePackTestSteps.uploadFeaturePack("/feature-packs/fp-1", "fp-1")
        String uploadedWithFPConfigId = featurePackDto.inputs[0].id

        when: "Get input configuration"
        ResultActions result = inputTestSteps.getInputConfigResult("1000", uploadedWithFPConfigId)

        then: "Response is as expected"
        result.andExpect(status().is(404))
                .andExpect(jsonPath("\$.errorCode").value(FP_NOT_FOUND.errorCode))
    }

    def "Get input configuration returns 404 when configuration does not exist in feature pack"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto =
                featurePackTestSteps.uploadFeaturePack("/feature-packs/fp-1", "fp-1")

        when: "Get input configuration"
        ResultActions result = inputTestSteps.getInputConfigResult(featurePackDto.id, "1000")

        then: "Response is as expected"
        result.andExpect(status().is(404))
                .andExpect(jsonPath("\$.errorCode").value(CONFIG_NOT_FOUND.errorCode))
    }
}
