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

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import jakarta.annotation.PostConstruct;

/**
 * Converts numbers and booleans to their string representation.
 * <p>
 * For example will convert 88 to "88", or false to "false".
 * </p>
 * <p>
 * All numbers and booleans in lists and maps will be converted.
 * </p>
 */
@Component
public class NumberAndBooleanStringifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(NumberAndBooleanStringifier.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    private void configureObjectMapper() {
        final var module = new SimpleModule();
        module.addSerializer(Boolean.class, new BooleanSerializer());
        module.addSerializer(Number.class, new NumberSerializer());
        objectMapper.registerModule(module);
    }

    /**
     * Converts numbers and booleans to their string representation.
     * Converts if object is of type Boolean or any subclass of Number.
     * If object passed is a map or list then all number and boolean elements
     * contained will be converted.
     *
     * @param object
     *         number, boolean map or list
     * @return object with numbers and booleans in string format
     */
    public Object toString(Object object) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Converting numbers and booleans: {}", object);
        }
        if (object instanceof String) {
            return object;
        }
        if (object instanceof Map) {
            object = doMap((Map<String, Object>) object);
        } else if (object instanceof List) {
            object = doList((List<Object>) object);
        } else if (object instanceof Number) {
            object = String.valueOf(object);
        } else if (object instanceof Boolean) {
            object = String.valueOf(object);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Converted numbers and booleans: {}", object);
        }
        return object;
    }

    private Map<String, Object> doMap(final Map<String, Object> map) {
        return objectMapper.convertValue(map, Map.class);
    }

    private List<Object> doList(final List<Object> list) {
        return objectMapper.convertValue(list, List.class);
    }

    private static class BooleanSerializer extends JsonSerializer<Boolean> {
        @Override
        public void serialize(final Boolean value, final JsonGenerator gen, final SerializerProvider serializers)
                throws IOException {
            gen.writeString(String.valueOf(value));
        }
    }

    private static class NumberSerializer extends JsonSerializer<Number> {
        @Override
        public void serialize(final Number value, final JsonGenerator gen, final SerializerProvider serializers)
                throws IOException {
            gen.writeString(String.valueOf(value));
        }
    }
}
