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
package com.ericsson.bos.dr.tests.unit.subscription

import com.ericsson.bos.dr.jpa.ListenerMessageSubscriptionRepository
import com.ericsson.bos.dr.jpa.model.ListenerMessageSubscriptionEntity
import com.ericsson.bos.dr.service.http.HttpClient
import com.ericsson.bos.dr.service.subscriptions.kafka.KafkaSubscriber
import com.ericsson.bos.dr.service.subscriptions.kafka.KafkaSubscriptionRegistry
import com.ericsson.bos.dr.service.subscriptions.kafka.security.KafkaKubernetesSecretsWatcher
import com.ericsson.bos.dr.service.subscriptions.kafka.security.KakfaKubernetesSecretsEventReceiver
import com.ericsson.bos.dr.service.subsystem.ConnectedSystem
import com.ericsson.bos.dr.service.subsystem.KafkaConnectionProperties
import io.fabric8.kubernetes.api.model.Secret
import io.fabric8.kubernetes.api.model.SecretBuilder
import io.fabric8.kubernetes.api.model.StatusBuilder
import io.fabric8.kubernetes.api.model.WatchEvent
import io.fabric8.kubernetes.api.model.WatchEventBuilder
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.Watcher
import io.fabric8.kubernetes.client.server.mock.KubernetesServer
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

@ContextConfiguration(classes = [KakfaKubernetesSecretsEventReceiver, KafkaKubernetesSecretsWatcher,
        KafkaSubscriptionRegistry, KafkaKubernetesSecretWatcherConfig ])
@TestPropertySource(properties = ["service.message-subscriptions.kafka.tls.secret-watcher.timer-delay=200"])
class KafkaKubernetesSecretsWatcherSpec extends Specification {

    @Shared
    static KubernetesServer kubernetesServer = new KubernetesServer(false)

    @Shared
    KubernetesClient kubernetesClient

    @Autowired
    KafkaSubscriptionRegistry kafkaSubscriptionRegistry

    @Autowired
    KafkaKubernetesSecretsWatcher kafkaKubernetesSecretsWatcher

    @MockBean
    KafkaSubscriber kafkaSubscriber

    @MockBean
    ListenerMessageSubscriptionRepository listenerMessageSubscriptionRepository

    ConnectedSystem<KafkaConnectionProperties> connectedSystem = new ConnectedSystem<>(name: "kafka1",
            connectionProperties: [new KafkaConnectionProperties(sslEnabled: true, keyStoreSecretName: "secret1",
                    trustStoreSecretName: "secret2")])
    ListenerMessageSubscriptionEntity messageSubscriptionEntity = new ListenerMessageSubscriptionEntity(id: 100, subsystemName:
            connectedSystem.name)

    PollingConditions pollingConditions = new PollingConditions()

    def setupSpec() {
        kubernetesServer.before()
    }

    def cleanupSpec() {
        kubernetesServer.after()
    }

    def cleanup() {
        kafkaSubscriptionRegistry.getAll().each { kafkaSubscriptionRegistry.remove(it.messageSubscriptionEntityId) }
    }

    def "Re-subscribe performed for kafka message subscription when certificate secret is create or modified"() {

        setup: "Add Kafka Subscription to registry"
        kafkaSubscriptionRegistry.add(messageSubscriptionEntity, connectedSystem)

        and: "Mock listenerMessageSubscriptionRepository to return entity"
        Mockito.when(listenerMessageSubscriptionRepository.findById(100)).thenReturn(Optional.of(messageSubscriptionEntity))

        when: "kubernetes server emits secret modified event"
        Secret secret = new SecretBuilder().withNewMetadata().withName("secret1").endMetadata().build()
        kubernetesServer.expect().withPath("/api/v1/namespaces/test/secrets?allowWatchBookmarks=true&watch=true")
                .andUpgradeToWebSocket()
                .open()
                .waitFor(10)
                .andEmit(new WatchEvent(secret, "MODIFIED"))
                .done()
                .once()

        then: "Unsubscribe and subscribe performed for message subscription"
        Mockito.verify(kafkaSubscriber, Mockito.timeout(1000).times(1)).unsubscribe("100")
        Mockito.verify(kafkaSubscriber, Mockito.timeout(1000).times(1)).subscribe(messageSubscriptionEntity)

        where:
        action | _
        "ADDED" | _
        "MODIFIED" | _
    }

