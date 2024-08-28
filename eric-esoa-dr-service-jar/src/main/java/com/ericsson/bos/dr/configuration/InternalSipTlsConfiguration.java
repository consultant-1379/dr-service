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
package com.ericsson.bos.dr.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * If TLS is required for internal service to service communication (i.e. from Dr-service to Rest-service)
 * then the eric-eso-mtls-helper library is scanned.
 * <p>
 *     The eric-eso-mtls-helper library creates the JKS trust and key stores, populated with the certificates and keys
 *     generated by the SIP-TLS service. The library watches the file systems for changes in the certificates and keys
 *     and automatically updates the trust and key stores.
 * </p>
 */
@Configuration
@ComponentScan("com.ericsson.bos.so.security.mtls")
@ConditionalOnProperty(name = "security.tls.enabled", havingValue = "true")
public class InternalSipTlsConfiguration {
}
