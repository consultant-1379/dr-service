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
import com.ericsson.bos.dr.service.subscriptions.kafka.KafkaSubscriber
import com.ericsson.bos.dr.service.subscriptions.kafka.KafkaSubscriptionRegistry
import com.ericsson.bos.dr.service.subscriptions.kafka.security.KakfaKubernetesSecretsEventReceiver
import io.fabric8.kubernetes.api.model.Secret
import io.fabric8.kubernetes.api.model.SecretBuilder
import spock.lang.Specification

import static io.fabric8.kubernetes.client.Watcher.Action

class KafkaKubernetesSecretEventReceiverSpec extends Specification {

    KafkaSubscriptionRegistry kafkaSubscriptionRegistryMock = Mock()
    KafkaSubscriber kafkaSubscriberMock = Mock()
    ListenerMessageSubscriptionRepository messageSubscriptionRepositoryMock = Mock()
    KakfaKubernetesSecretsEventReceiver kakfaKubernetesSecretsEventReceiver = new KakfaKubernetesSecretsEventReceiver(
            kafkaSubscriber:  kafkaSubscriberMock,
            kafkaSubscriptionRegistry: kafkaSubscriptionRegistryMock,
            messageSubscriptionRepository:  messageSubscriptionRepositoryMock)

    def "Ignore secrets which are not modified or are not referenced by an exiting kafka subscription"() {

        setup: "Mock kafkaSubscriptionRegistry to return empty subscriptions list"
        kafkaSubscriptionRegistryMock.findBySecret(_) >> []

        when: "Accept event"
        Secret secret = new SecretBuilder().withNewMetadata().withName("secret1").endMetadata().build()
        kakfaKubernetesSecretsEventReceiver.accept(action, secret)

        then: "Event is ignored"
        0 * messageSubscriptionRepositoryMock._
        0 * kafkaSubscriberMock._

        where:
        action | _
        Action.ADDED | _
        Action.DELETED | _
        Action.MODIFIED | _
    }
}