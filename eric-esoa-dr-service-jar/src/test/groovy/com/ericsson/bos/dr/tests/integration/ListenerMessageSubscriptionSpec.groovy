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
package com.ericsson.bos.dr.tests.integration

import com.ericsson.bos.dr.jpa.model.ListenerMessageSubscriptionEntity
import com.ericsson.bos.dr.service.exceptions.DRServiceException
import com.ericsson.bos.dr.service.subscriptions.kafka.KafkaSubscriber
import com.ericsson.bos.dr.service.subscriptions.kafka.KafkaSubscriptionRegistry
import com.ericsson.bos.dr.service.subsystem.ConnectedSystemClient
import com.ericsson.bos.dr.service.utils.JKS
import com.ericsson.bos.dr.tests.integration.utils.IOUtils
import com.ericsson.bos.dr.web.v1.api.model.FeaturePackDto
import com.ericsson.bos.dr.web.v1.api.model.ListenerMessageSubscriptionListDto
import com.ericsson.bos.dr.web.v1.api.model.ListenerMessageSubscriptionResponseDto
import org.spockframework.spring.SpringBean
import org.spockframework.spring.SpringSpy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.web.servlet.ResultActions
import spock.lang.Unroll

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import static com.ericsson.bos.dr.service.exceptions.ErrorCode.BAD_REQUEST_PARAM
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.FAILED_TO_DELETE_MESSAGE_SUBSCRIPTION
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.FP_NOT_FOUND
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.GENERAL_ERROR
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.KAFKA_TOPIC_NOT_FOUND
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.KAKFA_BROKER_NOT_AVAILABLE
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.LISTENER_MESSAGE_SUBSCRIPTION_NOT_FOUND
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.LISTENER_NOT_FOUND
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@EmbeddedKafka(kraft = false, partitions = 1, brokerProperties = ["log.dir=target/embedded-kafka-1"], topics= ["topic_1", "topic_2"])
class ListenerMessageSubscriptionSpec extends BaseSpec {

    @Autowired
    KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry

    @SpringBean
    ConnectedSystemClient connectedSystemClient = Mock(ConnectedSystemClient)

    @SpringSpy
    KafkaSubscriber kafkaSubscriber

    @Autowired
    KafkaSubscriptionRegistry kafkaSubscriptionRegistry

    @Value("\${service.message-subscriptions.kafka.tls.jks-dir}")
    private String jksBaseDir;

    @Value('${spring.embedded.kafka.brokers}')
    private String brokerAddresses

    Map configuration

    def setup() {
        configuration = listenersTestSteps.createSampleMessageSubscription("Msg_subscription_1", "subsystem_1")
        connectedSystemClient.fetchKafkaConnectedSystem(configuration["subsystemName"]) >>
                listenersTestSteps.createConnectedSystem(brokerAddresses)
    }

    def cleanup() {
        kafkaListenerEndpointRegistry.getListenerContainerIds().each{ id ->
            kafkaListenerEndpointRegistry.unregisterListenerContainer(id)
        }
    }

    def "Should create listener message subscription"() {
        setup: "Upload feature pack containing a listener"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-6", "fp-6-${System.currentTimeMillis()}")
        String listenerName = featurePackDto.listeners.first().name
        String listenerId = featurePackDto.listeners.first().id

        when: "Create message subscription"
        ListenerMessageSubscriptionResponseDto response = listenersTestSteps.createMessageSubscription(
                featurePackDto.name, listenerName, configuration)

        then: "Listener message subscription created"
        ListenerMessageSubscriptionEntity listenerMessageSubscriptionEntity =
                listenerMessageSubscriptionRepository.findById(Long.valueOf(response.id)).get()
        with(listenerMessageSubscriptionEntity) {
            name == configuration.get("name")
            description == configuration.get("description")
            messageBrokerType == configuration.get("messageBrokerType")
            subsystemName == configuration.get("subsystemName")
            consumerConfiguration == configuration.get("messageConsumerConfiguration")
            listenerEntity.id == Long.valueOf(listenerId)
        }

        and: "kafka listener container is created"
        kafkaListenerEndpointRegistry.getAllListenerContainers().asList().size() == 1
        var messageListenerContainer = kafkaListenerEndpointRegistry.getAllListenerContainers().asList().get(0)
        messageListenerContainer.containerProperties.topics == ["topic_1", "topic_2"].toArray()
        messageListenerContainer.groupId == "group_1"

        and: "Kafka subscription added to registry"
        kafkaSubscriptionRegistry.get(response.id.toLong()).isPresent()

        and: "Response contains message consumer id"
        response.id == String.valueOf(listenerMessageSubscriptionEntity.id)
    }

