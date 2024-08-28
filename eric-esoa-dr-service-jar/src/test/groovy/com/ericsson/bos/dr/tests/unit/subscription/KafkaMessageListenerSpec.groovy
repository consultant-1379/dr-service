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

import com.ericsson.bos.dr.service.ListenersService
import com.ericsson.bos.dr.service.subscriptions.kafka.KafkaMessageListener
import com.ericsson.bos.dr.service.utils.SpringContextHolder
import com.fasterxml.jackson.databind.RuntimeJsonMappingException
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.context.ApplicationContext
import spock.lang.Specification

class KafkaMessageListenerSpec extends Specification {

    def "Should throw RuntimeJsonMappingException when message is not valid json"() {

        setup: "Mock Spring application context to return ListenerService bean"
        ApplicationContext applicationContextMock = Mock()
        new SpringContextHolder().setApplicationContext(applicationContextMock)
        ListenersService listenersServiceMock = Mock()
        applicationContextMock.getBean(ListenersService) >> listenersServiceMock

        and: "Mock ConsumerRecord to return invalid json value"
        ConsumerRecord consumerRecord = Mock()
        consumerRecord.value() >> "text"

        when: "Message listener is called"
        KafkaMessageListener kafkaMessageListener =
                new KafkaMessageListener(["topic1"], "fp1", "listener1", 1)
        kafkaMessageListener.onMessage(consumerRecord)

        then: "RuntimeJsonMappingException is thrown"
        thrown(RuntimeJsonMappingException)

        and: "ListenerService is never called"
        0 * listenersServiceMock._
    }
}