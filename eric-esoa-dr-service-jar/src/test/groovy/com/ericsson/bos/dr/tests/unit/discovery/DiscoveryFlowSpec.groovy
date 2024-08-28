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
package com.ericsson.bos.dr.tests.unit.discovery

import com.ericsson.bos.dr.service.discovery.DiscoveryFlow
import com.ericsson.bos.dr.service.discovery.DiscoveredObject
import com.ericsson.bos.dr.service.discovery.DiscoveryContext
import com.ericsson.bos.dr.service.discovery.functions.CompareSourcesAndTargets
import com.ericsson.bos.dr.service.discovery.functions.DiscoveryCompleted
import com.ericsson.bos.dr.service.discovery.functions.DiscoveryFailed
import com.ericsson.bos.dr.service.discovery.functions.DiscoveryFunctionFactory
import com.ericsson.bos.dr.service.discovery.functions.EnrichDiscoveryObject
import com.ericsson.bos.dr.service.discovery.functions.FetchSources
import com.ericsson.bos.dr.service.discovery.functions.FetchTargets
import com.ericsson.bos.dr.service.discovery.functions.LinkSourceAndTarget
import com.ericsson.bos.dr.service.discovery.functions.SaveDiscoveryObjects
import com.ericsson.bos.dr.service.discovery.functions.ValidateInputs
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationActionDto
import spock.lang.Specification

