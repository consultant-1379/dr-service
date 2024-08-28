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

import static com.ericsson.bos.dr.service.exceptions.ErrorCode.FP_JOB_INPROGRESS
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.FP_NOT_FOUND
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.INVALID_FILTER_PARAM
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.INVALID_SORTING_PARAM
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.SCHEMA_ERROR
import static com.ericsson.bos.dr.tests.integration.asserts.JsonAssertComparators.FP_COMPARATOR
import static com.ericsson.bos.dr.tests.integration.asserts.JsonAssertComparators.FP_LIST_COMPARATOR
import static com.ericsson.bos.dr.tests.integration.utils.IOUtils.readClasspathResource
import static com.ericsson.bos.dr.tests.integration.utils.IOUtils.readZipEntries
import static com.ericsson.bos.dr.tests.integration.utils.JsonUtils.toJsonString
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

import java.util.stream.Collectors

import org.springframework.test.web.servlet.ResultActions

import com.ericsson.bos.dr.jpa.model.JobEntity
import com.ericsson.bos.dr.service.exceptions.ErrorCode
import com.ericsson.bos.dr.service.utils.YAML
import com.ericsson.bos.dr.web.v1.api.model.FeaturePackDto
import com.ericsson.bos.dr.web.v1.api.model.FeaturePackListDto
import com.ericsson.bos.dr.web.v1.api.model.FeaturePackSummaryDto
import com.ericsson.bos.dr.web.v1.api.model.InputConfigurationDto

import spock.lang.Unroll

class FeaturePackServiceSpec extends BaseSpec {

    def "Upload a feature pack is successful"() {

        setup: "Setup"
        String featurePackName = "fp-1"
        String expectedResponse = readClasspathResource("/feature-packs/fp-1/responses/expected_fp_response.json")
                .replace("%NAME%", featurePackName)

        when: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack("/feature-packs/fp-1", featurePackName)

        then: "Response is as expected"
        assertEquals(expectedResponse, toJsonString(featurePackDto), FP_COMPARATOR)

        and: "Feature pack is persisted"
        FeaturePackDto uploadedFeaturePack = featurePackTestSteps.getFeaturePack(featurePackDto.id)
        uploadedFeaturePack.name == featurePackName
        uploadedFeaturePack.applications.size() == 1
        uploadedFeaturePack.inputs.size() == 1
        uploadedFeaturePack.listeners.size() == 1
        uploadedFeaturePack.assets.size() == 1
        uploadedFeaturePack.properties.size() == 3
    }

    @Unroll
    def "Upload a feature pack returns 403 when user is not authorized"() {
        // Only required until BAM RBAC proxy provides more fine grained control
        setup: "Upload feature pack"
        String featurePackName = "fp-1"
        featurePackTestSteps.uploadFeaturePack("/feature-packs/fp-1", featurePackName)

        when: "Upload feature pack with same name"
        ResultActions result = featurePackTestSteps.uploadFeaturePackResultToken("/feature-packs/fp-1", featurePackName, accessToken)

        then: "Response is conflict"
        result.andExpect(status().is(httpResponse))

        where:
        accessToken | httpResponse
        readClasspathResource("/tokens/writer_token.json") | 403
        readClasspathResource("/tokens/bad_token.json") | 403
        "Bearer " | 403
    }

    def "Upload a feature pack returns 409"() {
        setup: "Upload feature pack"
        String featurePackName = "fp-1"
        featurePackTestSteps.uploadFeaturePack("/feature-packs/fp-1", featurePackName)

        when: "Upload feature pack with same name"
        ResultActions result = featurePackTestSteps.uploadFeaturePackResult("/feature-packs/fp-1", featurePackName)

        then: "Response is conflict"
        result.andExpect(status().is(409))
                .andExpect(jsonPath("\$.errorCode").value(ErrorCode.FP_ALREADY_EXISTS.errorCode))
    }

