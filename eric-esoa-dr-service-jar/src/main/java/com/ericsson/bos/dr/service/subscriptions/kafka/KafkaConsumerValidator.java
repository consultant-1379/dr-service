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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;

import com.ericsson.bos.dr.jpa.model.KafkaConsumerConfiguration;
import com.ericsson.bos.dr.jpa.model.ListenerMessageSubscriptionEntity;
import com.ericsson.bos.dr.service.exceptions.DRServiceException;
import com.ericsson.bos.dr.service.exceptions.ErrorCode;
import com.ericsson.bos.dr.service.utils.JSON;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.errors.SslAuthenticationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.stereotype.Component;

/**
 * Validates the kafka consumer configuration.
 * <ol>
 *     <li>Validates the connection to the configured broker.</li>
 *     <li>Validates the configured topics exist on the broker.</li>
 * </ol>
 */
@Component
public class KafkaConsumerValidator implements BiConsumer<ListenerMessageSubscriptionEntity,
        ConcurrentKafkaListenerContainerFactory<Object, Object>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaConsumerValidator.class);

    @Value("${service.message-subscriptions.kafka.list-topics-api-timeout}")
    private int listTopicRequestTimeout;

    @Override
    public void accept(ListenerMessageSubscriptionEntity listenerMessageSubscriptionEntity,
                       ConcurrentKafkaListenerContainerFactory<Object, Object> containerFactory) {
        try (AdminClient kafkaAdminClient = AdminClient.create(getConsumerProperties(containerFactory))) {
            final Set<String> brokerTopics = kafkaAdminClient.listTopics().names().get();
            checkTopicsExist(listenerMessageSubscriptionEntity, brokerTopics);
        } catch (ExecutionException e) {
            final String bootstrapServers = containerFactory.getConsumerFactory().getConfigurationProperties()
                    .get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG).toString();
            LOGGER.error("Error listing topics on broker: " + bootstrapServers, e);
            if (e.getCause() instanceof SslAuthenticationException) {
                throw new DRServiceException(ErrorCode.KAFKA_SSL_AUTH_ERROR, bootstrapServers, ExceptionUtils.getRootCause(e).getMessage());
            }
            throw new DRServiceException(ErrorCode.KAKFA_BROKER_NOT_AVAILABLE, bootstrapServers);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private Map<String, Object> getConsumerProperties(final ConcurrentKafkaListenerContainerFactory<Object, Object> containerFactory) {
        final Map<String, Object> consumerProperties = new HashMap<>(containerFactory.getConsumerFactory().getConfigurationProperties());
        consumerProperties.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, listTopicRequestTimeout);
        consumerProperties.put(ConsumerConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, listTopicRequestTimeout);
        return consumerProperties;
    }

    private void checkTopicsExist(final ListenerMessageSubscriptionEntity listenerMessageSubscriptionEntity,
                                  final Set<String> brokerTopics) {
        final KafkaConsumerConfiguration kafkaConsumerConfiguration = JSON.convert(listenerMessageSubscriptionEntity.getConsumerConfiguration(),
                KafkaConsumerConfiguration.class);
        final List<String> missingTopics = kafkaConsumerConfiguration.topicNames().stream()
                .filter(topic -> !brokerTopics.contains(topic)).toList();
        if (!missingTopics.isEmpty()) {
            throw new DRServiceException(ErrorCode.KAFKA_TOPIC_NOT_FOUND,  missingTopics.toString());
        }
    }
}