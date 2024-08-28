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

import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

/**
 * Common JSONSchema operations.
 */
public abstract class JSONSchema {

    private static final JsonSchemaFactory FACTORY =
            JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);

    private JSONSchema() {}

    /**
     * Validate data against a json schema.
     * @param schemaPath schema file path
     * @param data the data to be validated
     * @return ValidationMessage set which will be empty if validation is successful
     */
    public static Set<ValidationMessage> validate(final String schemaPath, final byte[] data)  {
        final var jsonSchema = FACTORY.getSchema(JSONSchema.class.getResourceAsStream(schemaPath));
        final var jsonNode = YAML.read(data, JsonNode.class);
        return jsonSchema.validate(jsonNode);
    }
}