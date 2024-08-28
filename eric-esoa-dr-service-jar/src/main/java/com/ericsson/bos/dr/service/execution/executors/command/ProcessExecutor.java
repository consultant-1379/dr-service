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
package com.ericsson.bos.dr.service.execution.executors.command;

import static com.ericsson.bos.dr.service.execution.executors.command.CommandFormatter.constructFullCommand;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.ericsson.bos.dr.service.execution.executors.CommandExecutorException;
import com.ericsson.bos.dr.service.execution.executors.CommandResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Executes commands/scripts in the shell
 */
@Component
public class ProcessExecutor {

    private static final Pattern NON_ASCII_CHARS = Pattern.compile("[^\\p{ASCII}]");

    /**
     * Limit the process to executing commands in the configured path.
     */
    @Value("${service.shell-executor.process-path}")
    private String processPath;

    /**
     * Executes the given command and returns the response.
     *
     * @param command   command to execute
     * @param env variables to include in the process env
     * @return CommandResponse
     * @throws IOException          IOException
     * @throws InterruptedException InterruptedException
     */
    public CommandResponse executeProcess(String command, Map<String, String> env) throws IOException, InterruptedException {
        final List<String> commandsToExecute = constructFullCommand(command);
        final var processBuilder = new ProcessBuilder(commandsToExecute).redirectErrorStream(true);
        setProcessPathEnv(processBuilder);
        processBuilder.environment().putAll(env);
        Process process = null;
        try {
            process = processBuilder.start();
            final var commandOutput = readProcessInputStream(process);
            process.waitFor();
            if (process.exitValue() != 0) {
                throw new CommandExecutorException(commandsToExecute.toString(), commandOutput);
            }
            return new CommandResponse(commandsToExecute.toString(), commandOutput);
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    /**
     * Executes the given command and returns the response.
     *
     * @param command   command to execute
     * @return CommandResponse
     * @throws IOException          IOException
     * @throws InterruptedException InterruptedException
     */
    public CommandResponse executeProcess(String command) throws IOException, InterruptedException {
        return executeProcess(command, Collections.emptyMap());
    }

    private String readProcessInputStream(final Process process) throws IOException {
        String output;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) { // NOSONAR
            output = br.lines().map(String::trim)
                    .collect(Collectors.joining(System.lineSeparator()));
            final var m = NON_ASCII_CHARS.matcher(output);
            output = m.replaceAll("");
        }
        return output;
    }

    private void setProcessPathEnv(final ProcessBuilder processBuilder) {
        final Map<String, String> env = processBuilder.environment();
        env.put("PATH", processPath);
    }
}