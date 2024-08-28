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
package com.ericsson.bos.dr.service.subscriptions.kafka.security;

import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

import com.ericsson.bos.dr.service.subscriptions.kafka.KafkaSubscriptionRegistry;
import com.ericsson.bos.dr.service.subscriptions.kafka.KafkaSubscriptionRegistry.KafaSubscriptionRegistryEvent;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.kubernetes.commons.ConditionalOnKubernetesConfigEnabled;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Starts a KubernetesClient watcher to monitor changes to secrets for kafka certificates.
 * The watcher is only started when there is a <code>KafkaSubscription</code> with ssl enabled.
 * When <code>KafkaSubscription</code> are removed, the watcher will be stopped if there are no other
 * subscriptions with ssl enabled.
 * <p>
 * The <code>KakfaKubernetesSecretsEventReceiver</code> is called when a secret is modified.
 * </p>
 */
@Component
@ConditionalOnKubernetesConfigEnabled
public class KafkaKubernetesSecretsWatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaKubernetesSecretsWatcher.class);

    @Autowired
    private KafkaSubscriptionRegistry kafkaSubscriptionRegistry;

    @Autowired
    private KakfaKubernetesSecretsEventReceiver kakfaKubernetesSecretsEventReceiver;

    @Autowired
    private KubernetesClient kubernetesClient;

    @Value("${service.message-subscriptions.kafka.tls.secret-watcher.timer-delay}")
    private long timerDelay;

    private Timer timer;
    private Watch watch;

    /**
     * Process <code>KafaSubscriptionRegistryEvent</code>.
     * @param event kafka subscription registry event
     */
    @EventListener
    public void handleKafkaRegistryEvent(KafaSubscriptionRegistryEvent event) {
        if (KafkaSubscriptionRegistry.Action.ADDED.equals(event.action()) && watch == null) {
            if (!kafkaSubscriptionRegistry.findBySslEnabled().isEmpty()) {
                startSecretsWatcher();
            }
        } else if (KafkaSubscriptionRegistry.Action.REMOVED.equals(event.action()) &&
                (watch != null || timer != null)) {
            if (kafkaSubscriptionRegistry.findBySslEnabled().isEmpty()) {
                stopSecretsWatcher();
            }
        } else {
            LOGGER.debug("Nothing to do for {}", event);
        }
    }

    /**
     * Starts a <code>TimerTask</code> to run repeatedly with fixed delay until
     * kubernetes secret watcher is successfully running.
     */
    private synchronized void startSecretsWatcher() {
        if (watch != null) {
            LOGGER.info("kubernetes secret watcher for kafka certificates already running.");
            return;
        }
        if (this.timer != null) {
            LOGGER.info("Timer task to start kubernetes secret watcher for kafka certificates already running.");
            return;
        }
        this.timer = new Timer("start-kafka-kubernetes-secret-watcher");
        this.timer.scheduleAtFixedRate(new StartSecretWatcherTimerTask(), 0, timerDelay);
    }

    /**
     * Stop kubernetes secret watcher if running.
     */
    private synchronized void stopSecretsWatcher() {
        LOGGER.info("Stop kubernetes secrets watcher for kafka certificates");
        Optional.ofNullable(watch).ifPresent(Watch::close);
        Optional.ofNullable(timer).ifPresent(Timer::cancel);
        this.timer = null;
        this.watch = null;
    }

    /**
     * Check if kubernetes secret watcher is starting.
     * @return true if starting, otherwise false.
     */
    public boolean isStarting() {
        return this.timer != null;
    }

    /**
     * Check if the kubernetes secret watcher is running.
     * @return true if running, otherwise false
     */
    public boolean isRunning() {
        return this.watch != null;
    }

    private class StartSecretWatcherTimerTask extends TimerTask {

        @Override
        public void run() {
            try {
                LOGGER.info("Start watching kubernetes secrets for kafka certificates.");
                KafkaKubernetesSecretsWatcher.this.watch = watchSecrets();
                cancel();
                KafkaKubernetesSecretsWatcher.this.timer = null;
                LOGGER.info("Kubernetes secret watcher started.");
            } catch (Exception e) {
                LOGGER.warn("Error initiating kubernetes secret watcher for kafka certificates.", e);
            }
        }

        private Watch watchSecrets() {
            return kubernetesClient.secrets().watch(new Watcher<>() {
                @Override
                public void eventReceived(final Action action, final Secret secret) {
                    kakfaKubernetesSecretsEventReceiver.accept(action, secret);
                }

                @Override
                public void onClose(final WatcherException e) {
                    LOGGER.warn("Kubernetes secrets watcher for kafka certificates has closed.", e);
                    stopSecretsWatcher();
                    startSecretsWatcher();
                }
            });
        }
    }
}