    def "Upload a feature pack returns 400 when validation error"() {

        when: "Upload feature pack"
        ResultActions result = featurePackTestSteps.uploadFeaturePackResult(
                "/feature-packs/invalid/${featurePack}", "fp-invalid")

        then: "Response is bad parameter"
        result.andExpect(status().is(400))
                .andExpect(jsonPath("\$.errorCode").value(error.errorCode))

        where:
        featurePack | error
        "fp-multiple-properties-config" | ErrorCode.MULTIPLE_PROPERTIES_CONFIG
        "fp-no-appconfig" | ErrorCode.MISSING_APP_CONFIG
        "fp-duplicate-app-name" | ErrorCode.DUPLICATE_CONFIG_NAME
        "fp-duplicate-listener-name" | ErrorCode.DUPLICATE_CONFIG_NAME
        "fp-duplicate-input-name" | ErrorCode.DUPLICATE_CONFIG_NAME
        "fp-invalid-app-yaml" | ErrorCode.SCHEMA_ERROR
        "fp-invalid-app-schema" | ErrorCode.SCHEMA_ERROR
        "fp-invalid-app-schema2" | ErrorCode.SCHEMA_ERROR
        "fp-invalid-app-schema3" | ErrorCode.SCHEMA_ERROR
        "fp-invalid-app-schema4" | ErrorCode.SCHEMA_ERROR
        "fp-invalid-app-schema5" | ErrorCode.SCHEMA_ERROR
        "fp-invalid-app-schema6" | ErrorCode.SCHEMA_ERROR
        "fp-invalid-app-schema7" | ErrorCode.SCHEMA_ERROR
        "fp-invalid-app-schema8" | ErrorCode.SCHEMA_ERROR
        "fp-invalid-app-schema9" | ErrorCode.SCHEMA_ERROR
        "fp-invalid-app-schema10" | ErrorCode.SCHEMA_ERROR
        "fp-invalid-app-schema11" | ErrorCode.SCHEMA_ERROR
        "fp-invalid-app-schema12" | ErrorCode.SCHEMA_ERROR
        "fp-invalid-app-schema13" | ErrorCode.SCHEMA_ERROR
        "fp-invalid-input-schema" | ErrorCode.SCHEMA_ERROR
        "fp-invalid-listener-schema" | ErrorCode.SCHEMA_ERROR
        "fp-invalid-properties-schema" | ErrorCode.SCHEMA_ERROR
    }

    def "Upload a feature pack returns 400 when file is not a zip archive"() {

        when: "Upload feature pack which is not a valid zip archive"
        byte[] archive = "this is not an archive"
        ResultActions result = featurePackTestSteps.uploadFeaturePackResult(
                "fp-invalid", archive)

        then: "Response is bad parameter"
        result.andExpect(status().is(400))
                .andExpect(jsonPath("\$.errorCode").value(ErrorCode.EMPTY_FP_ARCHIVE.errorCode))
    }

    def "Get feature pack is successful"() {
        setup: "Upload feature pack"
        String featurePackName = "fp-1"
        String expectedResponse = readClasspathResource("/feature-packs/fp-1/responses/expected_fp_response.json")
                .replace("%NAME%", featurePackName)
        FeaturePackDto uploadedFeaturePackDto =
                featurePackTestSteps.uploadFeaturePack("/feature-packs/fp-1", featurePackName)

        when: "Get feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.getFeaturePack(uploadedFeaturePackDto.id)

        then: "Response is as expected"
        assertEquals(expectedResponse, toJsonString(featurePackDto), FP_COMPARATOR)
    }

    def "Get feature pack returns 404"() {

        when: "Get feature pack which does not exist"
        ResultActions result = featurePackTestSteps.getFeaturePackResult("1000")

        then: "Response is not found"
        result.andExpect(status().is(404))
                .andExpect(jsonPath("\$.errorCode").value(FP_NOT_FOUND.errorCode))
    }

    def "Get feature packs is successful"() {
        setup: "Upload 3 feature packs"
        String expectedResponse = readClasspathResource("/feature-packs/fp-1/responses/expected_fp_3_response.json")
        featurePackTestSteps.uploadFeaturePack("/feature-packs/fp-1", "fp-1")
        featurePackTestSteps.uploadFeaturePack("/feature-packs/fp-1", "fp-2")
        featurePackTestSteps.uploadFeaturePack("/feature-packs/fp-1", "fp-3")

        when: "Get feature packs"
        FeaturePackListDto featurePackListDto = featurePackTestSteps.getFeaturePacks()

        then: "Response is as expected"
        assertEquals(expectedResponse, toJsonString(featurePackListDto), FP_LIST_COMPARATOR)
    }

