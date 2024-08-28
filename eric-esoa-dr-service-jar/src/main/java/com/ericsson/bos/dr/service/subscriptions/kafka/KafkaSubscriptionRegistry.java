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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import com.ericsson.bos.dr.jpa.model.ListenerMessageSubscriptionEntity;
import com.ericsson.bos.dr.service.subsystem.ConnectedSystem;
import com.ericsson.bos.dr.service.subsystem.KafkaConnectionProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Registry of all active subscriptions to external kafka systems.
 */
@Component
public class KafkaSubscriptionRegistry {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    private Set<KafkaSubscription> subscriptions = new CopyOnWriteArraySet<>();

    public enum Action { ADDED, REMOVED }

    public record KafaSubscriptionRegistryEvent(Action action, KafkaSubscription kafkaSubscription) {}

    /**
     * Add Kafka subscription to the registry.
     * @param listenerMessageSubscriptionEntity message subscription entity
     * @param connectedSystem connected kafka system
     * @return KafkaSubscription
     */
    public KafkaSubscription add(ListenerMessageSubscriptionEntity listenerMessageSubscriptionEntity,
                     ConnectedSystem<KafkaConnectionProperties> connectedSystem) {
        final var kafkaSubscription = new KafkaSubscription(listenerMessageSubscriptionEntity.getId(), connectedSystem);
        subscriptions.add(kafkaSubscription);
        eventPublisher.publishEvent(new KafaSubscriptionRegistryEvent(Action.ADDED, kafkaSubscription));
        return kafkaSubscription;
    }

    /**
     * Remove kafka subscription from the registry.
     * @param listenerMessageSubscriptionEntityId message subscription entity id
     * @return true if subscription removed, otherwise false.
     */
    public boolean remove(final long listenerMessageSubscriptionEntityId) {
        final Optional<KafkaSubscription> kafkaSubscription = subscriptions.stream()
                .filter(s -> s.messageSubscriptionEntityId() == listenerMessageSubscriptionEntityId)
                .findAny();
        if (kafkaSubscription.isPresent()) {
            subscriptions.remove(kafkaSubscription.get());
            eventPublisher.publishEvent(new KafaSubscriptionRegistryEvent(Action.REMOVED, kafkaSubscription.get()));
            return true;
        }
        return false;
    }

    /**
     * Get kafka subscription.
     * @param listenerMessageSubscriptionEntityId message subscription entity id
     * @return KafkaSubscription if existing
     */
    public Optional<KafkaSubscription> get(final long listenerMessageSubscriptionEntityId) {
        return subscriptions.stream().filter(s -> s.messageSubscriptionEntityId() == listenerMessageSubscriptionEntityId).findFirst();
    }

    /**
     * Get all kafka subscriptions
     * @return KafkaSubscription set
     */
    public Set<KafkaSubscription> getAll() {
        return Collections.unmodifiableSet(subscriptions);
    }

    /**
     * Find all kafka subscriptions towards a connected system using a keyStoreSecret or trustStoreSecret name matching
     * the supplied secret name.
     * @param secretName kubernetes secret name
     * @return KafkaSubscription list
     */
    public List<KafkaSubscription> findBySecret(final String secretName) {
        return subscriptions.stream().filter(s -> {
            final var kafkaConnectionProps = s.kafkaConnectedSystem().getConnectionProperties().get(0);
            return secretName.equals(kafkaConnectionProps.getKeyStoreSecretName()) ||
                    secretName.equals(kafkaConnectionProps.getTrustStoreSecretName());
        }).toList();
    }

    /**
     * Find all kafka subscriptions towards a connected system with ssl enabled.
     * @return KafkaSubscription list
     */
    public List<KafkaSubscription> findBySslEnabled() {
        return subscriptions.stream().filter(s -> s.kafkaConnectedSystem().getConnectionProperties().get(0).isSslEnabled()).toList();
    }
}