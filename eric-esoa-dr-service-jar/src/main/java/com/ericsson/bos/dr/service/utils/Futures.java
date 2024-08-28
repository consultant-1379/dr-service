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
package com.ericsson.bos.dr.service.utils;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

/**
 * Utility methods for handling of Futures.
 */
public class Futures {

    private Futures() {
    }

    /**
     * Extends the behaviour of {{@linkplain CompletableFuture#allOf(CompletableFuture[])}}
     * to cancel the futures when an error occurs. Ongoing futures will continue to
     * completion. Futures which have not yet started will be cancelled.
     * @param futures futures
     * @return CompletableFuture
     */
    public static CompletableFuture<Object> allOfCancelOnFailure(final CompletableFuture<?>... futures) {
        final CompletableFuture<?> failure = new CompletableFuture<>();
        for (final CompletableFuture<?> f : futures) {
            f.exceptionally(ex -> {
                failure.completeExceptionally(ex);
                return null;
            });
        }
        failure.exceptionally(ex -> {
            Arrays.stream(futures).forEach(f -> f.cancel(true));
            return null;
        });
        return CompletableFuture.anyOf(failure, CompletableFuture.allOf(futures));
    }
}