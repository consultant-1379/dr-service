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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ericsson.bos.dr.service.exceptions.DRServiceException;
import com.ericsson.bos.dr.service.exceptions.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;

/**
 * JQ query operations.
 */
public abstract class JQ {

    private static final Scope SCOPE;
    private static final ObjectMapper OM_INSTANCE = new ObjectMapper();

    static {
        SCOPE = Scope.newEmptyScope();
        SCOPE.loadFunctions(Scope.class.getClassLoader());
    }

    private JQ() {
    }

    /**
     * Apply each jq expression value in the map to the json node.
     * @param jqExpressions jq expressions map
     * @param jsonNode json node
     * @return Map<String, Object> containing the original key and jq query result value.
     */
    public static Map<String, Object> queryEach(final Map<String, Object> jqExpressions, final JsonNode jsonNode) {
        return jqExpressions.entrySet().stream()
                .collect(LinkedHashMap::new,
                        (m, e) -> m.put(e.getKey(), query(e.getValue().toString(), jsonNode).getObject()),
                        LinkedHashMap::putAll);
    }

    /**
     * Apply jq query expression to the <code>JsonNode</code>.
     * @param jqExpression jq expression
     * @param jsonNode json node
     * @return <code>QueryResult</code>
     */
    public static JQResult query(final String jqExpression, final JsonNode jsonNode) {
        try {
            final var jsonQuery = JsonQuery.compile(jqExpression);
            return new JQResult(jsonQuery.apply(SCOPE, jsonNode));
        } catch (JsonQueryException e) {
            throw new DRServiceException(e, ErrorCode.JQ_ERROR, jqExpression, e.getMessage());
        }
    }

    /**
     * JQ Query result.
     */
    public static class JQResult {

        private final List<JsonNode> jsonNodes;

        /**
         * QueryResult
         * @param jsonNodes jq result
         */
        public JQResult(final List<JsonNode> jsonNodes) {
            this.jsonNodes = jsonNodes;
        }

        /**
         * Get the query result.
         * @return <code>JsonNode</code> list
         */
        public List<JsonNode> get() {
            return jsonNodes;
        }

        /**
         * Get jq result converted to an object using jackson json.
         * @return Object
         */
        public Object getObject() {
            if (jsonNodes.size() == 1) {
                return OM_INSTANCE.convertValue(jsonNodes.get(0), Object.class);
            } else {
                return OM_INSTANCE.convertValue(jsonNodes, Object.class);
            }
        }
    }
}