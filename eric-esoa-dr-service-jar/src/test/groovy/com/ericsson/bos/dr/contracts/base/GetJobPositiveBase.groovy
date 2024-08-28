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
package com.ericsson.bos.dr.contracts.base

import com.ericsson.bos.dr.service.DiscoveryService
import com.ericsson.bos.dr.service.JobService
import com.ericsson.bos.dr.service.ReconcileService
import com.ericsson.bos.dr.web.JobsController
import com.ericsson.bos.dr.web.v1.api.model.JobDto
import com.ericsson.bos.dr.web.v1.api.model.JobSummaryDto
import io.restassured.module.mockmvc.RestAssuredMockMvc
import org.junit.Before
import org.mockito.InjectMocks
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.doReturn

@ActiveProfiles("contracts")
@WebMvcTest(JobsController.class)
@ContextConfiguration(classes = [JobsController.class])
class GetJobPositiveBase extends Specification{
    @MockBean
    DiscoveryService discoveryService

    @MockBean
    JobService jobService

    @MockBean
    private ReconcileService reconcileService;

    @Autowired
    @InjectMocks
    JobsController jobsController


    @Before
    void setup() {
        doReturn(getJobDto()).when(jobService).getJobById(any())
        RestAssuredMockMvc.standaloneSetup(jobService, jobsController)
    }

    private JobDto getJobDto() {
        JobDto jobDto = new JobDto()
        jobDto.setId("1")
        jobDto.setDescription("This is a description")
        jobDto.setName("TEST_NAME")
        jobDto.setApplicationId("applicationId")
        jobDto.setApplicationName("applicationName")
        jobDto.setApplicationJobName("jobName")
        jobDto.setFeaturePackId("featurePackId")
        jobDto.setFeaturePackName("featurePackName")
        jobDto.setInputs(new HashMap<String, Object>())
        jobDto.setDiscoveredObjectsCount(1)
        jobDto.setReconciledObjectsCount(0)
        jobDto.setReconciledObjectsErrorCount(0)
        jobDto.setStatus(JobSummaryDto.StatusEnum.DISCOVERY_INPROGRESS)
        jobDto.setStartDate(new Date().toInstant().toString())
        jobDto.setCompletedDate(new Date().toInstant().toString())
        return jobDto
    }
}