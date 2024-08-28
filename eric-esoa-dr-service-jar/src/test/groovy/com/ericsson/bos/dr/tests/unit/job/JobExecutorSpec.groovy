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

import com.ericsson.bos.dr.jpa.JobRepository
import com.ericsson.bos.dr.jpa.model.JobEntity
import com.ericsson.bos.dr.service.DiscoveryService
import com.ericsson.bos.dr.service.JobService
import com.ericsson.bos.dr.service.ReconcileService
import com.ericsson.bos.dr.service.job.ExecutableJobConfiguration
import com.ericsson.bos.dr.service.job.JobExecutor
import com.ericsson.bos.dr.service.utils.SpringContextHolder
import com.ericsson.bos.dr.web.v1.api.model.JobSummaryDto
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.task.TaskExecutorBuilder
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@ContextConfiguration(classes = [SpringContextHolder, ExecutableJobConfiguration, JobExecutor])
class JobExecutorSpec extends Specification {

    @SpringBean
    private JobService jobService = Mock()

    @SpringBean
    private DiscoveryService discoveryService = Mock()

    @SpringBean
    private ReconcileService reconcileService = Mock()

    @SpringBean
    private JobRepository jobRepository = Mock()

    @SpringBean
    @Qualifier("jobsExecutor")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor = new TaskExecutorBuilder()
            .corePoolSize(1).maxPoolSize(1).queueCapacity(0).build()

    @Autowired
    private JobExecutor jobExecutor

    def setup() {
        threadPoolTaskExecutor.initialize()
    }

    def cleanup() {
        threadPoolTaskExecutor.shutdown()
    }

    def "Job is unlocked after task rejection error"() {

        setup: "2 JobEntity in state NEW"
        JobEntity jobEntity1 = new JobEntity(id: 1l, jobStatus: JobSummaryDto.StatusEnum.NEW)
        JobEntity jobEntity2 = new JobEntity(id: 2l, jobStatus: JobSummaryDto.StatusEnum.NEW)
        CountDownLatch countDownLatch = new CountDownLatch(2)

        when: "Execute job"
        jobExecutor.execute([jobEntity1, jobEntity2])
        countDownLatch.await(1, TimeUnit.SECONDS)

        then: "Job1 is executed"
        1 * discoveryService.executeDiscovery(jobEntity1) >> { Thread.sleep(200) }

        and: "Job2 task rejected and then unlocked"
        0 * discoveryService.executeDiscovery(jobEntity2)
        1 * jobService.unlockJob(jobEntity2.id) >> { countDownLatch.countDown() }
    }
}