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
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.ericsson.bos.dr.jpa.model.FeaturePackEntity;

/**
 * Feature pack repository.
 */
public interface FeaturePackRepository extends JpaRepository<FeaturePackEntity, Long>, JpaSpecificationExecutor<FeaturePackEntity> {

    /**
     * Find feature pack by name.
     * @param name feature pack  name.
     * @return FeaturePackEntity optional
     */
    Optional<FeaturePackEntity> findByName(String name);
}
