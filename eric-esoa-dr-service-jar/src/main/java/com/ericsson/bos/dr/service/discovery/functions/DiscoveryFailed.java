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

import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.ericsson.bos.dr.service.JobService;
import com.ericsson.bos.dr.service.discovery.DiscoveryContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Update discovery job after failure.
 */
@DiscoveryFunction
@Component
public class DiscoveryFailed implements Consumer<DiscoveryContext> {

    @Autowired
    private JobService jobService;

    @Override
    public void accept(DiscoveryContext discoveryContext) {
        final var errorMessage = discoveryContext.getExceptions().stream()
                .map(Throwable::getMessage)
                .collect(Collectors.joining(";"));
        jobService.discoveryFailed(discoveryContext.getJobId(), Optional.ofNullable(errorMessage));
    }
}