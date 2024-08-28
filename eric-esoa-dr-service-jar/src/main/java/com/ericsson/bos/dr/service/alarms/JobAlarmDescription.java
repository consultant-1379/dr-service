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

package com.ericsson.bos.dr.service.alarms;

import java.util.function.Supplier;

import com.ericsson.bos.dr.jpa.model.JobEntity;

/**
 * Provides alarm description for failed jobs.
 */
public class JobAlarmDescription implements Supplier<String> {

    private static final String DESCRIPTION =
            "%s Please refer to the job resource in the D&R UI or NBI for details on the specific reason for the failure.";
    private final JobEntity jobEntity;

    /**
     * Constructor.
     *
     * @param jobEntity
     *         jobEntity
     */
    public JobAlarmDescription(final JobEntity jobEntity) {
        this.jobEntity = jobEntity;
    }

    @Override
    public String get() {
        if (jobEntity.isJobInitiatedByMessage()) {
            return String.format(DESCRIPTION, "A D&R job triggered by a message subscription is in a failed state.");
        } else if (jobEntity.isJobInitiatedBySchedule()) {
            return String.format(DESCRIPTION, "A scheduled D&R job is in a failed state.");
        } else {
            throw new IllegalArgumentException("Job alarm description not found for job with id: " + jobEntity.getId());
        }
    }
}
