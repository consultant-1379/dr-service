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

package com.ericsson.bos.dr.tests.unit.reconcile


import com.ericsson.bos.dr.jpa.model.DiscoveryObjectEntity
import com.ericsson.bos.dr.jpa.model.FilterEntity
import com.ericsson.bos.dr.service.DiscoveryService
import com.ericsson.bos.dr.service.execution.ExecutionEngine
import com.ericsson.bos.dr.service.reconcile.ReconcileContext
import com.ericsson.bos.dr.service.reconcile.functions.ReconcileSingleObject
import com.ericsson.bos.dr.service.reconcile.functions.ReconcileStateHandler
import com.ericsson.bos.dr.web.v1.api.model.ExecuteReconcileDtoObjectsInner
import spock.lang.Specification

import static com.ericsson.bos.dr.web.v1.api.model.DiscoveredObjectDto.StatusEnum.DISCOVERED
import static com.ericsson.bos.dr.web.v1.api.model.FilterDtoReconcileAction.StatusEnum.COMPLETED

class ReconcileSingleObjectSpec extends Specification {

    DiscoveryService discoveryServiceMock = Mock()
    ReconcileStateHandler reconcileStateHandlerMock = Mock()
    ExecutionEngine executionEngineMock = Mock()

    ReconcileSingleObject reconcileSingleObject = new ReconcileSingleObject(discoveryService: discoveryServiceMock,
            reconcileState:  reconcileStateHandlerMock, executionEngine: executionEngineMock)

    def "Completed filters are skipped"() {
        setup: "DiscoveredObjectEntity with 2 completed fitlers"
        FilterEntity filterEntity1 = new FilterEntity(name: "filter1", reconcileStatus: COMPLETED)
        FilterEntity filterEntity2 = new FilterEntity(name: "filter2", reconcileStatus: COMPLETED)
        DiscoveryObjectEntity discoveryObjectEntity = new DiscoveryObjectEntity(status: DISCOVERED, filters: [filterEntity1, filterEntity2])

        and: "DiscoveryObjectRepository mocked to return DiscoveredObjectEntity"
        discoveryServiceMock.findDiscoveryObjectEntityById(_) >> discoveryObjectEntity

        when: "Reconcile object"
        ExecuteReconcileDtoObjectsInner reconcileObject = new ExecuteReconcileDtoObjectsInner().objectId("1")
        reconcileSingleObject.reconcileObject = reconcileObject
        reconcileSingleObject.accept(new ReconcileContext(filters: ["filter1", "filter2"]))

        then: "No attempt to execute reconcile action"
        0 * reconcileStateHandlerMock._
        0 * executionEngineMock._
    }
}