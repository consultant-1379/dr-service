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
package com.ericsson.bos.dr.service.subscriptions.kafka;

import com.ericsson.bos.dr.service.subsystem.ConnectedSystem;
import com.ericsson.bos.dr.service.subsystem.KafkaConnectionProperties;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Subscription to a kafka connected system.
 * @param messageSubscriptionEntityId  message subscription entity id
 * @param  kafkaConnectedSystem kafka connected system
 */
public record KafkaSubscription(long messageSubscriptionEntityId, ConnectedSystem<KafkaConnectionProperties> kafkaConnectedSystem) {

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final KafkaSubscription that = (KafkaSubscription) o;
        return new EqualsBuilder().append(messageSubscriptionEntityId, that.messageSubscriptionEntityId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(messageSubscriptionEntityId).toHashCode();
    }
}