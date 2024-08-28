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

package com.ericsson.bos.dr.jpa.specifications;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.ericsson.bos.dr.jpa.model.DiscoveryObjectEntity;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;

/**
 * Custom filter Specification for the sourceProperties and targetProperties fields in <code>DiscoveryObjectEntity</code>.
 * <p>
 * The sourceProperties and targetProperties fields are json, there contents are determined by the feature pack and therefore
 * require a custom filter Specification.
 * </p>
 * <p>
 * It's possible to filter on any property contained in the sourceProperties and targetProperties fields including nested properties.
 * If the given property starts with 'source.' then only sourceProperties are searched for a match. If the given property starts with 'target.'
 * then only targetProperties are searched for a match. If a given property does not start with either 'source.' or 'target.' then both the
 * sourceProperties and targetProperties fields are searched for a match.
 * </p>
 */
public class DiscoveredObjectPropertyFilterSpecification implements PropertyFilterSpecification<DiscoveryObjectEntity> {
    private static final String JSONB_EXTRACT_PATH_TEXT = "jsonb_extract_path_text";
    private static final String SOURCE_COLUMN = "sourceProperties";
    private static final String TARGET_COLUMN = "targetProperties";

    @Override
    public Specification<DiscoveryObjectEntity> get(final FIQLExpression fiqlExpression) {
        if (isTargetProperty(fiqlExpression.property())) {
            return getSpecification(TARGET_COLUMN, fiqlExpression);
        } else if (isSourceProperty(fiqlExpression.property())) {
            return getSpecification(SOURCE_COLUMN, fiqlExpression);
        } else { // Search in both source and target columns
            return Specification.where(getSpecification(SOURCE_COLUMN, fiqlExpression))
                    .or(getSpecification(TARGET_COLUMN, fiqlExpression));
        }
    }

    private Specification<DiscoveryObjectEntity> getSpecification(final String column, final FIQLExpression fiqlExpression) {
        return fiqlExpression.valueContainsLikeMarker() ?
                Specification.where(
                        likeCriteria(column, fiqlExpression.property(), fiqlExpression.getValueAsString().replace("*", "%"))) :
                Specification.where(
                        equalsCriteria(column, fiqlExpression.property(), fiqlExpression.getValueAsString()));
    }

    private Specification<DiscoveryObjectEntity> equalsCriteria(final String column, final String key, final String value) {
        return (root, query, criteriaBuilder)
                -> criteriaBuilder.equal(
                criteriaBuilder.function(
                        JSONB_EXTRACT_PATH_TEXT, String.class, buildExpressions(root, criteriaBuilder, column, key)), value
        );
    }

    private Specification<DiscoveryObjectEntity> likeCriteria(final String column, final String key, final String value) {
        return (root, query, criteriaBuilder)
                -> criteriaBuilder.like(
                criteriaBuilder.function(
                        JSONB_EXTRACT_PATH_TEXT, String.class, buildExpressions(root, criteriaBuilder, column, key)), value
        );
    }

    private Expression<String>[] buildExpressions(
            final Root<DiscoveryObjectEntity> root, final CriteriaBuilder criteriaBuilder, final String column, final String property) {
        final List<Expression<String>> expressions = new ArrayList<>();
        expressions.add(root.get(column));
        for (final String part : getPropertyParts(property)) {
            expressions.add(criteriaBuilder.literal(part));
        }
        return expressions.toArray(new Expression[0]);
    }

    private List<String> getPropertyParts(final String property) {
        return Arrays.asList(
                property.replaceFirst("properties.", "")
                        .replaceFirst("source.", "")
                        .replaceFirst("target.", "")
                        .split("\\."));
    }

    private boolean isTargetProperty(final String property) {
        return property.startsWith("properties.target");
    }

    private boolean isSourceProperty(final String property) {
        return property.startsWith("properties.source");
    }
}
