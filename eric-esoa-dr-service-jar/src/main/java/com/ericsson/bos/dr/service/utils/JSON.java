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
package com.ericsson.bos.dr.service.utils;

import static com.fasterxml.jackson.core.JsonParser.Feature.INCLUDE_SOURCE_IN_LOCATION;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * JSON operations.
 */
public abstract class JSON {

    private static final Logger LOGGER = LoggerFactory.getLogger(JSON.class);

    private static final ObjectMapper INSTANCE = new ObjectMapper().enable(INCLUDE_SOURCE_IN_LOCATION);

    private JSON() {}

    /**
     * Checks if a string is valid json
     * @param jsonStr String to be checked
     * @return boolean true (json string) or false (not json string)
     */
    public static boolean isJsonStr(final String jsonStr) {
        try {
            readObject(jsonStr);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Read json string to an appropriate object, which may
     * be a list, map, string, number or boolean.
     * @param json json value
     * @return Object
     */
    public static Object readObject(String json) {
        try {
            final var jsonNode = INSTANCE.readTree(json);
            if (jsonNode.isArray()) {
                return readList(json, Object.class);
            } else if (jsonNode.isObject()) {
                return read(json, Map.class);
            } else {
                return read(json, Object.class);
            }
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Read json string to a target class type.
     * @param value json string
     * @param type target class type
     * @param <T> target type
     * @return object of target type
     */
    public static <T> T read(final String value, final Class<T> type) {
        try {
            return INSTANCE.readValue(value, type);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Read json string to target type reference.
     * @param value json string
     * @param typeRef target TypeReference
     * @param <T> target type
     * @return Object of target type reference
     */
    public static <T> T read(final String value, final TypeReference<T> typeRef) {
        try {
            return INSTANCE.readValue(value, typeRef);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Read json array string to list of target class type.
     * @param value json array string
     * @param type target class type
     * @param <T> target type
     * @return list of target object types
     */
    public static <T> List<T> readList(final String value, final Class<T> type) {
        try {
            return INSTANCE.readValue(value,
                    INSTANCE.getTypeFactory().constructCollectionType(List.class, type));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Convert Json obect to another type.
     * @param value current value
     * @param type target value type
     * @param <T> type
     * @return Object
     */
    public static <T> T convert(Object value, Class<T> type) {
        return INSTANCE.convertValue(value, type);
    }

    /**
     * Write Object as json string
     * @param value value
     * @return json string
     */
    public static String toString(Object value) {
        try {
            return INSTANCE.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Removes whitespace and newlines from a json string.
     * If an Exception is thrown the original json string will be returned.
     *
     * @param uncompactedJson
     *         json string, containing whitespace
     * @return json string with whitespace removed.
     */
    public static String compact(final String uncompactedJson) {
        try {
            final JsonNode jsonNode = INSTANCE.readTree(uncompactedJson);
            return toString(jsonNode);
        } catch (Exception e) {
            LOGGER.trace("An error occurred when trying to compact json string: {}", uncompactedJson, e);
            return uncompactedJson;
        }
    }
}