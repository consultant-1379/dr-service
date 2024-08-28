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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Connected System
 * @param <T> Type of connection properties
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConnectedSystem<T> {

    private String name;
    private String url;
    private SubsystemType subsystemType;
    private List<T> connectionProperties;

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public SubsystemType getSubsystemType() {
        return subsystemType;
    }

    public void setSubsystemType(final SubsystemType subsystemType) {
        this.subsystemType = subsystemType;
    }

    public List<T> getConnectionProperties() {
        return connectionProperties;
    }

    public void setConnectionProperties(final List<T> connectionProperties) {
        this.connectionProperties = connectionProperties;
    }

    @Override
    public String toString() {
        return "ConnectedSystem{" +
               "name='" + name + '\'' +
               ", url='" + url + '\'' +
               ", subsystemType=" + subsystemType +
               ", connectionProperties=" + connectionProperties +
               '}';
    }
}
