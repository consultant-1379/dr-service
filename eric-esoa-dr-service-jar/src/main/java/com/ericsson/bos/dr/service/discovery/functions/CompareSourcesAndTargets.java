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

import java.util.function.Consumer;

import com.ericsson.bos.dr.service.compare.ComparisonEngine;
import com.ericsson.bos.dr.service.discovery.DiscoveryContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Perform comparison of source and target objects based on filters defined in the
 * application configuration job.
 */
@Component
@DiscoveryFunction
public class CompareSourcesAndTargets implements Consumer<DiscoveryContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompareSourcesAndTargets.class);

    @Autowired
    private ComparisonEngine comparisonEngine;

    @Override
    public void accept(DiscoveryContext discoveryContext) {
        LOGGER.info("Applying filters for jobId={}", discoveryContext.getJobId());
        comparisonEngine.applyFilters(discoveryContext);
    }
}