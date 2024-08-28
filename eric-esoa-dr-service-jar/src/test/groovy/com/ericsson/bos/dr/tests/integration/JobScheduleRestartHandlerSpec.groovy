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
package com.ericsson.bos.dr.tests.integration

import com.ericsson.bos.dr.jpa.model.JobEntity
import com.ericsson.bos.dr.jpa.model.JobScheduleEntity
import com.ericsson.bos.dr.jpa.model.JobSpecificationEntity
import com.ericsson.bos.dr.service.job.housekeeping.JobSchedulePodRestartHandler
import com.ericsson.bos.dr.tests.integration.utils.SpringContextUtils
import com.ericsson.bos.dr.web.v1.api.model.ExecuteJobDtoExecutionOptions
import io.fabric8.kubernetes.api.model.ContainerStateBuilder
import io.fabric8.kubernetes.api.model.PodBuilder
import io.fabric8.kubernetes.api.model.PodListBuilder
import io.fabric8.kubernetes.api.model.PodStatusBuilder
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.server.mock.KubernetesServer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import spock.lang.Shared

import java.time.Instant

import static com.ericsson.bos.dr.web.v1.api.model.JobSummaryDto.StatusEnum.COMPLETED
import static com.ericsson.bos.dr.web.v1.api.model.JobSummaryDto.StatusEnum.DISCOVERED
import static com.ericsson.bos.dr.web.v1.api.model.JobSummaryDto.StatusEnum.DISCOVERY_FAILED
import static com.ericsson.bos.dr.web.v1.api.model.JobSummaryDto.StatusEnum.RECONCILE_FAILED

class JobScheduleRestartHandlerSpec extends BaseSpec {

    JobSpecificationEntity jobSpecificationEntity = new JobSpecificationEntity(name: "job-1", description: "my job", applicationId: 1,
            applicationName: "app_1", featurePackName: "fp-1", featurePackId: 1, executionOptions: new ExecuteJobDtoExecutionOptions().autoReconcile (
            true), inputs: ["input1": 1], applicationJobName: "job-1")

    @Shared
    KubernetesServer kubernetesServer = new KubernetesServer(false)

    @Shared
    KubernetesClient kubernetesClient

    @Autowired
    JobSchedulePodRestartHandler jobSchedulePodRestartHandler

    def setupSpec() {
        kubernetesServer.before()
    }

    def cleanupSpec() {
        kubernetesServer.after()
        Optional.ofNullable(kubernetesClient).ifPresent(SpringContextUtils::destroyBean)
    }

    def setup() {
        if (kubernetesClient == null) {
            kubernetesClient = kubernetesServer.getClient()
            SpringContextUtils.registerBean("kubernetesClient", kubernetesClient)
        }
    }

    @Transactional
    def "Repair running jobs after pod restarts while scheduled jobs are inprogress"() {

        setup: "Create 2 job schedules"
        JobScheduleEntity savedJobScheduleEntity1 = jobScheduleRepository.save(createJobScheduleEntity())
        JobScheduleEntity savedJobScheduleEntity2 = jobScheduleRepository.save(createJobScheduleEntity())

        and: "Create Completed and InProgress jobs for the schedules"
        JobEntity savedJobEntity1_1= jobRepository.save(createJobEntity(savedJobScheduleEntity1, "DISCOVERY_INPROGRESS"))
        JobEntity savedJobEntity1_2= jobRepository.save(createJobEntity(savedJobScheduleEntity1, "COMPLETED"))
        JobEntity savedJobEntity2_1= jobRepository.save(createJobEntity(savedJobScheduleEntity2, "RECONCILE_INPROGRESS"))
        JobEntity savedJobEntity2_2= jobRepository.save(createJobEntity(savedJobScheduleEntity2, "DISCOVERED"))

        when: "Repair job schedules after restart"
        jobSchedulePodRestartHandler.repairJobSchedulesAfterRestart()

        then: "Running jobs are set to failed state"
        jobRepository.findById(savedJobEntity1_1.id).get().jobStatus == DISCOVERY_FAILED.toString()
        jobRepository.findById(savedJobEntity2_1.id).get().jobStatus == RECONCILE_FAILED.toString()

        and: "Next scheduled job is created"
        jobRepository.findByJobScheduleIdAndJobStatus(savedJobScheduleEntity1.id, "SCHEDULED").size() == 1
        jobRepository.findByJobScheduleIdAndJobStatus(savedJobScheduleEntity2.id, "SCHEDULED").size() == 1

        and: "Completed jobs are unchanged"
        jobRepository.findById(savedJobEntity1_2.id).get().jobStatus == COMPLETED.toString()
        jobRepository.findById(savedJobEntity2_2.id).get().jobStatus == DISCOVERED.toString()
    }


    @Transactional
    def "Running scheduled job from other pods are unchanged after pod restarts"() {

        setup: "Create job schedule with running job and lockedBy set to other pod"
        JobScheduleEntity savedJobScheduleEntity1 = jobScheduleRepository.save(createJobScheduleEntity())
        JobEntity jobEntity1_1 = createJobEntity(savedJobScheduleEntity1, "DISCOVERY_INPROGRESS")
        jobEntity1_1.setExecutor("pod-1")
        JobEntity savedJobEntity1_1 = jobRepository.save(jobEntity1_1)

        when: "Repair job schedules after restart"
        jobSchedulePodRestartHandler.repairJobSchedulesAfterRestart()

        then: "Running job is unchanged"
        jobRepository.findById(savedJobEntity1_1.id).get().jobStatus == savedJobEntity1_1.jobStatus

        and: "No scheduled job is created"
        jobRepository.findByJobScheduleIdAndJobStatus(savedJobScheduleEntity1.id, "SCHEDULED").size() == 0
    }

