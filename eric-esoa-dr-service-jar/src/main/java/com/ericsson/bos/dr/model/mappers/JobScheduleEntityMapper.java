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
import com.ericsson.bos.dr.web.v1.api.model.CreateJobScheduleDto;

/**
 * Map <code>CreateJobScheduleDto</code> to <code>JobScheduleEntity</code>.
 */
public class JobScheduleEntityMapper implements Mapper<CreateJobScheduleDto, JobScheduleEntity> {

    private final JobSpecificationEntity jobSpecificationEntity;

    /**
     * JobScheduleEntityMapper.
     * @param jobSpecificationEntity job specification entity
     */
    public JobScheduleEntityMapper(final JobSpecificationEntity jobSpecificationEntity) {
        this.jobSpecificationEntity = jobSpecificationEntity;
    }

    @Override
    public JobScheduleEntity apply(CreateJobScheduleDto createJobScheduleDto) {
        final var jobScheduleEntity = new JobScheduleEntity();
        jobScheduleEntity.setName(createJobScheduleDto.getName());
        jobScheduleEntity.setDescription(createJobScheduleDto.getDescription());
        jobScheduleEntity.setExpression(createJobScheduleDto.getExpression());
        jobScheduleEntity.setEnabled(true);
        jobScheduleEntity.setJobSpecification(jobSpecificationEntity);
        return jobScheduleEntity;
    }
}