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

package com.ericsson.bos.dr.service.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.ericsson.bos.dr.jpa.model.ListenerEntity;
import com.ericsson.bos.dr.web.v1.api.model.ListenerConfigurationDtoAllOfTriggers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Matches a trigger defined in the listener configuration based on a given event.
 */
@Component
public class TriggerMatcher {

    @Autowired
    private ListenerExpressionEvaluator expressionEvaluator;

    /**
     * Find matching trigger defined in listener configuration for an event.
     * @param listenerEntity listener entity
     * @param event event
     * @param featurePackId feature pack id
     * @return optional ListenerConfigurationDtoAllOfTriggers
     */
    public Optional<ListenerConfigurationDtoAllOfTriggers> match(ListenerEntity listenerEntity, Map<String, Object> event, long featurePackId) {
        final Map<String, Object> evaluationContext = new HashMap<>();
        evaluationContext.put("request", event);
        return listenerEntity.getConfig().getTriggers().stream()
                .filter(t -> Boolean.parseBoolean(expressionEvaluator.evaluate(t.getCondition(), evaluationContext, featurePackId)))
                .findAny();
    }
}
