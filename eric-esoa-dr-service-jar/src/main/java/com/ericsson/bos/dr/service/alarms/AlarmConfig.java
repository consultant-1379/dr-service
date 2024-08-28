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
package com.ericsson.bos.dr.service.alarms;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;


/**
 * Includes the eric-esoa-alarm-helper components in the DR microservice.
 * <p>
 *     The eric-esoa-alarm-helper library provides support for sending alarms.
 *     MTLS communication is supported. The library watches the file system for changes
 *     in the eric-esoa-dr-service-alarm-handler-int-cert certificate and key and automatically
 *     updates the key store.
 * </p>
 *
 * Note: The @ConditionalOnProperty was included so that in the integration tests the eric-esoa-alarm-helper lib
 * could be excluded. The eric-esoa-alarm-helper lib has a dependency on the eric-esoa-mtls-helper lib. Bringing
 * in the eric-esoa-alarm-helper lib in the tests would also require bringing in the eric-esoa-mtls-helper lib and
 * all its configuration.
 */
@Configuration
@ComponentScan("com.ericsson.bos.so.alarm")
@ConditionalOnProperty(name = "security.systemMonitoring.enabled", havingValue = "true")
public class AlarmConfig {}
