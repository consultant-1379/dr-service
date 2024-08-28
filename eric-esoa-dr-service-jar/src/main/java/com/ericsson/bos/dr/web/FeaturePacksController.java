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

import java.nio.charset.StandardCharsets;

import com.ericsson.bos.dr.service.FeaturePackService;
import com.ericsson.bos.dr.service.featurepacks.FeaturePackDownloadResource;
import com.ericsson.bos.dr.web.v1.api.FeaturePacksApi;
import com.ericsson.bos.dr.web.v1.api.model.FeaturePackDto;
import com.ericsson.bos.dr.web.v1.api.model.FeaturePackListDto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Feature Pack Management Controller.
 */
@RestController
public class FeaturePacksController implements FeaturePacksApi {

    @Autowired
    private FeaturePackService featurePackService;

    @Override
    public ResponseEntity<FeaturePackDto> uploadFeaturePack(String name, MultipartFile file, String description) {
        return new ResponseEntity<>(featurePackService.createFeaturePack(name, description, file), HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<Void> deleteFeaturePack(String featurePackId) {
        featurePackService.deleteFeaturePack(featurePackId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Resource> downloadFeaturePack(String featurePackId) {
        final FeaturePackDownloadResource featurePackResource = featurePackService.downloadFeaturePack(featurePackId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition
                        .attachment()
                        .filename(featurePackResource.getFeaturePackArchiveName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(featurePackResource.getResource());
    }

    @Override
    public ResponseEntity<FeaturePackDto> getFeaturePack(String featurePackId) {
        return ResponseEntity.ok(featurePackService.getFeaturePack(featurePackId));
    }

    @Override
    public ResponseEntity<FeaturePackListDto> getFeaturePacks(String offset, String limit, String sort, String filters) {
        final var featurePackListDto = featurePackService.getFeaturePacks(offset, limit, sort, filters);
        return ResponseEntity.ok(featurePackListDto);
    }

    @Override
    public ResponseEntity<FeaturePackDto> updateFeaturePack(String featurePackId, MultipartFile file, String description) {
        final var featurePackDto = featurePackService.replaceFeaturePack(featurePackId, description, file);
        return new ResponseEntity<>(featurePackDto, HttpStatus.CREATED);
    }
}