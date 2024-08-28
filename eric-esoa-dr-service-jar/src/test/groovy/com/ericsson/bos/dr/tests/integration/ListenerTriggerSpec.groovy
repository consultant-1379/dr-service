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

import com.ericsson.bos.dr.tests.integration.utils.JsonUtils
import com.ericsson.bos.dr.tests.integration.utils.WiremockUtil
import com.ericsson.bos.dr.web.v1.api.model.FeaturePackDto
import com.ericsson.bos.dr.web.v1.api.model.JobSummaryDto.StatusEnum
import com.ericsson.bos.dr.web.v1.api.model.ListenerTriggerResponseDto
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.test.web.servlet.ResultActions

import static com.ericsson.bos.dr.service.exceptions.ErrorCode.APP_NOT_FOUND
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.FP_NOT_FOUND
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.LISTENER_NOT_FOUND
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.NO_TRIGGER_MATCH
import static com.ericsson.bos.dr.tests.integration.utils.IOUtils.readClasspathResource
import static com.ericsson.bos.dr.web.v1.api.model.DiscoveredObjectDto.StatusEnum.RECONCILE_FAILED
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class ListenerTriggerSpec extends BaseSpec {

    def "Trigger listener executes successfully"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-6", "fp-6-${System.currentTimeMillis()}")

        and: "Read expected response"
        String expTriggerResponse = readClasspathResource("/feature-packs/fp-6/responses/${triggerResponse}")

        and: "Stub http executor wiremock requests"
        WiremockUtil.stubForGet("/fp-6/sources", "/feature-packs/fp-6/responses/${sources}")
        WiremockUtil.stubForGet("/fp-6/targets", "/feature-packs/fp-6/responses/${targets}")
        WiremockUtil.stubForGet("/fp-6/enrich/[0-9]+", "/feature-packs/fp-6/responses/${enrich}")
        WiremockUtil.stubForPost("/fp-6/reconcile/[0-9]+", "/feature-packs/fp-6/responses/${reconcile}")

        when: "Trigger listener to perform discover and reconcile"
        Map event = [sourcesUrl  : "${wireMock.baseUrl()}/fp-6/sources".toString(),
                     targetsUrl  : "${wireMock.baseUrl()}/fp-6/targets".toString(),
                     reconcileUrl: "${wireMock.baseUrl()}/fp-6/reconcile".toString(),
                     enrichUrl   : "${wireMock.baseUrl()}/fp-6/enrich".toString(),
                     eventType   : "CREATE"]
        String response = listenersTestSteps.trigger2(featurePackDto.name, listener, event)
        String jobId = JsonUtils.read(response, ListenerTriggerResponseDto).job.id

        then: "Job completes in state COMPLETED"
        assertJobInState(jobId, StatusEnum.COMPLETED)

        and: "Response is as expected"
        assertEquals(expTriggerResponse, response, JSONCompareMode.LENIENT)

        where:
        listener     | sources        | targets        | enrich        | reconcile        | triggerResponse
        "listener_1" | "source.json"  | ""             | ""            | "reconcile.json" | "trigger_response_1.json"
        "listener_2" | "source.json" | ""             | ""            | "reconcile.json" | "trigger_response_2.json"
        "listener_3" | "sources.json" | "targets.json" | "enrich.json" | "reconcile.json" | "trigger_response_3.json"
        "listener_4" | "sources.json" | "targets.json" | "enrich.json" | "reconcile.json" | "trigger_response_4.json"
        "listener_5" | "source.json"  | ""             | ""            | "reconcile.json" | "trigger_response_5.json"
        "listener_6" | "source.json"  | ""             | ""            | "reconcile.json" | "trigger_response_6.json"
    }

    def "Trigger listener with invalid feature pack returns 404"() {

        when: "Trigger listener wit unknown feature pack"
        ResultActions result = listenersTestSteps.triggerResult("invalid", "listener_1", [:])

        then: "Response is not found"
        result.andExpect(status().is(404))
                .andExpect(jsonPath("\$.errorCode").value(FP_NOT_FOUND.errorCode))
    }

    def "Trigger listener with invalid listener returns 404"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-6", "fp-6-${System.currentTimeMillis()}")

        when: "Trigger listener with unknown listener"
        ResultActions result = listenersTestSteps.triggerResult(featurePackDto.name, "unknown", [:])

        then: "Response is not found"
        result.andExpect(status().is(404))
                .andExpect(jsonPath("\$.errorCode").value(LISTENER_NOT_FOUND.errorCode))
    }

    def "Trigger listener with invalid application returns 404"() {
        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-6", "fp-6-${System.currentTimeMillis()}")

        when: "Trigger listener which references invalid application"
        Map event = [eventType: "CREATE"]
        ResultActions result = listenersTestSteps.triggerResult(featurePackDto.name, "listener_invalid_app", event)

        then: "Response is not found"
        result.andExpect(status().is(404))
                .andExpect(jsonPath("\$.errorCode").value(APP_NOT_FOUND.errorCode))
    }

    def "Trigger listener with no trigger match returns 404"() {
        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-6", "fp-6-${System.currentTimeMillis()}")

        when: "Trigger listener with payload that does not match any trigger condition"
        ResultActions result = listenersTestSteps.triggerResult(featurePackDto.name, "listener_1", [:])

        then: "Response is not found"
        result.andExpect(status().is(404))
                .andExpect(jsonPath("\$.errorCode").value(NO_TRIGGER_MATCH.errorCode))
    }

    def "Trigger listener completed with error when discovery fails"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-6", "fp-6-${System.currentTimeMillis()}")

        and: "Stub http executor wiremock request to return error fetching sources"
        WiremockUtil.stubForGet("/fp-6/sources", 500, "Error!")

        when: "Trigger listener to perform discover and reconcile"
        Map event = [sourcesUrl: "${wireMock.baseUrl()}/fp-6/sources".toString(),
                     eventType : "CREATE"]
        ListenerTriggerResponseDto response = listenersTestSteps.trigger(featurePackDto.name, "listener_1", event)

        then: "Job completes in state DISCOVERY_FAILED"
        assertJobInState(response.job.id, StatusEnum.DISCOVERY_FAILED)
    }

    def "Trigger listener completed with error when reconciliation fails for all objects"() {
        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-6", "fp-6-${System.currentTimeMillis()}")

        and: "Stub http executor wiremock requests, all reconcile requests configured to fail"
        WiremockUtil.stubForGet("/fp-6/sources", "/feature-packs/fp-6/responses/source.json")
        WiremockUtil.stubForGet("/fp-6/targets", "/feature-packs/fp-6/responses/targets.json")
        WiremockUtil.stubForPost("/fp-6/reconcile/[0-9]+", 500, "Error!")

        when: "Trigger listener to perform discover and reconcile"
        Map event = [sourcesUrl  : "${wireMock.baseUrl()}/fp-6/sources".toString(),
                     targetsUrl  : "${wireMock.baseUrl()}/fp-6/targets".toString(),
                     reconcileUrl: "${wireMock.baseUrl()}/fp-6/reconcile".toString(),
                     enrichUrl   : "${wireMock.baseUrl()}/fp-6/enrich".toString(),
                     eventType   : "CREATE"]
        ListenerTriggerResponseDto response = listenersTestSteps.trigger(featurePackDto.name, "listener_1", event)

        then: "Discovered objects in state RECONCILE_FAILED"
        assertDiscoveredObjectsInState(response.job.id, RECONCILE_FAILED)

        and: "Job completes in state RECONCILE_FAILED"
        assertJobInState(response.job.id, StatusEnum.RECONCILE_FAILED)
    }

    def "Trigger listener completed with error when reconciliation fails for one object and completes successfully for all others"() {
        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-6", "fp-6-${System.currentTimeMillis()}")

        and: "Stub http executor wiremock requests"
        WiremockUtil.stubForGet("/fp-6/sources", "/feature-packs/fp-6/responses/sources.json")
        WiremockUtil.stubForGet("/fp-6/targets", "/feature-packs/fp-6/responses/targets.json")
        WiremockUtil.stubForPost("/fp-6/reconcile/[0-9]+", "/feature-packs/fp-6/responses/reconcile.json")
        WiremockUtil.stubForPost("/fp-6/reconcile/1", 500, "Test Error!!!")

        when: "Trigger listener to perform discover and reconcile"
        Map event = [sourcesUrl  : "${wireMock.baseUrl()}/fp-6/sources".toString(),
                     targetsUrl  : "${wireMock.baseUrl()}/fp-6/targets".toString(),
                     reconcileUrl: "${wireMock.baseUrl()}/fp-6/reconcile".toString(),
                     enrichUrl   : "${wireMock.baseUrl()}/fp-6/enrich".toString(),
                     eventType   : "CREATE"]
        ListenerTriggerResponseDto response = listenersTestSteps.trigger(featurePackDto.name, "listener_1", event)

        then: "Job completes in state PARTIALLY_RECONCILED"
        assertJobInState(response.job.id, StatusEnum.PARTIALLY_RECONCILED)
    }
}