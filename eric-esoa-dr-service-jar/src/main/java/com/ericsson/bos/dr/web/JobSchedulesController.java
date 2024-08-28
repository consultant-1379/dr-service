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

import com.ericsson.bos.dr.service.JobScheduleService;
import com.ericsson.bos.dr.web.v1.api.JobSchedulesApi;
import com.ericsson.bos.dr.web.v1.api.model.CreateJobScheduleDto;
import com.ericsson.bos.dr.web.v1.api.model.EnableDisableJobScheduleRequest;
import com.ericsson.bos.dr.web.v1.api.model.JobScheduleDto;
import com.ericsson.bos.dr.web.v1.api.model.JobScheduleListDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JobSchedulesController implements JobSchedulesApi {

    @Autowired
    private JobScheduleService jobScheduleService;

    @Override
    public ResponseEntity<JobScheduleDto> createJobSchedule(CreateJobScheduleDto createJobScheduleDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(jobScheduleService.createJobSchedule(createJobScheduleDto));
    }

    @Override
    public ResponseEntity<Void> deleteJobSchedule(String jobScheduleId) {
        jobScheduleService.deleteJobSchedule(jobScheduleId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> enableDisableJobSchedule(String jobScheduleId,
                                                                   EnableDisableJobScheduleRequest enableDisableJobScheduleRequest) {
        jobScheduleService.enableJobSchedule(jobScheduleId, enableDisableJobScheduleRequest.getEnabled());
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<JobScheduleDto> getJobSchedule(String jobScheduleId) {
        return ResponseEntity.ok(jobScheduleService.getJobSchedule(jobScheduleId));
    }

    @Override
    public ResponseEntity<JobScheduleListDto> getJobSchedules(String offset, String limit, String sort, String filters) {
        final var jobScheduleListDto = jobScheduleService.getJobSchedules(offset, limit, sort, filters);
        return ResponseEntity.ok(jobScheduleListDto);
    }
}