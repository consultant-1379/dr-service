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

package com.ericsson.bos.dr.service.substitution.functions;

import static com.ericsson.bos.dr.service.substitution.SubstitutionEngine.FP_CTX_VAR;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.bos.dr.service.execution.ExecutionContext;
import com.ericsson.bos.dr.service.execution.ExecutionEngine;
import com.ericsson.bos.dr.service.utils.JSON;
import com.ericsson.bos.dr.service.utils.SpringContextHolder;
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationActionDto;
import com.hubspot.jinjava.interpret.JinjavaInterpreter;
import com.hubspot.jinjava.interpret.TemplateError;
import com.hubspot.jinjava.lib.fn.ELFunctionDefinition;

/**
 * Built-in jinja function to support executing actions.
 */
public class ExecuteActionFunction extends ELFunctionDefinition {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecuteActionFunction.class);

    /**
     * Constructor.
     */
    public ExecuteActionFunction() {
        super("fn", "execute", ExecuteActionFunction.class, "execute", String.class);
    }

    /**
     * Execute an action.
     *
     * @param executionOption
     *         execution option
     * @return Object
     */
    public static Object execute(String executionOption) {
        try {
            final var featurePackId = (Long) Optional.ofNullable(JinjavaInterpreter.getCurrent().getContext().get(FP_CTX_VAR))
                    .orElseThrow(() -> new IllegalStateException("Feature Pack id is not set in the context"));
            final var applicationConfigurationActionDto =
                    JSON.read(executionOption, ApplicationConfigurationActionDto.class);
            final var executionContext = new ExecutionContext(featurePackId, applicationConfigurationActionDto, new HashMap<>());
            final var executionEngine = SpringContextHolder.getBean(ExecutionEngine.class);
            final var executionResult = executionEngine.execute(executionContext);

            final List<Map<String, Object>> mappedResponse = executionResult.getMappedCommandResponse();
            final List<Object> result = mappedResponse.stream()
                    .map(r -> r.get("value"))
                    .filter(Objects::nonNull)
                    .filter(noneEmptyCollectionOrMap())
                    .collect(Collectors.toList());
            return JSON.toString(result);
        } catch (final Exception e) {
            LOGGER.error("Error occurred executing the action: " + executionOption, e);
            JinjavaInterpreter.getCurrent().addError(TemplateError.fromException(e));
        }
        return null;
    }

    // JQ will return an empty collection if the expression does not match any property in the object.
    // For example:
    // Object = [{"name", "subsystem_1"}, {"name", "subsystem_2"}]
    // Expression = 'select(.name| startswith (\"subsystem_1\"))|.name'
    // Will result in the following result from JQ: ["subsystem_1", []]
    // This method will remove the empty list so that result will now be: ["subsystem_1"]
    private static Predicate<Object> noneEmptyCollectionOrMap() {
        return object -> !(((object instanceof Collection) && (((Collection<?>) object).isEmpty())) ||
                            ((object instanceof Map) && (((Map<?, ?>) object).isEmpty())));
    }

}
