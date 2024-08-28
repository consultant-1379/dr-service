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
import com.ericsson.bos.dr.web.v1.api.model.JobScheduleSummaryDto;

/**
 * Map <code>JobScheduleEntity</code> to <code>JobScheduleSummaryDto</code>.
 */
public class JobScheduleSummaryMapper implements Mapper<JobScheduleEntity, JobScheduleSummaryDto> {

    private final JobSpecificationMapper jobSpecificationMapper = new JobSpecificationMapper();

    @Override
    public JobScheduleSummaryDto apply(JobScheduleEntity jobScheduleEntity) {
        final var jobScheduleSummaryDto = new JobScheduleSummaryDto();
        jobScheduleSummaryDto.setId(String.valueOf(jobScheduleEntity.getId()));
        jobScheduleSummaryDto.setName(jobScheduleEntity.getName());
        jobScheduleSummaryDto.setDescription(jobScheduleEntity.getDescription());
        jobScheduleSummaryDto.setExpression(jobScheduleEntity.getExpression());
        jobScheduleSummaryDto.setCreatedAt(jobScheduleEntity.getCreationDate().toInstant().toString());
        jobScheduleSummaryDto.setEnabled(jobScheduleEntity.isEnabled());
        jobScheduleSummaryDto.setJobSpecification(jobSpecificationMapper.apply(jobScheduleEntity));
        return jobScheduleSummaryDto;
    }
}