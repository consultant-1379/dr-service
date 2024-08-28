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


import com.ericsson.bos.dr.service.AssetService
import com.ericsson.bos.dr.service.PropertiesService
import com.ericsson.bos.dr.service.exceptions.ErrorCode
import com.ericsson.bos.dr.service.execution.ExecutionContext
import com.ericsson.bos.dr.service.execution.ExecutionEngine
import com.ericsson.bos.dr.service.execution.ExecutionEngineException
import com.ericsson.bos.dr.service.execution.ExecutionResult
import com.ericsson.bos.dr.service.execution.executors.CommandExecutor
import com.ericsson.bos.dr.service.execution.executors.CommandResponse
import com.ericsson.bos.dr.service.execution.executors.http.HttpExecutor
import com.ericsson.bos.dr.service.execution.executors.python.PythonCommandProcessor
import com.ericsson.bos.dr.service.execution.executors.python.PythonExecutor
import com.ericsson.bos.dr.service.substitution.SubstitutionEngine
import com.ericsson.bos.dr.tests.integration.utils.JsonUtils
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationActionDto
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import spock.lang.Specification
import spock.lang.Unroll

import static com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationActionDto.OutputFormatEnum
import static com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationActionDto.TypeEnum
import static org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE

@TestPropertySource(properties = ["service.substitution.fail-on-unknown-tokens=true",
        "security.tls.enabled=false"])
@ContextConfiguration(classes = ExecutionEngineTestConfig.class)
class ExecutionEngineSpec extends Specification {

    @Autowired
    ExecutionEngine engine

    @SpringBean
    PropertiesService propertiesServiceMock = Mock()

    @SpringBean
    AssetService assetService = Mock()

    String scriptContents

    @Unroll
    def "Execution flow completes successfully"() {

        setup: "ApplicationConfigurationActionDto"
        Map<String, Object> initialInputs = [inputs: ["id": "1", name: "object1"]]
        ApplicationConfigurationActionDto actionDto = new ApplicationConfigurationActionDto()
                .type(TypeEnum.REST)
                .preFunction(preFunction)
                .postFunction(postFunction)
                .properties(props)
                .outputFormat(OutputFormatEnum.JSON)
                .mapping(mappings)

        and: "Mock assetService to return script asset"
        String scriptContents = '{"id": "{{inputs.id}}", "name": "{{inputs.name}}"}'
        assetService.getAssetContent(_, _) >> scriptContents.bytes

        when: "Execute"
        ExecutionResult executionResult = engine.execute(new ExecutionContext(1l, actionDto, initialInputs))

        then: "The executed command and original command response are returned"
        executionResult.getCommand() == "a command"
        executionResult.getCommandResponse() == expectedCommandResponse

        and: "The mapped response is returned as expected"
        List mappedResponse = executionResult.getMappedCommandResponse()
        mappedResponse == expectedResponse

        where:
        [preFunction, postFunction, props, mappings, expectedCommandResponse, expectedResponse] <<
                [executionWithFunctions(),
                 executionWithoutFunctions(),
                 executionWithListResponse(),
                 executionWithPreFunctionScript()]
    }

    def "Mapped value is null when jq expression returns NullValue"() {

        setup: "use jqExpression which results in NullValue "
        ApplicationConfigurationActionDto actionDto = new ApplicationConfigurationActionDto()
                .type(TypeEnum.REST)
                .preFunction(null)
                .postFunction(null)
                .properties([commandResult: '{"id": 1}'])
                .outputFormat(OutputFormatEnum.JSON)
                .mapping([id: ".name"])

        when: "Execute"
        ExecutionResult executionResult = engine.execute(new ExecutionContext(1l, actionDto, [:]))

        then: "Mapped id value is null"
        executionResult.mappedCommandResponse[0]['id'] == null
    }

