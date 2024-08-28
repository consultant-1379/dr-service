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

import com.ericsson.bos.dr.service.DiscoveryService;
import com.ericsson.bos.dr.service.JobService;
import com.ericsson.bos.dr.service.ReconcileService;
import com.ericsson.bos.dr.web.v1.api.JobsApi;
import com.ericsson.bos.dr.web.v1.api.model.DeleteJobsResponseDto;
import com.ericsson.bos.dr.web.v1.api.model.DiscoveredObjectListDto;
import com.ericsson.bos.dr.web.v1.api.model.ExecuteJobDto;
import com.ericsson.bos.dr.web.v1.api.model.ExecuteJobResponseDto;
import com.ericsson.bos.dr.web.v1.api.model.ExecuteReconcileDto;
import com.ericsson.bos.dr.web.v1.api.model.JobDto;
import com.ericsson.bos.dr.web.v1.api.model.JobListDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Job Management Controller.
 */
@RestController
public class JobsController implements JobsApi {

    @Autowired
    private DiscoveryService discoveryService;

    @Autowired
    private JobService jobservice;

    @Autowired
    private ReconcileService reconcileService;

    @Override
    public ResponseEntity<ExecuteJobResponseDto> executeDiscoveryJob(ExecuteJobDto discoveryJob) {
        final String jobId = discoveryService.startDiscovery(discoveryJob);
        return ResponseEntity.accepted().body(new ExecuteJobResponseDto().id(jobId));
    }

    @Override
    public ResponseEntity<ExecuteJobResponseDto> duplicateDiscoveryJob(final String originalJobId) {
        final String jobId = discoveryService.startDuplicateDiscovery(originalJobId);
        return ResponseEntity.accepted().body(new ExecuteJobResponseDto().id(jobId));
    }

    @Override
    public ResponseEntity<Void> executeReconcile(String jobId, ExecuteReconcileDto executeReconcileDto) {
        reconcileService.requestReconcile(jobId, executeReconcileDto);
        return ResponseEntity.accepted().build();
    }

    @Override
    public ResponseEntity<DiscoveredObjectListDto> getDiscoveredObjects(String jobId, String offset, String limit, String sort, String filters) {
        final DiscoveredObjectListDto discoveredObjects = discoveryService.getDiscoveredObjects(jobId, offset, limit, sort, filters);
        return ResponseEntity.ok(discoveredObjects);
    }

    @Override
    public ResponseEntity<JobDto> getDiscoveryJob(String jobId) {
        return ResponseEntity.ok(jobservice.getJobById(jobId));
    }

    @Override
    public ResponseEntity<JobListDto> getDiscoveryJobs(String offset, String limit, String sort, String filters) {
        final var jobListDto = jobservice.getJobs(offset, limit, sort, filters);
        return ResponseEntity.ok(jobListDto);
    }

    @Override
    public ResponseEntity<Void> deleteJob(String jobId, Boolean force) {
        jobservice.deleteJobById(jobId, force);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<DeleteJobsResponseDto> deleteJobs(String filters, Boolean force) {
        return ResponseEntity.ok(jobservice.deleteJobs(filters, force));
    }
}