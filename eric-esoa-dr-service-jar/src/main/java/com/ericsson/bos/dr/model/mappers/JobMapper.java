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
import com.ericsson.bos.dr.web.v1.api.model.JobDto;
import com.ericsson.bos.dr.web.v1.api.model.JobSummaryDto;
import org.springframework.stereotype.Component;

/**
 * Map <code>JobEntity</code> to <code>JobDto</code>.
 */
@Component
public class JobMapper implements Mapper<JobEntity, JobDto> {


    @Override
    public JobDto apply(JobEntity jobEntity) {
        if (jobEntity == null) {
            return null;
        }
        final var jobDto = new JobDto();

        jobDto.setId(String.valueOf(jobEntity.getId()));
        jobDto.setName(jobEntity.getName());
        jobDto.setDescription(jobEntity.getDescription());
        jobDto.setFeaturePackId(String.valueOf(jobEntity.getFeaturePackId()));
        jobDto.setFeaturePackName(jobEntity.getFeaturePackName());
        jobDto.setApplicationId(String.valueOf(jobEntity.getApplicationId()));
        jobDto.setApplicationName(jobEntity.getApplicationName());
        jobDto.setApplicationJobName(jobEntity.getApplicationJobName());
        Optional.ofNullable(jobEntity.getStartDate()).ifPresent(date -> jobDto.setStartDate(date.toInstant().toString()));
        Optional.ofNullable(jobEntity.getCompletedDate())
                .ifPresent(date -> jobDto.setCompletedDate(date.toInstant().toString()));
        jobDto.setStatus(JobSummaryDto.StatusEnum.valueOf(jobEntity.getJobStatus().toUpperCase()));
        jobDto.setInputs(jobEntity.getInputs());
        jobDto.setDiscoveredObjectsCount(jobEntity.getDiscoveredObjectsCount());
        jobDto.setReconciledObjectsCount(jobEntity.getReconciledObjectsCount());
        jobDto.setReconciledObjectsErrorCount(jobEntity.getReconciledObjectsErrorCount());
        jobDto.setErrorMessage(jobEntity.getErrorMessage());
        Optional.ofNullable(jobEntity.getJobScheduleId()).ifPresent(id -> jobDto.setJobScheduleId(id.toString()));
        return jobDto;
    }
}
