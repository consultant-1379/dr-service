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
import com.ericsson.bos.dr.web.v1.api.model.JobScheduleDto;

/**
 * Map <code>JobScheduleEntity</code> to <code>JobScheduleDto</code>.
 */
public class JobScheduleMapper implements Mapper<JobScheduleEntity, JobScheduleDto> {

    private final JobSpecificationMapper jobSpecificationMapper = new JobSpecificationMapper();

    @Override
    public JobScheduleDto apply(JobScheduleEntity jobScheduleEntity) {
        final var jobScheduleDto = new JobScheduleDto();
        jobScheduleDto.setId(String.valueOf(jobScheduleEntity.getId()));
        jobScheduleDto.setName(jobScheduleEntity.getName());
        jobScheduleDto.setDescription(jobScheduleEntity.getDescription());
        jobScheduleDto.setExpression(jobScheduleEntity.getExpression());
        jobScheduleDto.setCreatedAt(jobScheduleEntity.getCreationDate().toInstant().toString());
        jobScheduleDto.setEnabled(jobScheduleEntity.isEnabled());
        jobScheduleDto.setJobSpecification(jobSpecificationMapper.apply(jobScheduleEntity));
        return jobScheduleDto;
    }
}