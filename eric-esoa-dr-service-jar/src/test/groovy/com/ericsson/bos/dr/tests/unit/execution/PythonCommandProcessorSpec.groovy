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
package com.ericsson.bos.dr.tests.unit.execution


import com.ericsson.bos.dr.service.PropertiesService
import com.ericsson.bos.dr.service.execution.ExecutionContext
import com.ericsson.bos.dr.service.execution.executors.python.PythonAssetFilesystemStore
import com.ericsson.bos.dr.service.execution.executors.python.PythonCommand
import com.ericsson.bos.dr.service.execution.executors.python.PythonCommandProcessor
import com.ericsson.bos.dr.service.substitution.SubstitutionEngine
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationActionDto
import spock.lang.Specification

import java.nio.file.Path

class PythonCommandProcessorSpec extends Specification {

    PythonAssetFilesystemStore pythonAssetFilesystemStore = Mock(PythonAssetFilesystemStore)
    SubstitutionEngine substitutionEngine = new SubstitutionEngine(propertiesService: Mock(PropertiesService))
    PythonCommandProcessor pythonCommandProcessor = new PythonCommandProcessor(
            substitutionEngine: substitutionEngine, pythonAssetStore: pythonAssetFilesystemStore)

    def "should parse action to get python command"() {

        setup: "Get python script path"
        String assetPath = "/tmp/asset/1/sources.py"
        pythonAssetFilesystemStore.getPath(1l, "sources.py") >> Path.of(assetPath)

        when: "Parse"
        ApplicationConfigurationActionDto actionDto = new ApplicationConfigurationActionDto()
                .command("sources.py")
                .properties(props)
        PythonCommand pythonCommand = pythonCommandProcessor.parse(
                new ExecutionContext(1l, actionDto, substitutionContext))

        then: "Generated python command is as expected"
        pythonCommand.getFinalCommand().replaceAll("\\\\", "/") == expectedCommand

        where:
        props                                          | substitutionContext      | expectedCommand
        [:]                                            | [:]                      | "python3 /tmp/asset/1/sources.py"
        ["arg2": "v2", "arg1": "v1"]                   | [:]                      | "python3 /tmp/asset/1/sources.py v1 v2"
        ["arg2": "v2", "arg1": "{{inputs.arg1}}"]      | [inputs: ["arg1": "v1"]] | "python3 /tmp/asset/1/sources.py v1 v2"
        ["other": "other"]                             | [:]                      | "python3 /tmp/asset/1/sources.py"
        ["arg11": "v11", "arg30": "v30", "arg2": "v2"] | [:]                      | "python3 /tmp/asset/1/sources.py v2 v11 v30"
    }
}