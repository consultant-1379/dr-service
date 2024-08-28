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

import com.ericsson.bos.dr.jpa.model.ListenerMessageSubscriptionEntity
import com.ericsson.bos.dr.model.mappers.ListenerMessageSubscriptionDtoMapper
import spock.lang.Specification

class ListenerMessageSubscriptionDtoMapperSpec extends Specification {

    def "Should map ListenerMessageSubscriptionEntity to ListenerMessageSubscriptionDto"() {

        setup: "Create ListenerMessageSubscriptionEntity"
        ListenerMessageSubscriptionEntity ListenerMessageSubscriptionEntity =
                new ListenerMessageSubscriptionEntity([id                   : 123,
                                                       name                 : "msg_1",
                                                       description          : "message one",
                                                       messageBrokerType    : "KAFKA",
                                                       subsystemName        : "subsystem_1",
                                                       consumerConfiguration: ["groupId": "g_1"]])

        when: "Map ListenerMessageSubscriptionEntity to ListenerMessageSubscriptionDto"
        def actualListenerMessageSubscriptionDto = new ListenerMessageSubscriptionDtoMapper().apply(ListenerMessageSubscriptionEntity)

        then: "ListenerMessageSubscriptionEntity mapped to ListenerMessageSubscriptionDto"
        with(actualListenerMessageSubscriptionDto) {
            id == String.valueOf(ListenerMessageSubscriptionEntity.id)
            name == ListenerMessageSubscriptionEntity.name
            description == ListenerMessageSubscriptionEntity.description
            messageBrokerType == ListenerMessageSubscriptionEntity.messageBrokerType
            subsystemName == ListenerMessageSubscriptionEntity.subsystemName
            messageConsumerConfiguration == ListenerMessageSubscriptionEntity.consumerConfiguration
        }

    }
}
