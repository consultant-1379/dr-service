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

import com.ericsson.bos.dr.jpa.DiscoveryObjectRepository;
import com.ericsson.bos.dr.jpa.model.StatusCount;
import com.ericsson.bos.dr.service.JobService;
import com.ericsson.bos.dr.service.discovery.DiscoveryContext;
import com.ericsson.bos.dr.web.v1.api.model.JobSummaryDto.StatusEnum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Update discovery job after successful completion.
 */
@Component
@DiscoveryFunction
public class DiscoveryCompleted implements Consumer<DiscoveryContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiscoveryCompleted.class);

    @Autowired
    private JobService jobService;

    @Autowired
    private DiscoveryObjectRepository discoveryObjectRepository;

    @Override
    public void accept(DiscoveryContext discoveryContext) {
        final int discoveredCount = discoveryObjectRepository.getCountsGroupedByStatus(discoveryContext.getJobId()).stream()
                .filter(sc -> sc.getStatus().equals(StatusEnum.DISCOVERED.name())).findFirst()
                .map(StatusCount::getCount).orElse(0);
        LOGGER.info("Discovery completed for jobId={}, discoveredObjectsCount={}", discoveryContext.getJobId(), discoveredCount);
        jobService.discoveryCompleted(discoveryContext.getJobId(), discoveredCount);
    }
}
