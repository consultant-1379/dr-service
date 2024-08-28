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

import java.util.Arrays;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

/**
 * Default property filter Specification is used for the majority of properties.
 * When a property does not declare a custom Specification the default is used.
 * It's used to filter properties where the condition is 'equal to' or 'like'.
 *
 * @param <E>
 *         The Entity type that the property belongs to.
 */
public class DefaultPropertyFilterSpecification<E> implements PropertyFilterSpecification<E> {

    @Override
    public Specification<E> get(final FIQLExpression fiqlExpression) {
        final var stringValue = fiqlExpression.getValueAsString();
        if (fiqlExpression.valueContainsLikeMarker()) {
            return propertyLike(fiqlExpression.property(), stringValue);
        } else {
            return propertyEquals(fiqlExpression.property(), fiqlExpression.value());
        }
    }

    private Specification<E> propertyEquals(String name, Object value) {
        return (root, query, criteriaBuilder)
                -> criteriaBuilder.equal(buildCriteriaPath(root, name), value);
    }

    private Specification<E> propertyLike(String name, String value) {
        return (root, query, criteriaBuilder)
                -> criteriaBuilder.like(buildCriteriaPath(root, name), value.replace("*", "%"));
    }

    private Path<String> buildCriteriaPath(Root<E> root, final String name) {
        final String[] names = name.split("\\.");
        Path<String> path = root.get(names[0]);
        for (final String n : Arrays.copyOfRange(names, 1, names.length)) {
            path = path.get(n);
        }
        return path;
    }
}
