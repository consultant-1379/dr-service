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
package com.ericsson.bos.dr.tests.unit.compare

import com.ericsson.bos.dr.jpa.model.ApplicationEntity
import com.ericsson.bos.dr.jpa.model.FeaturePackEntity
import com.ericsson.bos.dr.jpa.model.JobEntity
import com.ericsson.bos.dr.service.AssetService
import com.ericsson.bos.dr.service.compare.ComparisonEngine
import com.ericsson.bos.dr.service.discovery.DiscoveredObject
import com.ericsson.bos.dr.service.discovery.DiscoveryContext
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationDiscoverDto
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationDto
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationFilterDto
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationJobDto
import com.ericsson.bos.dr.web.v1.api.model.FilterConditionDto
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.ComponentScan
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

import static com.ericsson.bos.dr.service.discovery.DiscoveredObject.TYPE.SOURCE
import static com.ericsson.bos.dr.service.discovery.DiscoveredObject.TYPE.TARGET
import static com.ericsson.bos.dr.web.v1.api.model.FilterConditionDto.NameEnum.SOURCEINTARGET
import static com.ericsson.bos.dr.web.v1.api.model.FilterConditionDto.NameEnum.SOURCEMISMATCHEDINTARGET
import static com.ericsson.bos.dr.web.v1.api.model.FilterConditionDto.NameEnum.SOURCENOTINTARGET
import static com.ericsson.bos.dr.web.v1.api.model.FilterConditionDto.NameEnum.SOURCESCRIPT
import static com.ericsson.bos.dr.web.v1.api.model.FilterConditionDto.NameEnum.TARGETNOTINSOURCE
import static com.ericsson.bos.dr.web.v1.api.model.FilterConditionDto.NameEnum.TARGETSCRIPT

@ContextConfiguration(classes = ComparisonEngineTestConfig.class)
class ComparisonEngineSpec extends Specification {

    @Autowired
    private ComparisonEngine comparisonEngine

    @SpringBean
    private AssetService assetService = Mock(AssetService)

    String filterName = "filter1"

    def "Apply 'SourceInTarget' condition with arg and no linked targets"() {

        setup: "'SourceInTarget' condition"
        FilterConditionDto condition = new FilterConditionDto(name: SOURCEINTARGET, arg: conditionArg)
        Map<String, ApplicationConfigurationFilterDto> filterDto = [(filterName): new ApplicationConfigurationFilterDto(
                condition: condition, filterMatchText: "Filter has matched!!!")]

        and: "Source and target objects"
        DiscoveredObject sourceOne = new DiscoveredObject(1, SOURCE, ["idSource": "1", "fdnSource": "fdn=1"])
        DiscoveredObject sourceTwo = new DiscoveredObject(1, SOURCE, ["idSource": "2", "fdnSource": "fdn=not_in_target"])
        DiscoveredObject sourceThree = new DiscoveredObject(1, SOURCE, ["idSource": "3", "fdnSource": "fdn=3"])
        DiscoveredObject targetOne = new DiscoveredObject(1, TARGET, ["idTarget": "1", "fdnTarget": "fdn=1"])
        DiscoveredObject targetTwo = new DiscoveredObject(1, TARGET, ["idTarget": "2", "fdnTarget": "fdn=2"])
        DiscoveredObject targetThree = new DiscoveredObject(1, TARGET, ["idTarget": "3", "fdnTarget": "fdn=3"])
        DiscoveryContext discoveryContext = createDiscoveryContext(
                [sourceOne, sourceTwo, sourceThree], [targetOne, targetTwo, targetThree], filterDto)

        when: "Apply filters"
        comparisonEngine.applyFilters(discoveryContext)

        then: "Expected filters match"
        assertFilterMatching(sourceOne, filterName, filterDto.filter1.filterMatchText)
        assertFilterNotMatching(sourceTwo, filterName)
        assertFilterMatching(sourceThree, filterName, filterDto.filter1.filterMatchText)

        and: 'Discovery context is unchanged'
        discoveryContext.getSources().size() == 3
        discoveryContext.getTargets().size() == 3

        where:
        conditionArg                            | _
        "fdnSource:fdnTarget"                   | _
        "fdnSource:fdnTarget&idSource:idTarget" | _
    }

