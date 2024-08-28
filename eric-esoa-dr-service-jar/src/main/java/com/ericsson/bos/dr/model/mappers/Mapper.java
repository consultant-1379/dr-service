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

/**
 * Map from one object to another.
 * @param <F> from type
 * @param <T> to type
 */
public interface Mapper<F, T> {

    /**
     * Apply mapping to source object.
     * @param var1 the source object
     * @return the mapped object
     */
    T apply(F var1);
}
