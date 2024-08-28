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

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * A number of WebClient.Builder instances are pulled in from the eric-esoa-so-common-libs module.
 * This class is to ensure that DR has its own instance, that it configures from the application.yaml.
 */
@Component
public class WebclientBuilderConfig {

    /**
     * Return DRs instance of WebClient.Builder.
     */
    @Bean
    @Scope("prototype")
    @Primary
    public WebClient.Builder webClientBuilder(ObjectProvider<WebClientCustomizer> customizerProvider) {
        final WebClient.Builder builder = WebClient.builder();
        customizerProvider.orderedStream().forEach((customizer) ->
                customizer.customize(builder)
        );
        return builder;
    }
}
