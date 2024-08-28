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


import com.ericsson.bos.dr.service.discovery.DiscoveryContext
import com.ericsson.bos.dr.service.discovery.functions.InputsValidator
import com.ericsson.bos.dr.service.discovery.functions.ValidateInputs
import com.ericsson.bos.dr.service.exceptions.DRServiceException
import com.ericsson.bos.dr.service.exceptions.ErrorCode
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationDiscoverDto
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationInputsDto
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationJobDto
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationReconcileDto
import spock.lang.Specification

class ValidateInputsSpec extends Specification {

    ValidateInputs validateInputs = new ValidateInputs(inputsValidator: new InputsValidator())

    static ApplicationConfigurationInputsDto input1 = new ApplicationConfigurationInputsDto(name: "input1", mandatory: true)
    static ApplicationConfigurationInputsDto input2 = new ApplicationConfigurationInputsDto(name: "input2", mandatory: true)

    def "Should throw exception when mandatory discovery inputs are missing"() {

        setup: "Create ApplicationConfigurationDiscoverDto with mandatory discovery inputs"
        ApplicationConfigurationDiscoverDto discoverDto = new ApplicationConfigurationDiscoverDto(inputs: [input1, input2])
        ApplicationConfigurationJobDto jobDto = new ApplicationConfigurationJobDto(discover: discoverDto)

        when: "Validate with missing mandatory inputs"
        DiscoveryContext discoveryContext = new DiscoveryContext(inputs: jobInputs, jobConf: jobDto)
        validateInputs.accept(discoveryContext)

        then: 'DRServiceException is thrown'
        DRServiceException drServiceException = thrown(DRServiceException)
        drServiceException.errorMessage.errorCode == ErrorCode.MISSING_INPUTS.errorCode

        where:
        jobInputs | _
        [:] | _
        [input1: "value1"] | _
        [input2: "value2"] | _
    }

    def "Should throw exception when mandatory reconcile inputs are missing and autoreconcile is true"() {

        setup: "Create ApplicationConfigurationDiscoverDto with no inputs"
        ApplicationConfigurationDiscoverDto discoverDto = new ApplicationConfigurationDiscoverDto(inputs: [])

        and: "Create ApplicationConfigurationReconcileDto with mandatory reconcile inputs"
        ApplicationConfigurationReconcileDto reconcileDto = new ApplicationConfigurationReconcileDto(inputs: [input1, input2])

        when: "Validate with missing mandatory reconcile inputs"
        ApplicationConfigurationJobDto jobDto = new ApplicationConfigurationJobDto(discover: discoverDto, reconcile: reconcileDto)
        DiscoveryContext discoveryContext = new DiscoveryContext(inputs: jobInputs, jobConf: jobDto, autoReconcile: true)
        validateInputs.accept(discoveryContext)

        then: 'DRServiceException is thrown'
        DRServiceException drServiceException = thrown(DRServiceException)
        drServiceException.errorMessage.errorCode == ErrorCode.MISSING_INPUTS.errorCode

        where:
        jobInputs | _
        [:] | _
        [input1: "value1"] | _
        [input2: "value2"] | _
    }

    def "No exception thrown when mandatory inputs are supplied"() {

        setup: "Create ApplicationConfigurationDiscoverDto with no inputs"
        ApplicationConfigurationDiscoverDto discoverDto = new ApplicationConfigurationDiscoverDto(inputs: discoveryInputs)

        and: "Create ApplicationConfigurationReconcileDto with mandatory reconcile inputs"
        ApplicationConfigurationReconcileDto reconcileDto = new ApplicationConfigurationReconcileDto(inputs: reconcileInputs)

        when: "Validate with missing mandatory reconcile inputs"
        ApplicationConfigurationJobDto jobDto = new ApplicationConfigurationJobDto(discover: discoverDto, reconcile: reconcileDto)
        DiscoveryContext discoveryContext = new DiscoveryContext(inputs: jobInputs, jobConf: jobDto, autoReconcile: true)
        validateInputs.accept(discoveryContext)

        then: 'DRServiceException is not thrown'
        noExceptionThrown()

        where:
        jobInputs                     | discoveryInputs  | reconcileInputs  | autoReconcile
        [input1: "v1", input2: "v2 "] | [input1, input2] | []               | false
        [input1: "v1", input2: "v2 "] | []               | [input1, input2] | true
    }
}