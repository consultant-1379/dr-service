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
package com.ericsson.bos.dr.tests.unit.mappers

import com.ericsson.bos.dr.jpa.model.ListenerEntity
import com.ericsson.bos.dr.model.mappers.ListenerMessageSubscriptionEntityMapper
import com.ericsson.bos.dr.web.v1.api.model.KafkaListenerMessageSubscriptionRequestDto
import spock.lang.Specification

class ListenerMessageSubscriptionEntityMapperSpec extends Specification {

    def "Should map KafkaListenerMessageSubscriptionRequestDto to ListenerMessageSubscriptionEntity"() {
        setup: "Create KafkaListenerMessageSubscriptionRequestDto"
        KafkaListenerMessageSubscriptionRequestDto kafkaListenerMessageSubscriptionRequestDto =
                new KafkaListenerMessageSubscriptionRequestDto([name                        : "msg_1",
                                                                description                 : "message one",
                                                                messageBrokerType           : "KAFKA",
                                                                subsystemName               : "subsystem_1",
                                                                messageConsumerConfiguration: ["groupId"   : "g_1",
                                                                                               "topicNames": ["topic_1", "topic_2"]]])

        when: "Map KafkaListenerMessageSubscriptionRequestDto to ListenerMessageSubscriptionEntity"
        def actualListenerMessageSubscriptionEntity =
                new ListenerMessageSubscriptionEntityMapper(new ListenerEntity()).apply(kafkaListenerMessageSubscriptionRequestDto)

        then: "KafkaListenerMessageSubscriptionRequestDto mapped to ListenerMessageSubscriptionEntity"
        with(actualListenerMessageSubscriptionEntity) {
            name == kafkaListenerMessageSubscriptionRequestDto.name
            description == kafkaListenerMessageSubscriptionRequestDto.description
            messageBrokerType == kafkaListenerMessageSubscriptionRequestDto.messageBrokerType
            subsystemName == kafkaListenerMessageSubscriptionRequestDto.subsystemName
            consumerConfiguration.get("groupId") == kafkaListenerMessageSubscriptionRequestDto.messageConsumerConfiguration.groupId
            consumerConfiguration.get("topicNames") == kafkaListenerMessageSubscriptionRequestDto.messageConsumerConfiguration.topicNames
            listenerEntity != null
        }
    }

    def "Exception when attempt to map unsupported message broker type"() {
        setup: "Create KafkaListenerMessageSubscriptionRequestDto with unknown messageBrokerType"
        KafkaListenerMessageSubscriptionRequestDto kafkaListenerMessageSubscriptionRequestDto =
                new KafkaListenerMessageSubscriptionRequestDto([name                        : "msg_1",
                                                                description                 : "message one",
                                                                messageBrokerType           : "UNKNOWN_BROKER_TYPE",
                                                                subsystemName               : "subsystem_1",
                                                                messageConsumerConfiguration: ["groupId"   : "g_1",
                                                                                               "topicNames": ["topic_1", "topic_2"]]])

        when: "Attempt to map KafkaListenerMessageSubscriptionRequestDto to ListenerMessageSubscriptionEntity"
        new ListenerMessageSubscriptionEntityMapper(null).apply(kafkaListenerMessageSubscriptionRequestDto)

        then: "IllegalArgumentException thrown"
        thrown IllegalArgumentException
    }
}
