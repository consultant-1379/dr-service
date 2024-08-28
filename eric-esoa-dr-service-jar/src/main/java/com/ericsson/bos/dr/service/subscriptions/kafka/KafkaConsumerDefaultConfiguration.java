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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ericsson.bos.dr.jpa.model.ListenerMessageSubscriptionEntity;
import com.ericsson.bos.dr.service.subsystem.KafkaConnectionProperties;


/**
 * Returns the default configuration for a kafka consumer.
 */
@Component
public class KafkaConsumerDefaultConfiguration {

    @Value(value = "${service.message-subscriptions.kafka.auto-offset-reset:latest}")
    private String autoOffsetReset;

    @Value(value = "${service.message-subscriptions.kafka.consumer.reconnect-backoff}")
    private String reconnectBackoff;

    @Value(value = "${service.message-subscriptions.kafka.consumer.reconnect-backoff-max}")
    private String reconnectBackoffMax;

    /**
     * Returns the default configuration for a kafka consumer.
     *
     * @param listenerMessageSubscriptionEntity
     *         listener entity
     * @param kafkaConnectionProperties
     *         subsystem kafka connection properties
     * @return
     */
    public Map<String, Object> getConfig(final ListenerMessageSubscriptionEntity listenerMessageSubscriptionEntity,
            final KafkaConnectionProperties kafkaConnectionProperties) {
        final Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConnectionProperties.getBootstrapServer());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, getGroupId(listenerMessageSubscriptionEntity));
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        consumerProps.put(ConsumerConfig.RECONNECT_BACKOFF_MS_CONFIG, reconnectBackoff);
        consumerProps.put(ConsumerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, reconnectBackoffMax);
        return consumerProps;
    }

    private String getGroupId(final ListenerMessageSubscriptionEntity listenerMessageSubscriptionEntity) {
        String groupId = (String) listenerMessageSubscriptionEntity.getConsumerConfiguration().get("groupId");
        if (StringUtils.isBlank(groupId)) {
            groupId = listenerMessageSubscriptionEntity.getListenerEntity().getFeaturePack().getName() + "_" +
                      listenerMessageSubscriptionEntity.getListenerEntity().getName() + "_" + listenerMessageSubscriptionEntity.getName();
        }
        return groupId;
    }
}
