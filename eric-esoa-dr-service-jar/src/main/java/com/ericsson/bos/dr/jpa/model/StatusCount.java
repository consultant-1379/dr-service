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
package com.ericsson.bos.dr.jpa.model;

import org.springframework.beans.factory.annotation.Value;

/**
 * Projection class for object status count
 */
public interface StatusCount {

    /**.
     * @return status
     */
    @Value("#{target.status}")
    String getStatus();

    /**
     * @return count
     */
    @Value("#{target.count}")
    int getCount();
}