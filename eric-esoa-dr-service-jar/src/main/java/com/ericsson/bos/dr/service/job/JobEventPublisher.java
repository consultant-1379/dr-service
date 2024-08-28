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

import com.ericsson.bos.dr.jpa.model.JobEntity;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * JPA Entity listener to publish job events.
 */
@Component
public class JobEventPublisher {

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    /**
     * Publish job creation and update events.
     * @param jobEntity job entity
     */
    @PostPersist
    @PostUpdate
    void publishJobCreatedEvent(JobEntity jobEntity) {
        applicationEventPublisher.publishEvent(new JobEvent(jobEntity.getId(), jobEntity.getJobStatus()));
    }

    /**
     * Job Event.
     */
    public static class JobEvent {

        private long jobId;
        private String status;

        /**
         * JobEvent.
         * @param jobId jobId
         * @param status status
         */
        public JobEvent(long jobId, String status) {
            this.jobId = jobId;
            this.status = status;
        }

        public long getJobId() {
            return jobId;
        }

        public String getStatus() {
            return status;
        }
    }
}