    def "Repeatedly attempt to re-start watcher when it closes with exception"() {

        setup: "Add Kafka Subscription to registry"
        kafkaSubscriptionRegistry.add(messageSubscriptionEntity, connectedSystem)

        and: "Mock listenerMessageSubscriptionRepository to return entity"
        Mockito.when(listenerMessageSubscriptionRepository.findById(100)).thenReturn(Optional.of(messageSubscriptionEntity))

        when: "kubernetes server returns outdated event to close the watcher with exception"
        def outdatedEvent = new WatchEventBuilder().withType(Watcher.Action.ERROR.name())
                .withObject(new StatusBuilder().withCode(HttpURLConnection.HTTP_GONE)
                        .withMessage("410: The event in requested index is outdated and cleared")
                        .build())
                .build()
        kubernetesServer.expect().withPath("/api/v1/namespaces/test/secrets?allowWatchBookmarks=true&watch=true")
                .andUpgradeToWebSocket()
                .open(outdatedEvent)
                .done()
                .once()

        and: "kubernetes server returns 404 three times when attempting to restart the secrets watcher"
        kubernetesServer.expect()
                .withPath("/api/v1/namespaces/test/secrets?allowWatchBookmarks=true&watch=true")
                .andReturn(HttpURLConnection.HTTP_NOT_FOUND,
                        new StatusBuilder().withCode(HttpURLConnection.HTTP_NOT_FOUND).build())
                .times(3)


        and: "kubernetes server accepts secrets watch request and emits secret modified event"
        Secret secret = new SecretBuilder().withNewMetadata().withName("secret1").endMetadata().build()
        kubernetesServer.expect().withPath("/api/v1/namespaces/test/secrets?allowWatchBookmarks=true&watch=true")
                .andUpgradeToWebSocket()
                .open()
                .waitFor(10)
                .andEmit(new WatchEvent(secret, "MODIFIED"))
                .done()
                .once()

        then: "Unsubscribe and subscribe performed for message subscription"
        Mockito.verify(kafkaSubscriber, Mockito.timeout(5000).times(1)).unsubscribe("100")
        Mockito.verify(kafkaSubscriber, Mockito.timeout(5000).times(1)).subscribe(messageSubscriptionEntity)
    }

    def "Running timer task to start watcher is cancelled when kafka subscription is removed"() {

        setup: "Add Kafka Subscription to registry"
        kafkaSubscriptionRegistry.add(messageSubscriptionEntity, connectedSystem)

        and: "Mock listenerMessageSubscriptionRepository to return entity"
        Mockito.when(listenerMessageSubscriptionRepository.findById(100)).thenReturn(Optional.of(messageSubscriptionEntity))

        when: "kubernetes server returns 404 three times when attempting to start the secrets watcher"
        kubernetesServer.expect()
                .withPath("/api/v1/namespaces/test/secrets?allowWatchBookmarks=true&watch=true")
                .andReturn(HttpURLConnection.HTTP_NOT_FOUND,
                        new StatusBuilder().withCode(HttpURLConnection.HTTP_NOT_FOUND).build())
                .always()

        then: "secret watcher is repeatedly attempting to start"
        pollingConditions.within(1, {
            kafkaKubernetesSecretsWatcher.isStarting() == true
            kafkaKubernetesSecretsWatcher.isRunning() == false
        })

        and: "Remove the Kafka subscription"
        kafkaSubscriptionRegistry.remove(messageSubscriptionEntity.id)

        then: "secret watcher is stopped"
        pollingConditions.within(1, {
            kafkaKubernetesSecretsWatcher.isStarting() == false
            kafkaKubernetesSecretsWatcher.isRunning() == false
        })
    }

    @TestConfiguration
    static class KafkaKubernetesSecretWatcherConfig {

        @Bean
        KubernetesClient kubernetesClient() {
            return kubernetesServer.getClient()
        }
    }
}