    def "Should roll back database change when creating message subscription failed"() {
        setup: "Upload feature pack containing a listener"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-6", "fp-6-${System.currentTimeMillis()}")
        String listenerName = featurePackDto.listeners.first().name

        and: "Mock KafkaSubscriber to throw error"
        kafkaSubscriber.subscribe(_) >> {{ throw new IllegalStateException("Error!") }}

        when: "Create message subscription"
        Map configuration = listenersTestSteps.createSampleMessageSubscription("Msg_subscription_failed", "subsystem_2")
        ResultActions result = listenersTestSteps.createMessageSubscriptionResult(featurePackDto.name, listenerName, configuration)

        then: "ListenerEntity is deleted from database, 500 returned"
        listenerMessageSubscriptionRepository.findAll().isEmpty()
        result.andExpect(status().is(500))
              .andExpect(jsonPath("\$.errorCode").value(GENERAL_ERROR.errorCode))
    }

    def "Create listener message subscription with invalid feature pack returns 404"() {
        when: "Create message subscription with unknown feature pack"
        ResultActions result = listenersTestSteps
                .createMessageSubscriptionResult("unknown_fp", "listener_1", configuration)

        then: "Response is not found"
        result.andExpect(status().is(404))
                .andExpect(jsonPath("\$.errorCode").value(FP_NOT_FOUND.errorCode))
    }

    def "Create listener message subscription with invalid listener returns 404"() {
        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-6", "fp-6-${System.currentTimeMillis()}")

        when: "Create message subscription with unknown listener"
        ResultActions result = listenersTestSteps
                .createMessageSubscriptionResult(featurePackDto.name, "unknown_listener", configuration)

        then: "Response is not found"
        result.andExpect(status().is(404))
                .andExpect(jsonPath("\$.errorCode").value(LISTENER_NOT_FOUND.errorCode))
    }

    @Unroll
    def "Create listener message subscription with bad configuration data returns 400"() {
        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-6", "fp-6-${System.currentTimeMillis()}")
        String listenerName = featurePackDto.listeners.first().name

        when: "Create message subscription with bad configuration data"
        Map configuration = ["name"                        : name,
            "description"                 : "Message subscription",
            "messageBrokerType"           : messageBrokerType,
            "subsystemName"               : subsystemName,
            "messageConsumerConfiguration": messageConsumerConfiguration
        ]
        ResultActions result = listenersTestSteps
                .createMessageSubscriptionResult(featurePackDto.name, listenerName, configuration)

        then: "Response is bad data"
        result.andExpect(status().is(400))
                .andExpect(jsonPath("\$.errorCode").value(BAD_REQUEST_PARAM.errorCode))

        where:
        name    | messageBrokerType | subsystemName | messageConsumerConfiguration
        null    | "KAFKA"           | "subsystem_1" | ["clientId": "123", "topicNames": ["topic_1"]]
        ""      | "KAFKA"           | "subsystem_1" | ["clientId": "123", "topicNames": ["topic_1"]]
        "msg_1" | "KAFKA"           | "subsystem_1" | null
        "msg_1" | "KAFKA"           | "subsystem_1" | ""
        "msg_1" | "KAFKA"           | "subsystem_1" | ["clientId": "123", "topicNames": []]
        "msg_1" | "KAFKA"           | "subsystem_1" | ["clientId": "123", "topicNames": null]
        "msg_1" | "NOT_KAFKA"       | "subsystem_1" | ["clientId": "123", "topicNames": ["topic_1"]]
        "msg_1" | "KAFKA"           | null          | ["clientId": "123", "topicNames": ["topic_1"]]
        "msg_1" | "KAFKA"           | ""            | ["clientId": "123", "topicNames": ["topic_1"]]
    }

