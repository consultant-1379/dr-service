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

import com.ericsson.bos.dr.service.AssetService
import com.ericsson.bos.dr.web.v1.api.model.FeaturePackDto
import org.spockframework.spring.SpringSpy

import static org.springframework.test.util.AopTestUtils.getUltimateTargetObject

class AssetServiceSpec extends BaseSpec {

    @SpringSpy
    AssetService assetService

    def "Groovy asset is read from cache after initial fetch"() {
        setup: "Upload feature pack with groovy asset"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-8", "fp-8-${System.currentTimeMillis()}")

        when: "Get groovy asset multiple times"
        assetService.getGroovyScript("getUrl_asset.groovy", featurePackDto.id.toLong())
        assetService.getGroovyScript("getUrl_asset.groovy", featurePackDto.id.toLong())
        assetService.getGroovyScript("getUrl_asset.groovy", featurePackDto.id.toLong())

        then: "asset is only fetched from repository once"
        1 * getUltimateTargetObject(assetService).getAsset('getUrl_asset.groovy', featurePackDto.id.toLong())
    }
}