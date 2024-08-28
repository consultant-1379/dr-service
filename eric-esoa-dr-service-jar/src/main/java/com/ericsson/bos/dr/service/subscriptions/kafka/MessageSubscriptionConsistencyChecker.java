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

import com.ericsson.bos.dr.jpa.ListenerMessageSubscriptionRepository;
import com.ericsson.bos.dr.jpa.model.ListenerMessageSubscriptionEntity;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Consistently polling <code>listenerMessageSubscriptionRepository</code> on a specific interval
 * to synchronize the Kafka listener registry to match the subscriptions stored in the database.
 */
@Component
public class MessageSubscriptionConsistencyChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageSubscriptionConsistencyChecker.class);

    @Autowired
    private KafkaSubscriber kafkaSubscriber;

    @Autowired
    private ListenerMessageSubscriptionRepository listenerMessageSubscriptionRepository;

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    /**
     * Scheduled execution to synchronize the message subscriptions initially at startup and
     * then with an interval of <code>fixedRateString</code> milliseconds.
     * <p>Reads all existing <code>ListenerMessageSubscriptionEntity</code>. For each entity, if there is no <code>MessageListenerContainer</code>
     * existing with the same id as the entity, then {@linkplain KafkaSubscriber#subscribe(ListenerMessageSubscriptionEntity)} is called
     * to subscribe for messages on the configured topics.</p>
     * <p>For existing listener containers, if there is no  <code>ListenerMessageSubscriptionEntity</code>. with the same id, then the listener
     * container is unregistered and destroyed via call to {@linkplain KafkaSubscriber#unsubscribe(String)}.</p>
     */
    @Scheduled(fixedRateString = "${service.message-subscriptions.consistency-check.interval}")
    public void executeConsistencyCheck() {

        final var listenerContainerIdList = kafkaListenerEndpointRegistry.getAllListenerContainers()
                .stream()
                .map(m -> Long.parseLong(m.getListenerId()))
                .toList();
        final var subscriptionsInDatabase = listenerMessageSubscriptionRepository.findAll();
        final var subscriptionInDatabaseIdList = subscriptionsInDatabase
                .stream()
                .map(ListenerMessageSubscriptionEntity::getId)
                .toList();

        subscribeMissingListenerContainer(listenerContainerIdList, subscriptionsInDatabase);
        unsubscribeRedundantListenerContainer(listenerContainerIdList, subscriptionInDatabaseIdList);
    }

    private void subscribeMissingListenerContainer(List<Long> listenerContainerIdList,
                                                   List<ListenerMessageSubscriptionEntity> subscriptionsInDatabase) {
        subscriptionsInDatabase.stream()
                .filter(entity -> !listenerContainerIdList.contains(entity.getId()))
                .forEach(entity -> {
                    try {
                        kafkaSubscriber.subscribe(entity);
                        LOGGER.info("Subscriptions consistency check: Message listener with Id: {} is registered.", entity.getId());
                    } catch (final Exception exception) {
                        LOGGER.error("Error creating message subscription, id: {}, subsystemName: {}, groupId: {}, topics: {}, {}",
                                entity.getId(),
                                entity.getSubsystemName(),
                                entity.getConsumerConfiguration().get("groupId"),
                                entity.getConsumerConfiguration().get("topicNames"),
                                exception.getMessage());
                    }
                });
    }

    private void unsubscribeRedundantListenerContainer(List<Long> listenerContainerIdList,
                                                       List<Long> subscriptionInDatabaseIdList) {
        listenerContainerIdList.stream().filter(id -> !subscriptionInDatabaseIdList.contains(id))
                .forEach(id -> {
                    try {
                        kafkaSubscriber.unsubscribe(String.valueOf(id));
                    } catch (final Exception exception) {
                        LOGGER.error("Error deleting message subscription, id: {}, {}",
                                id, exception.getMessage());
                    }
                    LOGGER.info("Subscriptions consistency check: Message listener with Id: {} is unregistered.", id);
                });
    }

}
