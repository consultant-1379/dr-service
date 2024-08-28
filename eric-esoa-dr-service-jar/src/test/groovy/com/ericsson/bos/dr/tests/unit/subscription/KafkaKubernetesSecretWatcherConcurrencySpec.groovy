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

import com.ericsson.bos.dr.jpa.model.ListenerMessageSubscriptionEntity
import com.ericsson.bos.dr.service.subscriptions.kafka.KafkaSubscriptionRegistry
import com.ericsson.bos.dr.service.subscriptions.kafka.security.KafkaKubernetesSecretsWatcher
import com.ericsson.bos.dr.service.subscriptions.kafka.security.KakfaKubernetesSecretsEventReceiver
import com.ericsson.bos.dr.service.subsystem.ConnectedSystem
import com.ericsson.bos.dr.service.subsystem.KafkaConnectionProperties
import io.fabric8.kubernetes.api.model.Secret
import io.fabric8.kubernetes.api.model.SecretList
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.Watch
import io.fabric8.kubernetes.client.dsl.MixedOperation
import io.fabric8.kubernetes.client.dsl.Resource
import org.springframework.context.ApplicationEventPublisher
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.concurrent.CountDownLatch

class KafkaKubernetesSecretWatcherConcurrencySpec extends Specification {

    KubernetesClient kubernetesClientMock = Mock()
    ApplicationEventPublisher applicationEventPublisherMock = Mock()
    KafkaSubscriptionRegistry kafkaSubscriptionRegistry = new KafkaSubscriptionRegistry(eventPublisher:  applicationEventPublisherMock)
    KakfaKubernetesSecretsEventReceiver kakfaKubernetesSecretsEventReceiverMock = Mock()

    KafkaKubernetesSecretsWatcher kafkaKubernetesSecretsWatcher = new KafkaKubernetesSecretsWatcher(kubernetesClient: kubernetesClientMock,
            kafkaSubscriptionRegistry:  kafkaSubscriptionRegistry,
            kakfaKubernetesSecretsEventReceiver:  kakfaKubernetesSecretsEventReceiverMock,
            timerDelay: 100)

    PollingConditions pollingConditions = new PollingConditions()

    MixedOperation<Secret, SecretList, Resource<Secret>> mixedOperationMock = Mock()
    Watch watchMock = Mock()

    ConnectedSystem<KafkaConnectionProperties> connectedSystem1 = new ConnectedSystem<>(name: "kafka1",
            connectionProperties: [new KafkaConnectionProperties(sslEnabled: true, keyStoreSecretName: "secret1", trustStoreSecretName:
                    "secret2")])
    ListenerMessageSubscriptionEntity messageSubscriptionEntity1 = new ListenerMessageSubscriptionEntity(id: 100)
    ListenerMessageSubscriptionEntity messageSubscriptionEntity2 = new ListenerMessageSubscriptionEntity(id: 200)

    def setup() {
        applicationEventPublisherMock.publishEvent(_) >> { kafkaKubernetesSecretsWatcher.handleKafkaRegistryEvent(it) }
    }

    def "Secrets watcher is started/stopped when kafka subscriptions are added/removed sequentially"() {

        when: "Add 2 Kafka Subscriptions to registry amd wait for watcher to start"
        kafkaSubscriptionRegistry.add(messageSubscriptionEntity1, connectedSystem1)
        kafkaSubscriptionRegistry.add(messageSubscriptionEntity2, connectedSystem1)
        // need to poll here rather than in 'then' block, otherwise interaction verification will fail
        // as the 'then' block will be reached before the watcher is started so will be zero interactions.
        pollingConditions.within(1, {
            kafkaKubernetesSecretsWatcher.isRunning() == true
            kafkaKubernetesSecretsWatcher.isStarting() == false
        })

        then: "Secrets watcher is started"
        1 * kubernetesClientMock.secrets() >> mixedOperationMock
        1 * mixedOperationMock.watch(_) >> watchMock

        when: "Remove 1 Kafka Subscription"
        kafkaSubscriptionRegistry.remove(messageSubscriptionEntity1.id)

        then: "Secrets watcher remains running"
        kafkaKubernetesSecretsWatcher.isRunning() == true
        kafkaKubernetesSecretsWatcher.isStarting() == false

        when: "Remove 2nd Kafka Subscription and wait for watcher to be stopped"
        kafkaSubscriptionRegistry.remove(messageSubscriptionEntity2.id)
        pollingConditions.within(1, {
            assert kafkaKubernetesSecretsWatcher.isRunning() == false
            assert kafkaKubernetesSecretsWatcher.isStarting() == false
        })

        then: "Secrets watcher is stopped"
        1 * watchMock.close()
    }