import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class DiscoveryFlowSpec extends Specification {

    DiscoveryFlow concurrentDiscoveryFlow

    Executor executor = Executors.newFixedThreadPool(2)

    DiscoveryFunctionFactory factoryMock = Mock(DiscoveryFunctionFactory)
    ValidateInputs validateInputsMock = Mock(ValidateInputs)
    FetchSources fetchSourcesMock = Mock(FetchSources)
    FetchTargets fetchTargetsMock = Mock(FetchTargets)
    EnrichDiscoveryObject enrichMock = Mock(EnrichDiscoveryObject)
    CompareSourcesAndTargets compareMock = Mock(CompareSourcesAndTargets)
    SaveDiscoveryObjects saveMock = Mock(SaveDiscoveryObjects)
    DiscoveryFailed discoveryFailedMock = Mock(DiscoveryFailed)
    DiscoveryCompleted discoveryCompletedMock = Mock(DiscoveryCompleted)
    LinkSourceAndTarget mapSourcesToTargetsMock = Mock(LinkSourceAndTarget)

    def setup() {
        concurrentDiscoveryFlow = new DiscoveryFlow(executor: executor, factory: factoryMock)
        factoryMock.validateInputs >> validateInputsMock
        factoryMock.fetchSources >> fetchSourcesMock
        factoryMock.fetchTargets >> fetchTargetsMock
        factoryMock.getEnrichDiscoveryObject(_) >> enrichMock
        factoryMock.compareSourcesAndTargets >> compareMock
        factoryMock.linkSourcesAndTargets >> mapSourcesToTargetsMock
        factoryMock.saveDiscoveryObjects >> saveMock
        factoryMock.discoveryCompleted >> discoveryCompletedMock
        factoryMock.discoveryFailed >> discoveryFailedMock
    }

    def "Discovery flow executes all discovery functions"() {
        setup: "DiscoveryContext"
        DiscoveredObject source1 = Mock(DiscoveredObject)
        DiscoveredObject source2 = Mock(DiscoveredObject)
        DiscoveredObject target1 = Mock(DiscoveredObject)
        DiscoveredObject target2 = Mock(DiscoveredObject)

        DiscoveryContext discoveryContext = Mock(DiscoveryContext)
        discoveryContext.sources >> [source1, source2]
        discoveryContext.targets >> [target1, target2]
        discoveryContext.getDiscoverySourceEnrichAction() >> Optional.of(new ApplicationConfigurationActionDto())
        discoveryContext.getDiscoveryTargetEnrichAction() >> Optional.of(new ApplicationConfigurationActionDto())

        when: "Execute Discovery Flow"
        concurrentDiscoveryFlow.execute(discoveryContext).join()

        then: "Discovery functions are executed"
        1 * validateInputsMock.accept(_)
        1 * fetchTargetsMock.accept(_)
        4 * enrichMock.accept(_)
        1 * compareMock.accept(_)
        1 * mapSourcesToTargetsMock.accept(_)
        1 * saveMock.accept(_)
    }

    def "Discovery flow terminates when exception in validateInputs"() {
        setup: "DiscoveryContext"
        DiscoveryContext discoveryContext = Mock(DiscoveryContext)

        and: "Throw exception on validateInputs"
        1 * validateInputsMock.accept(discoveryContext) >> { throw new RuntimeException("error!") }

        when: "Execute Discovery Flow"
        concurrentDiscoveryFlow.execute(discoveryContext).join()

        then: "DiscoveryFailed function is executed"
        1 * discoveryFailedMock.accept(_)

        and: "Subsequent Discovery functions are not executed"
        0 * fetchSourcesMock.accept(_)
        0 * fetchTargetsMock.accept(_)
        0 * enrichMock.accept(_)
        0 * compareMock.accept(_)
        0 * saveMock.accept(_)
    }

    def "Discovery flow terminates when exception in fetchSources"() {
        setup: "DiscoveryContext"
        DiscoveryContext discoveryContext = Mock(DiscoveryContext)

        and: "Throw exception on fetchSources"
        1 * fetchSourcesMock.accept(discoveryContext) >> { throw new RuntimeException("error!") }

        when: "Execute Discovery Flow"
        concurrentDiscoveryFlow.execute(discoveryContext).join()

        then: "fetchTargets Discovery function is executed"
        1 * validateInputsMock.accept(_)
        1 * fetchTargetsMock.accept(_)

        and: "DiscoveryFailed function is executed"
        1 * discoveryFailedMock.accept(_)

        and: "Subsequent Discovery functions are not executed"
        0 * enrichMock.accept(_)
        0 * compareMock.accept(_)
        0 * saveMock.accept(_)
    }

    def "Discovery flow terminates when exception in enrich objects"() {
        setup: "DiscoveryContext"
        DiscoveredObject source1 = Mock(DiscoveredObject)
        DiscoveryContext discoveryContext = Mock(DiscoveryContext)
        discoveryContext.sources >> [source1]
        discoveryContext.targets >> []
        discoveryContext.getDiscoverySourceEnrichAction() >> Optional.of(new ApplicationConfigurationActionDto())
        discoveryContext.getDiscoveryTargetEnrichAction() >> Optional.empty()

        and: "Throw exception on enrichment"
        1 * enrichMock.accept(discoveryContext) >> { throw new RuntimeException("error!") }

        when: "Execute Discovery Flow"
        concurrentDiscoveryFlow.execute(discoveryContext).join()

        then: "DiscoveryFailed function is executed"
        1 * discoveryFailedMock.accept(_)

        and: "Subsequent Discovery functions are not executed"
        0 * compareMock.accept(_)
        0 * saveMock.accept(_)
    }

    def "Discovery flow terminates when exception in compare sources and targets"() {
        setup: "DiscoveryContext"
        DiscoveredObject source1 = Mock(DiscoveredObject)
        DiscoveryContext discoveryContext = Mock(DiscoveryContext)
        discoveryContext.sources >> [source1]
        discoveryContext.targets >> []
        discoveryContext.getDiscoverySourceEnrichAction() >> Optional.of(new ApplicationConfigurationActionDto())
        discoveryContext.getDiscoveryTargetEnrichAction() >> Optional.empty()

        and: "Throw exception on comparison"
        1 * compareMock.accept(discoveryContext) >> { throw new RuntimeException("error!") }

        when: "Execute Discovery Flow"
        concurrentDiscoveryFlow.execute(discoveryContext).join()

        then: "DiscoveryFailed function is executed"
        1 * discoveryFailedMock.accept(_)

        and: "Subsequent Discovery functions are not executed"
        0 * saveMock.accept(_)
    }

    def "Discovery flow terminates when exception in map sources to targets"() {
        setup: "DiscoveryContext"
        DiscoveredObject source1 = Mock(DiscoveredObject)
        DiscoveryContext discoveryContext = Mock(DiscoveryContext)
        discoveryContext.sources >> [source1]
        discoveryContext.targets >> []
        discoveryContext.getDiscoverySourceEnrichAction() >> Optional.of(new ApplicationConfigurationActionDto())
        discoveryContext.getDiscoveryTargetEnrichAction() >> Optional.empty()

        and: "Throw exception on comparison"
        1 * mapSourcesToTargetsMock.accept(discoveryContext) >> { throw new RuntimeException("error!") }

        when: "Execute Discovery Flow"
        concurrentDiscoveryFlow.execute(discoveryContext).join()

        then: "DiscoveryFailed function is executed"
        1 * discoveryFailedMock.accept(_)

        and: "Subsequent Discovery functions are not executed"
        0 * saveMock.accept(_)
    }

    def "Discovery flow terminates when exception in save discovery objects"() {
        setup: "DiscoveryContext"
        DiscoveredObject source1 = Mock(DiscoveredObject)
        DiscoveryContext discoveryContext = Mock(DiscoveryContext)
        discoveryContext.sources >> [source1]
        discoveryContext.targets >> []
        discoveryContext.getDiscoverySourceEnrichAction() >> Optional.of(new ApplicationConfigurationActionDto())
        discoveryContext.getDiscoveryTargetEnrichAction() >> Optional.empty()

        and: "Throw exception on save"
        1 * saveMock.accept(discoveryContext) >> { throw new RuntimeException("error!") }

        when: "Execute Discovery Flow"
        concurrentDiscoveryFlow.execute(discoveryContext).join()

        then: "DiscoveryFailed function is executed"
        1 * discoveryFailedMock.accept(_)
    }

    def "Source and target enrichment operations are not executed after an error occurs"() {
        setup: "DiscoveryContext"
        DiscoveryContext discoveryContext = Mock(DiscoveryContext)
        discoveryContext.sources >> (1..50).collect { Mock(DiscoveredObject) }
        discoveryContext.targets >> (1..50).collect { Mock(DiscoveredObject) }

        and: "Throw exception for one of the enrichment calls"
        AtomicInteger invocations = new AtomicInteger(0)
        enrichMock.accept(discoveryContext) >> {invocations.incrementAndGet() }
                >> { throw new RuntimeException("error!") }
                >> {  invocations.incrementAndGet() }

        when: "Execute Discovery Flow"
        concurrentDiscoveryFlow.execute(discoveryContext).join()

        then: "DiscoveryFailed function is executed"
        1 * discoveryFailedMock.accept(_)

        and: "Enrichment functions which have not started execution are cancelled"
        invocations.get() < 30 // would be 99 if all functions were executed after error
    }
}