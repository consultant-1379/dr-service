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

import com.ericsson.bos.dr.jpa.model.AssetEntity
import com.ericsson.bos.dr.service.AssetService
import com.ericsson.bos.dr.service.PropertiesService
import com.ericsson.bos.dr.service.execution.ExecutionContext
import com.ericsson.bos.dr.service.execution.executors.CommandExecutorException
import com.ericsson.bos.dr.service.execution.executors.CommandResponse
import com.ericsson.bos.dr.service.execution.executors.command.ProcessExecutor
import com.ericsson.bos.dr.service.execution.executors.python.PythonAssetFilesystemStore
import com.ericsson.bos.dr.service.execution.executors.python.PythonCommandProcessor
import com.ericsson.bos.dr.service.execution.executors.python.PythonEnvSupplier
import com.ericsson.bos.dr.service.execution.executors.python.PythonExecutor
import com.ericsson.bos.dr.service.substitution.SubstitutionEngine
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationActionDto
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Unroll

import static com.ericsson.bos.dr.tests.integration.utils.IOUtils.readClasspathResource

@Ignore("Python is not available in the docker image used for running tests")
class PythonExecutorSpec extends Specification {

    AssetService assetService = Mock(AssetService)
    PythonAssetFilesystemStore pythonAssetFilesystemStore = new PythonAssetFilesystemStore(assetService: assetService,
            pythonAssetsDir: System.getProperty("java.io.tmpdir"))
    ProcessExecutor processExecutor = new ProcessExecutor(processPath: System.getenv().get("Path"))
    SubstitutionEngine substitutionEngine = new SubstitutionEngine(propertiesService: Mock(PropertiesService))
    PythonCommandProcessor pythonCommandProcessor = new PythonCommandProcessor(
            substitutionEngine: substitutionEngine, pythonAssetStore: pythonAssetFilesystemStore)

    PythonEnvSupplier pythonTlsEnvSupplier = new PythonEnvSupplier(
            tlsEnabled: true,
            caCertDir: "/tmp",
            caCertFile: "ca.pem",
            tlsCertDir: "/tmp",
            tlsCertFile: "client.crt",
            tlsKeyFile: "client.key",
            restServiceUrl: "https://rest-service")

    PythonEnvSupplier pythonEnvSupplier = new PythonEnvSupplier(tlsEnabled: false)

    PythonExecutor pythonExecutor = new PythonExecutor(processExecutor: processExecutor, pythonEnvSupplier: pythonEnvSupplier,
            pythonCommandProcessor: pythonCommandProcessor)

    PythonExecutor pythonTlsExecutor = new PythonExecutor(processExecutor: processExecutor, pythonEnvSupplier: pythonTlsEnvSupplier,
            pythonCommandProcessor: pythonCommandProcessor)

    @Unroll
    def 'Python Executor completes successfully'() {
        setup: "Get python script path"
        byte[] pythonScript = readClasspathResource("/python_scripts/" + script).getBytes()
        AssetEntity entity = new AssetEntity(contents: pythonScript)
        assetService.getAsset(_, _) >> entity

        when: "Execute python script"
        ApplicationConfigurationActionDto actionDto = new ApplicationConfigurationActionDto()
                .command(script)
                .properties(props)
        ExecutionContext executionContext = new ExecutionContext(new Random().nextLong(), actionDto, substitutionCtx)
        CommandResponse commandResponse = tlsEnabled ? pythonTlsExecutor.execute(executionContext) : pythonExecutor.execute(executionContext)

        then: "Command response is as expected"
        commandResponse.getResponse() == response
        commandResponse.getCommand().contains(script)

        where:
        script          | tlsEnabled | props                     | substitutionCtx             | response
        "script.py"     | false      | [:]                       | [:]                         | "Goodbye, World!"
        "script_arg.py" | false      | [arg1: "{{inputs.name}}"] | [inputs: [name: "Athlone"]] | "Goodbye, Athlone !"
        "script_tls.py" | true       | [:]                       | [:]                         | "https://rest-service,/tmp/client.crt,/tmp/client.key"
        "script_tls.py" | false      | [:]                       | [:]                         | "null,None,None"
    }

    def 'Python Executor throws CommandExecutorException when non zero exit code'() {
        setup: "python script with syntax error"
        AssetEntity entity = new AssetEntity(contents: "print(".bytes)
        assetService.getAsset(_, _) >> entity

        when: "Execute python script"
        ApplicationConfigurationActionDto actionDto = new ApplicationConfigurationActionDto()
                .command("python_script")
                .properties([:])
        ExecutionContext executionContext = new ExecutionContext(new Random().nextLong(), actionDto, [:])
        pythonExecutor.execute(executionContext)

        then: "CommandExecutorException thrown containing script output"
        CommandExecutorException exception = thrown(CommandExecutorException)
        exception.getCommandOutput().contains("SyntaxError: unexpected EOF while parsing")
    }
}