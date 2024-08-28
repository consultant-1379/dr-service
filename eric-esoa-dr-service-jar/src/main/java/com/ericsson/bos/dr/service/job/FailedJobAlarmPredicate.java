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

package com.ericsson.bos.dr.service.job;

import java.util.function.Predicate;

import org.springframework.stereotype.Component;

import com.ericsson.bos.dr.jpa.model.JobEntity;
import com.ericsson.bos.dr.web.v1.api.model.JobSummaryDto;

/**
 * Predicate checks if an alarm should be raised based on the job status and how the job was initiated.
 */
@Component
public class FailedJobAlarmPredicate implements Predicate<JobEntity> {

    @Override
    public boolean test(final JobEntity jobEntity) {
        return (jobEntity.isJobInitiatedByMessage() || jobEntity.isJobInitiatedBySchedule()) &&
               (jobEntity.getJobStatus().equalsIgnoreCase(JobSummaryDto.StatusEnum.DISCOVERY_FAILED.getValue()) ||
                jobEntity.getJobStatus().equalsIgnoreCase(JobSummaryDto.StatusEnum.RECONCILE_FAILED.getValue()));
    }
}
