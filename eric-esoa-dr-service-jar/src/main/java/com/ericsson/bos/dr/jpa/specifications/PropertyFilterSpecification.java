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

import org.springframework.data.jpa.domain.Specification;

/**
 * Implementing classes should convert the given FIQL expression into a JPA <code>Specification</code>.
 *
 * @param <E>
 *         The Entity type that the property belongs to.
 */
public interface PropertyFilterSpecification<E> {

    /**
     * Translates the FIQL expression given in the filter to a JPA <code>Specification</code>.
     *
     * @param fiqlExpression
     *         Contains the property and value given in the FIQL expression.
     * @return JPA Specification
     */
    Specification<E> get(FIQLExpression fiqlExpression);
}