    def "Create listener message subscription with duplicate name returns 400"() {
        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-6", "fp-6-${System.currentTimeMillis()}")
        String listenerName = featurePackDto.listeners.first().name

        and: "Create message subscription"
        listenersTestSteps.createMessageSubscriptionResult(featurePackDto.name, listenerName, configuration)

        when: "Create message subscription with duplicate name"
        ResultActions result = listenersTestSteps
                .createMessageSubscriptionResult(featurePackDto.name, listenerName, configuration)

        then: "Response is bad data"
        result.andExpect(status().is(400))
                .andExpect(jsonPath("\$.errorCode").value(BAD_REQUEST_PARAM.errorCode))
    }

    def "Can create message subscriptions with same name on different listeners"() {
        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-6", "fp-6-${System.currentTimeMillis()}")
        String listenerNameOne = featurePackDto.listeners[0].name
        String listenerNameTwo = featurePackDto.listeners[1].name

        and: "Create message subscription on listener one"
        listenersTestSteps.createMessageSubscriptionResult(featurePackDto.name, listenerNameOne, configuration)

        when: "Create message subscription on listener two using same name as listener one"
        ResultActions result = listenersTestSteps
                .createMessageSubscriptionResult(featurePackDto.name, listenerNameTwo, configuration)

        then: "Response is ok"
        result.andExpect(status().is(201))
    }

    def "Should list all message subscriptions for a listener"() {
        setup: "Upload feature pack containing a listener"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-6", "fp-6-${System.currentTimeMillis()}")
        String listenerNameOne = featurePackDto.listeners[0].name
        String listenerNameTwo = featurePackDto.listeners[1].name

        and: "Create a number of message subscriptions for a listener"
        Map configurationOne = listenersTestSteps.createSampleMessageSubscription("Msg_subscription_1", "subsystem_1")
        Map configurationTwo = listenersTestSteps.createSampleMessageSubscription("Msg_subscription_2", "subsystem_1")
        Map configurationThree = listenersTestSteps.createSampleMessageSubscription("Msg_subscription_3", "subsystem_1")
        listenersTestSteps.createMessageSubscription(featurePackDto.name, listenerNameOne, configurationOne)
        listenersTestSteps.createMessageSubscription(featurePackDto.name, listenerNameOne, configurationTwo)
        listenersTestSteps.createMessageSubscription(featurePackDto.name, listenerNameOne, configurationThree)

        when: "Get all message subscriptions for a listener"
        ListenerMessageSubscriptionListDto listenerMessageSubscriptionsOne = listenersTestSteps.getMessageSubscriptions(
                featurePackDto.name, listenerNameOne)
        ListenerMessageSubscriptionListDto listenerMessageSubscriptionsTwo = listenersTestSteps.getMessageSubscriptions(
                featurePackDto.name, listenerNameTwo)

        then: "List of message subscriptions as expected"
        listenerMessageSubscriptionsOne.items.collect { it.name }.sort() ==
        [
            "Msg_subscription_1",
            "Msg_subscription_2",
            "Msg_subscription_3"
        ]
        listenerMessageSubscriptionsTwo.items == []
    }

