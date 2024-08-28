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

package com.ericsson.bos.dr.tests.unit.job.housekeeping

import com.ericsson.bos.dr.jpa.JobRepository
import com.ericsson.bos.dr.jpa.JobScheduleRepository
import com.ericsson.bos.dr.jpa.model.JobEntity
import com.ericsson.bos.dr.jpa.model.JobScheduleEntity
import com.ericsson.bos.dr.service.JobScheduleService
import com.ericsson.bos.dr.service.job.housekeeping.RepairJobScheduleAction
import com.ericsson.bos.dr.web.v1.api.model.JobSummaryDto
import spock.lang.Specification

class RepairJobScheduleActionSpec extends Specification {

    JobScheduleService jobScheduleServiceMock = Mock()
    JobRepository jobRepositoryMock = Mock()
    JobScheduleRepository jobScheduleRepositoryMock = Mock()

    RepairJobScheduleAction repairJobScheduleAction = new RepairJobScheduleAction(jobScheduleService: jobScheduleServiceMock,
            jobScheduleRepository:  jobScheduleRepositoryMock,
            jobRepository: jobRepositoryMock)


    def "Next schedule is not created when schedule no longer exists"() {

        setup: "Find jobSchedule returns empty"
        jobScheduleRepositoryMock.findById(1) >> Optional.empty()

        when: "Repair job schedule which no longer exists"
        JobEntity jobEntity = new JobEntity(id: 1, jobScheduleId: 1, jobStatus: JobSummaryDto.StatusEnum.DISCOVERY_INPROGRESS)
        repairJobScheduleAction.accept(jobEntity)

        then: "Next job schedule is not created"
        0 * jobScheduleServiceMock.createNextScheduledJob(_)
    }

    def "Next schedule is not created when schedule is disabled"() {
        setup: "Find jobSchedule returns disabled schedule"
        jobScheduleRepositoryMock.findById(1) >> Optional.of(new JobScheduleEntity(enabled: false))

        when: "Repair disabled job schedule"
        JobEntity jobEntity = new JobEntity(id: 1, jobScheduleId: 1, jobStatus: JobSummaryDto.StatusEnum.DISCOVERY_INPROGRESS)
        repairJobScheduleAction.accept(jobEntity)

        then: "Next job schedule is not created"
        0 * jobScheduleServiceMock.createNextScheduledJob(_)
    }
}