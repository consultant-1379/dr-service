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

import java.util.Objects;
import java.util.function.Consumer;

import com.ericsson.bos.dr.service.discovery.DiscoveredObject;
import com.ericsson.bos.dr.service.discovery.DiscoveryContext;
import com.ericsson.bos.dr.service.execution.ExecutionEngine;
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationActionDto;
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationDiscoverDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Enrich an individual discovered source or target object.
 */
@Component
@DiscoveryFunction
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class EnrichDiscoveryObject implements Consumer<DiscoveryContext> {

    @Autowired
    private ExecutionEngine executionEngine;

    @Autowired
    private EnrichAction enrichAction;

    private DiscoveredObject discoveredObject;

    @Override
    public void accept(DiscoveryContext discoveryContext) {
        Objects.requireNonNull(discoveredObject, "DiscoveredObject must be set");
        final ApplicationConfigurationDiscoverDto discoveryJobConf = discoveryContext.getDiscoveryJobConf();
        final ApplicationConfigurationActionDto action = DiscoveredObject.TYPE.SOURCE.equals(discoveredObject.getType()) ?
                discoveryJobConf.getSource().getEnrichAction() : discoveryJobConf.getTarget().getEnrichAction();
        enrichAction.execute(action, discoveredObject, discoveryContext);
    }

    public void setDiscoveredObject(DiscoveredObject discoveredObject) {
        this.discoveredObject = discoveredObject;
    }
}