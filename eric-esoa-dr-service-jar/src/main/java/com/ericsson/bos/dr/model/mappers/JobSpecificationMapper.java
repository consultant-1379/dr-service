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

import com.ericsson.bos.dr.jpa.model.JobScheduleEntity;
import com.ericsson.bos.dr.jpa.model.JobSpecificationEntity;
import com.ericsson.bos.dr.web.v1.api.model.ExecuteJobDto;

/**
 * Map <code>JobScheduleEntity</code> to <code>ExecuteJobDto</code>.
 */
public class JobSpecificationMapper implements Mapper<JobScheduleEntity, ExecuteJobDto> {

    @Override
    public ExecuteJobDto apply(JobScheduleEntity jobScheduleEntity) {
        final JobSpecificationEntity jobSpecificationEntity = jobScheduleEntity.getJobSpecification();
        final ExecuteJobDto jobSpecification = new ExecuteJobDto();
        jobSpecification.setName(jobSpecificationEntity.getName());
        jobSpecification.setFeaturePackId(jobSpecificationEntity.getFeaturePackId().toString());
        jobSpecification.setFeaturePackName(jobSpecificationEntity.getFeaturePackName());
        jobSpecification.setApplicationId(jobSpecificationEntity.getApplicationId().toString());
        jobSpecification.setApplicationName(jobSpecificationEntity.getApplicationName());
        jobSpecification.setApplicationJobName(jobSpecificationEntity.getApplicationJobName());
        jobSpecification.setExecutionOptions(jobSpecificationEntity.getExecutionOptions());
        jobSpecification.setInputs(jobSpecificationEntity.getInputs());
        jobSpecification.setDescription(jobSpecificationEntity.getDescription());
        return jobSpecification;
    }
}