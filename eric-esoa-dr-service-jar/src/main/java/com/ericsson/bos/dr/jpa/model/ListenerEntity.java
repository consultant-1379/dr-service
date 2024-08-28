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

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import com.ericsson.bos.dr.web.v1.api.model.ListenerConfigurationDto;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import org.hibernate.annotations.Type;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Listener Entity.
 */
@Table(name = "listener")
@EntityListeners(AuditingEntityListener.class)
@Entity
public class ListenerEntity extends ConfigurationFileEntity<ListenerConfigurationDto> {

    @Basic(fetch = FetchType.LAZY)
    @Column(name = "config", columnDefinition = "jsonb")
    @Type(JsonBinaryType.class)
    private ListenerConfigurationDto config;

    @OneToMany(mappedBy="listenerEntity", fetch = FetchType.LAZY)
    private Set<ListenerMessageSubscriptionEntity> listenerMessageSubscriptions = new HashSet<>();

    public ListenerConfigurationDto getConfig() {
        return config;
    }

    public void setConfig(final ListenerConfigurationDto config) {
        this.config = config;
    }

    public Set<ListenerMessageSubscriptionEntity> getListenerMessageSubscriptions() {
        return listenerMessageSubscriptions;
    }

    public void setListenerMessageSubscriptions(final Set<ListenerMessageSubscriptionEntity> listenerMessageSubscriptions) {
        this.listenerMessageSubscriptions = listenerMessageSubscriptions;
    }
}