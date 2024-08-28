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
package com.ericsson.bos.dr.tests.integration

import com.ericsson.bos.dr.jpa.model.FeaturePackEntity
import com.ericsson.bos.dr.jpa.model.JobEntity
import com.ericsson.bos.dr.jpa.model.ListenerEntity
import com.ericsson.bos.dr.jpa.model.ListenerMessageSubscriptionEntity
import com.ericsson.bos.dr.service.exceptions.DRServiceException
import com.ericsson.bos.dr.service.exceptions.ErrorCode
import com.ericsson.bos.dr.service.subscriptions.kafka.KafkaSubscriber
import com.ericsson.bos.dr.service.utils.JSON
import com.ericsson.bos.dr.tests.integration.utils.IOUtils
import com.ericsson.bos.dr.tests.integration.utils.WiremockUtil
import com.ericsson.bos.dr.web.v1.api.model.FeaturePackDto
import com.ericsson.bos.dr.web.v1.api.model.JobSummaryDto
import com.google.common.io.Resources
import io.fabric8.kubernetes.api.model.SecretBuilder
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.server.mock.KubernetesServer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import spock.lang.Shared
import com.ericsson.bos.dr.tests.integration.utils.SpringContextUtils
import spock.util.concurrent.PollingConditions

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.stream.StreamSupport

@EmbeddedKafka(kraft = false, partitions = 1, count = 1, brokerProperties = ["log.dir=target/embedded-kafka-2"],
        brokerPropertiesLocation = "classpath:/kafka/certs/broker.properties", topics= ["topic_one", "topic_two"])
class ListenerMessageTriggerMTLSSpec extends BaseSpec {

    static final String SECURE_BROKER_URL = "localhost:9093"

    @Autowired
    KafkaSubscriber kafkaSubscriber

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Shared
    KubernetesServer kubernetesServer = new KubernetesServer(false)

    @Shared
    KubernetesClient kubernetesClient

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    def setupSpec() {
        kubernetesServer.before()
        copyJKSFromResourceToFileSystem("kafka/certs/truststore.jks", "truststore.jks")
        copyJKSFromResourceToFileSystem("kafka/certs/keystore.jks", "keystore.jks")
    }

    def cleanupSpec() {
        kubernetesServer.after()
        Optional.ofNullable(kubernetesClient).ifPresent(SpringContextUtils::destroyBean)
    }

    def setup() {
        if (kubernetesClient == null) {
            kubernetesClient = kubernetesServer.getClient()
            SpringContextUtils.registerBean("kubernetesClient", kubernetesClient)
        }
    }

    def cleanup() {
        kafkaListenerEndpointRegistry.getListenerContainerIds()
                .forEach(id -> kafkaListenerEndpointRegistry.unregisterListenerContainer(id))
    }

    def "Message subscription, configured with mTLS, receives message and triggers job"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-24", "fp-24-${System.currentTimeMillis()}")

        and: "Stub http executor wiremock requests"
        WiremockUtil.stubForGet("/subsystem-manager/v2/subsystems(.*mtls_broker.*)",
                "/feature-packs/fp-24/responses/subsystem_broker_mtls.json", ["brokerUrl": SECURE_BROKER_URL])
        WiremockUtil.stubForGet("/fp-24/sources", "/feature-packs/fp-24/responses/source.json")
        WiremockUtil.stubForPost("/fp-24/reconcile/[0-9]+", "/feature-packs/fp-24/responses/reconcile.json")

        and: "Stub kubernetes request to return truststore secret"
        kubernetesServer.expect().get().withPath("/api/v1/namespaces/test/secrets/truststore_secret")
                .andReturn(200,
                        new SecretBuilder()
                                .withData(["server.p12":
                                                   Base64.encoder.encodeToString(
                                                           IOUtils.readClasspathResourceBytes("/kafka/certs/ca.p12"))])
                                .build()).once()

        and: "Stub kubernetes request to return keystore secret"
        kubernetesServer.expect().get().withPath("/api/v1/namespaces/test/secrets/keystore_secret")
                .andReturn(200,
                        new SecretBuilder()
                                .withData(["client.p12":
                                                   Base64.encoder.encodeToString(
                                                           IOUtils.readClasspathResourceBytes("/kafka/certs/client.p12"))])
                                .build()).once()

        and: "Create message subscription referencing subsystem with mTLS configuration"
        ListenerMessageSubscriptionEntity subscription =
                createMessageEntity("mtls_broker", "group-1", ["topic_one"], "listener_1", featurePackDto.name)
        kafkaSubscriber.subscribe(subscription)

        when: "Message published to topic_one"
        Map event = [sourcesUrl  : "${wireMock.baseUrl()}/fp-24/sources".toString(),
                     reconcileUrl: "${wireMock.baseUrl()}/fp-24/reconcile".toString(),
                     eventType   : "CREATE_JOB_ONE"]
        kafkaTemplate.send("topic_one", JSON.toString(event))

