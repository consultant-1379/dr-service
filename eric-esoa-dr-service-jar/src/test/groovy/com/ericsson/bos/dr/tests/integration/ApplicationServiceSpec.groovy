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

import static com.ericsson.bos.dr.service.exceptions.ErrorCode.FP_NOT_FOUND
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationDto
import com.ericsson.bos.dr.web.v1.api.model.ConfigurationListDto
import com.ericsson.bos.dr.web.v1.api.model.FeaturePackDto
import org.springframework.test.web.servlet.ResultActions

import static com.ericsson.bos.dr.service.exceptions.ErrorCode.APP_NOT_FOUND
import static com.ericsson.bos.dr.tests.integration.asserts.JsonAssertComparators.ID_LIST_COMPARATOR
import static com.ericsson.bos.dr.tests.integration.asserts.JsonAssertComparators.ID_COMPARATOR
import static com.ericsson.bos.dr.tests.integration.utils.IOUtils.readClasspathResource
import static com.ericsson.bos.dr.tests.integration.utils.JsonUtils.toJsonString
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class ApplicationServiceSpec extends BaseSpec {

    def "Get applications is successful"() {
        setup: "Upload feature pack"
        String expectedResponse = readClasspathResource("/feature-packs/fp-1/responses/expected_apps_response.json")
        FeaturePackDto uploadedFeaturePackDto =
                featurePackTestSteps.uploadFeaturePack("/feature-packs/fp-1", "fp-1")

        when: "Get applications"
        ConfigurationListDto apps = applicationTestSteps.getApplications(uploadedFeaturePackDto.getId())

        then: "Response is as expected"
        assertEquals(expectedResponse, toJsonString(apps), ID_LIST_COMPARATOR)
    }

    def "Get application is successful"() {
        setup: "Upload feature pack"
        String expectedResponse = readClasspathResource("/feature-packs/fp-1/responses/expected_app_response.json")
        FeaturePackDto uploadedFeaturePackDto =
                featurePackTestSteps.uploadFeaturePack("/feature-packs/fp-1", "fp-1")
        String appId = uploadedFeaturePackDto.applications.collect( { it.id }).find()

        when: "Get application"
        ApplicationConfigurationDto app = applicationTestSteps.getApplication(appId, uploadedFeaturePackDto.id)

        then: "Response is as expected"
        assertEquals(expectedResponse, toJsonString(app), ID_COMPARATOR)
    }

    def "Get application returns 404 when application does not exist in given feature pack"() {
        setup: "Upload feature pack"
        FeaturePackDto uploadedFeaturePackDto =
                featurePackTestSteps.uploadFeaturePack("/feature-packs/fp-1", "fp-1")

        when: "Get application"
        ResultActions result = applicationTestSteps.getApplicationResult("1000", uploadedFeaturePackDto.id)

        then: "Response is not found"
        result.andExpect(status().is(404))
                .andExpect(jsonPath("\$.errorCode").value(APP_NOT_FOUND.errorCode))
    }

    def "Get application returns 404 when feature pack does not exist"() {

        setup: "Upload feature pack"
        FeaturePackDto uploadedFeaturePackDto =
                featurePackTestSteps.uploadFeaturePack("/feature-packs/fp-1", "fp-1")
        String appId = uploadedFeaturePackDto.applications.collect( { it.id }).find()

        when: "Get application"
        def nonExistingFeaturePack = "999"
        ResultActions result = applicationTestSteps.getApplicationResult(appId, nonExistingFeaturePack)

        then: "Response is not found"
        result.andExpect(status().is(404))
                .andExpect(jsonPath("\$.errorCode").value(FP_NOT_FOUND.errorCode))
    }
}