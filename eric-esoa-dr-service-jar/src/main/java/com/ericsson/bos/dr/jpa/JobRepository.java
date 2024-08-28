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
package com.ericsson.bos.dr.jpa;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import com.ericsson.bos.dr.jpa.model.JobEntity;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

/**
 * Job repository.
 */
public interface JobRepository extends PagingAndSortingRepository<JobEntity, Long>, JpaSpecificationExecutor<JobEntity>,
    CrudRepository<JobEntity, Long> {

    /**
     * Find all jobs associated with a feature pack which are in one of the specified list of status.
     * @param featurePackId feature pack id
     * @param status status list
     * @return JobEntity list
     */
    List<JobEntity> findByJobSpecificationFeaturePackIdAndJobStatusIn(long featurePackId, Collection<String> status);

    /**
     * Find jobs which are in a state to be executed, where valid execution states are NEW, RECONCILE_REQUESTED and SCHEDULED.
     * In case of SCHEDULED, only those jobs with dueDate at or before the specified date will be returned.
     * <p>Uses SKIP_LOCKED option to lock the returned jobs and prevent them being read in another transaction.
     * Other transactions will not be blocked and will skip over the jobs.
     * </p>
     * @param pageable paging
     * @param dueDate due date in case of SCHEDULED jobs
     * @return JobEntity list
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value ="-2")}) // SKIP_LOCKED
    @Query(value = "select j from JobEntity j where j.locked=false AND "
            + "(j.jobStatus in ('NEW','RECONCILE_REQUESTED') OR (j.jobStatus='SCHEDULED' AND j.dueDate <= :date ))")
    List<JobEntity> findExecutableJobs(Pageable pageable, @Param("date") LocalDateTime dueDate);

    /**
     * Delete all Jobs with given status and older than given date.
     *
     * @param olderThan
     *         older than date
     * @param status
     *         job status
     * @return no of jobs deleted
     */
    long deleteByStartDateBeforeAndJobStatusIn(Date olderThan, Collection<String> status);

    /**
     * Find jobs associated with a schedule which are in the specified state.
     * @param jobScheduleId job schedule id
     * @param status job status
     * @return JobEntity list
     */
    List<JobEntity> findByJobScheduleIdAndJobStatus(long jobScheduleId, String status);

    /**
     * Find jobs associated with a schedule which are in any of the specified states.
     * @param jobScheduleId job schedule id
     * @param statuses job statuses
     * @return JobEntity list
     */
    List<JobEntity> findByJobScheduleIdAndJobStatusIn(long jobScheduleId, Collection<String> statuses);

    /**
     * Count the number of jobs for a schedule.
     * @param jobScheduleId job schedule id
     * @return count
     */
    long countByJobScheduleId(long jobScheduleId);

    /**
     * Find all scheduled jobs in any of the specified states.
     * @param status job status
     * @return JobEntity list
     */
    List<JobEntity> findByJobScheduleIdIsNotNullAndJobStatusIn(Collection<String> status);

}