    def "Apply 'SourceInTarget' condition with no arg and linked target"() {

        setup: "'SourceInTarget' condition with  no arg"
        FilterConditionDto condition = new FilterConditionDto(name: SOURCEINTARGET, arg: null)
        Map<String, ApplicationConfigurationFilterDto> filterDto = [(filterName): new ApplicationConfigurationFilterDto(
                condition: condition, filterMatchText: "Filter has matched!!!")]

        and: "Source object with a linked target object"
        DiscoveredObject targetOne = new DiscoveredObject(1, TARGET, ["idTarget": "1", "fdnTarget": "fdn=1"])
        DiscoveredObject sourceOne = new DiscoveredObject(1, SOURCE, ["idSource": "1", "fdnSource": "fdn=1"])
        sourceOne.setAdditionalProperties(targetOne.getProperties())
        DiscoveryContext discoveryContext = createDiscoveryContext([sourceOne], [targetOne], filterDto)

        when: "Apply filters"
        comparisonEngine.applyFilters(discoveryContext)

        then: "Source object filter is matched using the linked target object"
        assertFilterMatching(sourceOne, filterName, filterDto.filter1.filterMatchText)
    }

    def "Apply 'SourceInTarget' condition with arg and linked target exists"() {

        setup: "'SourceInTarget' condition with arg"
        FilterConditionDto condition = new FilterConditionDto(name: SOURCEINTARGET, arg: 'sourceProp:targetProp')
        Map<String, ApplicationConfigurationFilterDto> filterDto = [(filterName): new ApplicationConfigurationFilterDto(
                condition: condition, filterMatchText: "Filter has matched!!!")]

        and: "Source object and linked target object "
        DiscoveredObject targetOne = new DiscoveredObject(1, TARGET, ["idTarget": "1", "fdnTarget": "fdn=1", targetProp: "v1"])
        DiscoveredObject sourceOne = new DiscoveredObject(1, SOURCE, ["idSource": "1", "fdnSource": "fdn=1", sourceProp: "v1"])
        sourceOne.additionalProperties = targetOne.properties
        DiscoveryContext discoveryContext = createDiscoveryContext([sourceOne], [targetOne], filterDto)

        when: "Apply filters"
        comparisonEngine.applyFilters(discoveryContext)

        then: "Source object filter is matched using the linked target object"
        assertFilterMatching(sourceOne, filterName, filterDto.filter1.filterMatchText)
    }

    def "Apply 'SourceNotInTarget' condition with arg and no linked targets"() {

        setup: "'SourceNotInTarget' condition"
        FilterConditionDto condition = new FilterConditionDto(name: SOURCENOTINTARGET, arg: conditionArg)
        Map<String, ApplicationConfigurationFilterDto> filterDto = [(filterName): new ApplicationConfigurationFilterDto(
                condition: condition, filterMatchText: "Filter has matched!!!")]

        and: "Source and target objects"
        DiscoveredObject sourceOne = new DiscoveredObject(1, SOURCE, ["idSource": "1", "fdnSource": "fdn=1"])
        DiscoveredObject sourceTwo = new DiscoveredObject(1, SOURCE, ["idSource": "2", "fdnSource": "fdn=not_in_target"])
        DiscoveredObject sourceThree = new DiscoveredObject(1, SOURCE, ["idSource": "3", "fdnSource": "fdn=3"])
        DiscoveredObject targetOne = new DiscoveredObject(1, TARGET, ["idTarget": "1", "fdnTarget": "fdn=1"])
        DiscoveredObject targetTwo = new DiscoveredObject(1, TARGET, ["idTarget": "2", "fdnTarget": ""])
        DiscoveredObject targetThree = new DiscoveredObject(1, TARGET, ["idTarget": "3", "fdnTarget": "fdn=3"])
        DiscoveryContext discoveryContext = createDiscoveryContext(
                [sourceOne, sourceTwo, sourceThree], [targetOne, targetTwo, targetThree], filterDto)

        when: "Apply filters"
        comparisonEngine.applyFilters(discoveryContext)

        then: "Expected filters match"
        assertFilterNotMatching(sourceOne, filterName)
        assertFilterMatching(sourceTwo, filterName, filterDto.filter1.filterMatchText)
        assertFilterNotMatching(sourceThree, filterName)

        and: 'Discovery context is unchanged'
        discoveryContext.getSources().size() == 3
        discoveryContext.getTargets().size() == 3

        where:
        conditionArg                            | _
        "fdnSource:fdnTarget"                   | _
        "fdnSource:fdnTarget&idSource:idTarget" | _
    }

