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

import static com.ericsson.bos.dr.service.exceptions.ErrorCode.APP_NOT_FOUND
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.FP_NOT_FOUND
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.JOB_NOT_FOUND
import static com.ericsson.bos.dr.tests.integration.utils.IOUtils.readClasspathResource
import static com.ericsson.bos.dr.web.v1.api.model.DiscoveredObjectDto.StatusEnum.DISCOVERED
import static com.ericsson.bos.dr.web.v1.api.model.DiscoveredObjectDto.StatusEnum.RECONCILE_FAILED
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import com.ericsson.bos.dr.web.v1.api.model.ExecuteJobDtoExecutionOptions
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.test.web.servlet.ResultActions

import com.ericsson.bos.dr.jpa.model.JobEntity
import com.ericsson.bos.dr.service.exceptions.ErrorCode
import com.ericsson.bos.dr.tests.integration.utils.JsonUtils
import com.ericsson.bos.dr.tests.integration.utils.WiremockUtil
import com.ericsson.bos.dr.web.v1.api.model.DiscoveredObjectDto
import com.ericsson.bos.dr.web.v1.api.model.DiscoveredObjectListDto
import com.ericsson.bos.dr.web.v1.api.model.ExecuteReconcileDtoObjectsInner
import com.ericsson.bos.dr.web.v1.api.model.FeaturePackDto
import com.ericsson.bos.dr.web.v1.api.model.JobSummaryDto.StatusEnum
import com.github.tomakehurst.wiremock.client.WireMock

class DiscoverAndReconcileServiceSpec extends BaseSpec {

    def "Discover and Reconcile objects for all or specified filters completes successfully"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/${fp}", "fp-2-${System.currentTimeMillis()}")
        String appId = featurePackDto.applications.first().id

        and: "Read expected responses"
        String expDiscObjectsResponse = readClasspathResource("/feature-packs/${fp}/responses/discovered_objects.json")
        String expReconiledObjectsResponse = readClasspathResource("/feature-packs/${fp}/responses/reconciled_objects.json")

        and: "Stub http executor wiremock requests"
        WiremockUtil.stubForPost("/rest-service/v1/run/dummyEnm/dummyResourceConfig/dummyResource", "/feature-packs/fp-2/responses/sources.json")
        WiremockUtil.stubForPost("/fp-5/sources",
                "{\"jobName\": \"${job}\", \"featurePackId\": \"${featurePackDto.id}\", \"featurePackName\": \"${featurePackDto.name}\"}",
                "/feature-packs/fp-5/responses/sources.json")
        WiremockUtil.stubForGet("/${fp}/sources", "/feature-packs/${fp}/responses/sources.json")
        WiremockUtil.stubForGet("/${fp}/targets", "/feature-packs/${fp}/responses/targets.json")
        WiremockUtil.stubForGet("/${fp}/enrich/[0-9]+", "/feature-packs/${fp}/responses/enrich.json")
        WiremockUtil.stubForPost("/${fp}/reconcile/[0-9]+", "/feature-packs/${fp}/responses/reconcile.json")

        when: "Perform discovery"
        Map inputs = [baseUrl: "${wireMock.baseUrl()}".toString(),
                      sourcesUrl: "${wireMock.baseUrl()}/${fp}/sources".toString(),
                      targetsUrl: "${wireMock.baseUrl()}/${fp}/targets".toString(),
                      enrichUrl : "${wireMock.baseUrl()}/${fp}/enrich".toString()]
        String jobId = discoveryTestSteps.startDiscovery(featurePackDto.id, appId, job, inputs)

        then: "Job is in Discovered state"
        assertJobInState(jobId, StatusEnum.DISCOVERED)

        and: "Get discovered objects response is as expected"
        String discoveryObjectsResponse = discoveryTestSteps.getDiscoveredObjects(jobId)
        assertEquals(expDiscObjectsResponse, discoveryObjectsResponse, JSONCompareMode.LENIENT)

        when: "Reconcile"
        Map reconcileInputs = [baseUrl: "${wireMock.baseUrl()}".toString(),
                               reconcileUrl: "${wireMock.baseUrl()}/${fp}/reconcile".toString(),
                               enrichUrl   : "${wireMock.baseUrl()}/${fp}/enrich".toString()]
        reconcileTestSteps.startReconcile(jobId, reconcileInputs, filters)

        then: "Job completes"
        assertJobInState(jobId, StatusEnum.COMPLETED)

        and: "Get discovered objects response after reconcile is as expected"
        String reconcileObjectsResponse = discoveryTestSteps.getDiscoveredObjects(jobId)
        Map substitutionCtx = ["jobId": jobId, "jobName": getJob(jobId).name,
                               "featurePackId": featurePackDto.id, "featurePackName": featurePackDto.name]
        assertEquals(substitute(expReconiledObjectsResponse, substitutionCtx), reconcileObjectsResponse, JSONCompareMode.LENIENT)

        and: "Discovered object counts are as expected"
        assertDiscoveryObjectCounts(jobId, reconcileCount , reconcileCount, 0)

