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

package com.ericsson.bos.dr.service.compare.filters;

import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationFilterDto;
import com.ericsson.bos.dr.web.v1.api.model.FilterConditionDto.NameEnum;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;

/**
 * Check a source object exists in the target but has one or more mismatching properties.
 * Requires the linking of sources and targets has been performed in order to identify the source exists in the
 * target. The properties defined in the condition arg are used to identify mismatches.
 * <p>
 * If the source has not been linked to a target then the condition will return false.</p>
 * <p>
 * If source has a linked target and one of the properties are mismatching then will return true.</p>
 */
@Component
class SourceMismatchedInTarget implements Condition {

    @Override
    public boolean test(ApplicationConfigurationFilterDto filterDef, FilterContext filterCtx) {
        final var hasLinkedTarget = MapUtils.isNotEmpty(filterCtx.getSource().getAdditionalProperties());
        if (hasLinkedTarget) {
            final var source = filterCtx.getSourceProperties();
            final var target = filterCtx.getSource().getAdditionalProperties();
            final var propertiesArg = new PropertiesArg(filterDef.getCondition().getArg());
            final var propertiesPredicate = new PropertiesEqualsPredicate(source);
            return propertiesArg.getParts().stream().anyMatch(p -> propertiesPredicate.negate().test(p, target));
        }
        return false;
    }

    @Override
    public boolean supports(ApplicationConfigurationFilterDto filterDef) {
        return filterDef.getCondition() != null ? NameEnum.SOURCEMISMATCHEDINTARGET.equals(filterDef.getCondition().getName()): false;
    }

    @Override
    public TYPE getType() {
        return TYPE.SOURCE;
    }
}