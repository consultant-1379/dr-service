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
package com.ericsson.bos.dr.tests.integration.teststeps

import com.ericsson.bos.dr.jpa.FeaturePackRepository
import com.ericsson.bos.dr.jpa.ListenerMessageSubscriptionRepository
import com.ericsson.bos.dr.jpa.model.FeaturePackEntity
import com.ericsson.bos.dr.jpa.model.ListenerEntity
import com.ericsson.bos.dr.jpa.model.ListenerMessageSubscriptionEntity
import com.ericsson.bos.dr.service.subsystem.ConnectedSystem
import com.ericsson.bos.dr.service.subsystem.KafkaConnectionProperties
import com.ericsson.bos.dr.tests.integration.utils.JsonUtils
import com.ericsson.bos.dr.web.v1.api.model.ExecuteJobResponseDto
import com.ericsson.bos.dr.web.v1.api.model.ListenerMessageSubscriptionListDto
import com.ericsson.bos.dr.web.v1.api.model.ListenerMessageSubscriptionResponseDto
import com.ericsson.bos.dr.web.v1.api.model.ListenerTriggerResponseDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions

import java.util.concurrent.atomic.AtomicReference

import static com.ericsson.bos.dr.tests.integration.utils.IOUtils.readClasspathResource
import static org.springframework.http.MediaType.APPLICATION_JSON
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@Component
class ListenersTestSteps {

    private static final String LISTENERS_TRIGGER_URL = "/discovery-and-reconciliation/v1/feature-packs/%s/listener/%s/trigger"
    private static final String LISTENERS_MESSAGE_SUBSCRIPTION_URL = "/discovery-and-reconciliation/v1/feature-packs/%s/listener/%s/message-subscriptions"

    @Autowired
    private MockMvc mockMvc

    @Autowired
    FeaturePackRepository featurePackRepository

    @Autowired
    ListenerMessageSubscriptionRepository listenerMessageSubscriptionRepository

    ListenerTriggerResponseDto trigger(String featurePackName, String listenerName, Map event) {
        final AtomicReference<ExecuteJobResponseDto> response = new AtomicReference()
        triggerResult(featurePackName, listenerName, event)
                .andExpect(status().isOk())
                .andDo(result -> response.set(JsonUtils.read(result.getResponse().getContentAsString(), ListenerTriggerResponseDto.class)))
        return response.get()
    }

    String trigger2(String featurePackName, String listenerName, Map event) {
        final AtomicReference<ExecuteJobResponseDto> response = new AtomicReference()
        triggerResult(featurePackName, listenerName, event)
                .andExpect(status().isOk())
                .andDo(result -> response.set(result.getResponse().getContentAsString()))
        return response.get()
    }

    ResultActions triggerResult(String featurePackName, String listenerName, Map event) {
        return mockMvc.perform(post(String.format(LISTENERS_TRIGGER_URL, featurePackName, listenerName))
                .contentType(APPLICATION_JSON)
                .content(JsonUtils.toJsonString(event))
                .header(HttpHeaders.AUTHORIZATION, readClasspathResource("/tokens/admin_token.json")))
    }

    ListenerMessageSubscriptionResponseDto createMessageSubscription(String featurePackName, String listenerName, Map configuration) {
        final AtomicReference<ExecuteJobResponseDto> response = new AtomicReference()
        createMessageSubscriptionResult(featurePackName, listenerName, configuration)
                .andExpect(status().isCreated())
                .andDo(result -> response.set(JsonUtils.read(result.getResponse().getContentAsString(), ListenerMessageSubscriptionResponseDto.class)))
        return response.get()
    }

    ResultActions createMessageSubscriptionResult(String featurePackName, String listenerName, Map configuration) {
        return mockMvc.perform(post(String.format(LISTENERS_MESSAGE_SUBSCRIPTION_URL, featurePackName, listenerName))
                .contentType(APPLICATION_JSON)
                .content(JsonUtils.toJsonString(configuration))
                .header(HttpHeaders.AUTHORIZATION, readClasspathResource("/tokens/admin_token.json")))
    }

