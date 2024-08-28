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
package com.ericsson.bos.dr.service.discovery.functions;

import com.ericsson.bos.dr.service.discovery.DiscoveredObject;
import com.ericsson.bos.dr.service.discovery.DiscoveryContext;
import com.ericsson.bos.dr.service.execution.ExecutionContext;
import com.ericsson.bos.dr.service.execution.ExecutionEngine;
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationActionDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Executes an enrichment action based on the configuration <code>ApplicationConfigurationActionDto</code>.
 */
@Component
class EnrichAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnrichAction.class);

    @Autowired
    private ExecutionEngine executionEngine;

    /**
     * Execute the enrichment action.
     * The <code>DiscoveredObject</code> is updated with the enriched properties after successful
     * execution of the action. The enriched properties will overwrite the existing properties if
     * there are name conflicts.
     * @param enrichAction enrich action definition
     * @param discoveredObject discovered object
     * @param discoveryContext discovery context
     */
    public void execute(final ApplicationConfigurationActionDto enrichAction, final DiscoveredObject discoveredObject,
                 final DiscoveryContext discoveryContext) {
        LOGGER.info("Executing {} object enrichment action: jobId={}, objectProperties={}", discoveredObject.getType().toString().toLowerCase(),
                discoveryContext.getJobId(), discoveredObject.getProperties());
        final var executionContext = new ExecutionContext(discoveryContext.getFeaturePackId(), enrichAction,
                new DiscoverySubstitutionCtx(discoveryContext, discoveredObject).get());
        final var executionResult = executionEngine.execute(executionContext);
        discoveredObject.updateProperties(executionResult.getMappedCommandResponse().get(0));
    }
}