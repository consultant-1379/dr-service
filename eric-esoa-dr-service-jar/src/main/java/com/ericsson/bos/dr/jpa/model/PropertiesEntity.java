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

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Table;

import org.hibernate.annotations.Type;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.ericsson.bos.dr.web.v1.api.model.PropertiesConfigurationDto;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;

/**
 * Properties Entity.
 */
@Table(name = "properties")
@EntityListeners(AuditingEntityListener.class)
@Entity
public class PropertiesEntity extends ConfigurationFileEntity<PropertiesConfigurationDto> {

    @Basic(fetch = FetchType.LAZY)
    @Column(name = "config", columnDefinition = "jsonb")
    @Type(JsonBinaryType.class)
    private PropertiesConfigurationDto config;

    public PropertiesConfigurationDto getConfig() {
        return config;
    }

    public void setConfig(PropertiesConfigurationDto config) {
        this.config = config;
    }
}