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
package com.ericsson.bos.dr.web;

import java.util.Map;

import com.ericsson.bos.dr.service.ListenersService;
import com.ericsson.bos.dr.web.v1.api.ListenersApi;
import com.ericsson.bos.dr.web.v1.api.model.CreateListenerMessageSubscriptionRequest;
import com.ericsson.bos.dr.web.v1.api.model.ListenerMessageSubscriptionListDto;
import com.ericsson.bos.dr.web.v1.api.model.ListenerMessageSubscriptionResponseDto;
import com.ericsson.bos.dr.web.v1.api.model.ListenerTriggerResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Listener Controller.
 */
@RestController
public class ListenersController implements ListenersApi {

    @Autowired
    private ListenersService listenersService;

    @Override
    public ResponseEntity<ListenerTriggerResponseDto> triggerListener(String featurePackName, String listenerName, Map<String, Object> event) {
        return ResponseEntity.ok(listenersService.trigger(featurePackName, listenerName, event));
    }

    @Override
    public ResponseEntity<ListenerMessageSubscriptionResponseDto> createListenerMessageSubscription(final String featurePackName,
            final String listenerName,
            final CreateListenerMessageSubscriptionRequest createListenerMessageSubscriptionRequest) {
        return new ResponseEntity<>(
                listenersService.createMessageSubscription(
                        featurePackName, listenerName, createListenerMessageSubscriptionRequest), HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<Void> deleteListenerMessageSubscription(final String featurePackName, final String listenerName,
            final String messageSubscriptionId) {
        listenersService.deleteMessageSubscription(featurePackName, listenerName, messageSubscriptionId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ListenerMessageSubscriptionListDto> getListenerMessageSubscriptions(final String featurePackName,
            final String listenerName) {
        return ResponseEntity.ok(listenersService.getMessageSubscriptions(featurePackName, listenerName));
    }

}