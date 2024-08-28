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

import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import com.ericsson.bos.dr.jpa.model.DiscoveryObjectEntity;
import com.ericsson.bos.dr.jpa.model.StatusCount;

/**
 * DiscoveryObject repository.
 */
public interface DiscoveryObjectRepository extends PagingAndSortingRepository<DiscoveryObjectEntity, Long>,
        JpaSpecificationExecutor<DiscoveryObjectEntity>, CrudRepository<DiscoveryObjectEntity, Long> {

    /**
     * Specification to find discovery objects with job id.
     * @param jobId job id
     * @return Specification
     */
    static Specification<DiscoveryObjectEntity> jobIdEquals(final long jobId){
        return (root, query, criteriaBuilder)
                -> criteriaBuilder.equal(root.get("jobId"), jobId);
    }

    /**
     * Get ids for all discovery objects associated with a job which are not in the
     * specified status.
     * @param jobId  jobs
     * @param status status
     * @return ids
     */
    @Query(value = "select d.id from DiscoveryObjectEntity d where d.jobId = :jobid and d.status <> :status")
    List<Long> getIdsByJobIdAndStatusNot(@Param("jobid") long jobId, @Param("status") String status);

    /**
     * Get discovery object counts grouped  by status.
     * @param jobId  job id
     *
     * @return list of statusCounts
     */
    @Query(value = "select status,count(*) as count from discovered_object where job_id = :jobId group by status;", nativeQuery = true)
    List<StatusCount> getCountsGroupedByStatus(@Param("jobId") long jobId);
}