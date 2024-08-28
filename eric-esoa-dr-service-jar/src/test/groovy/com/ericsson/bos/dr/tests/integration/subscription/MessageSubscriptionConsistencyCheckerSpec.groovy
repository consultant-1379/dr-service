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
package com.ericsson.bos.dr.tests.integration.subscription

import com.ericsson.bos.dr.jpa.model.ListenerMessageSubscriptionEntity
import com.ericsson.bos.dr.service.exceptions.DRServiceException
import com.ericsson.bos.dr.service.exceptions.ErrorCode
import com.ericsson.bos.dr.service.subscriptions.kafka.KafkaSubscriber
import com.ericsson.bos.dr.service.subscriptions.kafka.MessageSubscriptionConsistencyChecker
import com.ericsson.bos.dr.service.subsystem.ConnectedSystemClient
import com.ericsson.bos.dr.tests.integration.BaseSpec
import org.spockframework.spring.SpringBean
import org.spockframework.spring.SpringSpy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.transaction.annotation.Transactional

@EmbeddedKafka(kraft = false, partitions = 1, brokerProperties = ["log.dir=target/embedded-kafka-4"], topics= ["topic_1", "topic_2"])
class MessageSubscriptionConsistencyCheckerSpec extends BaseSpec {

    @Autowired
    KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry

    @SpringBean
    ConnectedSystemClient connectedSystemClient = Mock(ConnectedSystemClient)

    @Autowired
    MessageSubscriptionConsistencyChecker messageSubscriptionConsistencyChecker

    @SpringSpy
    KafkaSubscriber kafkaSubscriber

    @Value('${spring.embedded.kafka.brokers}')
    private String brokerAddresses

    def cleanup() {
        kafkaListenerEndpointRegistry.getListenerContainerIds().each{ id ->
            kafkaListenerEndpointRegistry.unregisterListenerContainer(id)
        }
    }

    @Transactional
    def """Should create missing listener containers and delete redundant listener containers
            when synchronizing between the database and the local kafka container registry"""() {

        setup: "Upload feature pack and setup subsystem"
        def featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-6", "fp-6-${System.currentTimeMillis()}")
        def listenerName = featurePackDto.listeners.first().name

        ["subsystem_1", "subsystem_2", "subsystem_3"].each {subsystemName ->
            connectedSystemClient.fetchKafkaConnectedSystem(subsystemName) >>
                    listenersTestSteps.createConnectedSystem(brokerAddresses, subsystemName)
        }

        and: "Add 3 message listeners subscriptions directly to the database"
        listenersTestSteps.preCreateMessageListenerSubscriptions(featurePackDto.name, listenerName)

        when: "Calling subscription consistency checker"
        messageSubscriptionConsistencyChecker.executeConsistencyCheck()

        then: "Listener containers are registered"
        3 * kafkaSubscriber.subscribe(_)
        kafkaListenerEndpointRegistry.getListenerContainers().size() == 3

        when: "Create and register listener subscriptions that are not in the database"
        ListenerMessageSubscriptionEntity listenerMessageSubscription1 =
                listenersTestSteps.createMessageEntity(1001, "subsystem_1", "group-1", ["topic_1"], "l1", "fp1", )
        ListenerMessageSubscriptionEntity listenerMessageSubscription2 =
                listenersTestSteps.createMessageEntity(1002, "subsystem_1", "group-1", ["topic_1"], "l1", "fp1", )

        kafkaSubscriber.subscribe(listenerMessageSubscription1)
        kafkaSubscriber.subscribe(listenerMessageSubscription2)

        and: "Calling subscription consistency checker"
        messageSubscriptionConsistencyChecker.executeConsistencyCheck()

        then: "The new listener container is deleted to match database"
        1 * kafkaSubscriber.unsubscribe("1001")
        1 * kafkaSubscriber.unsubscribe("1002")
        kafkaListenerEndpointRegistry.getListenerContainers().size() == 3
    }

    @Transactional
    def "Should create/delete listener container even if error occurs on creating/deleting other subscriptions"() {

        setup: "Upload feature pack and setup subsystems"
        def featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-6", "fp-6-${System.currentTimeMillis()}")
        def listenerName = featurePackDto.listeners.first().name

        ["subsystem_1", "subsystem_unavailable"].each {subsystemName ->
            connectedSystemClient.fetchKafkaConnectedSystem(subsystemName) >>
                    listenersTestSteps.createConnectedSystem(brokerAddresses, subsystemName)
        }
        and: "Add 3 message listeners subscriptions directly to the database"
        listenersTestSteps.preCreateMessageListenerSubscriptions(featurePackDto.name, listenerName)

        when: "Calling subscription consistency checker"
        messageSubscriptionConsistencyChecker.executeConsistencyCheck()

        then: "Subscription with subsystemName:subsystem_1 is registered, Subscriptions with subsystemName:subsystem_unavailable are not"
        kafkaListenerEndpointRegistry.getListenerContainers().size() == 1

        when: "Create and register listener subscriptions that are not in the database"
        ListenerMessageSubscriptionEntity listenerMessageSubscription1 =
                listenersTestSteps.createMessageEntity(1003, "subsystem_1", "group-1", ["topic_1"], "l1", "fp1", )
        ListenerMessageSubscriptionEntity listenerMessageSubscription2 =
                listenersTestSteps.createMessageEntity(1004, "subsystem_1", "group-1", ["topic_1"], "l1", "fp1", )
        kafkaSubscriber.subscribe(listenerMessageSubscription1)
        kafkaSubscriber.subscribe(listenerMessageSubscription2)

        and: "Mock kafkaSubscriber to fail unsubscribe id:1003"
        kafkaSubscriber.unsubscribe("1003") >>
                {throw new DRServiceException(ErrorCode.FAILED_TO_DELETE_MESSAGE_SUBSCRIPTION, "s1")}

        then: "Calling subscription consistency checker"
        messageSubscriptionConsistencyChecker.executeConsistencyCheck()

        and: "Only the subscription with id 1004 is deleted"
        kafkaListenerEndpointRegistry.getListenerContainers().size() == 2
        kafkaListenerEndpointRegistry.listenerContainers.listenerId.contains("1003")
        !kafkaListenerEndpointRegistry.listenerContainers.listenerId.contains("1004")
    }
}
