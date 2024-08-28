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
package com.ericsson.bos.dr.service.execution.executors.python;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.ericsson.bos.dr.service.AssetService;
import com.ericsson.bos.dr.service.execution.ExecutionContext;
import com.ericsson.bos.dr.service.substitution.SubstitutionEngine;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Process a python command defined in the Action configuration.
 */
@Component
public class PythonCommandProcessor {

    @Autowired
    private SubstitutionEngine substitutionEngine;

    @Autowired
    private AssetService assetService;

    @Autowired
    private PythonAssetFilesystemStore pythonAssetStore;

    /**
     * Create PythonCommand for the supplied python Action definition.
     * Substitution is performed on the defined action properties using the
     * provided substitution context.
     * Writes the python asset file to the filesystem for execution if not already present.
     *
     * @param executionContext action execution context
     * @return <code>PythonCommand</code>
     */
    public PythonCommand parse(ExecutionContext executionContext) {
        final var pythonCommand = new PythonCommand();
        final String command = executionContext.getActionDto().getCommand();
        final long featurePackId = executionContext.getFeaturePackId();
        pythonCommand.setCommand(command);
        pythonCommand.setSubstitutedProperties(getArgs(executionContext));
        pythonCommand.setPath(pythonAssetStore.getPath(featurePackId, command));
        return pythonCommand;
    }

    private ArrayList<Object> getArgs(ExecutionContext executionContext) {
        final Map<String, Object> properties = Optional.ofNullable(executionContext.getActionDto().getProperties())
                .orElse(new HashMap<>());
        final Map<String, Object> substitutionCtx = executionContext.getSubstitutionCtx();
        final ArrayList<Object> props = new ArrayList<>();
        properties.entrySet().stream()
                .filter(entry -> StringUtils.startsWith(entry.getKey(), "arg"))
                .filter(entry -> StringUtils.isNotBlank((String) entry.getValue()))
                .sorted((Map.Entry.comparingByKey(argsComparator())))
                .map(entry -> (String) entry.getValue())
                .forEach(v -> props.add(substitute(v, substitutionCtx, executionContext.getFeaturePackId())));
        return props;
    }

    private String substitute(String command, Map<String, Object> substitutionCtx, long featurePackId) {
        return substitutionEngine.render(command, substitutionCtx, featurePackId);
    }

    private Comparator<String> argsComparator() {
        return (s1, s2) -> {
            final int arg1Index = Integer.parseInt(s1.substring(3));
            final int arg2Index = Integer.parseInt(s2.substring(3));
            return Integer.compare(arg1Index, arg2Index);
        };
    }
}