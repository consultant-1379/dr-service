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

import java.util.Date;
import java.util.Map;

import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.ericsson.bos.dr.service.subscriptions.kafka.security.DeleteKafkaJKSEntityListener;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Listener message subscription Entity.
 */
@Table(name = "listener_message_subscription")
@EntityListeners({AuditingEntityListener.class, DeleteKafkaJKSEntityListener.class})
@Entity
public class ListenerMessageSubscriptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="name")
    private String name;

    @Column(name="description")
    private String description;

    @Column(name="message_broker_type")
    private String messageBrokerType;

    @Column(name="subsystem_name")
    private String subsystemName;

    @Basic(fetch = FetchType.EAGER)
    @Column(name="config",  columnDefinition = "jsonb")
    @Type(JsonBinaryType.class)
    private Map<String,Object> consumerConfiguration;

    @ManyToOne
    @JoinColumn(name="listener_id")
    private ListenerEntity listenerEntity;

    @Version
    @Column(nullable = false)
    private Long version;

    @CreatedDate
    @Column(name = "creation_date")
    private Date creationDate;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getSubsystemName() {
        return subsystemName;
    }

    public void setSubsystemName(final String subsystemName) {
        this.subsystemName = subsystemName;
    }

    public String getMessageBrokerType() {
        return messageBrokerType;
    }

    public void setMessageBrokerType(final String messageBrokerType) {
        this.messageBrokerType = messageBrokerType;
    }

    public Map<String, Object> getConsumerConfiguration() {
        return consumerConfiguration;
    }

    public void setConsumerConfiguration(final Map<String, Object> consumerConfiguration) {
        this.consumerConfiguration = consumerConfiguration;
    }

    public ListenerEntity getListenerEntity() {
        return listenerEntity;
    }

    public void setListenerEntity(final ListenerEntity listenerEntity) {
        this.listenerEntity = listenerEntity;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(final Long version) {
        this.version = version;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(final Date creationDate) {
        this.creationDate = creationDate;
    }
}
