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

import java.util.Map;

import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationFilterDto;
import com.ericsson.bos.dr.web.v1.api.model.FilterConditionDto;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;

/**
 * Check for no matching source object in a list of target objects or no linked target object. Matching
 * is performed based on the source/target property keys defined in the condition arg.
 * <p>
 * If no arg is supplied then checks if the source has been linked to a target. Returns false if the source has a linked
 * target.</p>
 * <p>
 * If arg is supplied and source is linked to a target then checks the source and linked target properties.
 * Returns true if the properties are not equal.</p>
 * <p>
 * If arg is supplied and there is no linked target object, then checks for a match in the full target list. If
 * no match is found then returns true.
 * </p>
 */
@Component
class SourceNotInTarget implements Condition {

    @Override
    public boolean test(ApplicationConfigurationFilterDto filterDef, FilterContext filterCtx) {
        final var hasLinkedTarget = MapUtils.isNotEmpty(filterCtx.getSource().getAdditionalProperties());
        if (filterDef.getCondition().getArg() == null) {
            return !hasLinkedTarget;
        }

        final String arg = filterDef.getCondition().getArg();
        final Map<String, Object> source = filterCtx.getSourceProperties();
        final var propertiesArg = new PropertiesArg(arg);
        final var propertiesPredicate = new PropertiesEqualsPredicate(source);

        if (hasLinkedTarget) {
            return !propertiesArg.getParts().stream()
                    .allMatch(p -> propertiesPredicate.test(p, filterCtx.getSource().getAdditionalProperties()));
        } else {
            return filterCtx.getTargetsProperties()
                    .stream()
                    .noneMatch(target -> propertiesArg.getParts().stream().allMatch(p -> propertiesPredicate.test(p, target)));
        }
    }

    @Override
    public boolean supports(ApplicationConfigurationFilterDto filterDef) {
        return filterDef.getCondition() != null ? FilterConditionDto.NameEnum.SOURCENOTINTARGET.equals(filterDef.getCondition().getName()) : false;
    }

    @Override
    public TYPE getType() {
        return TYPE.SOURCE;
    }
}