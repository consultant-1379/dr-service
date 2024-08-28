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

package com.ericsson.bos.dr.service.compare;

import java.util.List;
import java.util.Map;

import com.ericsson.bos.dr.service.compare.filters.Condition;
import com.ericsson.bos.dr.service.compare.filters.FilterContext;
import com.ericsson.bos.dr.service.discovery.DiscoveredObject;
import com.ericsson.bos.dr.service.discovery.DiscoveryContext;
import com.ericsson.bos.dr.service.discovery.FilterResult;
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationFilterDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Comparison engine capable of applying the comparison filters defined in the <code>ApplicationConfigurationActionDto</code>,
 * to identify the discrepancies between discovered source and target objects.
 */
@Component
public class ComparisonEngine {

    @Autowired
    private List<Condition> conditions;

    /**
     * Executes the filter condition defined in the Job configuration.
     * Each source or target <code>DiscoveryObject</code> is updated with the filter result.
     *
     * @param discoveryContext
     *         discovery context
     */
    public void applyFilters(final DiscoveryContext discoveryContext) {
        final Map<String, ApplicationConfigurationFilterDto> filterDefs = discoveryContext.getJobConf().getDiscover().getFilters();

        for (Map.Entry<String, ApplicationConfigurationFilterDto> filterDef: filterDefs.entrySet()) {
            final var condition = getCondition(filterDef.getValue());
            if (Condition.TYPE.SOURCE.equals(condition.getType())) {
                applySourceCondition(condition, filterDef, discoveryContext);
            } else if (Condition.TYPE.TARGET.equals(condition.getType())) {
                applyTargetCondition(condition, filterDef, discoveryContext);
            }
        }
    }

    private Condition getCondition(ApplicationConfigurationFilterDto filterDef) {
        return conditions.stream().filter(c -> c.supports(filterDef)).findAny()
                .orElseThrow(() -> new IllegalStateException("Condition not supported: " + filterDef.getCondition().getClass().getName()));
    }

    private void applySourceCondition(final Condition condition, final Map.Entry<String, ApplicationConfigurationFilterDto> filterDef,
                                   final DiscoveryContext discoveryContext) {
        for (final DiscoveredObject discoveredObject : discoveryContext.getSources()) {
            final var filterContext = new FilterContext(
                    discoveredObject, discoveryContext.getTargets(), discoveryContext.getFeaturePackId(), discoveryContext.getInputs());
            applyCondition(condition, filterDef, filterContext, discoveredObject);
        }
    }

    private void applyTargetCondition(final Condition condition, final Map.Entry<String, ApplicationConfigurationFilterDto> filterDef,
                                      final DiscoveryContext discoveryContext) {
        for (final DiscoveredObject discoveredObject : discoveryContext.getTargets()) {
            final var filterContext = new FilterContext(
                    discoveredObject, discoveryContext.getSources(), discoveryContext.getFeaturePackId(), discoveryContext.getInputs());
            applyCondition(condition, filterDef, filterContext, discoveredObject);
        }
    }

    private void applyCondition(final Condition condition, final Map.Entry<String, ApplicationConfigurationFilterDto> filterDef,
                             FilterContext filterContext, DiscoveredObject discoveredObject) {
        final boolean matched = condition.test(filterDef.getValue(), filterContext);
        final var filterResult =
                new FilterResult(filterDef.getKey(), filterDef.getValue(), matched);
        discoveredObject.addFilterResult(filterResult);
    }
}