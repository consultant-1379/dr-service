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

package com.ericsson.bos.dr.tests.unit.job

import com.ericsson.bos.dr.service.JobService
import com.ericsson.bos.dr.service.job.housekeeping.ScheduledJobCleanup
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import spock.lang.Specification

import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

import static org.awaitility.Awaitility.await
import static org.mockito.Mockito.atLeast
import static org.mockito.Mockito.verify

@TestPropertySource(properties = ["service.jobs.cleanup-policy.cron-interval=* * * * * *",
        "service.jobs.cleanup-policy.maxAgeDays=1"])
@ContextConfiguration(classes = ScheduledJobCleanup)
@EnableScheduling
class ScheduledJobCleanupSpec extends Specification {

    @MockBean
    JobService jobService;

    def "Job cleanup is scheduled"() {

        expect: "Delete old jobs"
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verify(jobService, atLeast(2))
                            .deleteOldJobs(ChronoUnit.DAYS, 1);
                });
    }
}