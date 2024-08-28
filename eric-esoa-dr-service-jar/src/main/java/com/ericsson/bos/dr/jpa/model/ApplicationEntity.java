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
package com.ericsson.bos.dr.jpa.model;

import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationDto;
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationJobDto;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Table;
import org.hibernate.annotations.Type;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Application Entity.
 */
@Table(name = "application")
@EntityListeners({AuditingEntityListener.class})
@Entity
public class ApplicationEntity extends ConfigurationFileEntity<ApplicationConfigurationDto> {

    @Basic(fetch = FetchType.LAZY)
    @Column(name = "config", columnDefinition = "jsonb")
    @Type(JsonBinaryType.class)
    private ApplicationConfigurationDto config;

    @Override
    public ApplicationConfigurationDto getConfig() {
        return config;
    }

    public void setConfig(ApplicationConfigurationDto config) {
        this.config = config;
    }

    /**
     * Find named job.
     * @param jobName the job name
     * @return ApplicationConfigurationJobDto
     */
    public ApplicationConfigurationJobDto findJob(final String jobName) {
        return getConfig().getJobs().stream()
                .filter(j -> j.getName().equals(jobName)).findAny().orElse(null);
    }
}
