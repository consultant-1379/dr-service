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
package com.ericsson.bos.dr.service.reconcile.functions;

import java.util.List;
import java.util.function.Consumer;

import com.ericsson.bos.dr.service.reconcile.ReconcileContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Container of all <code>ReconcileFunction</code> implementations.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ReconcileFunctions {

    @Autowired
    @ReconcileFunction
    private List<Consumer<ReconcileContext>> functions;

    public List<Consumer<ReconcileContext>> getFunctions() {
        return functions;
    }

    public void setFunctions(List<Consumer<ReconcileContext>> functions) {
        this.functions = functions;
    }
}