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

package com.ericsson.bos.dr.tests.unit.jpa

import com.ericsson.bos.dr.jpa.model.DiscoveredObjectStatusEvaluator
import com.ericsson.bos.dr.jpa.model.DiscoveryObjectEntity
import com.ericsson.bos.dr.jpa.model.FilterEntity
import spock.lang.Specification

class DiscoveredObjectStatusEvaluatorSpec extends Specification {

    def "Discovered object status is evaluated as expected"() {

        setup: "create discovered object entity"
        DiscoveryObjectEntity discoveryObjectEntity = new DiscoveryObjectEntity()
        Set<FilterEntity> filterEntities = filterStatuses.collect {new FilterEntity(reconcileStatus: it) }.toSet()
        discoveryObjectEntity.setFilters(filterEntities)

        when: "evaluate status"
        String status = DiscoveredObjectStatusEvaluator.evaluate(discoveryObjectEntity)

        then: "status is as expected"
        status == expectedStatus

        where:
        filterStatuses                                | expectedStatus
        ["NOT_STARTED", "NOT_STARTED", "NOT_STARTED"] | "DISCOVERED"
        ["INPROGRESS", "NOT_STARTED", "NOT_STARTED"]  | "RECONCILING"
        ["INPROGRESS", "FAILED", "NOT_STARTED"]       | "RECONCILING"
        ["INPROGRESS", "COMPLETED", "NOT_STARTED"]    | "RECONCILING"
        ["COMPLETED", "COMPLETED", "NOT_STARTED"]     | "PARTIALLY_RECONCILED"
        ["COMPLETED", "FAILED", "NOT_STARTED"]        | "PARTIALLY_RECONCILED"
        ["FAILED", "NOT_STARTED", "NOT_STARTED"]      | "RECONCILE_FAILED"
        ["FAILED", "FAILED", "NOT_STARTED"]           | "RECONCILE_FAILED"
        ["FAILED", "FAILED", "FAILED"]                | "RECONCILE_FAILED"
        ["COMPLETED", "COMPLETED", "COMPLETED"]       | "RECONCILED"
    }
}