    @Transactional
    def "Repair job schedule completes without error when no running jobs"() {

        setup: "Create job schedules with no running jobs"
        JobScheduleEntity savedJobScheduleEntity1 = jobScheduleRepository.save(createJobScheduleEntity())
        JobEntity jobEntity1_1 = jobRepository.save(createJobEntity(savedJobScheduleEntity1, "COMPLETED"))
        JobScheduleEntity savedJobScheduleEntity2 = jobScheduleRepository.save(createJobScheduleEntity())
        JobEntity jobEntity2_1 = jobRepository.save(createJobEntity(savedJobScheduleEntity2, "COMPLETED"))

        when: "Repair job schedules after restart"
        jobSchedulePodRestartHandler.repairJobSchedulesAfterRestart()

        then: "No exception thrown"
        noExceptionThrown()

        and: "Completed jobs are unchanged"
        jobRepository.findById(jobEntity1_1.id).get().jobStatus == COMPLETED.toString()
        jobRepository.findById(jobEntity2_1.id).get().jobStatus == COMPLETED.toString()
    }

    @Transactional
    def "Repair job schedules after restart executes best effort"() {

        setup: "Create 2 job schedules with running jobs"
        JobScheduleEntity savedJobScheduleEntity1 = jobScheduleRepository.save(createJobScheduleEntity())
        JobScheduleEntity savedJobScheduleEntity2 = jobScheduleRepository.save(createJobScheduleEntity())
        JobEntity savedJobEntity1_1= jobRepository.save(createJobEntity(savedJobScheduleEntity1, "DISCOVERY_INPROGRESS"))
        JobEntity savedJobEntity2_1= jobRepository.save(createJobEntity(savedJobScheduleEntity2, "RECONCILE_INPROGRESS"))

        and: "Mock jobScheduleService to throw error creating next schedule for first job"
        jobScheduleService.createNextScheduledJob( { it.id == savedJobEntity1_1.id }) >>
                {{ throw new RuntimeException("Error!")}}

        when: "Repair job schedules after restart"
        jobSchedulePodRestartHandler.repairJobSchedulesAfterRestart()

        then: "Schedule2 running job is set to failed state and next scheduled job created"
        jobRepository.findById(savedJobEntity2_1.id).get().jobStatus == RECONCILE_FAILED.toString()
        jobRepository.findByJobScheduleIdAndJobStatus(savedJobScheduleEntity2.id, "SCHEDULED").size() == 1

        and: "Schedule1 running job is set to failed state but next scheduled job is not created"
        jobRepository.findById(savedJobEntity1_1.id).get().jobStatus == DISCOVERY_FAILED.toString()
        jobRepository.findByJobScheduleIdAndJobStatus(savedJobScheduleEntity1.id, "SCHEDULED").size() == 0
    }


    @Transactional
    def "Repair job schedule with running job which was executed by pod which no longer exists"() {

        setup: "Create job schedule with Inprogress job and job executor set to pod-1"
        JobScheduleEntity savedJobScheduleEntity1 = jobScheduleRepository.save(createJobScheduleEntity())
        JobEntity jobEntity1 = createJobEntity(savedJobScheduleEntity1, "DISCOVERY_INPROGRESS")
        jobEntity1.setExecutor("pod-1")
        JobEntity savedJobEntity1_1= jobRepository.save(jobEntity1)

        and: "Configure KubernetesService to return pod list which does not include pod-1"
        kubernetesServer.expect().get()
                .withPath("/api/v1/namespaces/test/pods?labelSelector=app%3Deric-esoa-dr-service")
                .andReturn(200, new PodListBuilder().addToItems(
                        new PodBuilder()
                                .withNewMetadata()
                                .withName("pod-2")
                                .endMetadata()
                                .withStatus(new PodStatusBuilder()
                                        .addNewContainerStatus().withStarted()
                                        .withState(new ContainerStateBuilder().withNewRunning().endRunning().build())
                                        .endContainerStatus()
                                        .build())
                                .build())
                        .build())
                .once()

        when: "Repair job schedules after restart"
        jobSchedulePodRestartHandler.repairJobSchedulesAfterRestart()

        then: "Running job is set to failed state"
        jobRepository.findById(savedJobEntity1_1.id).get().jobStatus == DISCOVERY_FAILED.toString()

        and: "Next scheduled job is created"
        jobRepository.findByJobScheduleIdAndJobStatus(savedJobScheduleEntity1.id, "SCHEDULED").size() == 1
    }

    private JobScheduleEntity createJobScheduleEntity() {
        return new JobScheduleEntity(name: "schedule-${UUID.randomUUID().toString()}", description: "my schedule",
                expression: "0 */15 * * * *", "enabled": true, version: 1,
                jobSpecification: jobSpecificationEntity, creationDate: Date.from(Instant.now()))
    }

    private JobEntity createJobEntity(JobScheduleEntity jobScheduleEntity, String status) {
        return new JobEntity(jobSpecification: jobScheduleEntity.jobSpecification, startDate:
                Date.from(Instant.now()), completedDate:
                Date.from (Instant.now()), jobStatus: status, applicationJobName: "job-1",
                jobScheduleId: jobScheduleEntity.id, executor: "eric-esoa-dr-service")
    }
}