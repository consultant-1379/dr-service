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
import com.ericsson.bos.dr.service.exceptions.DRServiceException
import com.ericsson.bos.dr.service.exceptions.ErrorCode
import com.ericsson.bos.dr.web.DRControllerAdvice
import com.ericsson.bos.dr.web.JobsController
import io.restassured.module.mockmvc.RestAssuredMockMvc
import org.junit.Before
import org.mockito.InjectMocks
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

import static org.mockito.Mockito.doThrow

@ActiveProfiles("contracts")
@WebMvcTest(JobsController.class)
@ContextConfiguration(classes = [JobsController.class])
class DeleteJobNegativeBase extends Specification {

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
        doThrow(new DRServiceException(ErrorCode.JOB_NOT_FOUND, "404")).when(jobService).deleteJobById("404", false)
        doThrow(new DRServiceException(ErrorCode.OPERATION_ONGOING, "409", "DISCOVERY_INPROGRESS")).when(jobService).deleteJobById("409", false)
        doThrow(new DRServiceException(ErrorCode.GENERAL_ERROR, "Error")).when(jobService).deleteJobById("500", false)
        RestAssuredMockMvc.standaloneSetup(jobService, jobsController, new DRControllerAdvice())
    }
}
