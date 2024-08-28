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

import static com.ericsson.bos.dr.service.exceptions.ErrorCode.OPERATION_ONGOING;
import static com.ericsson.bos.dr.web.v1.api.model.JobSummaryDto.StatusEnum.DISCOVERY_INPROGRESS;
import static com.ericsson.bos.dr.web.v1.api.model.JobSummaryDto.StatusEnum.RECONCILE_INPROGRESS;
import java.util.function.BiConsumer;

import com.ericsson.bos.dr.jpa.model.JobEntity;
import com.ericsson.bos.dr.jpa.model.JobScheduleEntity;
import com.ericsson.bos.dr.service.exceptions.DRServiceException;
import com.ericsson.bos.dr.service.exceptions.ErrorCode;

/**
 * Validate if a <code>JobEntity</code> can be deleted. Throws an exception if the JobEntity cannot be deleted.
 * A job cannot be deleted in the following cases:
 * <ul>
 *     <li>Job is in state SCHEDULED.</li>
 *     <li>Job is running and force flag is false.</li>
 *     <li>Job is running, force flag is true but associated job schedule is enabled.</li>
 * </ul>
 */
public class DeleteJobValidator implements BiConsumer<JobEntity, JobScheduleEntity> {

    private final boolean force;

    /**
     * DeleteJobValidator
     * @param force force delete flag
     */
    public DeleteJobValidator(boolean force) {
        this.force = force;
    }

    @Override
    public void accept(JobEntity jobEntity, JobScheduleEntity jobScheduleEntity) {
        if ("SCHEDULED".equals(jobEntity.getJobStatus())) {
            throw new DRServiceException(ErrorCode.CANNOT_DELETE_SCHEDULED_JOB, jobEntity.getId().toString());
        }

        if (isRunningJob(jobEntity)) {
            if (!force) {
                throw new DRServiceException(OPERATION_ONGOING, jobEntity.getId().toString(), jobEntity.getJobStatus());
            }
            if (jobScheduleEntity != null && jobScheduleEntity.isEnabled()) {
                throw new DRServiceException(ErrorCode.CANNOT_DELETE_ACTIVE_SCHEDULED_JOB, jobEntity.getId().toString());
            }
        }
    }

    private boolean isRunningJob(final JobEntity jobEntity) {
        final String jobStatus = jobEntity.getJobStatus();
        return jobStatus.equals(DISCOVERY_INPROGRESS.toString()) || jobStatus.equals(RECONCILE_INPROGRESS.toString());
    }
}