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

package com.ericsson.bos.dr.tests.integration.utils

import static com.ericsson.bos.dr.tests.integration.utils.IOUtils.readClasspathResource
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse
import static com.github.tomakehurst.wiremock.client.WireMock.get
import static com.github.tomakehurst.wiremock.client.WireMock.post
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.hubspot.jinjava.Jinjava

class WiremockUtil {

    static Jinjava jinjava = new Jinjava();

    static stubForGet(String url, String body) {
        stubForGet(url, 200, body, [:])
    }

    static stubForGet(String url, int status, String body) {
        stubForGet(url, status, body, [:])
    }

    static stubForGet(String url, int status, String body, Map bodySubstitutionContext) {
        stubFor(get(urlMatching(url))
                .willReturn(aResponse()
                        .withStatus(status)
                        .withBody(substitute(processResponseBody(body), bodySubstitutionContext))))
    }

    static stubForGet(String url, String body, Map bodySubstitutionContext) {
        stubForGet(url, 200, body, bodySubstitutionContext)
    }

    static stubForPost(String url, String requestBody, String responseBody) {
        stubForPost(url, 200, requestBody, responseBody)
    }

    static stubForPost(String url, String responseBody) {
        stubForPost(url, 200, responseBody)
    }

    static stubForPost(String url, int status, String responseBody) {
        stubFor(post(urlMatching(url))
                .willReturn(aResponse()
                        .withStatus(status)
                        .withBody(processResponseBody(responseBody))))
    }

    static stubForPost(String url, String responseBody, String scenario, final String inState, final String toState) {
        stubFor(post(urlMatching(url)).inScenario(scenario)
                .whenScenarioStateIs(inState)
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(processResponseBody(responseBody)))
                .willSetStateTo(toState))
    }

    static stubForPost(String url, int status, String requestBody, String responseBody) {
        stubFor(post(urlMatching(url))
                .withRequestBody(equalToJson(requestBody))
                .willReturn(aResponse()
                        .withStatus(status)
                        .withBody(processResponseBody(responseBody))))
    }

    static String processResponseBody(String body) {
        return body.startsWith("/") ? readClasspathResource(body) : body
    }

    static String substitute(String template, Map substitutionContext) {
        return jinjava.renderForResult(template, substitutionContext).getOutput()
    }
}