    @Unroll
    def "Get feature packs with pagination is successful"() {

        setup: "Upload 3 feature packs"
        featurePackTestSteps.uploadFeaturePack("/feature-packs/fp-1", "fp-1")
        featurePackTestSteps.uploadFeaturePack("/feature-packs/fp-1", "fp-2")
        featurePackTestSteps.uploadFeaturePack("/feature-packs/fp-1", "fp-3")

        when: "Get feature packs with pageRequest"
        FeaturePackListDto featurePackListDto = featurePackTestSteps.getFeaturePacksWithPagination(pageRequest)
        List<FeaturePackSummaryDto> featurePacks = featurePackListDto.getItems()
        List<String> featurePackNames = featurePacks.stream().map(FeaturePackSummaryDto::getName).collect(Collectors.toList())

        then: "Response is as expected"
        featurePackListDto.getTotalCount() == 3
        featurePackNames == expectedResults

        where:
        pageRequest                          | expectedResults
        ""                                   | ["fp-1", "fp-2", "fp-3"]
        "?sort="                             | ["fp-1", "fp-2", "fp-3"]
        "?sort=+name"                        | ["fp-1", "fp-2", "fp-3"]
        "?sort=-name"                        | ["fp-3", "fp-2", "fp-1"]
        "?sort=+id"                          | ["fp-1", "fp-2", "fp-3"]
        "?sort=-id"                          | ["fp-3", "fp-2", "fp-1"]
        "?sort=-createdAt"                   | ["fp-3", "fp-2", "fp-1"]
        "?sort=-modifiedAt"                  | ["fp-3", "fp-2", "fp-1"]
        "?sort=name"                         | ["fp-1", "fp-2", "fp-3"]
        "?sort= name"                        | ["fp-1", "fp-2", "fp-3"]
        "?offset=0"                           | ["fp-1", "fp-2", "fp-3"]
        "?offset=1"                           | ["fp-2", "fp-3"]
        "?offset=2"                           | ["fp-3"]
        "?offset=0&limit=1"                   | ["fp-1"]
        "?offset=0&limit=2"                   | ["fp-1", "fp-2"]
        "?offset=0&limit=3"                   | ["fp-1", "fp-2", "fp-3"]
        "?offset=1&limit=1"                   | ["fp-2"]
        "?offset=1&limit=2"                   | ["fp-2", "fp-3"]
        "?offset=2&limit=1"                   | ["fp-3"]
        "?offset=invalidValue&limit=100"     | ["fp-1", "fp-2", "fp-3"]
        "?offset=0&limit=invalidValue"       | ["fp-1", "fp-2", "fp-3"]
    }

    @Unroll
    def "Get feature packs with invalid pagination sorting parameters should return 400"() {

        when: "Get feature packs"
        ResultActions result = featurePackTestSteps.getFeaturePacksWithPaginationResult(pageRequest)

        then: "Response is as expected"
        result.andExpect(status().is(400))
                .andExpect(jsonPath("\$.errorCode").value(INVALID_SORTING_PARAM.errorCode))

        where:
        pageRequest          | _
        "?sort=+unknownName" | _
        "?sort=%name"        | _
    }

    @Unroll
    def "Filter feature packs is successful"() {

        setup: "Upload 3 feature packs"
        featurePackTestSteps.uploadFeaturePack("/feature-packs/fp-1", "fp-1")
        featurePackTestSteps.uploadFeaturePack("/feature-packs/fp-1", "fp-2")
        featurePackTestSteps.uploadFeaturePack("/feature-packs/fp-1", "fp-3")

        when: "Get feature packs with pageRequest"
        FeaturePackListDto featurePackListDto = featurePackTestSteps.getFeaturePacksWithPagination(filters)
        List<FeaturePackSummaryDto> featurePacks = featurePackListDto.getItems()
        List<String> featurePackNames = featurePacks.stream().map(FeaturePackSummaryDto::getName).collect(Collectors.toList())

        then: "Response is as expected"
        featurePackNames == expectedResults

        where:
        filters                                                                    | expectedResults
        "?filters=name==fp-1"                                                      | ["fp-1"]
        "?filters=name==fp*"                                                       | ["fp-1", "fp-2", "fp-3"]
        "?filters=name==*-1"                                                       | ["fp-1"]
        "?filters=name==*p-*"                                                      | ["fp-1", "fp-2", "fp-3"]
        "?filters=name==unknown"                                                   | []
        "?filters=description==test feature pack"                                  | ["fp-1", "fp-2", "fp-3"]
        "?filters=description==*test*"                                             | ["fp-1", "fp-2", "fp-3"]
        "?filters=description==unknown"                                            | []
        "?filters=name==fp-1,description==test feature pack"                       | ["fp-1", "fp-2", "fp-3"]
        "?filters=name==fp-1;description==test feature pack"                       | ["fp-1"]
        "?filters=name==fp-1,description==test*"                                   | ["fp-1", "fp-2", "fp-3"]
        "?filters=(name==fp-1;description==test*),(name==fp-2;description==test*)" | ["fp-1", "fp-2"]
        "?filters=name==fp-3,((name==fp-1;description==test*),(name==fp-2))"       | ["fp-1", "fp-2", "fp-3"]
    }

