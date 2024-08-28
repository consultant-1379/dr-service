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

import org.springframework.data.jpa.repository.JpaRepository;

import com.ericsson.bos.dr.jpa.model.PropertiesEntity;

/**
 * Properties repository.
 */
public interface PropertiesRepository extends JpaRepository<PropertiesEntity, Long> {

    /**
     * Find properties of a feature pack.
     *
     * @param featurePackId
     *         feature pack id.
     * @return Optional PropertiesEntity
     */
    Optional<PropertiesEntity> findByFeaturePackId(long featurePackId);
}