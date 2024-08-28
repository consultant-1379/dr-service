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
 * Provides alarm resource for failed jobs.
 */
public class JobAlarmResource implements Supplier<String> {

    private static final String FAULTY_RESOURCE = "/discovery-and-reconciliation/v1/jobs/%s[name=%s]";

    private final JobEntity jobEntity;

    /**
     * Constructor.
     *
     * @param jobEntity
     *         jobEntity
     */
    public JobAlarmResource(final JobEntity jobEntity) {
        this.jobEntity = jobEntity;
    }

    @Override
    public String get() {
        return String.format(FAULTY_RESOURCE, jobEntity.getId(), jobEntity.getName());
    }

}
