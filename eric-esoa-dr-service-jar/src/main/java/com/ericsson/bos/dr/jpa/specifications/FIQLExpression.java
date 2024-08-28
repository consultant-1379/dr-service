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

/**
 * Encapsulates the property and value parts of a FIQL expression given in a search filter.
 * Expression will be in the form:
 * <property>==<value> i.e. status==RECONCILE_FAILED
 *
 * @param property
 *         property part of the FIQL expression.
 * @param value
 *         value part of the FIQL expression.
 */
public record FIQLExpression(String property, Object value) {

    /**
     * Returns value as a string.
     *
     * @return value as a string
     */
    public String getValueAsString() {
        return value.toString();
    }

    /**
     * Checks if value contains like markers ('*') at start or end of value.
     *
     * @return true if value contains like markers, otherwise false
     */
    public boolean valueContainsLikeMarker() {
        return (value.toString().startsWith("*") || value.toString().endsWith("*"));
    }
}