        then: "Listener triggered by message received on topic_one, and Job COMPLETED"
        assertNamedJobInState("job1", JobSummaryDto.StatusEnum.COMPLETED)
    }

    def "Should throw exception when message subscription is created with invalid certificate"() {

        setup: "Stub http executor wiremock requests"
        WiremockUtil.stubForGet("/subsystem-manager/v2/subsystems(.*mtls_broker_bad.*)",
                "/feature-packs/fp-24/responses/subsystem_broker_badly_configured_mtls.json", ["brokerUrl": SECURE_BROKER_URL])

        and: "Stub kubernetes request to return truststore secret"
        kubernetesServer.expect().get().withPath("/api/v1/namespaces/test/secrets/truststore_secret")
                .andReturn(200, new SecretBuilder().withData(["server.p12": Base64.encoder.encodeToString(
                        IOUtils.readClasspathResourceBytes("/kafka/certs/ca.p12"))])
                        .build())
                .once()

        when: "Create message subscription referencing subsystem with bad mTLS configuration (missing keystore)"
        ListenerMessageSubscriptionEntity subscription =
                createMessageEntity("mtls_broker_bad", "group-1", ["topic_two"], "listener_1", "fp-1")
        kafkaSubscriber.subscribe(subscription)

        then: "Exception due to bad certificate"
        DRServiceException exception = thrown(DRServiceException)
        exception.errorMessage.errorCode == ErrorCode.KAFKA_SSL_AUTH_ERROR.errorCode
    }

    def "Should throw exception if secret does not exist"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-24", "fp-24-${System.currentTimeMillis()}")

        and: "Stub http executor wiremock requests"
        WiremockUtil.stubForGet("/subsystem-manager/v2/subsystems(.*mtls_broker.*)",
                "/feature-packs/fp-24/responses/subsystem_broker_mtls.json", ["brokerUrl": SECURE_BROKER_URL])

        and: "Stub kubernetes request to not find secret"
        kubernetesServer.expect().get().withPath("/api/v1/namespaces/test/secrets/truststore_secret")
                .andReturn(200, null).once()

        when: "Create message subscription referencing subsystem with mTLS configuration"
        ListenerMessageSubscriptionEntity subscription =
                createMessageEntity("mtls_broker", "group-1", ["topic_one"], "listener_1", featurePackDto.name)
        kafkaSubscriber.subscribe(subscription)

        then: "Exception when secret not found"
        final DRServiceException exception = thrown()
        exception.message == "DR-55:Kubernetes secret 'truststore_secret' not found for the kafka connected system."
    }

    def "Should throw exception if data field not found in secret"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-24", "fp-24-${System.currentTimeMillis()}")

        and: "Stub http executor wiremock requests"
        WiremockUtil.stubForGet("/subsystem-manager/v2/subsystems(.*mtls_broker.*)",
                "/feature-packs/fp-24/responses/subsystem_broker_mtls.json", ["brokerUrl": SECURE_BROKER_URL])

        and: "Stub kubernetes request to return truststore secret without expected data key field"
        kubernetesServer.expect().get().withPath("/api/v1/namespaces/test/secrets/truststore_secret")
                .andReturn(200, new SecretBuilder().withData(["field1": "value1"])
                        .build())
                .once()

        when: "Create message subscription referencing subsystem with mTLS configuration"
        ListenerMessageSubscriptionEntity subscription =
                createMessageEntity("mtls_broker", "group-1", ["topic_one"], "listener_1", featurePackDto.name)
        kafkaSubscriber.subscribe(subscription)

        then: "Exception when data field not found in secret"
        final DRServiceException exception = thrown()
        exception.message == "DR-56:Data field 'server.p12' not found in kubernetes secret 'truststore_secret' for the kafka connected system."
    }

    ListenerMessageSubscriptionEntity createMessageEntity(String subsystem, String group, List<String> topics, String listener, String featurePack) {
        return new ListenerMessageSubscriptionEntity([id: 1,
                                                      name                 : "test_subscription",
                                                      subsystemName        : subsystem,
                                                      consumerConfiguration: ["groupId": group, "topicNames": topics],
                                                      listenerEntity       : new ListenerEntity(
                                                              [name       : listener,
                                                               featurePack: new FeaturePackEntity(name: featurePack)])])
    }

    void assertNamedJobInState(String applicationJobName, JobSummaryDto.StatusEnum jobState) {
        Optional<JobEntity> optionalJob = null
        pollingConditions.eventually {
            interaction {
                optionalJob = StreamSupport.stream(jobRepository.findAll().spliterator(), false)
                        .filter(job -> applicationJobName.equalsIgnoreCase(job.applicationJobName))
                        .findFirst()
                assert !optionalJob.empty
                assert optionalJob.get().jobStatus == jobState.toString()
            }
        }
    }

    void assertNoJobsCreated() {
        new PollingConditions(initialDelay: 5).eventually {
            assert jobRepository.findAll().isEmpty()
        }
    }

    def copyJKSFromResourceToFileSystem(String sourcePath, String storeName) {
        Path trustStorePath = Paths.get((new File(Resources.getResource(sourcePath).getPath())).getAbsolutePath())
        Files.copy(trustStorePath, Paths.get(System.getProperty("java.io.tmpdir"), storeName), StandardCopyOption.REPLACE_EXISTING)
    }
}