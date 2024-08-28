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
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 * Configure <code>ExecutableJob</code> beans.
 */
@Configuration
public class ExecutableJobConfiguration {

    /**
     * Create <code>ExecutableJob</code> for the given JobEntity.
     * @param jobEntity job entity
     * @return ExecutableJob
     */
    @Bean
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public ExecutableJob createExecutableJob(JobEntity jobEntity) {
        if ("NEW".equals(jobEntity.getJobStatus())) {
            return new DiscoveryJob(jobEntity);
        } else if ("RECONCILE_REQUESTED".equals(jobEntity.getJobStatus())) {
            return new ReconcileJob(jobEntity);
        } else if ("SCHEDULED".equals(jobEntity.getJobStatus())) {
            return new ScheduledJob(jobEntity);
        }
        throw new IllegalArgumentException("No executable job found for status " + jobEntity.getJobStatus());
    }
}