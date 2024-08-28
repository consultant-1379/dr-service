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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.SystemUtils;

/**
 * Formats the command based the OS
 */
@SuppressWarnings("squid:S2068")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CommandFormatter {

    private static final String POWER_SHELL = "powershell.exe";
    private static final String POWER_SHELL_ARG = "-NoProfile";
    private static final String BASH = "bash";
    private static final String BASH_COMMAND_ARG = "-c";
    private static final String BASH_RESTRICTIVE_ARG = "-r";
    private static final  Pattern COMMAND_SUBSTITUTION_PATTEN = Pattern.compile("\\$\\([^)]+\\)+");


    /**
     * Constructs the full unix or windows command, where windows commands
     * are intended for local testing only.
     * <br><br>
     * In case of the target unix OS:
     * <ul>
     *     <li>The command is executed in a restricted bash shell.</li>
     *     <li>
     *         Command substitution is not supported. The special
     *         characters ('`', '$', '(', ')') used for substitution are escaped so
     *         they are treated as literals.
     *     </li>
     * </ul>
     * @param command command
     * @return List of command arguments
     */
    public static List<String> constructFullCommand(final String command) {
        final List<String> commandsToExecute = new ArrayList<>();
        if (SystemUtils.IS_OS_WINDOWS) {
            commandsToExecute.add(POWER_SHELL);
            commandsToExecute.add(POWER_SHELL_ARG);
        } else {
            commandsToExecute.add(BASH);
            commandsToExecute.add(BASH_COMMAND_ARG);
            commandsToExecute.add(BASH_RESTRICTIVE_ARG);
        }
        commandsToExecute.add(escapeCommandSubstitutions(command));
        return commandsToExecute;
    }

    private static String escapeCommandSubstitutions(final String command) {
        String escapedCommand  = command;
        final Matcher matcher = COMMAND_SUBSTITUTION_PATTEN.matcher(command);
        if (matcher.find()) {
            // find all command substitution statements e.g $(rm -rf) and backslash escape, so they
            // are not executed by the shell. For example statement '$(rm -rf)' will be updated to '\$\(rm -rf\)'
            escapedCommand = matcher.replaceAll(result -> result.group().replace("$", "\\\\\\$")
                    .replace("(", "\\\\\\(")
                    .replace(")", "\\\\\\)"));
        }
        // need to also backslash escape old command substitution style using backticks.
        return escapedCommand.replace("`", "\\`");
    }
}