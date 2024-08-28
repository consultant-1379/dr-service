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

import com.ericsson.bos.dr.service.JobService
import com.ericsson.bos.dr.service.ReconcileService
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.ContextConfiguration

import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.doReturn
import com.ericsson.bos.dr.service.DiscoveryService
import com.ericsson.bos.dr.web.JobsController
import io.restassured.module.mockmvc.RestAssuredMockMvc
import org.junit.Before
import org.mockito.InjectMocks
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import spock.lang.Specification

@ActiveProfiles("contracts")
@WebMvcTest(JobsController.class)
@ContextConfiguration(classes = [JobsController.class])
class CreateDuplicateJobPositiveBase extends Specification{

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
        doReturn(UUID.randomUUID().toString()).when(discoveryService).startDuplicateDiscovery(any())
        RestAssuredMockMvc.standaloneSetup(discoveryService, jobsController)
    }
}
