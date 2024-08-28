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

import static com.ericsson.bos.dr.service.discovery.DiscoveredObject.TYPE.TARGET;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.ericsson.bos.dr.service.discovery.DiscoveredObject;
import com.ericsson.bos.dr.service.discovery.DiscoveryContext;
import com.ericsson.bos.dr.service.execution.ExecutionContext;
import com.ericsson.bos.dr.service.execution.ExecutionEngine;
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationActionDto;
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationDiscoverDto;
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationDiscoverDtoSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Fetch the target objects based on the configuration in the discovery job.
 */
@Component
@DiscoveryFunction
public class FetchTargets implements Consumer<DiscoveryContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FetchTargets.class);

    @Autowired
    private ExecutionEngine executionEngine;

    @Override
    public void accept(DiscoveryContext discoveryContext) {
        final ApplicationConfigurationDiscoverDto discoveryJobConf = discoveryContext.getDiscoveryJobConf();

        final Optional<ApplicationConfigurationActionDto> fetchTargetAction = Optional.ofNullable(discoveryJobConf.getTarget())
                .map(ApplicationConfigurationDiscoverDtoSource::getFetchAction);

        if (fetchTargetAction.isPresent()) {
            LOGGER.info("Fetching targets for jobId={}", discoveryContext.getJobId());
            final var executionContext = new ExecutionContext(discoveryContext.getFeaturePackId(), fetchTargetAction.get(),
                    new DiscoverySubstitutionCtx(discoveryContext).get());
            final var targetExecutionResult = executionEngine.execute(executionContext);
            final List<DiscoveredObject> targetObjects = targetExecutionResult.getMappedCommandResponse().stream()
                    .map(props -> new DiscoveredObject(discoveryContext.getJobId(), TARGET, props)).collect(Collectors.toList());
            discoveryContext.setTargets(targetObjects);
            LOGGER.info("{} target objects fetched", targetObjects.size());
        }
    }
}