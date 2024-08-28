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

import static org.mockito.Mockito.verify
import com.ericsson.bos.dr.jpa.model.JobEntity
import com.ericsson.bos.dr.jpa.model.ListenerMessageSubscriptionEntity
import com.ericsson.bos.dr.service.alarms.FaultName
import com.ericsson.bos.dr.service.exceptions.DRServiceException
import com.ericsson.bos.dr.service.exceptions.ErrorCode
import com.ericsson.bos.dr.service.subscriptions.kafka.KafkaSubscriber
import com.ericsson.bos.dr.service.utils.JSON
import com.ericsson.bos.dr.tests.integration.utils.WiremockUtil
import com.ericsson.bos.dr.web.v1.api.model.FeaturePackDto
import com.ericsson.bos.dr.web.v1.api.model.JobSummaryDto
import com.ericsson.bos.so.alarm.model.Alarm
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.spockframework.spring.SpringSpy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.messaging.MessagingException
import spock.lang.Unroll
import static org.mockito.ArgumentMatchers.any

import java.util.stream.StreamSupport

@EmbeddedKafka(kraft = false, partitions = 2, count = 2, brokerProperties = ["log.dir=target/embedded-kafka-3"],
        topics= ["topic_one", "topic_two", "topic_three", "topic_four", "topic_five", "topic_six", "topic_seven", "topic_eight", "topic_nine",
        "topic_ten", "topic_eleven", "topic_twelve", "topic_thirteen", "topic_fourteen", "topic_fifteen"])
class ListenerMessageTriggerSpec extends BaseSpec {

    static final int BROKER_ONE = 0
    static final int BROKER_TWO = 1
    static final int PARTITION_ZERO = 0
    static final int PARTITION_ONE = 1
    static final long WAIT_MILLISECONDS = 3000

    @Autowired
    @SpringSpy
    KafkaSubscriber kafkaSubscriber

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate

    @Value('${spring.embedded.kafka.brokers}')
    private String brokerAddresses

    def cleanup() {
        kafkaListenerEndpointRegistry.getListenerContainerIds().each{id ->
            kafkaListenerEndpointRegistry.unregisterListenerContainer(id)
        }
    }

    def "Message subscription configured with single topic receives message and triggers job1, and again following unsubscribe and resubscribe"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-24", "fp-24-${System.currentTimeMillis()}")

        and: "Stub http executor wiremock requests"
        WiremockUtil.stubForGet("/subsystem-manager/v2/subsystems(.*broker.*)",
                "/feature-packs/fp-24/responses/subsystem_broker.json", ["brokerUrl": getBrokerAddress(BROKER_ONE)])
        WiremockUtil.stubForGet("/fp-24/sources", "/feature-packs/fp-24/responses/source.json")
        WiremockUtil.stubForPost("/fp-24/reconcile/[0-9]+", "/feature-packs/fp-24/responses/reconcile.json")

        and: "Create message subscription and configure with one topic"
        ListenerMessageSubscriptionEntity subscription =
                listenersTestSteps.createMessageEntity(1, "broker", "group-1", ["topic_one"], "listener_1", featurePackDto.name)
        kafkaSubscriber.subscribe(subscription)

        when: "Message published to topic_one"
        Map event = [sourcesUrl  : "${wireMock.baseUrl()}/fp-24/sources".toString(),
            reconcileUrl: "${wireMock.baseUrl()}/fp-24/reconcile".toString(),
            eventType   : "CREATE_JOB_ONE"]
        kafkaTemplate.send("topic_one", JSON.toString(event))

        then: "Listener triggered by message received on topic_one, and Job COMPLETED"
        assertNamedJobInState("job1", JobSummaryDto.StatusEnum.COMPLETED)

        then: "Expected number of jobs created is one"
        assertJobsCreatedFromListener("job1", "listener_1", 1)

        when: "Delete message subscription"
        kafkaSubscriber.unsubscribe("1")