    def "Apply 'SourceNotInTarget' condition with no arg and linked target"() {

        setup: "'SourceNotInTarget' condition with  no arg"
        FilterConditionDto condition = new FilterConditionDto(name: SOURCENOTINTARGET, arg: null)
        Map<String, ApplicationConfigurationFilterDto> filterDto = [(filterName): new ApplicationConfigurationFilterDto(
                condition: condition, filterMatchText: "Filter has matched!!!")]

        and: "Source object with a linked target object"
        DiscoveredObject targetOne = new DiscoveredObject(1, TARGET, ["idTarget": "1", "fdnTarget": "fdn=1"])
        DiscoveredObject sourceOne = new DiscoveredObject(1, SOURCE, ["idSource": "1", "fdnSource": "fdn=1"])
        sourceOne.setAdditionalProperties(targetOne.getProperties())
        DiscoveryContext discoveryContext = createDiscoveryContext([sourceOne], [targetOne], filterDto)

        when: "Apply filters"
        comparisonEngine.applyFilters(discoveryContext)

        then: "Source object filter is not matched using the linked target object"
        assertFilterNotMatching(sourceOne, filterName)
    }

    def "Apply 'SourceNotInTarget' condition with arg and linked target"() {

        setup: "'SourceNotInTarget' condition with  arg"
        FilterConditionDto condition = new FilterConditionDto(name: SOURCENOTINTARGET, arg: 'propSource:propTarget')
        Map<String, ApplicationConfigurationFilterDto> filterDto = [(filterName): new ApplicationConfigurationFilterDto(
                condition: condition, filterMatchText: "Filter has matched!!!")]

        and: "Source object with a linked target object"
        DiscoveredObject targetOne = new DiscoveredObject(1, TARGET, ["idTarget": "1", "fdnTarget": "fdn=1", propTarget: "v2"])
        DiscoveredObject sourceOne = new DiscoveredObject(1, SOURCE, ["idSource": "1", "fdnSource": "fdn=1", propSource: "v2"])
        sourceOne.setAdditionalProperties(targetOne.getProperties())
        DiscoveryContext discoveryContext = createDiscoveryContext([sourceOne], [targetOne], filterDto)

        when: "Apply filters"
        comparisonEngine.applyFilters(discoveryContext)

        then: "Source object filter is not matched using the linked target object"
        assertFilterNotMatching(sourceOne, filterName)
    }

    def "Apply 'SourceMismatchedInTarget' condition"() {

        setup: "'SourceMismatchedInTarget' condition"
        FilterConditionDto condition = new FilterConditionDto(name: SOURCEMISMATCHEDINTARGET, arg: conditionArg)
        Map<String, ApplicationConfigurationFilterDto> filterDto = [(filterName): new ApplicationConfigurationFilterDto(
                condition: condition, filterMatchText: "Filter has matched!!!")]

        and: "Source and linked target objects (link required for filter)"
        DiscoveredObject targetOne = new DiscoveredObject(1, TARGET, ["idTarget": "1", "prop1Target": "value1", "prop2Target": "value2"])
        DiscoveredObject targetTwo = new DiscoveredObject(1, TARGET, ["idTarget": "2", "prop1Target": "", "prop2Target": "value2"])
        DiscoveredObject targetThree = new DiscoveredObject(1, TARGET, ["idTarget": "3", "prop1Target": "value1", "prop2Target": "value2"])
        DiscoveredObject sourceOne = new DiscoveredObject(1, SOURCE, ["idSource": "1", "prop1Source": "value1", "prop2Source": "value2"])
        sourceOne.additionalProperties = targetOne.getProperties()
        DiscoveredObject sourceTwo = new DiscoveredObject(1, SOURCE, ["idSource": "2", "prop1Source": "value1", "prop2Source": "value2"])
        sourceTwo.additionalProperties = targetTwo.getProperties()
        DiscoveredObject sourceThree = new DiscoveredObject(1, SOURCE, ["idSource": "3", "prop1Source": "value1", "prop2Source": "value2"])
        sourceThree.additionalProperties = targetThree.getProperties()
        DiscoveryContext discoveryContext = createDiscoveryContext(
                [sourceOne, sourceTwo, sourceThree], [targetOne, targetTwo, targetThree], filterDto)

        when: "Apply filters"
        comparisonEngine.applyFilters(discoveryContext)

        then: "Expected filters match"
        assertFilterNotMatching(sourceOne, filterName)
        assertFilterMatching(sourceTwo, filterName, filterDto.filter1.filterMatchText)
        assertFilterNotMatching(sourceThree, filterName)

        where:
        conditionArg                                      | _
        "prop1Source:prop1Target"                         | _
        "prop1Source:prop1Target&prop2Source:prop2Target" | _
    }

