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

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.RuntimeJsonMappingException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.support.KafkaUtils;

import com.ericsson.bos.dr.service.ListenersService;
import com.ericsson.bos.dr.service.utils.JSON;
import com.ericsson.bos.dr.service.utils.SpringContextHolder;

/**
 * Kafka message listener to trigger a discovery and reconciliation on receiving a message from a configured kafka message subscription
 */
public class KafkaMessageListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaMessageListener.class);

    private final List<String> topics;
    private final String featurePackName;
    private final String listenerName;
    private final Long messageSubscriptionId;
    private final ListenersService listenersService;

    /**
     * Constructor
     *
     * @param topics
     *         topics
     * @param featurePackName
     *         featurePackName
     * @param listenerName
     *         listenerName
     * @param messageSubscriptionId
     */
    public KafkaMessageListener(final List<String> topics, final String featurePackName, final String listenerName,
            final Long messageSubscriptionId) {
        this.topics = topics;
        this.featurePackName = featurePackName;
        this.listenerName = listenerName;
        this.messageSubscriptionId = messageSubscriptionId;
        this.listenersService = SpringContextHolder.getBean(ListenersService.class);
    }

    /**
     * Kafka message listener. Triggers a discovery and reconciliation on receiving a message.
     *
     * @param consumerRecord
     *         consumerRecord containing the message
     */
    public void onMessage(final ConsumerRecord<String, String> consumerRecord) {
        LOGGER.info(String.format("Received message: featurePack name: %s, listenerName: %s, group_id: %s, topic: %s, message: %s",
                featurePackName, listenerName, KafkaUtils.getConsumerGroupId(), topics, consumerRecord.value()));

        final Map<String, Object> event;
        try {
            event = JSON.read(consumerRecord.value(), Map.class);
        } catch (final IllegalStateException e) {
            throw new RuntimeJsonMappingException(e.getMessage());
        }
        final String jobId = listenersService.triggerAsync(featurePackName, listenerName, event, messageSubscriptionId);
        LOGGER.info("Job with id '{}' created", jobId);
    }
}