    def "Should delete listener message subscription"() {
        setup: "Upload feature pack containing a listener"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-6", "fp-6-${System.currentTimeMillis()}")
        String listenerName = featurePackDto.listeners.first().name

        when: "Create listener message subscription"
        ListenerMessageSubscriptionResponseDto response = listenersTestSteps.createMessageSubscription(
                featurePackDto.name, listenerName, configuration)

        then: "Message subscription exists in database"
        listenersTestSteps.getMessageSubscriptions(featurePackDto.name, listenerName)
                .items.collect { it.name } == ["Msg_subscription_1"]

        and: "kafka listener container is created"
        kafkaListenerEndpointRegistry.getAllListenerContainers().asList().size() == 1

        when: "Delete listener message subscription"
        listenersTestSteps.deleteMessageSubscription(featurePackDto.name, listenerName, response.id)

        then: "Listener message subscription deleted from database"
        listenersTestSteps.getMessageSubscriptions(featurePackDto.name, listenerName).items == []

        and: "kafka listener container is deleted"
        kafkaListenerEndpointRegistry.getAllListenerContainers().asList().size() == 0

        and: "kafka subscription is removed from registry"
        kafkaSubscriptionRegistry.get(response.id.toLong()).isPresent() == false
    }

    def "Should fail to delete message subscription in database if kafka listener cannot be deleted"() {
        setup: "Upload feature pack containing a listener"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-6", "fp-6-${System.currentTimeMillis()}")
        String listenerName = featurePackDto.listeners.first().name

        and: "Mock failure of kafkaSubscriber unsubscribe() call"
        kafkaSubscriber.unsubscribe(_) >> { throw new DRServiceException(FAILED_TO_DELETE_MESSAGE_SUBSCRIPTION, "Msg_subscription_1") }

        and: "Create listener message subscription"
        ListenerMessageSubscriptionResponseDto response = listenersTestSteps.createMessageSubscription(
                featurePackDto.name, listenerName, configuration)

        when: "Delete listener message subscription"
        ResultActions result = listenersTestSteps.deleteMessageSubscriptionResult(featurePackDto.name, listenerName, response.id)

        then: "Delete fails with 500 error and FAILED_TO_DELETE_MESSAGE_SUBSCRIPTION message"
        result.andExpect(status().is(500))
                .andExpect(jsonPath("\$.errorCode").value(FAILED_TO_DELETE_MESSAGE_SUBSCRIPTION.errorCode))

        and: "Listener message subscription is not deleted from database"
        listenersTestSteps.getMessageSubscriptions(featurePackDto.name, listenerName)
                .items.collect { it.name } == ["Msg_subscription_1"]

        and: "kafka listener container is not deleted"
        kafkaListenerEndpointRegistry.getAllListenerContainers().asList().size() == 1
    }

