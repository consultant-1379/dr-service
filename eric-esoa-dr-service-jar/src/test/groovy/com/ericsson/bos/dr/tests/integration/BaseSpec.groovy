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

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.ericsson.bos.so.alarm.service.AlarmSender
import org.spockframework.spring.SpringSpy
import org.spockframework.spring.UnwrapAopProxy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.cache.CacheManager
import org.springframework.test.context.jdbc.Sql

import com.ericsson.bos.dr.DRApplication
import com.ericsson.bos.dr.jpa.DiscoveryObjectRepository
import com.ericsson.bos.dr.jpa.InputConfigurationsRepository
import com.ericsson.bos.dr.jpa.JobRepository
import com.ericsson.bos.dr.jpa.JobScheduleRepository
import com.ericsson.bos.dr.jpa.JobSpecificationRepository
import com.ericsson.bos.dr.jpa.ListenerMessageSubscriptionRepository
import com.ericsson.bos.dr.service.JobScheduleService
import com.ericsson.bos.dr.service.JobService
import com.ericsson.bos.dr.service.ListenersService
import com.ericsson.bos.dr.service.PropertiesService
import com.ericsson.bos.dr.tests.integration.config.PostgresDatabaseConfiguration
import com.ericsson.bos.dr.tests.integration.config.TestContainerConfiguration
import com.ericsson.bos.dr.tests.integration.teststeps.ApplicationTestSteps
import com.ericsson.bos.dr.tests.integration.teststeps.DiscoveryTestSteps
import com.ericsson.bos.dr.tests.integration.teststeps.FeaturePackTestSteps
import com.ericsson.bos.dr.tests.integration.teststeps.InputConfigurationsTestSteps
import com.ericsson.bos.dr.tests.integration.teststeps.JobScheduleTestSteps
import com.ericsson.bos.dr.tests.integration.teststeps.JobTestSteps
import com.ericsson.bos.dr.tests.integration.teststeps.ListenersTestSteps
import com.ericsson.bos.dr.tests.integration.teststeps.PropertiesTestSteps
import com.ericsson.bos.dr.tests.integration.teststeps.ReconcileTestSteps
import com.ericsson.bos.dr.tests.integration.utils.JsonUtils
import com.ericsson.bos.dr.web.v1.api.model.DiscoveredObjectDto
import com.ericsson.bos.dr.web.v1.api.model.DiscoveredObjectListDto
import com.ericsson.bos.dr.web.v1.api.model.JobDto
import com.ericsson.bos.dr.web.v1.api.model.JobSummaryDto
import com.github.tomakehurst.wiremock.WireMockServer
import com.hubspot.jinjava.Jinjava

import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

@SpringBootTest(
        classes = [TestContainerConfiguration.class, PostgresDatabaseConfiguration.class, DRApplication.class])
@AutoConfigureMockMvc
@Sql("classpath:sql/clean.sql")
abstract class BaseSpec extends Specification {

    @Autowired
    FeaturePackTestSteps featurePackTestSteps

    @Autowired
    ApplicationTestSteps applicationTestSteps

    @Autowired
    JobTestSteps jobTestSteps

    @Autowired
    JobScheduleTestSteps jobScheduleTestSteps

    @Autowired
    DiscoveryTestSteps discoveryTestSteps

    @Autowired
    ReconcileTestSteps reconcileTestSteps

    @Autowired
    ListenersTestSteps listenersTestSteps

    @Autowired
    InputConfigurationsTestSteps inputTestSteps

    @Autowired
    PropertiesTestSteps propertiesTestSteps

    @Autowired
    JobRepository jobRepository

    @Autowired
    JobScheduleRepository jobScheduleRepository

    @Autowired
    JobSpecificationRepository jobSpecificationRepository

    @Autowired
    DiscoveryObjectRepository discoveryObjectRepository

    @Autowired
    InputConfigurationsRepository inputConfigurationsRepository

    @Autowired
    ListenerMessageSubscriptionRepository listenerMessageSubscriptionRepository

    @Autowired
    PropertiesService propertiesService

    @SpyBean
    JobService jobService

    @SpringSpy
    @UnwrapAopProxy
    JobScheduleService jobScheduleService

    @SpringSpy
    @UnwrapAopProxy
    ListenersService listenersService

    @Autowired
    CacheManager cacheManager

    @MockBean
    AlarmSender alarmSenderMock

    Jinjava jinjava = new Jinjava()

    PollingConditions pollingConditions = new PollingConditions(timeout: 10)

    @Shared
    static WireMockServer wireMock = new WireMockServer(options().port(8081))

    def setupSpec() {
        wireMock.start()
        configureFor(wireMock.port())
    }

    def cleanupSpec() {
        wireMock.stop()
    }

    def cleanup() {
        wireMock.resetAll()
        cacheManager.getCache("fp_properties_cache").clear()
    }


    void assertJobInState(String jobId, JobSummaryDto.StatusEnum jobState) {
        pollingConditions.eventually {
            JobDto jobDto = jobTestSteps.getJob(jobId)
            assert jobDto.status == jobState
            if (jobDto.status == JobSummaryDto.StatusEnum.COMPLETED) {
                assert jobDto.completedDate != null
            }
        }
    }

    void assertDiscoveredObjectsInState(String jobId, DiscoveredObjectDto.StatusEnum state) {
        pollingConditions.eventually {
            List<DiscoveredObjectDto> discoveredObjects =
                    JsonUtils.read(discoveryTestSteps.getDiscoveredObjects(jobId), DiscoveredObjectListDto).getItems()
            discoveredObjects.each {
                assert it.status == state
            }
        }
    }

    void assertJobInDiscoveryFailedState(String jobId, String... errorMessage) {
        pollingConditions.eventually {
            JobDto jobDto = jobTestSteps.getJob(jobId)
            assert jobDto.status == JobSummaryDto.StatusEnum.DISCOVERY_FAILED
            errorMessage.each { assert jobDto.errorMessage.contains(it) }
        }
    }

    void assertDiscoveryObjectCounts(String jobId, int discoveryCount, int reconcileCount, int reconcileFailedCount) {
        JobDto jobDto = jobTestSteps.getJob(jobId)
        assert jobDto.discoveredObjectsCount == discoveryCount
        assert jobDto.reconciledObjectsCount == reconcileCount
        assert jobDto.reconciledObjectsErrorCount == reconcileFailedCount
    }

    def substitute(String template, Map substitutionContext) {
        return jinjava.renderForResult(template, substitutionContext).getOutput()
    }

    def getJob(String id) {
        jobService.getJobById(id)
    }
}