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

import com.ericsson.bos.dr.jpa.model.ListenerMessageSubscriptionEntity;
import com.ericsson.bos.dr.web.v1.api.model.ListenerMessageSubscriptionDto;

/**
 * Map <code>ListenerMessageSubscriptionEntity</code> to <code>ListenerMessageSubscriptionDto</code>.
 */
public class ListenerMessageSubscriptionDtoMapper
        implements Mapper<ListenerMessageSubscriptionEntity, ListenerMessageSubscriptionDto> {

    @Override
    public ListenerMessageSubscriptionDto apply(final ListenerMessageSubscriptionEntity listenerMessageConsumerEntity) {
        final ListenerMessageSubscriptionDto listenerMessageSubscriptionDto = new ListenerMessageSubscriptionDto();
        listenerMessageSubscriptionDto.setId(String.valueOf(listenerMessageConsumerEntity.getId()));
        listenerMessageSubscriptionDto.setName(listenerMessageConsumerEntity.getName());
        listenerMessageSubscriptionDto.setDescription(listenerMessageConsumerEntity.getDescription());
        listenerMessageSubscriptionDto.setSubsystemName(listenerMessageConsumerEntity.getSubsystemName());
        listenerMessageSubscriptionDto.setMessageBrokerType(listenerMessageConsumerEntity.getMessageBrokerType());
        listenerMessageSubscriptionDto.messageConsumerConfiguration(listenerMessageConsumerEntity.getConsumerConfiguration());
        return listenerMessageSubscriptionDto;
    }
}