    def "Apply 'TargetNotInSource' condition with arg and no linked targets"() {

        setup: "'TargetNotInSource' condition"
        FilterConditionDto condition = new FilterConditionDto(name: TARGETNOTINSOURCE, arg: conditionArg)
        Map<String, ApplicationConfigurationFilterDto> filterDto = [(filterName): new ApplicationConfigurationFilterDto(
                condition: condition, filterMatchText: "Filter has matched!!!")]

        and: "Source and target objects"
        DiscoveredObject sourceOne = new DiscoveredObject(1, SOURCE, ["idSource": "1", "fdnSource": "fdn=1"])
        DiscoveredObject targetOne = new DiscoveredObject(1, TARGET, ["idTarget": "1", "fdnTarget": "fdn=1"])
        DiscoveredObject targetTwo = new DiscoveredObject(1, TARGET, ["idTarget": "2", "fdnTarget": "fdn=2"])
        DiscoveryContext discoveryContext = createDiscoveryContext(
                [sourceOne], [targetOne, targetTwo], filterDto)

        when: "Apply filters"
        comparisonEngine.applyFilters(discoveryContext)

        then: "Expected filters match"
        assertFilterMatching(targetTwo, filterName, filterDto.filter1.filterMatchText)
        assertFilterNotMatching(targetOne, filterName)

        where:
        conditionArg                            | _
        "fdnSource:fdnTarget"                   | _
        "fdnSource:fdnTarget&idSource:idTarget" | _
    }

    def "Apply 'TargetNotInSource' condition with no arg and linked source"() {

        setup: "'TargetNotInSource' condition with no arg"
        FilterConditionDto condition = new FilterConditionDto(name: TARGETNOTINSOURCE)
        Map<String, ApplicationConfigurationFilterDto> filterDto = [(filterName): new ApplicationConfigurationFilterDto(
                condition: condition, filterMatchText: "Filter has matched!!!")]

        and: "Source and linked target object"
        DiscoveredObject sourceOne = new DiscoveredObject(1, SOURCE, ["idSource": "1", "fdnSource": "fdn=1"])
        DiscoveredObject targetOne = new DiscoveredObject(1, TARGET, ["idTarget": "1", "fdnTarget": "fdn=1"])
        DiscoveredObject targetTwo = new DiscoveredObject(1, TARGET, ["idTarget": "2", "fdnTarget": "fdn=2"])
        sourceOne.setAdditionalProperties(targetOne.properties)
        targetOne.setAdditionalProperties(sourceOne.properties)
        DiscoveryContext discoveryContext = createDiscoveryContext([sourceOne], [targetOne, targetTwo], filterDto)

        when: "Apply filters"
        comparisonEngine.applyFilters(discoveryContext)

        then: "target object filter is matched using the linked source object"
        assertFilterMatching(targetTwo, filterName, filterDto.filter1.filterMatchText)
        assertFilterNotMatching(targetOne, filterName)
    }

    def "Apply 'TargetNotInSource' condition with arg and linked source"() {

        setup: "'TargetNotInSource' condition with arg"
        FilterConditionDto condition = new FilterConditionDto(name: TARGETNOTINSOURCE, arg: 'sourceProp:targetProp')
        Map<String, ApplicationConfigurationFilterDto> filterDto = [(filterName): new ApplicationConfigurationFilterDto(
                condition: condition, filterMatchText: "Filter has matched!!!")]

        and: "Source and linked target objects"
        DiscoveredObject sourceOne = new DiscoveredObject(1, SOURCE, ["idSource": "1", "fdnSource": "fdn=1", sourceProp: "v1"])
        DiscoveredObject targetOne = new DiscoveredObject(1, TARGET, ["idTarget": "1", "fdnTarget": "fdn=1", targetProp: "v1"])
        DiscoveredObject targetTwo = new DiscoveredObject(1, TARGET, ["idTarget": "2", "fdnTarget": "fdn=2", targetProp: "v2"])
        sourceOne.setAdditionalProperties(targetOne.properties)
        targetOne.setAdditionalProperties(sourceOne.properties)
        DiscoveryContext discoveryContext = createDiscoveryContext([sourceOne], [targetOne, targetTwo], filterDto)

        when: "Apply filters"
        comparisonEngine.applyFilters(discoveryContext)

        then: "target object filter is matched using the linked source object"
        assertFilterMatching(targetTwo, filterName, filterDto.filter1.filterMatchText)
        assertFilterNotMatching(targetOne, filterName)
    }

