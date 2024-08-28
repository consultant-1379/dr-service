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
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.ericsson.bos.dr.service.exceptions.DRServiceException;
import com.ericsson.bos.dr.service.exceptions.ErrorCode;
import com.ericsson.bos.dr.service.utils.ExceptionChecks;
import com.ericsson.bos.so.common.logging.security.SecurityLogger;

import io.netty.channel.ChannelOption;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutException;
import io.netty.handler.timeout.WriteTimeoutHandler;
import reactor.core.publisher.Mono;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.transport.logging.AdvancedByteBufFormat;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

/**
 * Executes a http request
 * Uses Spring Fwk WebClient in synchronous mode for now, i.e. blocking
 */
@Component
public class HttpClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpClient.class);

    // remove in https://eteamproject.internal.ericsson.com/browse/ESOA-12901
    @Value("${spring.codec.max-in-memory-size-kb}")
    private String webClientInMemorySize;

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Value("${service.http-client.retry.max-attempts}")
    private long retryAttempts;

    @Value("${service.http-client.retry.delay}")
    private long retryDelay;

    @Autowired
    private SslContextSupplier sslContextSupplier;

    @Autowired
    private ConnectionProviderProperties connectionProviderProperties;

    private ConnectionProvider connectionProvider;

    /**
     * Initialize ConnectionProvider.
     */
    @PostConstruct
    void init() {
        connectionProvider = ConnectionProvider.builder("rest-service")
                .maxConnections(connectionProviderProperties.getMaxConnections())
                .maxIdleTime(connectionProviderProperties.getMaxIdleTime())
                .maxLifeTime(connectionProviderProperties.getMaxLifeTime())
                .evictInBackground(connectionProviderProperties.getEvictInterval())
                .disposeInactivePoolsInBackground(connectionProviderProperties.getDisposeInterval(),
                        connectionProviderProperties.getPoolInactivityTime())
                .build();
    }

    /**
     * Executes an Http request using the web client
     * @param properties the Http request properties, e.g. url, method, header and body
     * @return ResponseEntity response from the Http request
     */
    public ResponseEntity<String> executeRequest(final HttpRequest properties) {
        final var webClient = webClientBuilder.clone()
            .clientConnector(configureHttpClient(properties))
            .exchangeStrategies(ExchangeStrategies      //remove in https://eteamproject.internal.ericsson.com/browse/ESOA-12901
                .builder()
                .codecs(codecs -> codecs
                    .defaultCodecs()
                    .maxInMemorySize(Integer.parseInt(webClientInMemorySize) * 1024))
                .build())
            .build();

        final UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(properties.getUrl());
        if (properties.isEncodeUrl()) {
            uriBuilder.encode();
        }
        final var url = uriBuilder.build().toUri();

        final ResponseEntity<String> response = webClient.method(Objects.requireNonNull(HttpMethod.valueOf(properties.getMethod())))
            .uri(url)
            .headers(httpHeaders -> properties.getHeaders().ifPresent(httpHeaders::addAll))
            .bodyValue(properties.getBody().orElse(BodyInserters.empty()))
            .retrieve()
            .toEntity(String.class)
            .retryWhen(configureRetry(properties))
            .onErrorResume(error -> {
                if (ExceptionChecks.isNonRetryableConnectionIssue(error)) {
                    SecurityLogger.withFacility(
                        () -> LOGGER.error("Lost connectivity: {}. Properties: [{}]", error.getMessage().trim(), properties)
                    );
                }
                return Mono.error(error);
            }).block();

        Optional.ofNullable(response).ifPresent(r -> {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Http Response: code={}, headers={}", response.getStatusCode(), response.getHeaders());
            }
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Http Response body: {}", response.getBody());
            }
        });

        return response;
    }

    private ReactorClientHttpConnector configureHttpClient(final HttpRequest properties) {
        var httpClient = reactor.netty.http.client.HttpClient.create(connectionProvider)
                .wiretap("reactor.netty.http.client.HttpClient", LogLevel.DEBUG, AdvancedByteBufFormat.TEXTUAL);

        final Optional<Integer> connectTimeoutSeconds = properties.getConnectTimeoutSeconds();
        if (connectTimeoutSeconds.isPresent()) {
            LOGGER.debug("Configuring connection timeout: {}", connectTimeoutSeconds.get());
            httpClient = httpClient.option(
                ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) TimeUnit.SECONDS.toMillis(connectTimeoutSeconds.get()));
        }

        final Optional<Integer> writeTimeoutSeconds = properties.getWriteTimeoutSeconds();
        if (writeTimeoutSeconds.isPresent()) {
            LOGGER.debug("Configuring write timeout: {}", writeTimeoutSeconds.get());
            httpClient = httpClient.doOnConnected(
                connection -> connection.addHandlerLast(new WriteTimeoutHandler(writeTimeoutSeconds.get())));
        }

        final Optional<Integer> readTimeoutSeconds = properties.getReadTimeoutSeconds();
        if (readTimeoutSeconds.isPresent()) {
            LOGGER.debug("Configuring read timeout: {}", readTimeoutSeconds.get());
            httpClient = httpClient.doOnConnected(
                connection -> connection.addHandlerLast(new ReadTimeoutHandler(readTimeoutSeconds.get())));
        }
        httpClient = httpClient.secure(sslContextSpec -> sslContextSpec.sslContext(sslContextSupplier.get()));
        return new ReactorClientHttpConnector(httpClient);
    }

    private RetryBackoffSpec configureRetry(final HttpRequest properties) {
        return Retry.fixedDelay(retryAttempts, Duration.ofSeconds(retryDelay))
            .filter(ExceptionChecks::isRetryableConnectionIssue)
            .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                final Throwable cause = retrySignal.failure().getCause();
                final String causeMessage;
                // Note: setting cause for read & write timeout as
                // ReadTimeoutException & WriteTimeoutException do not contain a message
                if (cause instanceof ReadTimeoutException) {
                    causeMessage = "Read timeout exceeded";
                } else if (cause instanceof WriteTimeoutException) {
                    causeMessage = "Write timeout exceeded";
                } else {
                    causeMessage = cause.getMessage();
                }
                SecurityLogger.withFacility(
                    () -> LOGGER.error("Lost connectivity - retries exhausted: {}. Properties: [{}]", causeMessage.trim(), properties)
                );
                throw new DRServiceException(ErrorCode.GENERAL_ERROR,
                    String.format("Failed to reach external service after %d %s. Cause: %s.",
                        retrySignal.totalRetries(),
                        retrySignal.totalRetries() == 1 ? "retry" : "retries",
                        causeMessage)
                );
            });
    }
}