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
package com.ericsson.bos.dr.service.subsystem;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Subsystem type of the connected system
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubsystemType {

    private long id;
    private String type;

    public long getId() {
        return id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "SubsystemType{" +
               "id=" + id +
               ", type='" + type + '\'' +
               '}';
    }
}
