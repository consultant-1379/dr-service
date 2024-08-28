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


import com.ericsson.bos.dr.jpa.model.JobEntity;
import com.ericsson.bos.dr.web.v1.api.model.ExecuteJobDto;

/**
 * Map <code>JobEntity</code> to <code>ExecuteJobDto</code>.
 */
public class ExecuteJobDtoMapper implements Mapper<JobEntity, ExecuteJobDto> {

    @Override
    public ExecuteJobDto apply(final JobEntity jobEntity) {
        if (jobEntity == null) {
            return null;
        }

        final ExecuteJobDto executeJobDto = new ExecuteJobDto();

        executeJobDto.setName(jobEntity.getName());
        executeJobDto.setDescription(jobEntity.getDescription());
        executeJobDto.setFeaturePackId(String.valueOf(jobEntity.getFeaturePackId()));
        executeJobDto.setFeaturePackName(jobEntity.getFeaturePackName());
        executeJobDto.setApplicationId(String.valueOf(jobEntity.getApplicationId()));
        executeJobDto.setApplicationName(jobEntity.getApplicationName());
        executeJobDto.setApplicationJobName(jobEntity.getApplicationJobName());
        executeJobDto.setInputs(jobEntity.getInputs());
        executeJobDto.setExecutionOptions(jobEntity.getExecutionOptions());

        return executeJobDto;
    }
}
