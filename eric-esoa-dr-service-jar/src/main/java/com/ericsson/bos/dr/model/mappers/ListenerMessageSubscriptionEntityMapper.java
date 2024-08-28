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
package com.ericsson.bos.dr.model.mappers;

import java.util.Map;

import com.ericsson.bos.dr.jpa.model.ListenerEntity;
import com.ericsson.bos.dr.jpa.model.ListenerMessageSubscriptionEntity;
import com.ericsson.bos.dr.service.utils.JSON;
import com.ericsson.bos.dr.web.v1.api.model.CreateListenerMessageSubscriptionRequest;
import com.ericsson.bos.dr.web.v1.api.model.KafkaListenerMessageSubscriptionRequestDto;

/**
 * Map <code>CreateListenerMessageSubscriptionRequest</code> to <code>ListenerMessageSubscriptionEntity</code>.
 * The class will map the specific <code>CreateListenerMessageSubscriptionRequest</code> based on the message broker type.
 */
public class ListenerMessageSubscriptionEntityMapper implements Mapper<CreateListenerMessageSubscriptionRequest, ListenerMessageSubscriptionEntity> {

    private ListenerEntity listenerEntity;

    /**
     * Constructor takes the <code>ListenerEntity</code> to reference.
     *
     * @param listenerEntity
     *         listenerEntity
     */
    public ListenerMessageSubscriptionEntityMapper(final ListenerEntity listenerEntity) {
        this.listenerEntity = listenerEntity;
    }

    @Override
    public ListenerMessageSubscriptionEntity apply(final CreateListenerMessageSubscriptionRequest listenerMessageSubscriptionDto) {
        if ("KAFKA".equalsIgnoreCase(listenerMessageSubscriptionDto.getMessageBrokerType())) {
            final ListenerMessageSubscriptionEntity listenerMessageSubscriptionEntity = new KafkaMessageBrokerTypeMapper()
                    .apply((KafkaListenerMessageSubscriptionRequestDto) listenerMessageSubscriptionDto);
            listenerMessageSubscriptionEntity.setListenerEntity(listenerEntity);
            return listenerMessageSubscriptionEntity;
        } else {
            throw new IllegalArgumentException("Unsupported message broker type: " + listenerMessageSubscriptionDto.getMessageBrokerType());
        }
    }

    /**
     * Map <code>KafkaListenerMessageSubscriptionRequestDto</code> to <code>ListenerMessageSubscriptionEntity</code>.
     */
    private static class KafkaMessageBrokerTypeMapper implements Mapper<KafkaListenerMessageSubscriptionRequestDto,
            ListenerMessageSubscriptionEntity> {

        @Override
        public ListenerMessageSubscriptionEntity apply(final KafkaListenerMessageSubscriptionRequestDto dto) {
            final ListenerMessageSubscriptionEntity listenerMessageSubscriptionEntity = new ListenerMessageSubscriptionEntity();
            listenerMessageSubscriptionEntity.setName(dto.getName());
            listenerMessageSubscriptionEntity.setDescription(dto.getDescription());
            listenerMessageSubscriptionEntity.setSubsystemName(dto.getSubsystemName());
            listenerMessageSubscriptionEntity.setMessageBrokerType(dto.getMessageBrokerType());
            listenerMessageSubscriptionEntity.setConsumerConfiguration(
                    JSON.convert(dto.getMessageConsumerConfiguration(), Map.class));
            return listenerMessageSubscriptionEntity;
        }
    }
}
