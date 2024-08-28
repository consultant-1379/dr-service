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

package com.ericsson.bos.dr.service.compare.filters;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections4.keyvalue.MultiKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Builds a <code>MultiKey</code> instance using values from the given property map.
 * If a property value is a number or boolean type then coverts them to a string. This reason for doing this
 * is that a number/boolean property in the source may be in string format in the target and vice-versa.
 */
@Component
public class MultiKeyBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiKeyBuilder.class);

    @Autowired
    private NumberAndBooleanStringifier numberAndBooleanStringifier;

    /**
     * Build a <code>MultiKey</code> instance using values from the given property map.
     * Number and boolean values will be converted to their string representation, including
     * those numbers and booleans contained in maps or lists.
     * If any of the values in the property map are null then an empty optional will be returned.
     *
     * @param properties
     *         map of properties
     * @param args
     *         arguments whose values will be used as keys in the <code>MultiKey</code>
     * @return Optional MultiKey
     */
    public Optional<MultiKey<Object>> build(final Map<String, Object> properties, final List<String> args) {
        final List<Object> keys = new LinkedList<>();
        for (final String arg : args) {
            Object value = properties.get(arg);
            if (value == null) {
                LOGGER.warn("When building multikey, value for argument {} is null in properties {}", arg, properties);
                return Optional.empty();
            }
            value = numberAndBooleanStringifier.toString(value);
            keys.add(value);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("MultiKey keys: {}", keys);
        }
        return Optional.of(new MultiKey<>(keys.toArray()));
    }

}