    ListenerMessageSubscriptionListDto getMessageSubscriptions(String featurePackName, String listenerName) {
        final AtomicReference<ExecuteJobResponseDto> response = new AtomicReference()
        getMessageSubscriptionsResult(featurePackName, listenerName)
                .andExpect(status().isOk())
                .andDo(result -> response.set(JsonUtils.read(result.getResponse().getContentAsString(), ListenerMessageSubscriptionListDto.class)))
        return response.get()
    }

    ResultActions getMessageSubscriptionsResult(String featurePackName, String listenerName) {
        return mockMvc.perform(get(String.format(LISTENERS_MESSAGE_SUBSCRIPTION_URL, featurePackName, listenerName)))
    }


    void deleteMessageSubscription(String featurePackName, String listenerName, String messageSubscriptionId) {
        deleteMessageSubscriptionResult(featurePackName, listenerName, messageSubscriptionId)
                .andExpect(status().isNoContent())
    }

    ResultActions deleteMessageSubscriptionResult(String featurePackName, String listenerName, String messageSubscriptionId) {
        return mockMvc.perform(delete(String.format(LISTENERS_MESSAGE_SUBSCRIPTION_URL, featurePackName, listenerName) + "/" + messageSubscriptionId)
                .header(HttpHeaders.AUTHORIZATION, readClasspathResource("/tokens/admin_token.json")))
    }

    def preCreateMessageListenerSubscriptions(String featurePackName, String listenerName, List subscriptionAndSubsystemDistinguishers = [1, 2, 3]) {
        featurePackRepository.findByName(featurePackName)
                .ifPresent(featurePack -> {
                    ListenerEntity listenerEntity = featurePack.listeners.stream()
                            .filter(listener -> listener.name == listenerName)
                            .findAny().get()
                    def subscriptions = subscriptionAndSubsystemDistinguishers.collect {
                        def subscription = JsonUtils.convert(
                        createSampleMessageSubscriptionEntity("precreated_subscription_${it}", "subsystem_${it}"),
                        ListenerMessageSubscriptionEntity
                        )
                        subscription.listenerEntity = listenerEntity
                        return subscription
                    }
                    listenerMessageSubscriptionRepository.saveAllAndFlush(subscriptions)
                })
    }

    Map createSampleMessageSubscriptionEntity(String name, String subsystemName = "subsystem_1") {
        def sampleDto = createSampleMessageSubscription(name, subsystemName)
        sampleDto["consumerConfiguration"] = sampleDto["messageConsumerConfiguration"]
        sampleDto.remove("messageConsumerConfiguration")
        return sampleDto
    }

    Map createSampleMessageSubscription(String name, String subsystemName = "subsystem_1") {
        def configuration = [
            "name"                        : name,
            "description"                 : "Message subscription",
            "messageBrokerType"           : "KAFKA",
            "subsystemName"               : subsystemName,
            "messageConsumerConfiguration": [
                "groupId"   : "group_1",
                "topicNames": ["topic_1", "topic_2"]
            ]
        ]
        return configuration
    }

    ConnectedSystem<KafkaConnectionProperties> createConnectedSystem(String bootstrapServer, String subsystemName = "subsystem_1") {
        def connectedSystem = new ConnectedSystem<KafkaConnectionProperties>()

        def kafkaConnectionProperties = new KafkaConnectionProperties()
        kafkaConnectionProperties.bootstrapServer = bootstrapServer

        connectedSystem.name = subsystemName
        connectedSystem.connectionProperties = [kafkaConnectionProperties]
        return connectedSystem
    }

    ListenerMessageSubscriptionEntity createMessageEntity(Long id, String subsystem, String group, List<String> topics, String listener, String featurePack) {
        return new ListenerMessageSubscriptionEntity([name                 : "test_subscription",
                                                      id                   : id,
                                                      subsystemName        : subsystem,
                                                      consumerConfiguration: ["groupId": group, "topicNames": topics],
                                                      listenerEntity       : new ListenerEntity(
                                                              [name       : listener,
                                                               featurePack: new FeaturePackEntity(name: featurePack)])])
    }
}