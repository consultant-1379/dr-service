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
import com.ericsson.bos.dr.service.execution.executors.CommandExecutorException
import com.ericsson.bos.dr.service.execution.executors.CommandResponse
import com.ericsson.bos.dr.service.execution.executors.command.ProcessExecutor
import com.ericsson.bos.dr.service.execution.executors.shell.ShellCommandProcessor
import com.ericsson.bos.dr.service.execution.executors.shell.ShellExecutor
import com.ericsson.bos.dr.service.substitution.SubstitutionEngine
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationActionDto
import spock.lang.Specification
import spock.lang.Unroll

class ShellExecutorSpec extends Specification {

    ProcessExecutor processExecutor = new ProcessExecutor(processPath: "/usr/bin:/usr/local/bin")
    ShellCommandProcessor shellCommandProcessor = new ShellCommandProcessor(substitutionEngine: new SubstitutionEngine(
            propertiesService: Mock(PropertiesService)))

    ShellExecutor shellExecutor = new ShellExecutor(
            processExecutor: processExecutor,
            shellCommandProcessor: shellCommandProcessor)

    @Unroll
    def 'Shell Executor completes successfully with/without substitution'() {

        when: "Execute shell script"
        ApplicationConfigurationActionDto actionDto = new ApplicationConfigurationActionDto()
                .command(shellCommand)
                .properties([:])
        ExecutionContext executionContext = new ExecutionContext(1l, actionDto, substitutionCtx)
        CommandResponse commandResponse = shellExecutor.execute(executionContext)

        then:
        commandResponse.getResponse() == output

        where:
        shellCommand         | substitutionCtx         | output
        'echo "hello_world"' | [:]                     | "hello_world"
        'echo "{{output}}"'  | [output: "hello_world"] | "hello_world"
    }

    def 'Shell Executor throws CommandExecutorException when non zero exit code'() {

        when: "Execute shell script with syntax error"
        String shellCommand = 'ech "hello_world"'
        ApplicationConfigurationActionDto actionDto = new ApplicationConfigurationActionDto()
                .command(shellCommand)
                .properties([:])
        ExecutionContext executionContext = new ExecutionContext(1l, actionDto, [:])
        shellExecutor.execute(executionContext)

        then: "CommandExecutorException thrown containing command output"
        CommandExecutorException exception = thrown(CommandExecutorException)
        !exception.getCommandOutput().isEmpty()
   }
}
