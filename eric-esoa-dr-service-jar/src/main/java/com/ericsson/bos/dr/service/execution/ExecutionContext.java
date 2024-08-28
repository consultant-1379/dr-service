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

package com.ericsson.bos.dr.service.execution;

import java.util.Map;

import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationActionDto;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Execution Context for execution of a defined action.
 */
public class ExecutionContext {

    private final long featurePackId;
    private final ApplicationConfigurationActionDto actionDto;
    private final Map<String, Object> substitutionCtx;

    /**
     * ExecutionContext.
     * @param featurePackId feature pack id
     * @param actionDto action
     * @param substitutionCtx substitution context
     */
    public ExecutionContext(long featurePackId, ApplicationConfigurationActionDto actionDto, Map<String, Object> substitutionCtx) {
        this.featurePackId = featurePackId;
        this.actionDto = actionDto;
        this.substitutionCtx = substitutionCtx;
    }

    public long getFeaturePackId() {
        return featurePackId;
    }

    public ApplicationConfigurationActionDto getActionDto() {
        return actionDto;
    }

    public Map<String, Object> getSubstitutionCtx() {
        return substitutionCtx;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
