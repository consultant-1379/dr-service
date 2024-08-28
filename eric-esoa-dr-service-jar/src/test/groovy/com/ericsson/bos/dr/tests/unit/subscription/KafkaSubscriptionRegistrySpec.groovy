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
package com.ericsson.bos.dr.tests.unit.subscription

import com.ericsson.bos.dr.jpa.model.ListenerMessageSubscriptionEntity
import com.ericsson.bos.dr.service.subscriptions.kafka.KafkaSubscription
import com.ericsson.bos.dr.service.subscriptions.kafka.KafkaSubscriptionRegistry
import com.ericsson.bos.dr.service.subsystem.ConnectedSystem
import com.ericsson.bos.dr.service.subsystem.KafkaConnectionProperties
import org.springframework.context.ApplicationEventPublisher
import spock.lang.Specification

class KafkaSubscriptionRegistrySpec extends Specification {

    ApplicationEventPublisher applicationEventPublisherMock = Mock()
    KafkaSubscriptionRegistry kafkaSubscriptionRegistry = new KafkaSubscriptionRegistry(eventPublisher: applicationEventPublisherMock)

    def "Message subscriptions are added and removed from registry"() {

        when: "Add kafka subscription"
        ListenerMessageSubscriptionEntity messageSubscriptionEntity = new ListenerMessageSubscriptionEntity(id: 100)
        ConnectedSystem<KafkaConnectionProperties> connectedSystem = new ConnectedSystem<>()
        kafkaSubscriptionRegistry.add(messageSubscriptionEntity, connectedSystem)

        then: "Kafka subscription is added to registry"
        kafkaSubscriptionRegistry.get(messageSubscriptionEntity.id).isPresent()

        when: "Add another subscription"
        ListenerMessageSubscriptionEntity messageSubscriptionEntity2 = new ListenerMessageSubscriptionEntity(id: 200)
        kafkaSubscriptionRegistry.add(messageSubscriptionEntity2, connectedSystem)

        then: "Kafka subscription is added to registry"
        kafkaSubscriptionRegistry.get(messageSubscriptionEntity2.id).isPresent()

        when: "Delete kafka subscriptions"
        kafkaSubscriptionRegistry.remove(messageSubscriptionEntity.id)
        kafkaSubscriptionRegistry.remove(messageSubscriptionEntity2.id)

        then: "Kafka subscriptions are removed"
        kafkaSubscriptionRegistry.get(messageSubscriptionEntity.id).isPresent() == false
        kafkaSubscriptionRegistry.get(messageSubscriptionEntity2.id).isPresent() == false
    }


    def "Find kafka message subscriptions with keystore or truststore secret"() {

        setup: "Add Kafka message subscriptions"
        ConnectedSystem<KafkaConnectionProperties> connectedSystem1 = new ConnectedSystem<>(name: "kafka1",
                connectionProperties: [new KafkaConnectionProperties(keyStoreSecretName: "secret1", trustStoreSecretName: "secret2")])
        ConnectedSystem<KafkaConnectionProperties> connectedSystem2 = new ConnectedSystem<>(name: "kafka2",
                connectionProperties: [new KafkaConnectionProperties(keyStoreSecretName: "secret1")])
        ConnectedSystem<KafkaConnectionProperties> connectedSystem3 = new ConnectedSystem<>(name: "kafka2",
                connectionProperties: [new KafkaConnectionProperties(trustStoreSecretName: "secret2")])
        ListenerMessageSubscriptionEntity messageSubscriptionEntity1 = new ListenerMessageSubscriptionEntity(id: 100)
        ListenerMessageSubscriptionEntity messageSubscriptionEntity2 = new ListenerMessageSubscriptionEntity(id: 200)
        ListenerMessageSubscriptionEntity messageSubscriptionEntity3 = new ListenerMessageSubscriptionEntity(id: 300)
        kafkaSubscriptionRegistry.add(messageSubscriptionEntity1, connectedSystem1)
        kafkaSubscriptionRegistry.add(messageSubscriptionEntity2, connectedSystem2)
        kafkaSubscriptionRegistry.add(messageSubscriptionEntity3, connectedSystem3)

        when: "Find kafka subscriptions with connected system configured with secret secret1"
        List<KafkaSubscription> kafkaSubscriptions = kafkaSubscriptionRegistry.findBySecret("secret1")

        then: "kafka subscriptions are returned"
        kafkaSubscriptions.collect { it.messageSubscriptionEntityId }.sort() ==
                [messageSubscriptionEntity1.id, messageSubscriptionEntity2.id]

        when: "Find kafka subscriptions with connected system configured with secret secret2"
        kafkaSubscriptions = kafkaSubscriptionRegistry.findBySecret("secret2")

        then: "kafka subscriptions are returned"
        kafkaSubscriptions.collect { it.messageSubscriptionEntityId }.sort() ==
                [messageSubscriptionEntity1.id, messageSubscriptionEntity3.id]

        when: "Find kafka subscriptions with connected system configured with secret secret3"
        kafkaSubscriptions = kafkaSubscriptionRegistry.findBySecret("secret3")

        then: "No kafka subscriptions are returned"
        kafkaSubscriptions.isEmpty()
    }
}