    def "Secrets watcher is started only once when kafka subscriptions are added concurrently"() {

        when: "Add 10 Kafka Subscriptions concurrently to registry and wait for watcher to start"
        List<Thread> threads = []
        10.times {
            ListenerMessageSubscriptionEntity messageSubscriptionEntity = new ListenerMessageSubscriptionEntity(id: it)
            threads.add(new Thread(() -> kafkaSubscriptionRegistry.add(messageSubscriptionEntity, connectedSystem1)))
        }

        threads.each { it.start() }
        threads.each { it.join() }

        pollingConditions.within(1, {
            kafkaKubernetesSecretsWatcher.isRunning() == true
            kafkaKubernetesSecretsWatcher.isStarting() == false
        })

        then: "Only one watcher is started"
        1 * kubernetesClientMock.secrets() >> mixedOperationMock
        1 * mixedOperationMock.watch(_) >> watchMock
    }

    def "Secrets watcher is stopped only once when kafka subscriptions are removed concurrently"() {

        when: "Add 10 Kafka Subscriptions to registry and wait for watcher to start"
        10.times {
            ListenerMessageSubscriptionEntity messageSubscriptionEntity = new ListenerMessageSubscriptionEntity(id: it)
            kafkaSubscriptionRegistry.add(messageSubscriptionEntity, connectedSystem1)
        }
        pollingConditions.within(1, {
            kafkaKubernetesSecretsWatcher.isRunning() == true
            kafkaKubernetesSecretsWatcher.isStarting() == false
        })

        then: "Secrets watcher is started"
        1 * kubernetesClientMock.secrets() >> mixedOperationMock
        1 * mixedOperationMock.watch(_) >> watchMock

        when: "Remove the 10 Kafka Subscriptions concurrently and wait for watcher to stop"
        List<Thread> threads = []
        10.times {threads.add(new Thread(() -> kafkaSubscriptionRegistry.remove(it)))}
        threads.each { it.start() }
        threads.each { it.join() }

        pollingConditions.within(1, {
            kafkaKubernetesSecretsWatcher.isRunning() == false
            kafkaKubernetesSecretsWatcher.isStarting() == false
        })

        then: "Watcher is stopped once"
        1 * watchMock.close()
    }

    def "Secrets watcher is started/stopped only once when kafka subscriptions are added/removed concurrently"() {

        when: "Add and remove 10 Kafka Subscriptions concurrently to registry and wait for watcher to start and stop"
        List<Thread> threads = []
        10.times {
            CountDownLatch latch = new CountDownLatch(1) // use latch to ensure delete will execute after create for each subscription
            ListenerMessageSubscriptionEntity messageSubscriptionEntity = new ListenerMessageSubscriptionEntity(id: it)
            threads.add(new Thread(() -> {
                kafkaSubscriptionRegistry.add(messageSubscriptionEntity, connectedSystem1)
                latch.countDown()
            }))
            threads.add(new Thread(() -> {
                latch.await()
                sleep(200)
                kafkaSubscriptionRegistry.remove(messageSubscriptionEntity.id)
            }))
        }
        threads.each { it.start() }
        Thread.sleep(1000)

        then: "Secrets watcher is started and stopped once"
        1 * kubernetesClientMock.secrets() >> mixedOperationMock
        1 * mixedOperationMock.watch(_) >> watchMock
        1 * watchMock.close()
    }
}