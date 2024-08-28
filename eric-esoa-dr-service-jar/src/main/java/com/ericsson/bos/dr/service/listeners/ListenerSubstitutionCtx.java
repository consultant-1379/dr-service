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
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Substitution context for expression in listener configuration.
 */
public class ListenerSubstitutionCtx implements Supplier<Map<String, Object>> {

    private static final String REQUEST_KEY = "request";
    private static final String RESULTS_KEY = "results";

    private final Map<String, Object> substitutionCtx;

    /**
     * ListenerSubstitutionCtx.
     * @param event event
     */
    public ListenerSubstitutionCtx(final Map<String, Object> event) {
        this.substitutionCtx = new HashMap<>();
        this.substitutionCtx.put(REQUEST_KEY, event);
    }

    /**
     * Add results to the substitution context.
     * @param results results.
     */
    public void addResults(List<Map<String, Object>> results) {
        this.substitutionCtx.put(RESULTS_KEY, results);
    }

    @Override
    public Map<String, Object> get() {
        return this.substitutionCtx;
    }
}