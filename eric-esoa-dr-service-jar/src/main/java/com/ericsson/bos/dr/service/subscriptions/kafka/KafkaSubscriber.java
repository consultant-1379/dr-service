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

import com.ericsson.bos.dr.jpa.model.ListenerMessageSubscriptionEntity;
import com.ericsson.bos.dr.service.exceptions.DRServiceException;
import com.ericsson.bos.dr.service.exceptions.ErrorCode;
import com.ericsson.bos.dr.service.subscriptions.kafka.security.KafkaJKSManager;
import com.ericsson.bos.dr.service.subscriptions.kafka.security.KafkaConsumerTLSConfiguration;
import com.ericsson.bos.dr.service.subsystem.ConnectedSystem;
import com.ericsson.bos.dr.service.subsystem.ConnectedSystemClient;
import com.ericsson.bos.dr.service.subsystem.KafkaConnectionProperties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.RuntimeJsonMappingException;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.config.MethodKafkaListenerEndpoint;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Subscribes and unsubscribes for messages from topic(s) in a Kafka connected system.
 * Once subscribed, the received messages will be consumed by <code>KafkaMessageListener</code>.
 */
@Component
public class KafkaSubscriber {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaSubscriber.class);
    private static final String TOPIC_NAMES = "topicNames";

    @Value(value = "${service.message-subscriptions.kafka.backoff.interval}")
    private Long interval;

    @Value(value = "${service.message-subscriptions.kafka.backoff.max-failure}")
    private Long maxAttempts;

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    @Autowired
    private ConnectedSystemClient connectedSystemClient;

    @Autowired
    private KafkaConsumerDefaultConfiguration kafkaConsumerDefaultConfiguration;

    @Autowired
    private KafkaConsumerTLSConfiguration kafkaConsumerSSLConfiguration;

    @Autowired
    private KafkaJKSManager kafkaJKSManager;

    @Autowired
    private KafkaSubscriptionRegistry kafkaSubscriptionRegistry;

    @Autowired
    private KafkaConsumerValidator kafkaConsumerValidator;

    /**
     * Register message listener container
     * @param listenerMessageSubscriptionEntity listenerMessageSubscriptionEntity
     */
    public void subscribe(ListenerMessageSubscriptionEntity listenerMessageSubscriptionEntity) {
        final ConnectedSystem<KafkaConnectionProperties> connectedSystem =
                connectedSystemClient.fetchKafkaConnectedSystem(listenerMessageSubscriptionEntity.getSubsystemName());
        LOGGER.info("Kafka subscription properties: {}, connected system properties: {}",
                listenerMessageSubscriptionEntity.getConsumerConfiguration(), connectedSystem);
        final ConcurrentKafkaListenerContainerFactory<Object, Object> containerFactory =
                createConsumerFactory(listenerMessageSubscriptionEntity, connectedSystem.getConnectionProperties().get(0));
        kafkaConsumerValidator.accept(listenerMessageSubscriptionEntity, containerFactory);
        try {
            kafkaListenerEndpointRegistry.registerListenerContainer(createListenerEndpoint(listenerMessageSubscriptionEntity),
                    containerFactory, true);
            kafkaSubscriptionRegistry.add(listenerMessageSubscriptionEntity, connectedSystem);
        } catch (Exception e) {
            throw new DRServiceException(e, ErrorCode.FAILED_TO_CREATE_MESSAGE_SUBSCRIPTION,
                    connectedSystem.getConnectionProperties().get(0).getBootstrapServer(),
                    (String) containerFactory.getConsumerFactory().getConfigurationProperties().get(ConsumerConfig.GROUP_ID_CONFIG),
                    StringUtils.join(listenerMessageSubscriptionEntity.getConsumerConfiguration().get(TOPIC_NAMES)));
        }
    }

    /**
     * Unregister and destroy message listener container
     * @param messageListenerContainerId id of the message listener container
     */
    public void unsubscribe(final String messageListenerContainerId) {
        LOGGER.info("Unsubscribing from message listener: {}", messageListenerContainerId);
        try {
            final MessageListenerContainer messageListenerContainer =
                    kafkaListenerEndpointRegistry.unregisterListenerContainer(messageListenerContainerId);
            kafkaSubscriptionRegistry.remove(Long.valueOf(messageListenerContainerId));
            if (messageListenerContainer != null) {
                messageListenerContainer.destroy();
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Message listener container with id: {} is unregistered", messageListenerContainerId);
            }
        } catch (Exception e) {
            throw new DRServiceException(e, ErrorCode.FAILED_TO_DELETE_MESSAGE_SUBSCRIPTION, messageListenerContainerId);
        }
    }

    private ConcurrentKafkaListenerContainerFactory<Object, Object> createConsumerFactory(
            final ListenerMessageSubscriptionEntity listenerMessageSubscriptionEntity,
            final KafkaConnectionProperties kafkaConnectionProperties) {
        final Map<String, Object> consumerProps = new HashMap<>(
                kafkaConsumerDefaultConfiguration.getConfig(listenerMessageSubscriptionEntity, kafkaConnectionProperties));
        if (kafkaConnectionProperties.isSslEnabled()) {
            final KafkaJKSManager.JKSSpec jksStore = kafkaJKSManager.createStores(kafkaConnectionProperties, listenerMessageSubscriptionEntity);
            consumerProps.putAll(kafkaConsumerSSLConfiguration.getConfig(jksStore));
        }
        final ConcurrentKafkaListenerContainerFactory<Object, Object> containerFactory = new ConcurrentKafkaListenerContainerFactory<>();
        containerFactory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        containerFactory.setCommonErrorHandler(defaultErrorHandler());
        containerFactory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(consumerProps));
        return containerFactory;
    }

    private MethodKafkaListenerEndpoint<String, String> createListenerEndpoint(
            ListenerMessageSubscriptionEntity listenerMessageSubscriptionEntity) throws NoSuchMethodException {
        final MethodKafkaListenerEndpoint<String, String> kafkaListenerEndpoint =
                new MethodKafkaListenerEndpoint<>();
        kafkaListenerEndpoint.setId(String.valueOf(listenerMessageSubscriptionEntity.getId()));
        kafkaListenerEndpoint.setAutoStartup(true);
        kafkaListenerEndpoint.setTopics(
                ((List<String>)listenerMessageSubscriptionEntity.getConsumerConfiguration().get(TOPIC_NAMES)).toArray(new String[0]));
        kafkaListenerEndpoint.setMessageHandlerMethodFactory(new DefaultMessageHandlerMethodFactory());
        kafkaListenerEndpoint.setBean(
                new KafkaMessageListener(
                        (List<String>) listenerMessageSubscriptionEntity.getConsumerConfiguration().get(TOPIC_NAMES),
                        listenerMessageSubscriptionEntity.getListenerEntity().getFeaturePack().getName(),
                        listenerMessageSubscriptionEntity.getListenerEntity().getName(),
                        listenerMessageSubscriptionEntity.getId()));
        kafkaListenerEndpoint.setMethod(
                KafkaMessageListener.class.getMethod("onMessage", ConsumerRecord.class));
        return kafkaListenerEndpoint;
    }

    private DefaultErrorHandler defaultErrorHandler() {
        final BackOff fixedBackOff = new FixedBackOff(interval, maxAttempts);
        final DefaultErrorHandler errorHandler = new DefaultErrorHandler((consumerRecord, exception) -> {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Failed to process kafka message. Topic: " + consumerRecord.topic()
                                + " Offset: " + consumerRecord.offset()
                                + " Value:" + consumerRecord.value(),
                        exception);
            }
        }, fixedBackOff);
        errorHandler.addNotRetryableExceptions(DRServiceException.class, RuntimeJsonMappingException.class);
        return errorHandler;
    }
}