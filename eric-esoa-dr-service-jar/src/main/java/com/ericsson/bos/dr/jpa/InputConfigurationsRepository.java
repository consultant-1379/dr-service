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

import org.springframework.data.jpa.repository.JpaRepository;

import com.ericsson.bos.dr.jpa.model.InputConfigurationEntity;

/**
 * Inputs repository.
 */
public interface InputConfigurationsRepository extends JpaRepository<InputConfigurationEntity, Long> {

    /**
     * Find input configuration in a feature pack.
     *
     * @param featurePackId
     *         feature pack id.
     * @return ProfileEntity collection
     */
    Collection<InputConfigurationEntity> findByFeaturePackId(long featurePackId);

    /**
     * Find input configuration in a feature packs by its name.
     *
     * @param name
     *         input configuration name
     * @param featurePackId
     *         feature pack id
     * @return InputEntity optional
     */
    Optional<InputConfigurationEntity> findByNameAndFeaturePackId(String name, long featurePackId);

    /**
     * Find input configuration in a feature packs by its id.
     *
     * @param id
     *         input configuration id
     * @param featurePackId
     *         feature pack id
     * @return InputEntity optional
     */
    Optional<InputConfigurationEntity> findByIdAndFeaturePackId(long id, long featurePackId);
}