    def "Apply multiple filters"() {

        setup: "Multiple filters"
        FilterConditionDto condition1 = new FilterConditionDto(name: SOURCEINTARGET, arg: "fdnSource:fdnTarget")
        ApplicationConfigurationFilterDto filterDto1 = new ApplicationConfigurationFilterDto(
                condition: condition1, filterMatchText: "Filter one has matched!!!")
        FilterConditionDto condition2 = new FilterConditionDto(name: SOURCENOTINTARGET, arg: "fdnSource:fdnTarget")
        String filterName2 = "filter2"
        ApplicationConfigurationFilterDto filterDto2 = new ApplicationConfigurationFilterDto(
                condition: condition2, filterMatchText: "Filter two has matched!!!")
        Map<String, ApplicationConfigurationFilterDto> filterDtos = [(filterName): filterDto1, (filterName2): filterDto2]


        and: "Source and target objects"
        DiscoveredObject sourceOne = new DiscoveredObject(1, SOURCE, ["id": "1", "fdnSource": "fdn=1"])
        DiscoveredObject sourceTwo = new DiscoveredObject(1, SOURCE, ["id": "2", "fdnSource": "fdn=not_in_target"])
        DiscoveredObject sourceThree = new DiscoveredObject(1, SOURCE, ["id": "3", "fdnSource": "fdn=3"])
        DiscoveredObject targetOne = new DiscoveredObject(1, TARGET, ["id": "4", "fdnTarget": "fdn=1"])
        DiscoveredObject targetTwo = new DiscoveredObject(1, TARGET, ["id": "5", "fdnTarget": "fdn=2"])
        DiscoveredObject targetThree = new DiscoveredObject(1, TARGET, ["id": "6", "fdnTarget": "fdn=3"])
        DiscoveryContext discoveryContext = createDiscoveryContext(
                [sourceOne, sourceTwo, sourceThree], [targetOne, targetTwo, targetThree], filterDtos)

        when: "Apply filters"
        comparisonEngine.applyFilters(discoveryContext)

        then: "Filter one matches as expected"
        assertFilterMatching(sourceOne, filterName, filterDto1.filterMatchText)
        assertFilterNotMatching(sourceTwo, filterName)
        assertFilterMatching(sourceThree, filterName, filterDto1.filterMatchText)

        and: "Filter two matches as expected"
        assertFilterNotMatching(sourceOne, filterName2)
        assertFilterMatching(sourceTwo, filterName2, filterDto2.filterMatchText)
        assertFilterNotMatching(sourceThree, filterName2)
    }

    def "Apply custom source groovy filter expression"() {

        setup: "filter with groovy expression"
        FilterConditionDto condition = new FilterConditionDto(name: SOURCESCRIPT, arg: expression)
        Map<String, ApplicationConfigurationFilterDto> filterDto = [(filterName): new ApplicationConfigurationFilterDto(
                condition: condition, filterMatchText: "Filter has matched!!!")]

        and: "Source and target objects"
        DiscoveredObject sourceOne = new DiscoveredObject(1, SOURCE, ["id": "1"])
        DiscoveredObject sourceTwo = new DiscoveredObject(1, SOURCE, ["id": "2"])
        DiscoveredObject targetOne = new DiscoveredObject(1, TARGET, ["id": "1"])
        DiscoveryContext discoveryContext = createDiscoveryContext([sourceOne, sourceTwo], [targetOne], filterDto)

        when: "Apply filters"
        comparisonEngine.applyFilters(discoveryContext)

        then: "Expected filters match"
        if (sourceOneMatch) {
            assert assertFilterMatching(sourceOne, filterName, filterDto.filter1.filterMatchText)
        } else {
            assert assertFilterNotMatching(sourceOne, filterName)
        }
        if (sourceTwoMatch) {
            assert assertFilterMatching(sourceTwo, filterName, filterDto.filter1.filterMatchText)
        } else {
            assert assertFilterNotMatching(sourceTwo, filterName)
        }

        where:
        expression                                                              | sourceOneMatch | sourceTwoMatch
        true                                                                    | true           | true
        "true"                                                                  | true           | true
        false                                                                   | false          | false
        "false"                                                                 | false          | false
        "null"                                                                  | false          | false
        "targets.stream().noneMatch(t -> t.get('id').equals(source.get('id')))" | false          | true
    }

