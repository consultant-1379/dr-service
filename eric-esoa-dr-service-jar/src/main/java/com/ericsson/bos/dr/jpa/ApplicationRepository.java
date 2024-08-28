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

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import com.ericsson.bos.dr.jpa.model.ApplicationEntity;

/**
 * Application Repository.
 */
public interface ApplicationRepository extends CrudRepository<ApplicationEntity, Long> {

    /**
     * Find applications in a feature pack.
     * @param featurePackId feature pack id
     * @return ApplicationEntity collection
     */
    Collection<ApplicationEntity> findByFeaturePackId(Long featurePackId);

    /**
     * Find Application by id in a feature pack.
     * @param id application id
     * @param featurePackId feature pack id
     * @return optional ApplicationEntity
     */
    Optional<ApplicationEntity> findByIdAndFeaturePackId(long id, long featurePackId);
}