    def "Should cleanup JKS files and directories when listener message subscriptions are deleted"() {

        setup: "Upload feature pack containing a listener"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-6", "fp-6-${System.currentTimeMillis()}")
        String listener_1_name = featurePackDto.listeners.first().name
        String listener_2_name = featurePackDto.listeners[1].name

        and: "Create two message subscriptions"
        Map configurationOne = listenersTestSteps.
                createSampleMessageSubscription("msg_subscription_1", "subsystem_1")
        ListenerMessageSubscriptionResponseDto subscriptionOne = listenersTestSteps.
                createMessageSubscription(featurePackDto.name, listener_1_name, configurationOne)
        Map configurationTwo = listenersTestSteps.
                createSampleMessageSubscription("msg_subscription_2", "subsystem_1")
        ListenerMessageSubscriptionResponseDto subscriptionTwo = listenersTestSteps.
                createMessageSubscription(featurePackDto.name, listener_2_name, configurationTwo)

        and: "For each message subscription write associated trust and key JKS to file system"
        ListenerMessageSubscriptionEntity subscriptionEntityOne = listenerMessageSubscriptionRepository
                .findById(Long.parseLong(subscriptionOne.id)).get()
        JKS.write(IOUtils.readClasspathResourceBytes("/kafka/certs/ca.p12"),
                "password", createJKSPath("truststore",subscriptionEntityOne))
        JKS.write(IOUtils.readClasspathResourceBytes("/kafka/certs/ca.p12"),
                "password", createJKSPath("keystore",subscriptionEntityOne))

        ListenerMessageSubscriptionEntity subscriptionEntityTwo = listenerMessageSubscriptionRepository
                .findById(Long.parseLong(subscriptionTwo.id)).get()
        JKS.write(IOUtils.readClasspathResourceBytes("/kafka/certs/ca.p12"),
                "password", createJKSPath("truststore",subscriptionEntityTwo))
        JKS.write(IOUtils.readClasspathResourceBytes("/kafka/certs/ca.p12"),
                "password", createJKSPath("keystore",subscriptionEntityTwo))

        and: "Trust and key JKS files are on file system for subscription one"
        assertJKSExists("truststore", subscriptionEntityOne)
        assertJKSExists("keystore", subscriptionEntityOne)

        and: "Trust and key JKS files are on file system for subscription two"
        assertJKSExists("truststore", subscriptionEntityTwo)
        assertJKSExists("keystore", subscriptionEntityTwo)

        when: "Delete subscription one"
        listenerMessageSubscriptionRepository.deleteById(Long.parseLong(subscriptionOne.id))

        then: "Trust and key JKS files have been removed from the file system for subscription one"
        assertJKSDoesNotExist("truststore", subscriptionEntityOne)
        assertJKSDoesNotExist("keystore", subscriptionEntityOne)

        and: "Trust and key JKS files remain on the file system for subscription two"
        assertJKSExists("truststore", subscriptionEntityTwo)
        assertJKSExists("keystore", subscriptionEntityTwo)

        when: "Delete subscription two"
        listenerMessageSubscriptionRepository.deleteById(Long.parseLong(subscriptionTwo.id))

        then: "Trust and key JKS files have been removed from the file system for subscription two"
        assertJKSDoesNotExist("truststore", subscriptionEntityTwo)
        assertJKSDoesNotExist("keystore", subscriptionEntityTwo)

        and: "Directories cleaned up, as no remaining JKS files for the feature pack"
        assertJKSFeaturePackDirDoesNotExist(featurePackDto.name)
    }

    def "Listener message subscriptions should be deleted when feature pack is deleted"() {
        setup: "Upload feature pack containing a listener"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-6", "fp-6-${System.currentTimeMillis()}")
        String listenerName = featurePackDto.listeners.first().name

        and: "Create message subscription"
        ListenerMessageSubscriptionResponseDto response = listenersTestSteps.createMessageSubscription(
                featurePackDto.name, listenerName, configuration)

        and: "Message subscription exists"
        listenersTestSteps.getMessageSubscriptions(featurePackDto.name, listenerName)
                .items.collect { it.name } == ["Msg_subscription_1"]

        when: "Delete feature pack"
        featurePackTestSteps.deleteFeaturePack(featurePackDto.id)

        then: "Listener message subscription deleted"
        listenerMessageSubscriptionRepository.findAll() == []
    }

    def "Delete listener message subscription with invalid feature pack returns 404"() {
        when: "Delete message subscription with unknown feature pack"
        ResultActions result = listenersTestSteps.
                deleteMessageSubscriptionResult("unknown_fp", "listener_1", "msg_sub_1")

        then: "Response is not found"
        result.andExpect(status().is(404))
                .andExpect(jsonPath("\$.errorCode").value(FP_NOT_FOUND.errorCode))
    }

    def "Delete listener message subscription with invalid listener returns 404"() {
        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-6", "fp-6-${System.currentTimeMillis()}")

        when: "Delete message subscription with unknown listener"
        ResultActions result = listenersTestSteps
                .deleteMessageSubscriptionResult(featurePackDto.name, "unknown_listener", "msg_sub_1")

        then: "Response is not found"
        result.andExpect(status().is(404))
                .andExpect(jsonPath("\$.errorCode").value(LISTENER_NOT_FOUND.errorCode))
    }