    def "Apply custom source groovy filter script"() {

        setup: "Mock assetService to return groovy script"
        String scriptContent = "targets.stream().noneMatch(t -> t.get('id').equals(source.get('id')))"
        Class<Script> scriptClass = new GroovyShell().parse(scriptContent).getClass()
        assetService.getGroovyScript(_, _) >> scriptClass

        and: "filter with groovy script"
        FilterConditionDto condition = new FilterConditionDto(name: SOURCESCRIPT, arg: "@script.groovy")
        Map<String, ApplicationConfigurationFilterDto> filterDto = [(filterName): new ApplicationConfigurationFilterDto(
                condition: condition, filterMatchText: "Filter has matched!!!")]

        and: "Source and target objects"
        DiscoveredObject sourceOne = new DiscoveredObject(1, SOURCE, ["id": "1"])
        DiscoveredObject sourceTwo = new DiscoveredObject(1, SOURCE, ["id": "2"])
        DiscoveredObject targetOne = new DiscoveredObject(1, TARGET, ["id": "1"])
        DiscoveryContext discoveryContext = createDiscoveryContext([sourceOne, sourceTwo], [targetOne], filterDto)

        when: "Apply filters"
        comparisonEngine.applyFilters(discoveryContext)

        then: "Expected filters match"
        assert assertFilterMatching(sourceTwo, filterName, filterDto.filter1.filterMatchText)
        assert assertFilterNotMatching(sourceOne, filterName)
    }

    def "Apply custom target groovy filter expression"() {

        setup: "target filter condition with groovy expression"
        String expression = "sources.stream().noneMatch(s -> s.get('id').equals(target.get('id')))"
        FilterConditionDto condition = new FilterConditionDto(name: TARGETSCRIPT, arg: expression)
        Map<String, ApplicationConfigurationFilterDto> filterDto = [(filterName): new ApplicationConfigurationFilterDto(
                condition: condition, filterMatchText: "Filter has matched!!!")]

        and: "Source and target objects"
        DiscoveredObject sourceOne = new DiscoveredObject(1, SOURCE, ["id": "1"])
        DiscoveredObject targetOne = new DiscoveredObject(1, TARGET, ["id": "1"])
        DiscoveredObject targetTwo = new DiscoveredObject(1, TARGET, ["id": "2"])
        DiscoveryContext discoveryContext = createDiscoveryContext([sourceOne], [targetOne, targetTwo], filterDto)

        when: "Apply filters"
        comparisonEngine.applyFilters(discoveryContext)

        then: "Expected filters match"
        assertFilterNotMatching(targetOne, filterName)
        assertFilterMatching(targetTwo, filterName, filterDto.filter1.filterMatchText)
    }

    DiscoveryContext createDiscoveryContext(List sources, List targets, Map<String, ApplicationConfigurationFilterDto> filterDto) {
        def jobConfigurationDto = new ApplicationConfigurationJobDto(name: "test",
                discover: new ApplicationConfigurationDiscoverDto(filters: filterDto))
        def applicationEntity = new ApplicationEntity(
                id: 1, featurePack: new FeaturePackEntity(id: 1), config: new ApplicationConfigurationDto(
                jobs: [jobConfigurationDto]))
        DiscoveryContext discoveryContext = DiscoveryContext
                .initialize(applicationEntity, new JobEntity(id: 1, applicationJobName: "test"))
        discoveryContext.setSources(sources)
        discoveryContext.setTargets(targets)
        return discoveryContext
    }

    boolean assertFilterMatching(DiscoveredObject discoveredObject, String filterName, String discrepancy) {
        return discoveredObject.getFilterResults()
                .stream()
                .anyMatch(f -> (filterName.equalsIgnoreCase(f.name) && f.matched && discrepancy.equalsIgnoreCase(f.discrepency)))
    }

    boolean assertFilterNotMatching(DiscoveredObject discoveredObject, String filterName) {
        return discoveredObject.getFilterResults()
                .stream()
                .anyMatch(f -> (filterName.equalsIgnoreCase(f.name) && !f.matched))
    }

    @TestConfiguration
    @ComponentScan(basePackages = ["com.ericsson.bos.dr.service.compare"])
    static class ComparisonEngineTestConfig {
    }
}