        where:
        fp      | job    | filters     | reconcileCount
        "fp-2"  | "job1" | null        | 4
        "fp-2"  | "job1" | ["filter1"] | 4
        "fp-4"  | "job1" | ["filter1"] | 2
        "fp-5"  | "job1" | ["filter1"] | 2
        "fp-7"  | "job1" | null        | 1
        "fp-8"  | "job1" | null        | 1
        "fp-9"  | "job1" | null        | 1
        "fp-10" | "job1" | null        | 1
        "fp-11" | "job1" | null        | 2
        "fp-20" | "job1" | null        | 4
        "fp-21" | "job1" | null        | 1
    }

    def "Discover and Reconcile for specified object(s) completes successfully"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-2", "fp-2-${System.currentTimeMillis()}")
        String appId = featurePackDto.applications.first().id

        and: "Read expected responses"
        String expReconiledObjectsResponse = readClasspathResource("/feature-packs/fp-2/responses/${reconcileResponse}")

        and: "Stub http executor wiremock requests"
        WiremockUtil.stubForPost("/rest-service/v1/run/dummyEnm/dummyResourceConfig/dummyResource", "/feature-packs/fp-2/responses/sources.json")
        WiremockUtil.stubForGet("/fp-2/sources", "/feature-packs/fp-2/responses/sources.json")
        WiremockUtil.stubForGet("/fp-2/targets", "/feature-packs/fp-2/responses/targets.json")
        WiremockUtil.stubForGet("/fp-2/enrich/[0-9]+", "/feature-packs/fp-2/responses/enrich.json")
        WiremockUtil.stubForPost("/fp-2/reconcile/[0-9]+", "/feature-packs/fp-2/responses/reconcile.json")

        when: "Perform discovery"
        Map inputs = [sourcesUrl: "${wireMock.baseUrl()}/fp-2/sources".toString(),
                      targetsUrl: "${wireMock.baseUrl()}/fp-2/targets".toString(),
                      enrichUrl : "${wireMock.baseUrl()}/fp-2/enrich".toString()]
        String jobId = discoveryTestSteps.startDiscovery(featurePackDto.id, appId, "job1", inputs)

        then: "Job is in Discovered state"
        assertJobInState(jobId, StatusEnum.DISCOVERED)

        and: "Reconcile"
        List<DiscoveredObjectDto> discoveredObjects = discoveryTestSteps.getDiscoveredObjectItems(jobId)

        Map reconcileInputs = [reconcileUrl: "${wireMock.baseUrl()}/fp-2/reconcile".toString()]
        Map globalInputs = includeGlobalInputs ? reconcileInputs : null
        Map objectInputs = includeObjectInputs ? reconcileInputs : null
        List selectedDiscoveredObjects = discoveredObjects
                .findAll { objectPropIds.contains(it.properties['id']) }
        List objectsToReconcile = selectedDiscoveredObjects.collect {
            new ExecuteReconcileDtoObjectsInner().objectId(it.objectId).inputs(objectInputs).filters(objectFilters)
        }
        reconcileTestSteps.startReconcile(jobId, globalInputs, globalFilters, objectsToReconcile)

        then: "Job completes in state Partially Reconciled"
        assertJobInState(jobId, StatusEnum.PARTIALLY_RECONCILED)

        and: "Get discovered objects response after reconcile is as expected"
        String reconcileObjectsResponse = discoveryTestSteps.getDiscoveredObjects(jobId)
        assertEquals(expReconiledObjectsResponse, reconcileObjectsResponse, JSONCompareMode.LENIENT)

        and: "Discovered object counts are as expected"
        assertDiscoveryObjectCounts(jobId, 4 , reconcileCount, 0)

        where:
        objectPropIds   | globalFilters | objectFilters | includeGlobalInputs | includeObjectInputs | reconcileResponse            | reconcileCount
        ["7"]           | ["filter1"]   | null          | true                | false               | "reconciled_objects_1.json"  | 1
        ["7"]           | null          | ["filter1"]   | true                | false               | "reconciled_objects_1.json"  | 1
        ["7"]           | null          | null          | true                | false               | "reconciled_objects_1.json"  | 1
        //["7"]           | null          | ["filter1"]   | false               | true                | "reconciled_objects_1.json" | 1
        ["7", "8", "9"] | ["filter1"]   | null          | true                | false               | "reconciled_objects_3.json"  | 3
    }

    def "Discover and Auto Reconcile completes successfully"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-2", "fp-2-${System.currentTimeMillis()}")
        String appId = featurePackDto.applications.first().id

        and: "Read expected responses"
        String expReconiledObjectsResponse = readClasspathResource("/feature-packs/fp-2/responses/reconciled_objects.json")

        and: "Stub http executor wiremock requests"
        WiremockUtil.stubForPost("/rest-service/v1/run/dummyEnm/dummyResourceConfig/dummyResource", "/feature-packs/fp-2/responses/sources.json")
        WiremockUtil.stubForGet("/fp-2/targets", "/feature-packs/fp-2/responses/targets.json")
        WiremockUtil.stubForGet("/fp-2/enrich/[0-9]+", "/feature-packs/fp-2/responses/enrich.json")
        WiremockUtil.stubForPost("/fp-2/reconcile/[0-9]+", "/feature-packs/fp-2/responses/reconcile.json")

        when: "Perform discovery and auto reconcile"
        Map inputs = [baseUrl: "${wireMock.baseUrl()}".toString(),
                      sourcesUrl: "${wireMock.baseUrl()}/fp-2/sources".toString(),
                      targetsUrl: "${wireMock.baseUrl()}/fp-2/targets".toString(),
                      reconcileUrl: "${wireMock.baseUrl()}/fp-2/reconcile".toString(),
                      enrichUrl : "${wireMock.baseUrl()}/fp-2/enrich".toString()]
        String jobId = discoveryTestSteps.startDiscoveryAndReconcile(featurePackDto.id, appId, "job1", inputs)

        then: "Job completes"
        assertJobInState(jobId, StatusEnum.COMPLETED)

        and: "Get discovered objects response after reconcile is as expected"
        String reconcileObjectsResponse = discoveryTestSteps.getDiscoveredObjects(jobId)
        assertEquals(expReconiledObjectsResponse, reconcileObjectsResponse, JSONCompareMode.LENIENT)

        and: "Discovered object counts are as expected"
        assertDiscoveryObjectCounts(jobId, 4 , 4, 0)
    }

    def "Duplicate Discover and Reconcile completes successfully"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-2", "fp-2-${System.currentTimeMillis()}")
        String appId = featurePackDto.applications.first().id
        String appName = featurePackDto.applications.first().name

        and: "Read expected responses"
        String expReconiledObjectsResponse = readClasspathResource("/feature-packs/fp-2/responses/reconciled_objects.json")

        and: "Stub http executor wiremock requests"
        WiremockUtil.stubForPost("/rest-service/v1/run/dummyEnm/dummyResourceConfig/dummyResource", "/feature-packs/fp-2/responses/sources.json")
        WiremockUtil.stubForGet("/fp-2/targets", "/feature-packs/fp-2/responses/targets.json")
        WiremockUtil.stubForGet("/fp-2/enrich/[0-9]+", "/feature-packs/fp-2/responses/enrich.json")
        WiremockUtil.stubForPost("/fp-2/reconcile/[0-9]+", "/feature-packs/fp-2/responses/reconcile.json")

        and: "Create original auto-reconciled Job"
        Map inputs = [baseUrl: "${wireMock.baseUrl()}".toString(),
                      sourcesUrl: "${wireMock.baseUrl()}/fp-2/sources".toString(),
                      targetsUrl: "${wireMock.baseUrl()}/fp-2/targets".toString(),
                      reconcileUrl: "${wireMock.baseUrl()}/fp-2/reconcile".toString(),
                      enrichUrl : "${wireMock.baseUrl()}/fp-2/enrich".toString()]
        JobEntity jobEntity = new JobEntity([name: "original",
                                             featurePackId: Long.valueOf(featurePackDto.id),
                                             featurePackName: featurePackDto.name,
                                             applicationId: Long.valueOf(appId),
                                             applicationName:  appName,
                                             applicationJobName: 'job1',
                                             inputs: inputs,
                                             executionOptions: new ExecuteJobDtoExecutionOptions([autoReconcile: true]),
                                             jobStatus: "completed"])
        JobEntity originalJobEntity = jobRepository.save(jobEntity)

        when: "Perform duplicate discovery and auto reconcile"
        String jobId = discoveryTestSteps.startDuplicateDiscovery(String.valueOf(originalJobEntity.id))

        then: "Duplicate Job completes"
        assertJobInState(jobId, StatusEnum.COMPLETED)

        and: "Get discovered objects response after reconcile is as expected"
        String reconcileObjectsResponse = discoveryTestSteps.getDiscoveredObjects(jobId)
        assertEquals(expReconiledObjectsResponse, reconcileObjectsResponse, JSONCompareMode.LENIENT)

        and: "Discovered object counts are as expected"
        assertDiscoveryObjectCounts(jobId, 4 , 4, 0)
    }

    def "Reconcile fails for one object and completes successfully for all others"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-2", "fp-2-${System.currentTimeMillis()}")
        String appId = featurePackDto.applications.first().id

        and: "Read expected responses"
        String expReconiledObjectsResponse = readClasspathResource("/feature-packs/fp-2/responses/reconcile_error_3.json")

        and: "Stub http executor wiremock requests"
        WiremockUtil.stubForPost("/rest-service/v1/run/dummyEnm/dummyResourceConfig/dummyResource", "/feature-packs/fp-2/responses/sources.json")
        WiremockUtil.stubForGet("/fp-2/sources", "/feature-packs/fp-2/responses/sources.json")
        WiremockUtil.stubForGet("/fp-2/targets", "/feature-packs/fp-2/responses/targets.json")
        WiremockUtil.stubForGet("/fp-2/enrich/[0-9]+", "/feature-packs/fp-2/responses/enrich.json")
        WiremockUtil.stubForPost("/fp-2/reconcile/[0-9]+", "/feature-packs/fp-2/responses/reconcile.json")
        WiremockUtil.stubForPost("/fp-2/reconcile/7", 500, "Test Error!!!")

        when: "Perform discovery"
        Map inputs = [sourcesUrl: "${wireMock.baseUrl()}/fp-2/sources".toString(),
                      targetsUrl: "${wireMock.baseUrl()}/fp-2/targets".toString(),
                      enrichUrl : "${wireMock.baseUrl()}/fp-2/enrich".toString()]
        String jobId = discoveryTestSteps.startDiscovery(featurePackDto.id, appId, "job1", inputs)

        then: "Job is in Discovered state"
        assertJobInState(jobId, StatusEnum.DISCOVERED)

        and: "Reconcile"
        Map reconcileInputs = [reconcileUrl: "${wireMock.baseUrl()}/fp-2/reconcile".toString()]
        reconcileTestSteps.startReconcile(jobId, reconcileInputs, null)

        then: "Job completes in state Partially Reconciled"
        assertJobInState(jobId, StatusEnum.PARTIALLY_RECONCILED)

        and: "Discovered object with id 7 is failed, all others are reconciled"
        String reconcileObjectsResponse = discoveryTestSteps.getDiscoveredObjects(jobId)
        assertEquals(expReconiledObjectsResponse, reconcileObjectsResponse, JSONCompareMode.LENIENT)

        and: "Discovered object counts are as expected"
        assertDiscoveryObjectCounts(jobId, 4 , 3, 1)
    }

    def "Reconcile object which previously failed"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-2", "fp-2-${System.currentTimeMillis()}")
        String appId = featurePackDto.applications.first().id

        and: "Stub http executor wiremock requests"
        WiremockUtil.stubForPost("/rest-service/v1/run/dummyEnm/dummyResourceConfig/dummyResource", "/feature-packs/fp-2/responses/sources.json")
        WiremockUtil.stubForGet("/fp-2/sources", "/feature-packs/fp-2/responses/sources.json")
        WiremockUtil.stubForGet("/fp-2/targets", "/feature-packs/fp-2/responses/targets.json")
        WiremockUtil.stubForGet("/fp-2/enrich/[0-9]+", "/feature-packs/fp-2/responses/enrich.json")
        WiremockUtil.stubForPost("/fp-2/reconcile/[0-9]+", "/feature-packs/fp-2/responses/invalid", "Reconcile", "STARTED", "RECONCILE_SUCCESS")
        WiremockUtil.stubForPost("/fp-2/reconcile/[0-9]+", "/feature-packs/fp-2/responses/reconcile.json", "Reconcile", "RECONCILE_SUCCESS", "")
        WireMock.setScenarioState("Reconcile", "STARTED")

        when: "Perform discovery"
        Map inputs = [sourcesUrl: "${wireMock.baseUrl()}/fp-2/sources".toString(),
                      targetsUrl: "${wireMock.baseUrl()}/fp-2/targets".toString(),
                      enrichUrl : "${wireMock.baseUrl()}/fp-2/enrich".toString()]
        String jobId = discoveryTestSteps.startDiscovery(featurePackDto.id, appId, "job1", inputs)

        then: "Job is in Discovered state"
        assertJobInState(jobId, StatusEnum.DISCOVERED)

        and: "Reconcile single object"
        String objectId = discoveryTestSteps.getDiscoveredObjectItems(jobId)[0].objectId
        Map reconcileInputs = [reconcileUrl: "${wireMock.baseUrl()}/fp-2/reconcile".toString()]
        List objectsToReconcile = [new ExecuteReconcileDtoObjectsInner().objectId(objectId)]
        reconcileTestSteps.startReconcile(jobId, reconcileInputs, [], objectsToReconcile)

        then: "Job completes in state Reconcile Failed"
        assertJobInState(jobId, StatusEnum.RECONCILE_FAILED)

        and: "Reconcile fails for the object"
        discoveryTestSteps.getDiscoveredObject(jobId, objectId).status == DiscoveredObjectDto.StatusEnum.RECONCILE_FAILED

        when: "Reconcile object again"
        reconcileTestSteps.startReconcile(jobId, reconcileInputs, [], objectsToReconcile)

        then: "Object is in state RECONCILED"
        pollingConditions.eventually {
            DiscoveredObjectDto discoveredObject = discoveryTestSteps.getDiscoveredObject(jobId, objectId)
            assert discoveredObject.status == DiscoveredObjectDto.StatusEnum.RECONCILED
            assert discoveredObject.errorMessage == null

        and: "Discovered object counts are as expected"
        assertDiscoveryObjectCounts(jobId, 4 , 1, 0)
        }

        and: "Job completes in state Partially Reconciled"
        assertJobInState(jobId, StatusEnum.PARTIALLY_RECONCILED)
    }

    def "Reconcile fails for all objects due to error substituting http url"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-3", "fp-3-${System.currentTimeMillis()}")
        String appId = featurePackDto.applications.first().id

        and: "Read expected responses"
        String expReconiledObjectsResponse = readClasspathResource("/feature-packs/fp-3/responses/reconcile_substitution_error.json")

        and: "Stub http executor wiremock requests"
        WiremockUtil.stubForGet("/fp-3/sources", "/feature-packs/fp-3/responses/sources.json")
        WiremockUtil.stubForGet("/fp-3/targets", "/feature-packs/fp-3/responses/targets.json")

        when: "Perform discovery"
        Map inputs = [sourcesUrl: "${wireMock.baseUrl()}/fp-3/sources".toString(),
                      targetsUrl: "${wireMock.baseUrl()}/fp-3/targets".toString()]
        String jobId = discoveryTestSteps.startDiscovery(featurePackDto.id, appId, "job1", inputs)

        then: "Job is in Discovered state"
        assertJobInState(jobId, StatusEnum.DISCOVERED)

        and: "Reconcile"
        Map reconcileInputs = [reconcileUrl: "${wireMock.baseUrl()}/fp-3/reconcile".toString()]
        reconcileTestSteps.startReconcile(jobId, reconcileInputs, null)

        then: "Job completes in state Reconcile Failed"
        assertJobInState(jobId, StatusEnum.RECONCILE_FAILED)

        and: "Reconcile fails for all objects due to substitution error"
        String reconcileObjectsResponse = discoveryTestSteps.getDiscoveredObjects(jobId)
        assertEquals(expReconiledObjectsResponse, reconcileObjectsResponse, JSONCompareMode.LENIENT)

        and: "Discovered object counts are as expected"
        assertDiscoveryObjectCounts(jobId, 1 , 0, 1)
    }

    def "Reconcile fails for all objects due to mapping error"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-2", "fp-2-${System.currentTimeMillis()}")
        String appId = featurePackDto.applications.first().id

        and: "Read expected responses"
        String expReconiledObjectsResponse = readClasspathResource("/feature-packs/fp-2/responses/reconcile_mapping_error.json")

        and: "Stub http executor wiremock requests"
        WiremockUtil.stubForPost("/rest-service/v1/run/dummyEnm/dummyResourceConfig/dummyResource", "/feature-packs/fp-2/responses/sources.json")
        WiremockUtil.stubForGet("/fp-2/sources", "/feature-packs/fp-2/responses/sources.json")
        WiremockUtil.stubForGet("/fp-2/targets", "/feature-packs/fp-2/responses/targets.json")
        WiremockUtil.stubForGet("/fp-2/enrich/[0-9]+", "/feature-packs/fp-2/responses/enrich.json")
        WiremockUtil.stubForPost("/fp-2/reconcile/[0-9]+", 200, "not_json_string")

        when: "Perform discovery"
        Map inputs = [sourcesUrl: "${wireMock.baseUrl()}/fp-2/sources".toString(),
                      targetsUrl: "${wireMock.baseUrl()}/fp-2/targets".toString(),
                      enrichUrl : "${wireMock.baseUrl()}/fp-2/enrich".toString()]
        String jobId = discoveryTestSteps.startDiscovery(featurePackDto.id, appId, "job1", inputs)

        then: "Job is in Discovered state"
        assertJobInState(jobId, StatusEnum.DISCOVERED)

        and: "Reconcile"
        Map reconcileInputs = [reconcileUrl: "${wireMock.baseUrl()}/fp-2/reconcile".toString()]
        reconcileTestSteps.startReconcile(jobId, reconcileInputs, null)

        then: "Job completes in state Reconcile Failed"
        assertJobInState(jobId, StatusEnum.RECONCILE_FAILED)

        and: "Reconcile fails for all objects due to mapping error"
        String reconcileObjectsResponse = discoveryTestSteps.getDiscoveredObjects(jobId)
        assertEquals(expReconiledObjectsResponse, reconcileObjectsResponse, JSONCompareMode.LENIENT)

        and: "Discovered object counts are as expected"
        assertDiscoveryObjectCounts(jobId, 4 , 0, 4)
    }

    def "Reconcile does not error and skips invalid filter names"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-2", "fp-2-${System.currentTimeMillis()}")
        String appId = featurePackDto.applications.first().id

        and: "Stub http executor wiremock requests"
        WiremockUtil.stubForPost("/rest-service/v1/run/dummyEnm/dummyResourceConfig/dummyResource", "/feature-packs/fp-2/responses/sources.json")
        WiremockUtil.stubForGet("/fp-2/sources", "/feature-packs/fp-2/responses/sources.json")
        WiremockUtil.stubForGet("/fp-2/targets", "/feature-packs/fp-2/responses/targets.json")
        WiremockUtil.stubForGet("/fp-2/enrich/[0-9]+", "/feature-packs/fp-2/responses/enrich.json")

        when: "Perform discovery"
        Map inputs = [sourcesUrl: "${wireMock.baseUrl()}/fp-2/sources".toString(),
                      targetsUrl: "${wireMock.baseUrl()}/fp-2/targets".toString(),
                      enrichUrl : "${wireMock.baseUrl()}/fp-2/enrich".toString()]
        String jobId = discoveryTestSteps.startDiscovery(featurePackDto.id, appId, "job1", inputs)

        then: "Job is in Discovered state"
        assertJobInState(jobId, StatusEnum.DISCOVERED)

        and: "Reconcile"
        Map reconcileInputs = [reconcileUrl: "${wireMock.baseUrl()}/fp-2/reconcile".toString()]
        reconcileTestSteps.startReconcile(jobId, reconcileInputs, ["invalidFilter"])

        then: "Job completes in state Discovered"
        assertJobInState(jobId, StatusEnum.DISCOVERED)

        and: "All discovered objects are in state Discovered"
        assertDiscoveredObjectsInState(jobId, DISCOVERED)

        and: "Discovered object counts are as expected"
        assertDiscoveryObjectCounts(jobId, 4 , 0, 0)
    }

    def "Reconcile does not error and skips invalid object ids"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-2", "fp-2-${System.currentTimeMillis()}")
        String appId = featurePackDto.applications.first().id

        and: "Stub http executor wiremock requests"
        WiremockUtil.stubForPost("/rest-service/v1/run/dummyEnm/dummyResourceConfig/dummyResource", "/feature-packs/fp-2/responses/sources.json")
        WiremockUtil.stubForGet("/fp-2/sources", "/feature-packs/fp-2/responses/sources.json")
        WiremockUtil.stubForGet("/fp-2/targets", "/feature-packs/fp-2/responses/targets.json")
        WiremockUtil.stubForGet("/fp-2/enrich/[0-9]+", "/feature-packs/fp-2/responses/enrich.json")

        when: "Perform discovery"
        Map inputs = [sourcesUrl: "${wireMock.baseUrl()}/fp-2/sources".toString(),
                      targetsUrl: "${wireMock.baseUrl()}/fp-2/targets".toString(),
                      enrichUrl : "${wireMock.baseUrl()}/fp-2/enrich".toString()]
        String jobId = discoveryTestSteps.startDiscovery(featurePackDto.id, appId, "job1", inputs)

        then: "Job is in Discovered state"
        assertJobInState(jobId, StatusEnum.DISCOVERED)

        and: "Reconcile"
        Map reconcileInputs = [reconcileUrl: "${wireMock.baseUrl()}/fp-2/reconcile".toString()]
        List objectsToReconcile = [new ExecuteReconcileDtoObjectsInner().objectId("999")]
        reconcileTestSteps.startReconcile(jobId, reconcileInputs, [], objectsToReconcile)

        then: "Job completes in state Discovered"
        assertJobInState(jobId, StatusEnum.DISCOVERED)

        and: "All discovered objects are in state Discovered"
        assertDiscoveredObjectsInState(jobId, DISCOVERED)

        and: "Discovered object counts are as expected"
        assertDiscoveryObjectCounts(jobId, 4 , 0, 0)
    }

    def "Discovery flow fails when mandatory input not supplied"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-2", "fp-2-${System.currentTimeMillis()}")
        String appId = featurePackDto.applications.first().id

        when: "Perform discovery without supplying mandatory inputs"
        String jobId = discoveryTestSteps.startDiscovery(featurePackDto.id, appId, "job1", [:])

        then: "Job is in Discovery_Failed state with expected error message"
        assertJobInDiscoveryFailedState(jobId,
                "DR-16:Mandatory inputs '[sourcesUrl, targetsUrl]' are missing.")

        and: "Discovered object counts are as expected"
        assertDiscoveryObjectCounts(jobId, 0 , 0, 0)
    }

    def "Discovery flow fails when error discovering sources and targets"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-2", "fp-2-${System.currentTimeMillis()}")
        String appId = featurePackDto.applications.first().id

        and: "Stub http executor wiremock requests"
        WiremockUtil.stubForPost("/rest-service/v1/run/dummyEnm/dummyResourceConfig/dummyResource", 500, "Error!")
        WiremockUtil.stubForGet("/fp-2/targets", 500, "Error!")

        when: "Perform discovery"
        Map inputs = [sourcesUrl: "${wireMock.baseUrl()}/fp-2/sources".toString(),
                      targetsUrl: "${wireMock.baseUrl()}/fp-2/targets".toString()]
        String jobId = discoveryTestSteps.startDiscovery(featurePackDto.id, appId, "job1", inputs)

        then: "Job is in Discovery_Failed state with expected error messages"
        String errorMessage1Part1 = "DR-20:Execution step 'CommandStep' failed: 'Command 'Method: POST\nURL: http://localhost:8081/rest-service/v1/run/dummyEnm/dummyResourceConfig/dummyResource\nHeaders:"
        String errorMessage1Part2 = "Content-Type:\"application/json\""
        String errorMessage1Part3 = "Accept:\"*/*\""
        String errorMessage1Part4 = "' failed with output 'Error!'"
        String errorMessage2 = "DR-20:Execution step 'CommandStep' failed: 'Command 'Method: GET\nURL: http://localhost:8081/fp-2/targets\nHeaders: [Content-Type:\"application/json\"]' failed with output 'Error!'"
        assertJobInDiscoveryFailedState(jobId, errorMessage1Part1, errorMessage1Part2, errorMessage1Part3, errorMessage1Part4, errorMessage2)

        and: "Discovered object counts are as expected"
        assertDiscoveryObjectCounts(jobId, 0 , 0, 0)
    }

    def "Discovery flow fails when enrichment fails for some sources and targets"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-2", "fp-2-${System.currentTimeMillis()}")
        String appId = featurePackDto.applications.first().id

        and: "Stub http executor wiremock requests"
        WiremockUtil.stubForPost("/rest-service/v1/run/dummyEnm/dummyResourceConfig/dummyResource", "/feature-packs/fp-2/responses/sources.json")
        WiremockUtil.stubForGet("/fp-2/targets", "/feature-packs/fp-2/responses/targets.json")
        WiremockUtil.stubForGet("/fp-2/enrich/[0-9]+", "/feature-packs/fp-2/responses/enrich.json")
        WiremockUtil.stubForGet("/fp-2/enrich/1", 500, "Error!")

        when: "Perform discovery"
        Map inputs = [sourcesUrl: "${wireMock.baseUrl()}/fp-2/sources".toString(),
                      targetsUrl: "${wireMock.baseUrl()}/fp-2/targets".toString(),
                      enrichUrl : "${wireMock.baseUrl()}/fp-2/enrich".toString()]
        String jobId = discoveryTestSteps.startDiscovery(featurePackDto.id, appId, "job1", inputs)

        then: "Job is in Discovery_Failed state with expected error messages"
        String errorMessage = "DR-20:Execution step 'CommandStep' failed: 'Command 'Method: GET\nURL: http://localhost:8081/fp-2/enrich/1\nHeaders: [Content-Type:\"application/json\"]' failed with output 'Error!'"
        assertJobInDiscoveryFailedState(jobId, errorMessage)

        and: "Discovered object counts are as expected"
        assertDiscoveryObjectCounts(jobId, 0 , 0, 0)
    }

    def "Reconcile flow fails when mandatory input not supplied"() {
        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-2", "fp-2-${System.currentTimeMillis()}")
        String appId = featurePackDto.applications.first().id

        and: "Stub http executor wiremock requests"
        WiremockUtil.stubForPost("/rest-service/v1/run/dummyEnm/dummyResourceConfig/dummyResource", "/feature-packs/fp-2/responses/sources.json")
        WiremockUtil.stubForGet("/fp-2/sources", "/feature-packs/fp-2/responses/sources.json")
        WiremockUtil.stubForGet("/fp-2/targets", "/feature-packs/fp-2/responses/targets.json")
        WiremockUtil.stubForGet("/fp-2/enrich/[0-9]+", "/feature-packs/fp-2/responses/enrich.json")

        when: "Perform discovery"
        Map inputs = [sourcesUrl: "${wireMock.baseUrl()}/fp-2/sources".toString(),
                      targetsUrl: "${wireMock.baseUrl()}/fp-2/targets".toString(),
                      enrichUrl : "${wireMock.baseUrl()}/fp-2/enrich".toString()]
        String jobId = discoveryTestSteps.startDiscovery(featurePackDto.id, appId, "job1", inputs)

        then: "Job is in Discovered state"
        assertJobInState(jobId, StatusEnum.DISCOVERED)

        when: "Reconcile without supplying mandatory inputs"
        ResultActions result = reconcileTestSteps.startReconcileResult(jobId)

        then: "400 response returned"
        result.andExpect(status().is(400))
                .andExpect(jsonPath("\$.errorCode").value(ErrorCode.MISSING_INPUTS.errorCode))

        and: "Discovered object counts are as expected"
        assertDiscoveryObjectCounts(jobId, 4, 0, 0)

        and: "Job completes in state Discovered"
        assertJobInState(jobId, StatusEnum.DISCOVERED)
    }

    def "Discovery flow fails when source maps to more than one target"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-4", "fp-4-${System.currentTimeMillis()}")
        String appId = featurePackDto.applications.first().id

        and: "Stub http executor wiremock requests"
        WiremockUtil.stubForGet("/fp-4/sources", "/feature-packs/fp-4/responses/sources.json")
        WiremockUtil.stubForGet("/fp-4/targets", "/feature-packs/fp-4/responses/targets_with_duplicate.json")

        when: "Perform discovery"
        Map inputs = [sourcesUrl: "${wireMock.baseUrl()}/fp-4/sources".toString(),
                      targetsUrl: "${wireMock.baseUrl()}/fp-4/targets".toString()]
        String jobId = discoveryTestSteps.startDiscovery(featurePackDto.id, appId, "job1", inputs)

        then: "Job is in Discovery_Failed state with expected error messages"
        String errorMessage = "DR-25:Multiple targets are mapped 'id:id&name:name' to the same source."
        assertJobInDiscoveryFailedState(jobId, errorMessage)

        and: "Discovered object counts are as expected"
        assertDiscoveryObjectCounts(jobId, 0 , 0, 0)
    }

    def "Discovery fails when requested feature pack id does not exist"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-2", "fp-2-${System.currentTimeMillis()}")
        String appId = featurePackDto.applications.first().id

        when: "Perform discovery using invalid feature pack id"
        ResultActions result = discoveryTestSteps.startDiscoveryResult("999", appId, "job1", [:])

        then: "Discovery request fails with error APP_NOT_FOUND"
        result.andExpect(status().is(404))
                .andExpect(jsonPath("\$.errorCode").value(FP_NOT_FOUND.errorCode))

        and: "No job exists"
        jobTestSteps.getJobs().totalCount == 0
    }

    def "Discovery fails when requested application id does not exist"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-2", "fp-2-${System.currentTimeMillis()}")

        when: "Perform discovery using invalid app id"
        ResultActions result = discoveryTestSteps.startDiscoveryResult(featurePackDto.id, "10000", "job1", [:])

        then: "Discovery request fails with error APP_NOT_FOUND"
        result.andExpect(status().is(404))
                .andExpect(jsonPath("\$.errorCode").value(APP_NOT_FOUND.errorCode))

        and: "No job exists"
        jobTestSteps.getJobs().totalCount == 0
    }

    def "Discovery fails when requested job does not exist"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-2", "fp-2-${System.currentTimeMillis()}")
        String appId = featurePackDto.applications.first().id

        when: "Perform discovery using invalid job name"
        ResultActions result = discoveryTestSteps.startDiscoveryResult(featurePackDto.id, appId, "job2", [:])

        then: "Discovery request fails with error JOB_NOT_FOUND"
        result.andExpect(status().is(404))
                .andExpect(jsonPath("\$.errorCode").value(JOB_NOT_FOUND.errorCode))

        and: "No job exists"
        jobTestSteps.getJobs().totalCount == 0
    }

    def "Reconcile fails enriching source object"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-22", "fp-22-${System.currentTimeMillis()}")
        String appId = featurePackDto.applications.first().id

        and: "Stub http executor wiremock requests, return 500 for enrich request"
        WiremockUtil.stubForGet("/fp-22/sources", "/feature-packs/fp-22/responses/sources.json")
        WiremockUtil.stubForGet("/fp-22/targets", "/feature-packs/fp-22/responses/targets.json")
        WiremockUtil.stubForGet("/fp-22/enrich/[0-9]+", 500, "Test Error!!!")

        when: "Perform discovery"
        Map inputs = [sourcesUrl: "${wireMock.baseUrl()}/fp-22/sources".toString(),
                      targetsUrl: "${wireMock.baseUrl()}/fp-22/targets".toString()]
        String jobId = discoveryTestSteps.startDiscovery(featurePackDto.id, appId, "job1", inputs)

        then: "Job is in Discovered state"
        assertJobInState(jobId, StatusEnum.DISCOVERED)

        and: "Reconcile"
        Map reconcileInputs = [enrichUrl   : "${wireMock.baseUrl()}/fp-22/enrich".toString(),
                               reconcileUrl: "${wireMock.baseUrl()}/fp-22/reconcile".toString()]
        reconcileTestSteps.startReconcile(jobId, reconcileInputs, null)

        then: "Job completes in state Reconcile Failed"
        assertJobInState(jobId, StatusEnum.RECONCILE_FAILED)

        and: "Discovered objects are RECONCILE_FAILED with error message set"
        List<DiscoveredObjectDto> discoveredObjects =
                JsonUtils.read(discoveryTestSteps.getDiscoveredObjects(jobId), DiscoveredObjectListDto).getItems()
        discoveredObjects.every {
            it.status == RECONCILE_FAILED && it.errorMessage.contains("Test Error!!!")
        }
    }

    def "Job completes when no discovered objects"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-2", "fp-2-${System.currentTimeMillis()}")
        String appId = featurePackDto.applications.first().id

        and: "Stub http executor wiremock requests to return no sources and targets"
        WiremockUtil.stubForPost("/rest-service/v1/run/dummyEnm/dummyResourceConfig/dummyResource", "[]")
        WiremockUtil.stubForGet("/fp-2/sources", "[]")
        WiremockUtil.stubForGet("/fp-2/targets", "[]]")

        when: "Perform discovery"
        Map inputs = [sourcesUrl: "${wireMock.baseUrl()}/fp-2/sources".toString(),
                      targetsUrl: "${wireMock.baseUrl()}/fp-2/targets".toString()]
        String jobId = discoveryTestSteps.startDiscovery(featurePackDto.id, appId, "job1", inputs)

        then: "Job is in Completed state"
        assertJobInState(jobId, StatusEnum.COMPLETED)
    }

    def "Reconcile flow fails when job is in an invalid state"() {
        setup: "Create job in state"
        JobEntity jobEntity = new JobEntity(name: "job-name", jobStatus: jobStatus,
                featurePackId: 1l, featurePackName: "fp1",  applicationId: 1l, applicationName: "app1", applicationJobName: "appJob1")
        jobRepository.save(jobEntity)

        when: "Reconcile"
        ResultActions result = reconcileTestSteps.startReconcileResult(jobEntity.id.toString())

        then: "409 response returned"
        result.andExpect(status().is(409))
                .andExpect(jsonPath("\$.errorCode").value(ErrorCode.INVALID_STATE_FOR_RECONCILE.errorCode))

        where:
        jobStatus | _
        "NEW" | _
        "DISCOVERY_INPROGRESS" | _
        "DISCOVERY_FAILED" | _
        "RECONCILE_REQUESTED" | _
        "COMPLETED" | _
    }

    def "Reconcile objects filter by filter when objects match multiple filters"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-23", "fp-23-${System.currentTimeMillis()}")
        String appId = featurePackDto.applications.first().id

        and: "Stub http executor wiremock requests, reconcile initially configured to return error"
        WiremockUtil.stubForGet("/fp-23/sources", "/feature-packs/fp-23/responses/sources.json")
        WiremockUtil.stubForGet("/fp-23/targets", "/feature-packs/fp-23/responses/targets.json")
        WiremockUtil.stubForPost("/fp-23/reconcile/[0-9]+", 500, "Test Error!!!")

        when: "Perform discovery"
        Map inputs = [sourcesUrl: "${wireMock.baseUrl()}/fp-23/sources".toString(),
                      targetsUrl: "${wireMock.baseUrl()}/fp-23/targets".toString()]
        String jobId = discoveryTestSteps.startDiscovery(featurePackDto.id, appId, "job1", inputs)

        then: "Job is in Discovered state"
        assertJobInState(jobId, StatusEnum.DISCOVERED)

        and: "Reconcile filter1"
        Map reconcileInputs = [reconcileUrl: "${wireMock.baseUrl()}/fp-23/reconcile".toString()]
        reconcileTestSteps.startReconcile(jobId, reconcileInputs, ["filter1"])

        then: "Job in state Reconcile Failed"
        assertJobInState(jobId, StatusEnum.RECONCILE_FAILED)
        String reconcileObjectsResponse = discoveryTestSteps.getDiscoveredObjects(jobId)
        String expReconiledObjectsResponse = readClasspathResource("/feature-packs/fp-23/responses/discovered_objects_filter1_error.json")
        assertEquals(expReconiledObjectsResponse, reconcileObjectsResponse, JSONCompareMode.LENIENT)

        and: "Replace reconcile request mapping so it succeeds"
        WiremockUtil.stubForPost("/fp-23/reconcile/[0-9]+", "/feature-packs/fp-23/responses/reconcile.json")

        and: "Reconcile filter1 again"
        reconcileTestSteps.startReconcile(jobId, reconcileInputs, ["filter1"])

        then: "Job in state Partially Completed"
        assertJobInState(jobId, StatusEnum.PARTIALLY_RECONCILED)
        String reconcileObjectsResponse2 = discoveryTestSteps.getDiscoveredObjects(jobId)
        String expReconciledObjectsResponse2 = readClasspathResource("/feature-packs/fp-23/responses/discovered_objects_filter1.json")
        assertEquals(expReconciledObjectsResponse2, reconcileObjectsResponse2, JSONCompareMode.LENIENT)

        and: "Replace reconcile request mapping so it fails"
        WiremockUtil.stubForPost("/fp-23/reconcile/[0-9]+", 500, "Test Error!!!")

        and: "Reconcile filter2"
        reconcileTestSteps.startReconcile(jobId, reconcileInputs, ["filter2"])

        then: "Job remains in state Partially Reconciled"
        assertJobInState(jobId, StatusEnum.PARTIALLY_RECONCILED)
        String reconcileObjectsResponse3 = discoveryTestSteps.getDiscoveredObjects(jobId)
        String expReconciledObjectsResponse3 = readClasspathResource("/feature-packs/fp-23/responses/discovered_objects_filter2_error.json")
        assertEquals(expReconciledObjectsResponse3, reconcileObjectsResponse3, JSONCompareMode.LENIENT)

        and: "Replace reconcile request mapping so it succeeds"
        WiremockUtil.stubForPost("/fp-23/reconcile/[0-9]+", "/feature-packs/fp-23/responses/reconcile.json")

        and: "Reconcile filter2 again"
        reconcileTestSteps.startReconcile(jobId, reconcileInputs, ["filter2"])

        then: "Job in state Completed"
        assertJobInState(jobId, StatusEnum.COMPLETED)
        String reconcileObjectsResponse4 = discoveryTestSteps.getDiscoveredObjects(jobId)
        String expReconciledObjectsResponse4 = readClasspathResource("/feature-packs/fp-23/responses/discovered_objects_filter2.json")
        assertEquals(expReconciledObjectsResponse4, reconcileObjectsResponse4, JSONCompareMode.LENIENT)
    }
}