    def "Delete listener message subscription with invalid subscription id returns 404"() {
        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-6", "fp-6-${System.currentTimeMillis()}")
        String listenerName = featurePackDto.listeners.first().name

        when: "Delete message subscription with unknown id"
        ResultActions result = listenersTestSteps
                .deleteMessageSubscriptionResult(featurePackDto.name, listenerName, "unknown_msg_sub")

        then: "Response is not found"
        result.andExpect(status().is(404))
                .andExpect(jsonPath("\$.errorCode").value(LISTENER_MESSAGE_SUBSCRIPTION_NOT_FOUND.errorCode))
    }

    def "Should fail to create message subscription when broker is not available"() {
        setup: "Upload feature pack containing a listener"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-6", "fp-6-${System.currentTimeMillis()}")
        String listenerName = featurePackDto.listeners.first().name

        and: "Mock connectedSystemClient to return system with unreachable broker url"
        configuration = listenersTestSteps.createSampleMessageSubscription("Msg_subscription_100", "subsystem_100")
        connectedSystemClient.fetchKafkaConnectedSystem(configuration["subsystemName"]) >>
                listenersTestSteps.createConnectedSystem("localhost:9999")

        when: "Create message subscription"
        ResultActions result = listenersTestSteps.createMessageSubscriptionResult(featurePackDto.name, listenerName, configuration)

        then: "ListenerEntity is deleted from database, 500 returned"
        listenerMessageSubscriptionRepository.findAll().isEmpty()
        result.andExpect(status().is(500))
                .andExpect(jsonPath("\$.errorCode").value(KAKFA_BROKER_NOT_AVAILABLE.errorCode))
    }

    def "Should fail to create message subscription when topics do not exist"() {
        setup: "Upload feature pack containing a listener"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-6", "fp-6-${System.currentTimeMillis()}")
        String listenerName = featurePackDto.listeners.first().name

        when: "Create message subscription"
        configuration = listenersTestSteps.createSampleMessageSubscription("Msg_subscription_1", "subsystem_1")
        configuration['messageConsumerConfiguration']['topicNames'] = ["topicX1", "topicX2"]
        ResultActions result = listenersTestSteps.createMessageSubscriptionResult(featurePackDto.name, listenerName, configuration)

        then: "ListenerEntity is deleted from database, 500 returned"
        listenerMessageSubscriptionRepository.findAll().isEmpty()
        result.andExpect(status().is(500))
                .andExpect(jsonPath("\$.errorCode").value(KAFKA_TOPIC_NOT_FOUND.errorCode))
    }

    boolean assertJKSExists(String jksName, ListenerMessageSubscriptionEntity listenerMsgSubscription) {
        String featurePackName = listenerMsgSubscription.getListenerEntity().getFeaturePack().name
        String listenerName = listenerMsgSubscription.getListenerEntity().name
        String subscriptionName = listenerMsgSubscription.name
        return Files.exists(Paths.get(jksBaseDir, featurePackName, listenerName, subscriptionName, jksName + ".jks"))
    }

    boolean assertJKSDoesNotExist(String jksName, ListenerMessageSubscriptionEntity listenerMsgSubscription) {
        return !assertJKSExists(jksName, listenerMsgSubscription)
    }

    boolean assertJKSFeaturePackDirDoesNotExist(String featurePackName) {
        return !Files.exists(Paths.get(jksBaseDir, featurePackName))
    }

    Path createJKSPath(final String storeName, final ListenerMessageSubscriptionEntity listenerMsgSubscriptionEntity) {
        return Paths.get(jksBaseDir,
                listenerMsgSubscriptionEntity.getListenerEntity().getFeaturePack().getName(),
                listenerMsgSubscriptionEntity.getListenerEntity().getName(),
                listenerMsgSubscriptionEntity.getName(),
                storeName + ".jks");
    }
}