        and: "Message published to topic_one"
        kafkaTemplate.send("topic_one", JSON.toString(event))

        and: "Wait, just in case the message was actually consumed and it took a few seconds before a job was created"
        sleep(WAIT_MILLISECONDS)

        then: "Expected number of jobs created is still one"
        assertJobsCreatedFromListener("job1", "listener_1", 1)

        when: "Recreate message subscription and configure with same group and topic as before"
        kafkaSubscriber.subscribe(subscription)

        and: "Wait for kafka re-balancing to happen"
        sleep(WAIT_MILLISECONDS)

        then: "expected number of jobs created is two"
        assertJobsCreatedFromListener("job1", "listener_1", 2)
    }

    def "Message subscription configured with two topics receive messages and triggers job1 and job2"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-24", "fp-24-${System.currentTimeMillis()}")

        and: "Stub http executor wiremock requests"
        WiremockUtil.stubForGet("/subsystem-manager/v2/subsystems(.*broker.*)",
                "/feature-packs/fp-24/responses/subsystem_broker.json", ["brokerUrl": getBrokerAddress(BROKER_ONE)])
        WiremockUtil.stubForGet("/fp-24/sources", "/feature-packs/fp-24/responses/source.json")
        WiremockUtil.stubForPost("/fp-24/reconcile/[0-9]+", "/feature-packs/fp-24/responses/reconcile.json")

        and: "Create message subscription and configure with two topics"
        ListenerMessageSubscriptionEntity subscription =
                listenersTestSteps.createMessageEntity(1, "broker", "group-2", ["topic_two", "topic_three"], "listener_1", featurePackDto.name)
        kafkaSubscriber.subscribe(subscription)

        when: "Message published to topic_two"
        Map eventOne = [sourcesUrl  : "${wireMock.baseUrl()}/fp-24/sources".toString(),
            reconcileUrl: "${wireMock.baseUrl()}/fp-24/reconcile".toString(),
            eventType   : "CREATE_JOB_ONE"]
        kafkaTemplate.send("topic_two", JSON.toString(eventOne))

        and: "Message published to topic_three"
        Map eventTwo = [sourcesUrl  : "${wireMock.baseUrl()}/fp-24/sources".toString(),
            reconcileUrl: "${wireMock.baseUrl()}/fp-24/reconcile".toString(),
            eventType   : "CREATE_JOB_TWO"]
        kafkaTemplate.send("topic_three", JSON.toString(eventTwo))

        then: "Listener triggered by message received on topic_two, and Job1 COMPLETED"
        assertNamedJobInState("job1", JobSummaryDto.StatusEnum.COMPLETED)

        and: "Listener triggered by message received on topic_two, and Job2 COMPLETED"
        assertNamedJobInState("job2", JobSummaryDto.StatusEnum.COMPLETED)
    }

    def "Two message subscriptions each configured with one topic receive messages and trigger job1 and job2"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-24", "fp-24-${System.currentTimeMillis()}")

        and: "Stub http executor wiremock requests"
        WiremockUtil.stubForGet("/subsystem-manager/v2/subsystems(.*broker.*)",
                "/feature-packs/fp-24/responses/subsystem_broker.json", ["brokerUrl": getBrokerAddress(BROKER_ONE)])
        WiremockUtil.stubForGet("/fp-24/sources", "/feature-packs/fp-24/responses/source.json")
        WiremockUtil.stubForPost("/fp-24/reconcile/[0-9]+", "/feature-packs/fp-24/responses/reconcile.json")

        and: "Create message subscription and configure with one topic"
        ListenerMessageSubscriptionEntity subscriptionOne =
                listenersTestSteps.createMessageEntity(1, "broker", "group-3", ["topic_four"], "listener_1", featurePackDto.name)
        kafkaSubscriber.subscribe(subscriptionOne)

        and: "Create another message subscription and configure with one topic"
        ListenerMessageSubscriptionEntity subscriptionTwo =
                listenersTestSteps.createMessageEntity(2, "broker", "group-4", ["topic_five"], "listener_1", featurePackDto.name)
        kafkaSubscriber.subscribe(subscriptionTwo)

        when: "Message published to topic_four"
        Map eventOne = [sourcesUrl  : "${wireMock.baseUrl()}/fp-24/sources".toString(),
            reconcileUrl: "${wireMock.baseUrl()}/fp-24/reconcile".toString(),
            eventType   : "CREATE_JOB_ONE"]
        kafkaTemplate.send("topic_four", JSON.toString(eventOne))

        and: "Message published to topic_five"
        Map eventTwo = [sourcesUrl  : "${wireMock.baseUrl()}/fp-24/sources".toString(),
            reconcileUrl: "${wireMock.baseUrl()}/fp-24/reconcile".toString(),
            eventType   : "CREATE_JOB_TWO"]
        kafkaTemplate.send("topic_five", JSON.toString(eventTwo))

        then: "Listener triggered by message received on topic_four, and Job1 COMPLETED"
        assertNamedJobInState("job1", JobSummaryDto.StatusEnum.COMPLETED)

        and: "Listener triggered by message received on topic_five and Job2 COMPLETED"
        assertNamedJobInState("job2", JobSummaryDto.StatusEnum.COMPLETED)
    }

    def "Two message subscription each configured with one topic and different brokers receive messages and trigger job1 and job2"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-24", "fp-24-${System.currentTimeMillis()}")

        and: "Stub http executor wiremock requests"
        WiremockUtil.stubForGet("/subsystem-manager/v2/subsystems(.*broker_1.*)",
                "/feature-packs/fp-24/responses/subsystem_broker.json", ["brokerUrl": getBrokerAddress(BROKER_ONE)])
        WiremockUtil.stubForGet("/subsystem-manager/v2/subsystems(.*broker_2.*)",
                "/feature-packs/fp-24/responses/subsystem_broker.json", ["brokerUrl": getBrokerAddress(BROKER_TWO)])
        WiremockUtil.stubForGet("/fp-24/sources", "/feature-packs/fp-24/responses/source.json")
        WiremockUtil.stubForPost("/fp-24/reconcile/[0-9]+", "/feature-packs/fp-24/responses/reconcile.json")

        and: "Create message subscription and configure with one topic using broker one"
        ListenerMessageSubscriptionEntity subscriptionOne =
                listenersTestSteps.createMessageEntity(1, "broker_1", "group-5", ["topic_six"], "listener_1", featurePackDto.name)
        kafkaSubscriber.subscribe(subscriptionOne)

        and: "Create another message subscription and configure with one topic using broker two"
        ListenerMessageSubscriptionEntity subscriptionTwo =
                listenersTestSteps.createMessageEntity(2, "broker_2", "group-6", ["topic_seven"], "listener_1", featurePackDto.name)
        kafkaSubscriber.subscribe(subscriptionTwo)

        when: "Message published to topic_six"
        Map eventOne = [sourcesUrl  : "${wireMock.baseUrl()}/fp-24/sources".toString(),
            reconcileUrl: "${wireMock.baseUrl()}/fp-24/reconcile".toString(),
            eventType   : "CREATE_JOB_ONE"]
        kafkaTemplate.send("topic_six", JSON.toString(eventOne))

        and: "Message published to topic_seven"
        Map eventTwo = [sourcesUrl  : "${wireMock.baseUrl()}/fp-24/sources".toString(),
            reconcileUrl: "${wireMock.baseUrl()}/fp-24/reconcile".toString(),
            eventType   : "CREATE_JOB_TWO"]
        kafkaTemplate.send("topic_seven", JSON.toString(eventTwo))

        then: "Listener triggered by message received on topic_six, and Job1 COMPLETED"
        assertNamedJobInState("job1", JobSummaryDto.StatusEnum.COMPLETED)

        and: "Listener triggered by message received on topic_seven and Job2 COMPLETED"
        assertNamedJobInState("job2", JobSummaryDto.StatusEnum.COMPLETED)
    }

    def """Message subscriptions configured with the same group will read messages from a single partition.
           On deletion of one message subcription, the other message subscription continues to read messages"""() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-24", "fp-24-${System.currentTimeMillis()}")

        and: "Stub http executor wiremock requests"
        WiremockUtil.stubForGet("/subsystem-manager/v2/subsystems(.*broker.*)",
                "/feature-packs/fp-24/responses/subsystem_broker.json", ["brokerUrl": getBrokerAddress(BROKER_ONE)])
        WiremockUtil.stubForGet("/fp-24/sources", "/feature-packs/fp-24/responses/source.json")
        WiremockUtil.stubForPost("/fp-24/reconcile/[0-9]+", "/feature-packs/fp-24/responses/reconcile.json")

        and: "Create message subscription configured with a group"
        ListenerMessageSubscriptionEntity subscriptionOne =
                listenersTestSteps.createMessageEntity(1, "broker", "group-7", ["topic_eight"], "listener_1", featurePackDto.name)
        kafkaSubscriber.subscribe(subscriptionOne)

        and: "Create another message subscription configured with the same group"
        ListenerMessageSubscriptionEntity subscriptionTwo =
                listenersTestSteps.createMessageEntity(2, "broker", "group-7", ["topic_eight"], "listener_2", featurePackDto.name)
        kafkaSubscriber.subscribe(subscriptionTwo)

        and: "Wait for both containers to be ready, otherwise may get duplicate message on same listener"
        sleep(WAIT_MILLISECONDS)

        when: "Message published to partition 0"
        Map eventOne = [sourcesUrl  : "${wireMock.baseUrl()}/fp-24/sources".toString(),
            reconcileUrl: "${wireMock.baseUrl()}/fp-24/reconcile".toString(),
            eventType   : "CREATE_JOB_ONE"]
        kafkaTemplate.send("topic_eight", PARTITION_ZERO, "KEY1", JSON.toString(eventOne))

        and: "Message published to partition 1"
        Map eventTwo = [sourcesUrl  : "${wireMock.baseUrl()}/fp-24/sources".toString(),
            reconcileUrl: "${wireMock.baseUrl()}/fp-24/reconcile".toString(),
            eventType   : "CREATE_JOB_ONE"]
        kafkaTemplate.send("topic_eight", PARTITION_ONE, "KEY2", JSON.toString(eventTwo))

        then: "Each listener consumes a message from a single partition"
        String jobIdOne = assertJobCreatedFromListener("job1", "listener_1")
        String jobIdTwo = assertJobCreatedFromListener("job1", "listener_2")

        and: "Job1 completes in state COMPLETED"
        assertJobInState(jobIdOne, JobSummaryDto.StatusEnum.COMPLETED)

        and: "Job2 completes in state COMPLETED"
        assertJobInState(jobIdTwo, JobSummaryDto.StatusEnum.COMPLETED)

        and: "Total of two messages sent and two received"
        assertJobsCreatedCount("job1", 2)

        when: "delete message subscription for listener_2"
        kafkaSubscriber.unsubscribe("2")

        and: "Message published to topic_eight"
        kafkaTemplate.send("topic_eight", JSON.toString(eventOne))

        then: "expected number of jobs created on listener_1 is increased to 2, and jobs created on listener_2 remains at 1"
        assertJobsCreatedFromListener("job1", "listener_1", 2)
        assertJobsCreatedFromListener("job1", "listener_2", 1)
    }

    def "Message subscriptions configured with different groups will read messages from all partitions"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-24", "fp-24-${System.currentTimeMillis()}")

        and: "Stub http executor wiremock requests"
        WiremockUtil.stubForGet("/subsystem-manager/v2/subsystems(.*broker.*)",
                "/feature-packs/fp-24/responses/subsystem_broker.json", ["brokerUrl": getBrokerAddress(BROKER_ONE)])
        WiremockUtil.stubForGet("/fp-24/sources", "/feature-packs/fp-24/responses/source.json")
        WiremockUtil.stubForPost("/fp-24/reconcile/[0-9]+", "/feature-packs/fp-24/responses/reconcile.json")

        and: "Create message subscription configured with a group"
        ListenerMessageSubscriptionEntity subscriptionOne =
                listenersTestSteps.createMessageEntity(1, "broker", "group-8", ["topic_nine"], "listener_1", featurePackDto.name)
        kafkaSubscriber.subscribe(subscriptionOne)

        and: "Create another message subscription configured with a different group"
        ListenerMessageSubscriptionEntity subscriptionTwo =
                listenersTestSteps.createMessageEntity(2, "broker", "group-9", ["topic_nine"], "listener_2", featurePackDto.name)
        kafkaSubscriber.subscribe(subscriptionTwo)

        when: "Message published to partition 0"
        Map eventOne = [sourcesUrl  : "${wireMock.baseUrl()}/fp-24/sources".toString(),
            reconcileUrl: "${wireMock.baseUrl()}/fp-24/reconcile".toString(),
            eventType   : "CREATE_JOB_ONE"]
        kafkaTemplate.send("topic_nine", PARTITION_ZERO, "1234", JSON.toString(eventOne))

        and: "Message published to partition 1"
        Map eventTwo = [sourcesUrl  : "${wireMock.baseUrl()}/fp-24/sources".toString(),
            reconcileUrl: "${wireMock.baseUrl()}/fp-24/reconcile".toString(),
            eventType   : "CREATE_JOB_ONE"]
        kafkaTemplate.send("topic_nine", PARTITION_ONE, "5678", JSON.toString(eventTwo))

        then: "Each listener consumes the messages from all partitions"
        List listenerOneJobIds = assertJobsCreatedFromListener("job1", "listener_1", 2)
        List listenerTwoJobIds = assertJobsCreatedFromListener("job1", "listener_2", 2)

        and: "All Jobs complete in state COMPLETED"
        assertJobInState(listenerOneJobIds.get(0), JobSummaryDto.StatusEnum.COMPLETED)
        assertJobInState(listenerOneJobIds.get(1), JobSummaryDto.StatusEnum.COMPLETED)
        assertJobInState(listenerTwoJobIds.get(0), JobSummaryDto.StatusEnum.COMPLETED)
        assertJobInState(listenerTwoJobIds.get(1), JobSummaryDto.StatusEnum.COMPLETED)

        and: "Total of two messages sent and four received"
        assertJobsCreatedCount("job1", 4)
    }

    def "Retry message on exception"() {

        setup: "Stub http executor wiremock requests"
        WiremockUtil.stubForGet("/subsystem-manager/v2/subsystems(.*broker.*)",
                "/feature-packs/fp-24/responses/subsystem_broker.json", ["brokerUrl": getBrokerAddress(BROKER_ONE)])

        and: "Create message subscription"
        ListenerMessageSubscriptionEntity subscription =
                listenersTestSteps.createMessageEntity(1, "broker", "group-10", ["topic_ten"], "listener_1", "fp_1")
        kafkaSubscriber.subscribe(subscription)

        when: "Message published to topic_ten"
        kafkaTemplate.send("topic_ten", JSON.toString(["key": "message_1"]))

        and: "Wait for all messages to be received before verifying interactions"
        sleep(WAIT_MILLISECONDS)

        then: "Listener throws MessagingException. Message retried"
        3 * listenersService.triggerAsync("fp_1", "listener_1", ["key": "message_1"], subscription.id)
        >> { throw new MessagingException("some exception") }
    }

    def "Will not retry message on DRServiceException"() {

        setup: "Stub http executor wiremock requests"
        WiremockUtil.stubForGet("/subsystem-manager/v2/subsystems(.*broker.*)",
                "/feature-packs/fp-24/responses/subsystem_broker.json", ["brokerUrl": getBrokerAddress(BROKER_ONE)])

        and: "Create message subscription"
        ListenerMessageSubscriptionEntity subscription =
                listenersTestSteps.createMessageEntity(1, "broker", "group-11", ["topic_eleven"], "listener_1", "fp_1")
        kafkaSubscriber.subscribe(subscription)

        when: "Message published to topic_eleven"
        kafkaTemplate.send("topic_eleven", JSON.toString(["key": "message_1"]))

        and: "Another message published to topic_eleven"
        kafkaTemplate.send("topic_eleven", JSON.toString(["key": "message_2"]))

        and: "Wait for all messages to be received before verifying interactions"
        sleep(WAIT_MILLISECONDS)

        then: "Listener throws DRServiceException. Message processed once, no retry"
        1 * listenersService.triggerAsync("fp_1", "listener_1", ["key": "message_1"], subscription.id)
        >> { throw new DRServiceException(ErrorCode.BAD_REQUEST_PARAM, "X") }

        and: "Next message is consumed ok"
        1 * listenersService.triggerAsync("fp_1", "listener_1", ["key": "message_2"], subscription.id)
        >> "jobId_1"
    }

    @Unroll
    def "Message subscription created with default group id if none provided"() {

        setup: "Stub http executor wiremock requests"
        WiremockUtil.stubForGet("/subsystem-manager/v2/subsystems(.*broker.*)",
                "/feature-packs/fp-24/responses/subsystem_broker.json", ["brokerUrl": getBrokerAddress(BROKER_ONE)])

        when: "Create message subscription with empty group id"
        ListenerMessageSubscriptionEntity subscription =
                listenersTestSteps.createMessageEntity(1, "broker", groupId, ["topic_twelve"], "listener_1", "FP_1")
        kafkaSubscriber.subscribe(subscription)

        and: "Fetch the container just created"
        def container = kafkaListenerEndpointRegistry
                .getAllListenerContainers()
                .findAll {
                    it.properties.get("containerProperties").properties.get("topics") == ["topic_twelve"]
                }[0]

        then: "Default group id assigned"
        assert container.groupId == "FP_1_listener_1_test_subscription"

        where:
        groupId | _
        ""      | _
        null    | _
    }

    def "Throws DRServiceException if problem setting up message subscription"() {

        setup: "Stub http executor wiremock request to return empty subsystem configuration"
        WiremockUtil.stubForGet("/subsystem-manager/v2/subsystems(.*broker.*)",
                "/feature-packs/fp-24/responses/empty_connection_properties.json", ["brokerUrl": getBrokerAddress(BROKER_ONE)])

        when: "Attempt to create message subscription"
        ListenerMessageSubscriptionEntity subscription =
                listenersTestSteps.createMessageEntity(1, "broker", "group_1", ["topic_thirteen"], "listener_1", "FP_1")
        kafkaSubscriber.subscribe(subscription)

        then: "DRServiceException thrown"
        thrown DRServiceException
    }

    @Unroll
    def "Alarm raised when discovery or reconcile fails"() {

        setup: "Upload feature pack"
        FeaturePackDto featurePackDto = featurePackTestSteps.uploadFeaturePack(
                "/feature-packs/fp-24", "fp-24-${System.currentTimeMillis()}")

        and: "Stub http executor wiremock requests. Will return error during either the discovery or reconcile"
        WiremockUtil.stubForGet("/subsystem-manager/v2/subsystems(.*broker.*)",
                "/feature-packs/fp-24/responses/subsystem_broker.json", ["brokerUrl": getBrokerAddress(BROKER_ONE)])
        WiremockUtil.stubForGet("/fp-24/sources", get_sources_status, get_sources_body)
        WiremockUtil.stubForPost("/fp-24/reconcile/[0-9]+", reconcile_status, reconcile_body)

        and: "Create message subscription"
        ListenerMessageSubscriptionEntity subscription =
                listenersTestSteps.createMessageEntity(1, "broker", "group-1", [topic], "listener_1", featurePackDto.name)
        kafkaSubscriber.subscribe(subscription)

        when: "Message published to topic"
        Map event = [sourcesUrl  : "${wireMock.baseUrl()}/fp-24/sources".toString(),
                     reconcileUrl: "${wireMock.baseUrl()}/fp-24/reconcile".toString(),
                     eventType   : "CREATE_JOB_ONE"]
        kafkaTemplate.send(topic, JSON.toString(event))

        then: "Listener triggered by message received on topic, and Job DISCOVERY_FAILED | RECONCILE_FAILED"
        assertNamedJobInState("job1", expected_job_state)

        and: "Alarm raised with expected values"
        ArgumentCaptor<Alarm>  alarmCaptor = ArgumentCaptor.forClass(Alarm.class);
        verify(alarmSenderMock, Mockito.timeout(10000).times(1)).postAlarm(alarmCaptor.capture(), any())
        with(alarmCaptor.getValue()) {
            assert serviceName == "eric-esoa-dr-service"
            assert faultName == FaultName.JOB_FAILED.getName()
            assert faultyUrlResource ==
                    "/discovery-and-reconciliation/v1/jobs/${getJobId('listener_1-job1')}[name=listener_1-job1]"
            assert description == "A D&R job triggered by a message subscription is in a failed state. Please refer to the job resource in the D&R " +
                    "UI or NBI for details on the specific reason for the failure."
            assert expiration == 600
            assert eventTime != null
        }

        where: 'Set up discovery and reconcile success or failure'
        get_sources_status | get_sources_body                             | reconcile_status | reconcile_body     | expected_job_state                        | topic
        500                | "Discovery Error!"                           | 0                | "N/A"              | JobSummaryDto.StatusEnum.DISCOVERY_FAILED | "topic_fourteen"
        200                | "/feature-packs/fp-24/responses/source.json" | 500              | "Reconcile Error!" | JobSummaryDto.StatusEnum.RECONCILE_FAILED | "topic_fifteen"
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

    String assertJobCreatedFromListener(String applicationJobName, String listenerName) {
        Optional<JobEntity> optionalJob = null
        pollingConditions.eventually {
            interaction {
                optionalJob = StreamSupport.stream(jobRepository.findAll().spliterator(), false)
                .filter(job -> applicationJobName.equalsIgnoreCase(job.applicationJobName))
                .filter(job -> listenerName.equalsIgnoreCase(job.jobSpecification.getInputs().get("listenerName")))
                .findFirst()
                assert !optionalJob.empty
            }
        }
        return optionalJob.get().id
    }

    List<String> assertJobsCreatedFromListener(String applicationJobName, String listenerName, long expectedCount) {
        List<JobEntity> jobs = null
        pollingConditions.eventually {
            interaction {
                jobs = StreamSupport.stream(jobRepository.findAll().spliterator(), false)
                .filter(job -> applicationJobName.equalsIgnoreCase(job.applicationJobName))
                .filter(job -> listenerName.equalsIgnoreCase(job.jobSpecification.getInputs().get("listenerName")))
                .toList()
                assert jobs.size() == expectedCount
            }
        }
        return jobs.collect { String.valueOf(it.id) }
    }

    void assertJobsCreatedCount(String applicationJobName, long expectedCount) {
        long actualCount = StreamSupport.stream(jobRepository.findAll().spliterator(), false)
                .filter(job -> applicationJobName.equalsIgnoreCase(job.applicationJobName))
                .count()
        assert actualCount == expectedCount
    }

    String getJobId(String jobName) {
        StreamSupport.stream(jobRepository.findAll().spliterator(), false)
                .filter(job -> jobName.equalsIgnoreCase(job.name))
                .findFirst()
                .get().id
    }

    String getBrokerAddress(int brokerNo) {
        brokerAddresses.tokenize(",").get(brokerNo)
    }

}
