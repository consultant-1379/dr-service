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
import com.ericsson.bos.dr.jpa.model.JobSpecificationEntity;
import com.ericsson.bos.dr.web.v1.api.model.ExecuteJobDto;
import com.ericsson.bos.dr.web.v1.api.model.JobSummaryDto.StatusEnum;

/**
 * Map <code>ExecuteJobDto</code> to <code>JobEntity</code>.
 */
public class JobEntityMapper implements Mapper<ExecuteJobDto, JobEntity> {

    private Long messageSubscriptionId;

    /**
     * Default constructor.
     */
    public JobEntityMapper() {
    }

    /**
     * Constructor to provide messageSubscriptionId.
     *
     * @param messageSubscriptionId
     *         messageSubscriptionId
     */
    public JobEntityMapper(final Long messageSubscriptionId) {
        this.messageSubscriptionId = messageSubscriptionId;
    }


    @Override
    public JobEntity apply(ExecuteJobDto discoveryJob) {
        final var jobEntity = new JobEntity();
        final var jobSpec = new JobSpecificationEntity();
        jobSpec.setApplicationId(Long.valueOf(discoveryJob.getApplicationId()));
        jobSpec.setApplicationName(discoveryJob.getApplicationName());
        jobSpec.setApplicationJobName(discoveryJob.getApplicationJobName());
        jobSpec.setDescription(discoveryJob.getDescription());
        jobSpec.setFeaturePackId(Long.valueOf(discoveryJob.getFeaturePackId()));
        jobSpec.setFeaturePackName(discoveryJob.getFeaturePackName());
        jobSpec.setInputs(discoveryJob.getInputs());
        jobSpec.setName(discoveryJob.getName());
        jobSpec.setExecutionOptions(discoveryJob.getExecutionOptions());
        jobEntity.setJobSpecification(jobSpec);
        jobEntity.setJobStatus(StatusEnum.NEW.toString());
        Optional.ofNullable(messageSubscriptionId)
                .ifPresent(jobEntity::setMessageSubscriptionId);
        return jobEntity;
    }
}