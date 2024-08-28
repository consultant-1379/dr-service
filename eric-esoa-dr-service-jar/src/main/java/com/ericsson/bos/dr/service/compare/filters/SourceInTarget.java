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

import static com.ericsson.bos.dr.web.v1.api.model.FilterConditionDto.NameEnum;
import java.util.Map;

import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationFilterDto;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;

/**
 * Check for matching source object in a list of target objects or a linked target object. Matching
 * is performed based on the source/target property keys defined in the condition arg.
 * <p>
 * If no arg is supplied then checks if the source has been linked to a target. Returns true if the source has a linked
 * target.</p>
 * <p>
 * If arg is supplied and source is linked to a target then checks the source and linked target properties.
 * Returns true if the properties are equal.</p>
 * <p>
 * If arg is supplied and there is no linked target object, then checks for a match in the full target list. If
 * a match is found, then the matched target node is removed from the target list to improve efficiency when processing
 * the next source object in the same filter context.
 * </p>
 */
@Component
class SourceInTarget implements Condition {

    @Override
    public boolean test(ApplicationConfigurationFilterDto filterDef, FilterContext filterCtx) {
        final var hasLinkedTarget = MapUtils.isNotEmpty(filterCtx.getSource().getAdditionalProperties());
        if (filterDef.getCondition().getArg() == null){
            // no arg, then check for linked target
            return hasLinkedTarget;
        }

        final Map<String, Object> source = filterCtx.getSourceProperties();
        final String arg = filterDef.getCondition().getArg();
        final var propertiesArg = new PropertiesArg(arg);
        final var propertiesPredicate = new PropertiesEqualsPredicate(source);

        if (hasLinkedTarget) {
            // linked target so check additional properties
            return propertiesArg.getParts().stream().allMatch(p -> propertiesPredicate.test(p, filterCtx.getSource().getAdditionalProperties()));
        } else {
            // no linked target so check all targets
            filterCtx.getTargetsProperties()
                    .removeIf(target -> propertiesArg.getParts().stream().anyMatch(p -> propertiesPredicate.negate().test(p, target)));
            return !filterCtx.getTargetsProperties().isEmpty();
        }
    }

    @Override
    public boolean supports(ApplicationConfigurationFilterDto filterDef) {
        return filterDef.getCondition() != null ? NameEnum.SOURCEINTARGET.equals(filterDef.getCondition().getName()) : false;
    }

    @Override
    public TYPE getType() {
        return TYPE.SOURCE;
    }
}