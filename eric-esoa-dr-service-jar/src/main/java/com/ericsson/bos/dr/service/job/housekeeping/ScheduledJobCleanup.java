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
package com.ericsson.bos.dr.service.job.housekeeping;

import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ericsson.bos.dr.service.JobService;

/**
 * Cleanup jobs older than the number of days configured in application.yaml.
 */
@Component
public class ScheduledJobCleanup {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledJobCleanup.class);

    @Autowired
    private JobService jobService;

    @Value("${service.jobs.cleanup-policy.maxAgeDays}")
    private int maxAgeDays;

    /**
     * Scheduled job cleanup. Delete jobs older than maxAgeDays.
     */
    @Scheduled(cron = "${service.jobs.cleanup-policy.cron-interval}")
    public void cleanupJobs() {
        final long count = jobService.deleteOldJobs(ChronoUnit.DAYS, maxAgeDays);
        LOGGER.info("{} job(s) older than {} days have been deleted", count, maxAgeDays);
    }
}
