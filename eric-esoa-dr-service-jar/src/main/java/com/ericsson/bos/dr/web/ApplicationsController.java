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
package com.ericsson.bos.dr.web;

import com.ericsson.bos.dr.service.ApplicationService;
import com.ericsson.bos.dr.web.v1.api.ApplicationsApi;
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationDto;
import com.ericsson.bos.dr.web.v1.api.model.ConfigurationListDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Applications Management Controller.
 */
@RestController
public class ApplicationsController implements ApplicationsApi {

    @Autowired
    private ApplicationService applicationService;

    @Override
    public ResponseEntity<ApplicationConfigurationDto> getApplication(String featurePackId, String applicationId) {
        return ResponseEntity.ok(applicationService.getApplication(featurePackId, applicationId));
    }

    @Override
    public ResponseEntity<ConfigurationListDto> getApplications(String featurePackId) {
        return ResponseEntity.ok(applicationService.getApplications(featurePackId));
    }
}