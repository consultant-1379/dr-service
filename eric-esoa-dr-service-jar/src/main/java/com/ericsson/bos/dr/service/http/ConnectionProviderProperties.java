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
package com.ericsson.bos.dr.service.http;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties to configure netty connection pool.
 */
@ConfigurationProperties(prefix = "netty.http-client.connection-pool")
@Configuration
public class ConnectionProviderProperties {

    private int maxConnections;
    private Duration maxIdleTime;
    private Duration maxLifeTime;
    private Duration evictInterval;
    private Duration disposeInterval;
    private Duration poolInactivityTime;

    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public Duration getMaxIdleTime() {
        return maxIdleTime;
    }

    public void setMaxIdleTime(Duration maxIdleTime) {
        this.maxIdleTime = maxIdleTime;
    }

    public Duration getMaxLifeTime() {
        return maxLifeTime;
    }

    public void setMaxLifeTime(Duration maxLifeTime) {
        this.maxLifeTime = maxLifeTime;
    }

    public Duration getEvictInterval() {
        return evictInterval;
    }

    public void setEvictInterval(Duration evictInterval) {
        this.evictInterval = evictInterval;
    }

    public Duration getDisposeInterval() {
        return disposeInterval;
    }

    public void setDisposeInterval(Duration disposeInterval) {
        this.disposeInterval = disposeInterval;
    }

    public Duration getPoolInactivityTime() {
        return poolInactivityTime;
    }

    public void setPoolInactivityTime(Duration poolInactivityTime) {
        this.poolInactivityTime = poolInactivityTime;
    }
}