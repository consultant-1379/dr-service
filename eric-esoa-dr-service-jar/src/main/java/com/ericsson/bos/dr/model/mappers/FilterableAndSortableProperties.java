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

package com.ericsson.bos.dr.model.mappers;

import java.util.Map;

import com.ericsson.bos.dr.jpa.specifications.DefaultPropertyFilterSpecification;
import com.ericsson.bos.dr.jpa.specifications.PropertyFilterSpecification;

/**
 * Classes that identify properties that are filterable and/or sortable should implement this interface.
 *
 * @param <E>
 *         The entity type containing the properties.
 */
public interface FilterableAndSortableProperties<E> {

    /**
     * Determines if the given property is filterable.
     *
     * @param propertyName
     *         property name as given in the D&R NBI.
     * @return true if the property is filterable, otherwise false.
     */
    boolean isFilterable(String propertyName);

    /**
     * Returns a Map of the sortable properties. The key contains the property name as given in the D&R NBI,
     * the value contains the corresponding mapped name in the entity.
     *
     * @return Map of the sortable properties.
     */
    Map<String, String> getSortable();

    /**
     * Return a class of type <code>PropertyFilterSpecification</code> that provides
     * a filter <code>Specification</code> for a given property.
     *
     * If class does not implement this method then a default specification
     * <code>DefaultPropertyFilterSpecification</code> is returned.
     *
     * @param propertyName
     *         property name
     * @return class that provides a filter <code>Specification</code>.
     */
    default PropertyFilterSpecification<E> getFilterSpecification(String propertyName) {
        return new DefaultPropertyFilterSpecification<>();
    }

    /**
     * Maps the property name given in the D&R NBI to the corresponding name in the entity.
     *
     * @param propertyName
     *         property name
     * @return the mapped name
     */
    String getMappedName(String propertyName);
}
