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

import java.time.Duration;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.task.TaskExecutorBuilder;
import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import io.micrometer.context.ContextRegistry;
import io.micrometer.context.ContextSnapshot;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.contextpropagation.ObservationAwareSpanThreadLocalAccessor;

/**
 * Configure Task Pool Executors.
 */
@Configuration
public class ExecutorsConfiguration {

    public static final String ALARM_ASYNC_EXECUTOR = "alarmAsyncExecutor";

    /**
     * Executor to execute individual tasks in a job.
     * Executor is configured to propagate the micrometer tracing context.
     * @param taskExecutorBuilder taskExecutorBuilder
     * @param coreSize core pool size
     * @param maxSize max pool size
     * @return Executor
     */
    @Bean(name = "tasksExecutor")
    public Executor tasksExecutor(final TaskExecutorBuilder taskExecutorBuilder,
                                  @Value("${service.jobs.execution.task-executor.core-size}") final int coreSize,
                                  @Value("${service.jobs.execution.task-executor.max-size}") final int maxSize) {
        return taskExecutorBuilder.corePoolSize(coreSize)
                .maxPoolSize(maxSize)
                .threadNamePrefix("taskExecutor-")
                .taskDecorator(runnable -> ContextSnapshot.captureAll().wrap(runnable)) // propagate micrometer tracing
                .build();
    }

    /**
     * Executor to execute jobs.
     * Starts a tracing span prior to executing the runnable and is configured to propagate the tracing context.
     * @param taskExecutorBuilder taskExecutorBuilder
     * @param tracer brave tracer
     * @param coreSize core pool size
     * @param maxSize max pool size
     * @param queueSize queue size
     * @param awaitTermination await termination
     * @param awaitTerminationSeconds await termination period in seconds
     * @return ThreadPoolTaskExecutor
     */
    @Bean(name = "jobsExecutor")
    public ThreadPoolTaskExecutor jobsExecutor(final TaskExecutorBuilder taskExecutorBuilder, final Tracer tracer,
                                    @Value("${service.jobs.execution.job-executor.core-size}") final int coreSize,
                                    @Value("${service.jobs.execution.job-executor.max-size}") final int maxSize,
                                    @Value("${service.jobs.execution.job-executor.queue-size}") final int queueSize,
                                    @Value("${service.jobs.execution.job-executor.await-termination}") final boolean awaitTermination,
                                    @Value("${service.jobs.execution.job-executor.await-termination-seconds}") final int awaitTerminationSeconds) {
        return taskExecutorBuilder
                .corePoolSize(coreSize)
                .maxPoolSize(maxSize)
                .queueCapacity(queueSize)
                .awaitTermination(awaitTermination)
                .awaitTerminationPeriod(Duration.ofSeconds(awaitTerminationSeconds))
                .threadNamePrefix("jobExecutor-")
                .taskDecorator(runnable -> () -> { // start span and propagate micrometer tracing
                    final var span = tracer.startScopedSpan("job");
                    try {
                        ContextRegistry.getInstance().registerThreadLocalAccessor(new ObservationAwareSpanThreadLocalAccessor(tracer));
                        ContextSnapshot.captureAll().wrap(runnable).run();
                    } finally {
                        ContextRegistry.getInstance().removeThreadLocalAccessor(ObservationAwareSpanThreadLocalAccessor.KEY);
                        span.end();
                    }
                })
                .build();
    }

    /**
     * Executor for async methods.
     * @param taskExecutorBuilder task executor builder
     * @return Executor
     */
    @Bean(name = "asyncExecutor")
    public Executor asyncExecutor(final TaskExecutorBuilder taskExecutorBuilder) {
        return taskExecutorBuilder.corePoolSize(2)
                .maxPoolSize(5)
                .queueCapacity(10)
                .threadNamePrefix("asyncExecutor-")
                .build();
    }

    /**
     * Executor for alarm handler.
     *
     * @param threadPoolTaskExecutorBuilder
     *         threadPoolTaskExecutorBuilder
     * @param coreSize
     *         core pool size
     * @param maxSize
     *         max pool size
     * @param queueSize
     *         queue size
     * @return ThreadPoolTaskExecutor
     */
    @Bean(name = ALARM_ASYNC_EXECUTOR)
    public ThreadPoolTaskExecutor alarmAsyncExecutor(final ThreadPoolTaskExecutorBuilder threadPoolTaskExecutorBuilder,
            @Value("${security.systemMonitoring.async-executor.core-size}") final int coreSize,
            @Value("${security.systemMonitoring.async-executor.max-size}") final int maxSize,
            @Value("${security.systemMonitoring.async-executor.queue-size}") final int queueSize) {
        return threadPoolTaskExecutorBuilder
                .corePoolSize(coreSize)
                .maxPoolSize(maxSize)
                .queueCapacity(queueSize)
                .threadNamePrefix("alarmAsyncExecutor-")
                .build();
    }
}