    @Unroll
    def "Filter feature packs with invalid filter should return 400"() {

        when: "Get feature packs"
        ResultActions result = featurePackTestSteps.getFeaturePacksWithPaginationResult(filters)

        then: "Response is as expected"
        result.andExpect(status().is(400))
                .andExpect(jsonPath("\$.errorCode").value(INVALID_FILTER_PARAM.errorCode))

        where:
        filters              | _
        "?filters=invalid==fp-1" | _
        "?filters=name!=fp-1" | _
        "?filters=name=fp-1" | _
    }

    def "Delete feature packs is successful"() {
        setup: "Upload 2 feature packs"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack("/feature-packs/fp-1", "fp-1")
        FeaturePackDto featurePackDto2 = featurePackTestSteps.uploadFeaturePack("/feature-packs/fp-1", "fp-2")

        when: "Delete feature pack"
        featurePackTestSteps.deleteFeaturePack(featurePackDto.id)
        List<FeaturePackSummaryDto> remainingFeaturePacks = featurePackTestSteps.getFeaturePacks().items

        then: "Feature pack is deleted"
        featurePackTestSteps.getFeaturePackResult(featurePackDto.id)
                .andExpect(status().is(404))
        applicationTestSteps.getApplications(featurePackDto.id).totalCount == 0
        propertiesTestSteps.getProperties(featurePackDto.id).empty
        /**
         * Need to add InputsTestSteps.getInputs()
         */

        and: "Other feature pack still exists"
        featurePackTestSteps.getFeaturePackResult(featurePackDto2.id)
                .andExpect(status().is(200))
    }

    def "Delete feature pack returns 404"() {

        when: "Delete feature pack which does not exist"
        ResultActions result = featurePackTestSteps.deleteFeaturePackResult("1000")

        then: "Response is not found"
        result.andExpect(status().is(404))
                .andExpect(jsonPath("\$.errorCode").value(FP_NOT_FOUND.errorCode))
    }

    def "Delete feature pack returns 409 when job is InProgress"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack("/feature-packs/fp-1", "fp-1")

        and: "create job associated with the feature pack"
        JobEntity jobEntity1 = jobTestSteps.createSampleJobEntity(featurePackDto, jobStatus)
        jobRepository.save(jobEntity1)

        when: "Delete feature pack which does not exist"
        ResultActions result = featurePackTestSteps.deleteFeaturePackResult(featurePackDto.id)

        then: "Response is conflict"
        result.andExpect(status().is(409))
                .andExpect(jsonPath("\$.errorCode").value(FP_JOB_INPROGRESS.errorCode))

