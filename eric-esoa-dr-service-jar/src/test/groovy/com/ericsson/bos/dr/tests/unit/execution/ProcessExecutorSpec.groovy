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

import com.ericsson.bos.dr.service.execution.executors.CommandResponse
import com.ericsson.bos.dr.service.execution.executors.command.ProcessExecutor
import com.ericsson.bos.dr.tests.integration.utils.IOUtils
import org.apache.commons.lang3.SystemUtils
import spock.lang.IgnoreIf
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path

@IgnoreIf({ SystemUtils.IS_OS_WINDOWS })
class ProcessExecutorSpec extends Specification {

    ProcessExecutor processExecutor = new ProcessExecutor(processPath: "/usr/bin:/usr/local/bin")

    def 'Process Executor returns large command output'() {

        setup: "write file to the filesystem"
        String response = IOUtils.readClasspathResource("/python_scripts/enm_export.xml")
        Path path = Path.of(System.getProperty("java.io.tmpdir") + "/output.xml")
        Files.write(path, response.getBytes())

        when: "Execute command"
        CommandResponse commandResponse = processExecutor.executeProcess("cat ${path.toString()}")

        then: "Command output is returned"
        noExceptionThrown()
        commandResponse.getResponse().replaceAll("\\s", "") == response.replaceAll("\\s", "")
    }

    def 'Command arguments containing command substitutions are escaped to prevent execution'() {

        when: "Execute echo command with arg containing command substitution"
        String command = "echo ${commandArg}"
        CommandResponse commandResponse = processExecutor.executeProcess(command)

        then: "Command output is returned as expected"
        commandResponse.getResponse() == commandArg

        where:
        commandArg | _
        "`touch /tmp/test.txt`" | _
        "\$(touch /tmp/test.txt)" | _
        "\$(touch /tmp/test.txt \$ls))" | _
        "\$(touch /tmp/test.txt `ls`))" | _
        "`touch /tmp/test.txt \$(ls)`" | _
    }
}