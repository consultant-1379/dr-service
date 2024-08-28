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

import com.ericsson.bos.dr.jpa.model.JobEntity
import com.ericsson.bos.dr.service.reconcile.ReconcileContext
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationDiscoverDto
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationJobDto
import com.ericsson.bos.dr.web.v1.api.model.ExecuteReconcileDto
import spock.lang.Specification

class ReconcileContextSpec extends Specification {

    ApplicationConfigurationJobDto appConfigJobDto =
            new ApplicationConfigurationJobDto(discover: new ApplicationConfigurationDiscoverDto(filters: [:]))

    def "Job inputs are set in reconcile ctx"() {

        given: "Job inputs"
        JobEntity jobEntity = new JobEntity(
                inputs: ["input1": "value1", "input2": 2, "input3": true], featurePackId: 0)

        and: "No inputs passed in reconcile"
        ExecuteReconcileDto reconcileDetails = new ExecuteReconcileDto(inputs: [:])


        when: "Initialize"
        def result = ReconcileContext.initialize(0, jobEntity, reconcileDetails, appConfigJobDto)

        then: "Job inputs are set in reconcile ctx"
        result.getInputs() == ["input1": "value1", "input2": 2, "input3": true]
    }

    def "Job and reconcile inputs are merged in reconcile ctx"() {

        given: "Job inputs"
        JobEntity jobEntity = new JobEntity(
                inputs: ["input1": "value1", "input2": 2, "input3": true], featurePackId: 0)

        and: "Reconcile inputs"
        ExecuteReconcileDto reconcileDetails = new ExecuteReconcileDto(inputs: ["input2": null, "input3": false, "input4": "value4"])

        when: "Initialize"
        def result = ReconcileContext.initialize(0, jobEntity, reconcileDetails, appConfigJobDto)

        then: "Job and reconcile inputs are merged. Reconcile inputs will override job inputs"
        result.getInputs() == ["input1": "value1", "input2": 2, "input3": false, "input4": "value4"]
    }
}