    def "Exception is thrown when jq expression is invalid"() {

        setup: "invalid jqExpression"
        ApplicationConfigurationActionDto actionDto = new ApplicationConfigurationActionDto()
                .type(TypeEnum.REST)
                .preFunction(null)
                .postFunction(null)
                .properties([commandResult: '{"id": 1}'])
                .outputFormat(OutputFormatEnum.JSON)
                .mapping([id: jqExpr])

        when: "Execute"
        engine.execute(new ExecutionContext(1l, actionDto, [:]))

        then: "ExecutionEngineException is thrown"
        ExecutionEngineException exception = thrown(ExecutionEngineException)
        exception.errorMessage.errorCode == ErrorCode.EXECUTION_STEP_ERROR.errorCode
        exception.message.contains("DR-13:JQ expression '${jqExpr}' resulted in an error")

        where:
        jqExpr | _
        ".[] | .id" | _
        "..." | _
    }

    def "Execution flow completes successfully when no mappings are specified for JSON output format"() {

        setup: "no mappings"
        ApplicationConfigurationActionDto actionDto = new ApplicationConfigurationActionDto()
                .type(TypeEnum.REST)
                .preFunction(null)
                .postFunction(null)
                .properties([commandResult: '{"id": 1}'])
                .outputFormat(OutputFormatEnum.JSON)
                .mapping(null)

        when: "Execute"
        ExecutionResult executionResult = engine.execute(new ExecutionContext(1l, actionDto, [:]))

        then: "The executed command is returned"
        executionResult.getCommand() == "a command"
    }

    def executionWithFunctions() {
        return ['{"id": "{{inputs.id}}", "name": "{{inputs.name}}"}',
                '{{originalOutputs}}',
                [commandResult: '{"id":"{{preFunction.id}}","name":"{{preFunction.name}}","state":"Active"}'],
                [id:".id","name":".name","state":".state"],
                '{"id":"1","name":"object1","state":"Active"}',
                [["id":"1","name":"object1",state:"Active"]]]
    }

    def executionWithoutFunctions() {
        return [null,
                null,
                [commandResult: '{"id":"{{inputs.id}}","name":"{{inputs.name}}","state":"Active"}'],
                [id:".id","name":".name","state":".state"],
                '{"id":"1","name":"object1","state":"Active"}',
                [["id":"1","name":"object1",state:"Active"]]]
    }

    def executionWithListResponse() {
        String commandResult = '[{"id":"1","name":"object1","state":"Active"},' +
                '{"id":"2","name":"object2","state":"Active"},' +
                '{"id":"3","name":"object3","state":"Active"}]'
        return [null,
                null,
                [commandResult: commandResult],
                [id:".id","name":".name","state":".state"],
                commandResult,
                JsonUtils.readList(commandResult, Map)]
    }

    def executionWithPreFunctionScript() {
        return ['@script.groovy',
                '{{originalOutputs}}',
                [commandResult: '{"id":"{{preFunction.id}}","name":"{{preFunction.name}}","state":"Active"}'],
                [id:".id","name":".name","state":".state"],
                '{"id":"1","name":"object1","state":"Active"}',
                [["id":"1","name":"object1",state:"Active"]]]
    }

    @TestConfiguration
    @ComponentScan(basePackages = ["com.ericsson.bos.dr.service.execution", "com.ericsson.bos.dr.service.substitution"],
            excludeFilters = @ComponentScan.Filter(type = ASSIGNABLE_TYPE, classes = [HttpExecutor.class, PythonExecutor.class,
                    PythonCommandProcessor.class]))
    static class ExecutionEngineTestConfig {

        @Bean
        CommandExecutor testExecutor(SubstitutionEngine substitutionEngine) {
            /**
             * Test Command Executor which returns the value of the prop 'commandResult' after performing
             * jinja substitution.
             */
            return new CommandExecutor() {
                @Override
                CommandResponse execute(ExecutionContext executionContext) {
                    final String command = "a command"
                    final String commandResult = (String) executionContext.getActionDto().getProperties().get("commandResult");
                    return new CommandResponse(command, substitutionEngine.render(commandResult, executionContext.getSubstitutionCtx(), -1l));
                }

                @Override
                boolean canExecute(String type) {
                    return true
                }
            }
        }
    }
}