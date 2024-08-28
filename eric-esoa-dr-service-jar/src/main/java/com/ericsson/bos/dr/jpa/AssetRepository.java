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

import org.springframework.data.repository.CrudRepository;

import com.ericsson.bos.dr.jpa.model.AssetEntity;

/**
 * Asset Repository.
 */
public interface AssetRepository extends CrudRepository<AssetEntity, Long> {

    /**
     * Find Asset by id in a feature pack.
     *
     * @param name          asset name.
     * @param featurePackId feature pack id
     * @return optional AssetEntity
     */
    Optional<AssetEntity> findByNameAndFeaturePackId(String name, long featurePackId);
}
