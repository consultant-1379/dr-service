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
package com.ericsson.bos.dr.service.featurepacks.validators;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.ericsson.bos.dr.service.exceptions.DRServiceException;
import com.ericsson.bos.dr.service.exceptions.ErrorCode;
import com.ericsson.bos.dr.service.featurepacks.extractors.ConfigurationFile;
import com.ericsson.bos.dr.service.utils.JSONSchema;
import com.networknt.schema.ValidationMessage;

/**
 * Validate all files in the feature pack which have an associated schema.
 */
@Component
@Order(3)
public class ValidateSchemas implements Validator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidateSchemas.class);

    @Value("${service.feature-packs.validation.ignore-schema-errors}")
    private boolean ignoreSchemaErrors;

    @Override
    public void validate(final String featurePackName, final List<ConfigurationFile> featurePackFiles) {
        final Map<String, Set<ValidationMessage>> results = new HashMap<>();
        featurePackFiles.stream()
                .filter(eachConfigFile -> eachConfigFile.getType().hasSchema())
                .forEach(configFile -> {
                    final Set<ValidationMessage> result =
                            JSONSchema.validate(configFile.getType().getSchema(), configFile.getOriginalContents());
                    if (!result.isEmpty()) {
                        results.put(configFile.getFilename(), result);
                    }
                });
        if (!results.isEmpty() && !ignoreSchemaErrors) {
            LOGGER.warn("Schema validation failed: {}", results);
            throw new DRServiceException(ErrorCode.SCHEMA_ERROR, results.toString());
        }
    }
}