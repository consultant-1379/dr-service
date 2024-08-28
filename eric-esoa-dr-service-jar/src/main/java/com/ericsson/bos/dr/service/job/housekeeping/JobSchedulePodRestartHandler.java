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

import java.util.List;
import java.util.Optional;

import com.ericsson.bos.dr.jpa.model.JobEntity;
import com.ericsson.bos.dr.service.JobService;
import com.ericsson.bos.dr.service.utils.KUBE;
import com.ericsson.bos.dr.service.utils.KUBE.PodSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Check and repair job schedules after a pod restarts or is deleted. Corrective action is required to the job schedule if there was a running
 * job when the pod was either restarted or deleted. The next scheduled job is created when the job completes so if the job did not complete
 * before the restart, then there will be no further jobs executed for the schedule.
 * <p>
 *  On startup, all running scheduled jobs are read and repair is performed if the job was executed by the current pod or was executed by a pod
 *  which no longer exists. In both cases, the {@linkplain RepairJobScheduleAction} is called to repair the schedule.
 * </p>
 */
@Component
public class JobSchedulePodRestartHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobSchedulePodRestartHandler.class);

    @Value("${podName}")
    private String podName;

    @Autowired
    private JobService jobService;

    @Autowired
    private RepairJobScheduleAction repairJobScheduleActon;

    /**
     * Repair job schedules after pod restart.
     */
    @EventListener(ApplicationStartedEvent.class)
    public void repairJobSchedulesAfterRestart() {
        final List<JobEntity> runningScheduledJobs =
                jobService.findRunningScheduledJobs().stream().filter(j -> j.getExecutor() != null).toList();
        if (!runningScheduledJobs.isEmpty()) {
            List<PodSummary> pods = null;
            for (final JobEntity jobEntity: runningScheduledJobs) {
                if (isJobExecutedByCurrentPod(jobEntity)) {
                    LOGGER.info("Repair job schedule {}. Job {} was running on pod {} which has restarted", jobEntity.getJobScheduleId(),
                            jobEntity.getId(),
                            jobEntity.getExecutor());
                    repairJobSchedule(jobEntity);
                } else {
                    pods = Optional.ofNullable(pods).orElseGet(KUBE::getPodNames);
                    LOGGER.info("Current pods: {}", pods);
                    final List<String> podNames = pods.stream().map(PodSummary::getName).toList();
                    if (isJobExecutedByPodWhichNoLongerExists(jobEntity, podNames)) {
                        LOGGER.info("Repair job schedule {}. Job {} was running on pod {} which no longer exists. ", jobEntity.getJobScheduleId(),
                                jobEntity.getId(),
                                jobEntity.getExecutor());
                        repairJobSchedule(jobEntity);
                    }
                }
            }
        }
    }

    private void repairJobSchedule(final JobEntity jobEntity) {
        try {
            repairJobScheduleActon.accept(jobEntity);
        } catch (final Exception e) {
            LOGGER.error(String.format("Error repairing job schedule %s after restart", jobEntity.getJobScheduleId()), e);
        }
    }

    private boolean isJobExecutedByCurrentPod(final JobEntity jobEntity) {
        return podName != null && podName.equals(jobEntity.getExecutor());
    }

    private boolean isJobExecutedByPodWhichNoLongerExists(final JobEntity jobEntity, final List<String> podNames) {
        return !podNames.isEmpty() && !podNames.contains(jobEntity.getExecutor());
    }
}