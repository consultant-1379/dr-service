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

package com.ericsson.bos.dr.service.subscriptions.kafka.security;

import java.util.List;
import java.util.function.BiConsumer;

import com.ericsson.bos.dr.jpa.ListenerMessageSubscriptionRepository;
import com.ericsson.bos.dr.service.subscriptions.kafka.KafkaSubscriber;
import com.ericsson.bos.dr.service.subscriptions.kafka.KafkaSubscription;
import com.ericsson.bos.dr.service.subscriptions.kafka.KafkaSubscriptionRegistry;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Re-subscribe to the topics in a message subscription when the keystore or truststore secret configured for
 * the kafka connected system are changed.
 */
@Component
public class KakfaKubernetesSecretsEventReceiver implements BiConsumer<Watcher.Action, Secret> {

    private static final Logger LOGGER = LoggerFactory.getLogger(KakfaKubernetesSecretsEventReceiver.class);

    @Autowired
    private KafkaSubscriptionRegistry kafkaSubscriptionRegistry;

    @Autowired
    private KafkaSubscriber kafkaSubscriber;

    @Autowired
    private ListenerMessageSubscriptionRepository messageSubscriptionRepository;

    @Override
    public void accept(Watcher.Action action, Secret secret) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Event Received: action={}, secret={}", action, secret);
        }
        if (Watcher.Action.MODIFIED.equals(action) || Watcher.Action.ADDED.equals(action)) {
            final List<KafkaSubscription> subscriptions = kafkaSubscriptionRegistry.findBySecret(secret.getMetadata().getName());
            if (!subscriptions.isEmpty()) {
                LOGGER.info("Handle event: action={}, secret={}", action, secret.getMetadata().getName());
            }
            for (final KafkaSubscription subscription: subscriptions) {
                messageSubscriptionRepository.findById(subscription.messageSubscriptionEntityId()).ifPresent(entity -> {
                    try {
                        kafkaSubscriber.unsubscribe(entity.getId().toString());
                        kafkaSubscriber.subscribe(entity);
                    } catch (final Exception e) {
                        LOGGER.error(String.format("Error re-subscribing for message subscription '%s' after "
                                + "certificate secret '%s' was modified", entity.getName(), secret.getMetadata().getName()), e);
                    }
                });
            }
        }
    }
}