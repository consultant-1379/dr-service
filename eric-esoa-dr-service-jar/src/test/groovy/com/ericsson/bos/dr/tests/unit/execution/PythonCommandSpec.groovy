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

import com.ericsson.bos.dr.service.execution.executors.python.PythonCommand
import spock.lang.Specification

import java.nio.file.Paths

class PythonCommandSpec extends Specification {

    def "should construct the full command"() {

        setup: "Get file as byte array"
        PythonCommand command = new PythonCommand()
        command.setCommand("python_script")
        command.setPath(Paths.get("/test/path"))
        command.setSubstitutedProperties(props)

        when: "full command constructed"
        String finalCommand = command.getFinalCommand()

        then: "command is as expected"
        command.getCommand() == "python_script"
        finalCommand.toString().startsWith("python")
        finalCommand.endsWith(expectedCommand)

        where:
        props | expectedCommand
        []    | "path"
        ["arg1"] | "path arg1"
    }
}
