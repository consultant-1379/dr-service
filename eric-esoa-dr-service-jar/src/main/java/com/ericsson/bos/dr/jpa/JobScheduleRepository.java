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

import java.util.Optional;

import com.ericsson.bos.dr.jpa.model.JobScheduleEntity;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * Job Schedule repository.
 */
public interface JobScheduleRepository extends PagingAndSortingRepository<JobScheduleEntity, Long>, JpaSpecificationExecutor<JobScheduleEntity>,
    CrudRepository<JobScheduleEntity, Long> {

    /**
     * Find Job schedule identified by its name.
     * @param name job schedule name
     * @return optional JobScheduleEntity
     */
    Optional<JobScheduleEntity> findByName(String name);
}