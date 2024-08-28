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
package com.ericsson.bos.dr.model.mappers;

import java.util.Optional;

import com.ericsson.bos.dr.jpa.model.JobEntity;
import com.ericsson.bos.dr.web.v1.api.model.JobSummaryDto;
import org.springframework.stereotype.Component;

/**
 * Map <code>JobEntity</code> to <code>JobSummaryDto</code>.
 */
@Component
public class JobSummaryMapper implements Mapper<JobEntity, JobSummaryDto> {


    @Override
    public JobSummaryDto apply(JobEntity jobEntity) {

        if (jobEntity == null) {
            return null;
        }
        final var jobSummaryDto = new JobSummaryDto();

        Optional.ofNullable(jobEntity.getStartDate())
                .ifPresent(date -> jobSummaryDto.setStartDate(date.toInstant().toString()));
        Optional.ofNullable(jobEntity.getCompletedDate())
                .ifPresent(date -> jobSummaryDto.setCompletedDate(date.toInstant().toString()));
        jobSummaryDto.setStatus(JobSummaryDto.StatusEnum.fromValue(jobEntity.getJobStatus()));
        jobSummaryDto.setId(String.valueOf(jobEntity.getId()));
        jobSummaryDto.setName(jobEntity.getName());
        jobSummaryDto.setDescription(jobEntity.getDescription());
        jobSummaryDto.setFeaturePackId(String.valueOf(jobEntity.getFeaturePackId()));
        jobSummaryDto.setFeaturePackName(jobEntity.getFeaturePackName());
        jobSummaryDto.setApplicationId(String.valueOf(jobEntity.getApplicationId()));
        jobSummaryDto.setApplicationName(jobEntity.getApplicationName());
        jobSummaryDto.setApplicationJobName(jobEntity.getApplicationJobName());
        Optional.ofNullable(jobEntity.getJobScheduleId()).ifPresent(id -> jobSummaryDto.setJobScheduleId(id.toString()));

        return jobSummaryDto;
    }
}