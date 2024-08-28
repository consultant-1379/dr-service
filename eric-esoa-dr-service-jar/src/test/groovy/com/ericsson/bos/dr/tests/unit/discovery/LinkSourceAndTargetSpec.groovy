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

import com.ericsson.bos.dr.jpa.model.ApplicationEntity
import com.ericsson.bos.dr.jpa.model.FeaturePackEntity
import com.ericsson.bos.dr.jpa.model.JobEntity
import com.ericsson.bos.dr.service.compare.filters.NumberAndBooleanStringifier
import com.ericsson.bos.dr.service.discovery.DiscoveredObject
import com.ericsson.bos.dr.service.discovery.DiscoveryContext
import com.ericsson.bos.dr.service.discovery.functions.LinkSourceAndTarget
import com.ericsson.bos.dr.service.exceptions.DRServiceException
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationDiscoverDto
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationDto
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationJobDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification
import com.ericsson.bos.dr.service.compare.filters.MultiKeyBuilder

import static com.ericsson.bos.dr.service.discovery.DiscoveredObject.TYPE.SOURCE
import static com.ericsson.bos.dr.service.discovery.DiscoveredObject.TYPE.TARGET

@ContextConfiguration(classes = [LinkSourceAndTarget.class, MultiKeyBuilder.class, NumberAndBooleanStringifier.class])
class LinkSourceAndTargetSpec extends Specification {

    @Autowired
    LinkSourceAndTarget linkSourceAndTarget;

    def "Link sources and targets links each source to the matching target"() {

        setup: "linkSourceAndTarget expression"
        String linkSourceAndTargetExpression = 'fdnSource:fdnTarget&idSource:idTarget'

        and: "Source and target objects"
        DiscoveredObject sourceOneMatch = new DiscoveredObject(1, SOURCE, ["idSource": id_source, "fdnSource": "fdn=1"])
        DiscoveredObject sourceTwoNoMatch = new DiscoveredObject(1, SOURCE, ["idSource": "2", "fdnSource": "fdn=not_in_target"])
        DiscoveredObject sourceThreeNoMatch = new DiscoveredObject(1, SOURCE, ["idSource": "3", "fdnSource": "fdn=3"])
        DiscoveredObject targetOne = new DiscoveredObject(1, TARGET, ["idTarget": id_target, "fdnTarget": "fdn=1"])
        DiscoveredObject targetThree = new DiscoveredObject(1, TARGET, ["idTarget": "X", "fdnTarget": "fdn=3"])
        DiscoveryContext discoveryContext = createDiscoveryContext(
                [sourceOneMatch, sourceTwoNoMatch, sourceThreeNoMatch], [targetOne, targetThree], linkSourceAndTargetExpression)

        when: "Link sources and targets"
        linkSourceAndTarget.accept(discoveryContext)

        then: 'sourceOne is linked to targetOne, the others are not linked'
        sourceOneMatch.additionalProperties == targetOne.properties
        targetOne.additionalProperties == sourceOneMatch.properties
        sourceTwoNoMatch.additionalProperties == [:]
        sourceThreeNoMatch.additionalProperties == [:]

        where: 'Same property can be a string in source and a number in target, and vice-versa'
        id_source                        | id_target
        "abc"                            | "abc"
        "1"                              | "1"
        "1"                              | 1
        1                                | "1"
        1                                | 1
        -1                               | "-1"
        "1.1"                            | 1.1
        1.1                              | "1.1"
        "false"                          | false
        ["1", "2"]                       | ["1", "2"]
        ["1", "2"]                       | [1, 2]
        [k1: "v1", "k2": "v2"]           | [k1: "v1", "k2": "v2"]
        [k1: "v1", "k2": "v2"]           | ["k2": "v2", "k1": "v1"]
        [k1: "1", "k2": "2", k3: "true"] | [k1: 1, "k2": 2, k3: true]
        [k1: "1", "k2": "2", k3: "true"] | [k3: true, k1: 1, "k2": 2]
        ["true", ["k1": "1"]]            | [true, ["k1": 1]]
        ["k1": ["1", "false"]]           | ["k1": [1, false]]
    }

    def "Throws DrServiceExceptions when source is linked to more than one target"() {

        setup: "linkSourceAndTarget expression"
        String linkSourceAndTargetExpression = 'fdnSource:fdnTarget&idSource:idTarget'

        and: "Source with properties matching 2 target objects"
        DiscoveredObject sourceOne = new DiscoveredObject(1, SOURCE, ["idSource": "1", "fdnSource": "fdn=1"])
        DiscoveredObject targetOne = new DiscoveredObject(1, TARGET, ["idTarget": "1", "fdnTarget": "fdn=1"])
        DiscoveredObject targetTwo = new DiscoveredObject(1, TARGET, ["idTarget": "1", "fdnTarget": "fdn=1"])
        DiscoveryContext discoveryContext = createDiscoveryContext([sourceOne], [targetOne, targetTwo], linkSourceAndTargetExpression)

        when: "Link sources and targets"
        linkSourceAndTarget.accept(discoveryContext)

        then: 'DRServiceException is thrown'
        DRServiceException drServiceException = thrown(DRServiceException)
        drServiceException.errorMessage.errorCode == "DR-25"
    }

    DiscoveryContext createDiscoveryContext(List sources, List targets, String linkSourceAndTargetExpression) {
        def jobConfigurationDto = new ApplicationConfigurationJobDto(name: "test",
                discover: new ApplicationConfigurationDiscoverDto(linkSourceAndTarget: linkSourceAndTargetExpression))
        def applicationEntity = new ApplicationEntity(
                id: 1, featurePack: new FeaturePackEntity(id: 1), config: new ApplicationConfigurationDto(
                jobs: [jobConfigurationDto]))
        DiscoveryContext discoveryContext = DiscoveryContext
                .initialize(applicationEntity, new JobEntity(id: 1, applicationJobName: "test"))
        discoveryContext.setSources(sources)
        discoveryContext.setTargets(targets)
        return discoveryContext
    }

}