        where:
        jobStatus | _
        "DISCOVERY_INPROGRESS" | _
        "RECONCILE_INPROGRESS" | _
    }


    def "Replace feature pack is successful"() {
        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack("/feature-packs/fp-1", "fp-1")

        when: "Replace feature pack"
        FeaturePackDto replacedFeaturePackDto =
                featurePackTestSteps.replaceFeaturePack("/feature-packs/fp-1", featurePackDto.id)

        then: "Original feature pack is deleted"
        featurePackTestSteps.getFeaturePackResult(featurePackDto.id)
                .andExpect(status().is(404))

        and: "Replacement feature pack exist"
        featurePackTestSteps.getFeaturePackResult(replacedFeaturePackDto.id)
                .andExpect(status().is(200))
    }

    def "Replace feature pack returns 404"() {
        when: "Replace feature pack which does not exist"
        ResultActions result = featurePackTestSteps.replaceFeaturePackResult("/feature-packs/fp-1", "1000")

        then: "Response is not found"
        result.andExpect(status().is(404))
                .andExpect(jsonPath("\$.errorCode").value(FP_NOT_FOUND.errorCode))
    }

    def "Replace a feature pack returns 400"() {
        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack("/feature-packs/fp-1", "fp-1")

        when: "Replace feature pack"
        ResultActions result = featurePackTestSteps.replaceFeaturePackResult("/feature-packs/invalid/fp-invalid-app-schema", featurePackDto.id)

        then: "Response is 400 schema error"
        result.andExpect(status().is(400))
                .andExpect(jsonPath("\$.errorCode").value(SCHEMA_ERROR.errorCode))
    }

    def "Replace feature pack returns 409 when job is InProgress"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack("/feature-packs/fp-1", "fp-1")

        and: "create job associated with the feature pack"
        JobEntity jobEntity1 = jobTestSteps.createSampleJobEntity(featurePackDto, jobStatus)
        jobRepository.save(jobEntity1)

        when: "Replace feature pack"
        ResultActions result = featurePackTestSteps.replaceFeaturePackResult("/feature-packs/fp-1", featurePackDto.id)

        then: "Response is conflict"
        result.andExpect(status().is(409))
                .andExpect(jsonPath("\$.errorCode").value(FP_JOB_INPROGRESS.errorCode))

        where:
        jobStatus | _
        "DISCOVERY_INPROGRESS" | _
        "RECONCILE_INPROGRESS" | _
    }

    def "Original feature pack still exists when replace fails due to validation error"() {
        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack("/feature-packs/fp-1", "fp-1")

        when: "Replace using invalid feature pack"
        ResultActions result =
                featurePackTestSteps.replaceFeaturePackResult("/feature-packs/invalid/fp-invalid-app-schema", featurePackDto.id)

        then: "Response is 400 schema error"
        result.andExpect(status().is(400))
                .andExpect(jsonPath("\$.errorCode").value(SCHEMA_ERROR.errorCode))

        and: "Original feature pack still exists"
        featurePackTestSteps.getFeaturePackResult(featurePackDto.id)
                .andExpect(status().is(200))
    }

    def "Download feature pack is successful"() {
        setup: "Upload feature pack"
        String uploadedZipFilePath = System.getProperty("java.io.tmpdir") + "/" + "fp-1.zip"
        String downloadedZipFilePath = System.getProperty("java.io.tmpdir") + "/" + "test.zip"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack("/feature-packs/fp-1", "fp-1")

        when: "Download feature pack"
        byte[] archive =
                featurePackTestSteps.downloadFeaturePack(featurePackDto.id)
        try(OutputStream outputStream = new FileOutputStream(downloadedZipFilePath)) {
            outputStream.write(archive)
            outputStream.close()
        }

        then: "Downloaded feature pack contents are identical to uploaded archive"
        readZipEntries(downloadedZipFilePath) == readZipEntries(uploadedZipFilePath)
    }

    def "Download feature pack with additional input configuration is successful"() {
        setup: "Upload feature pack"
        String downloadedZipFilePath = System.getProperty("java.io.tmpdir") + "/" + "test.zip"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack("/feature-packs/fp-1", "fp-1")

        and: "Create input configuration"
        InputConfigurationDto inputConfigDto = inputTestSteps.createInputConfigRequest("newInput")
        inputTestSteps.createInputConfig(inputConfigDto, featurePackDto.id)

        when: "Download feature pack"
        byte[] archive =
                featurePackTestSteps.downloadFeaturePack(featurePackDto.id)
        try (OutputStream outputStream = new FileOutputStream(downloadedZipFilePath)) {
            outputStream.write(archive)
            outputStream.close()
        }
        Map<String, String> entries = readZipEntries(downloadedZipFilePath)

        then: "Downloaded feature pack contains new input configuration"
        entries.get("job_inputs/newInput.yml").getBytes() == YAML.write(inputConfigDto)
    }

    def "Download feature pack returns 404"() {

        when: "Download feature pack which does not exist"
        ResultActions result = featurePackTestSteps.downloadFeaturePackResult("1000")

        then: "Response is not found"
        result.andExpect(status().is(404))
                .andExpect(jsonPath("\$.errorCode").value(FP_NOT_FOUND.errorCode))
    }

    def "Can delete a feature pack with an associated job"() {
        setup: "Create job"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-1", "fp-1")
        JobEntity jobEntity = jobTestSteps.createSampleJobEntity(featurePackDto, "DISCOVERED")
        JobEntity saved = jobRepository.save(jobEntity)

        when: "delete feature pack"
        featurePackTestSteps.deleteFeaturePack(featurePackDto.id)

        then: "feature pack is deleted"
        featurePackTestSteps.getFeaturePackResult(saved.id.toString())
                .andExpect(status().is(404))
    }
}
