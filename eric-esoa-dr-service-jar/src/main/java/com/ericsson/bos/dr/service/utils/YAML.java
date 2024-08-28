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

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

/**
 * Common Yaml operations.
 */
public abstract class YAML {

    private static final ObjectMapper INSTANCE;

    private YAML() {}

    static {
        INSTANCE = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
    }

    /**
     * Read yaml value to an object of specific class type.
     * @param yaml the yaml data
     * @param type the target class type
     * @param <T> the target class type
     * @return Object of specified type
     */
    public static <T> T read(final byte[] yaml, final Class<T> type) {
        try {
            return INSTANCE.readValue(yaml, type);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Write object to yaml.
     * @param data the object data
     * @return byte[]
     */
    public static byte[] write(final Object data) {
        try {
            return INSTANCE.writeValueAsBytes(data);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}