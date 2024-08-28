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
package com.ericsson.bos.dr.service.execution.steps;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import com.ericsson.bos.dr.service.execution.ExecutionContext;
import com.ericsson.bos.dr.service.utils.JQ;
import com.ericsson.bos.dr.service.utils.JSON;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;

/**
 * Json Mapping steps which applies the defined jq mappings on the command response.
 */
@Component
public class JsonMappingStep implements ExecutionStep<String, List<Map<String, Object>>> {

    @Override
    public List<Map<String, Object>> execute(String input, ExecutionContext executionContext) {
        final Map<String, Object> mappings = executionContext.getActionDto().getMapping();
        final List<Map<String, Object>> mappingOutput = new ArrayList<>();
        if (MapUtils.isEmpty(mappings)) {
            return mappingOutput;
        }
        final JsonNode node = JSON.read(input, JsonNode.class);
        if (node.isArray()) {
            StreamSupport.stream(node.spliterator(), false)
                    .forEach(childNode -> mappingOutput.add(JQ.queryEach(mappings, childNode)));
        } else if (node.isObject()) {
            mappingOutput.add(JQ.queryEach(mappings, node));
        } else {
            throw new IllegalArgumentException("Unexpected input: " + input);
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.trace("JSON mapping output: {}", mappingOutput);
        }
        return mappingOutput;
    }
}