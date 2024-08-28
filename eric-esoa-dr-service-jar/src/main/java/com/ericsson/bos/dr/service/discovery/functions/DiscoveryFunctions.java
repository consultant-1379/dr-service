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

import java.util.List;
import java.util.function.Consumer;

import com.ericsson.bos.dr.service.discovery.DiscoveryContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Container of all <code>DiscoveryFunction</code> implementations.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DiscoveryFunctions {

    @Autowired
    @DiscoveryFunction
    private List<Consumer<DiscoveryContext>> functions;

    public List<Consumer<DiscoveryContext>> getFunctions() {
        return functions;
    }

    public void setFunctions(List<Consumer<DiscoveryContext>> functions) {
        this.functions = functions;
    }
}