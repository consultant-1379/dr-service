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
import com.ericsson.bos.dr.web.v1.api.model.JobListDto
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
class GetJobsPositiveBase extends Specification{

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
        doReturn(getJobListDto()).when(jobService).getJobs(any(), any(), any(), any())
        RestAssuredMockMvc.standaloneSetup(jobService, jobsController)
    }

    private JobListDto getJobListDto() {
        JobSummaryDto jobSummaryDto = getJobSummaryDto("1")
        JobSummaryDto jobSummaryDto1 = getJobSummaryDto("2")
        List<JobSummaryDto> jobSummaryDtoList = List.of(jobSummaryDto,jobSummaryDto1);
        return new JobListDto().items(jobSummaryDtoList).totalCount(jobSummaryDtoList.size());
    }

    private JobSummaryDto getJobSummaryDto(id) {
        JobSummaryDto jobSummaryDto = new JobSummaryDto()
        jobSummaryDto.setId(id)
        jobSummaryDto.setDescription("This is a description")
        jobSummaryDto.setName("TEST_NAME"+id)
        jobSummaryDto.setApplicationId("applicationId")
        jobSummaryDto.setApplicationName("applicationName")
        jobSummaryDto.setApplicationJobName("applicationJobName")
        jobSummaryDto.setFeaturePackId("featurePackId")
        jobSummaryDto.setFeaturePackName("featurePackName")
        jobSummaryDto.setStatus(JobSummaryDto.StatusEnum.DISCOVERY_INPROGRESS)
        jobSummaryDto.setStartDate("2023-03-16T12:30:11.281Z")
        jobSummaryDto.setCompletedDate("2023-03-16T12:31:11.281Z")
        return jobSummaryDto
    }
}