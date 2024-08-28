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
package com.ericsson.bos.dr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.http.HttpHeaders;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Discovery and Reconciliation Springboot Application.
 */
@SpringBootApplication
@EnableAsync
@EnableRetry
@EnableScheduling
@EnableJpaAuditing
@EnableCaching
@EnableConfigurationProperties
@EnableAutoConfiguration
@EnableKafka
@EntityScan(basePackages = {"com.ericsson.bos.dr.jpa.model"})
public class DRApplication {

    /**
     * Run Spring Boot Application.
     *
     * @param args runtime args
     */
    public static void main(final String[] args) {
        SpringApplication.run(DRApplication.class, args);
    }

    /**
     * Log requests and responses.
     *
     * @return CommonsRequestLoggingFilter
     */
    @Bean
    @Primary
    public CommonsRequestLoggingFilter loggingFilter() {
        final CommonsRequestLoggingFilter loggingFilter = new CommonsRequestLoggingFilter() {

            @Override
            protected boolean shouldLog(final HttpServletRequest request) {
                return super.shouldLog(request) &&
                        !request.getRequestURI().startsWith("/actuator");
            }
        };
        loggingFilter.setIncludeQueryString(true);
        loggingFilter.setIncludePayload(true);
        loggingFilter.setIncludeHeaders(true);
        loggingFilter.setMaxPayloadLength(1024);
        loggingFilter.setHeaderPredicate(s -> !HttpHeaders.AUTHORIZATION.equalsIgnoreCase(s) &&
                !HttpHeaders.COOKIE.equalsIgnoreCase(s));
        return loggingFilter;
    }
}