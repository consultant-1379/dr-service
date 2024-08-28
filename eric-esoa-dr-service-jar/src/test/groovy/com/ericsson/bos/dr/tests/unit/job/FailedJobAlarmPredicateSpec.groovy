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

package com.ericsson.bos.dr.tests.unit.job

import com.ericsson.bos.dr.jpa.model.JobEntity
import com.ericsson.bos.dr.service.job.FailedJobAlarmPredicate
import com.ericsson.bos.dr.web.v1.api.model.JobSummaryDto
import spock.lang.Specification

class FailedJobAlarmPredicateSpec extends Specification {

    def "Predicate should return expected result, indicating if an alarm should be raised or not"() {

        setup: "Various permutations of JobEntity properties"
        JobEntity jobEntity = new JobEntity(messageSubscriptionId: message_id, jobScheduleId: schedule_id, jobStatus: status)

        expect: "Correct result for alarm predicate test"
        assert new FailedJobAlarmPredicate().test(jobEntity) == expected_result

        where:
        message_id | schedule_id | status                                        | expected_result
        1          | null        | JobSummaryDto.StatusEnum.DISCOVERY_FAILED     | true
        1          | null        | JobSummaryDto.StatusEnum.RECONCILE_FAILED     | true
        null       | 1           | JobSummaryDto.StatusEnum.DISCOVERY_FAILED     | true
        null       | 1           | JobSummaryDto.StatusEnum.RECONCILE_FAILED     | true
        null       | null        | JobSummaryDto.StatusEnum.DISCOVERY_FAILED     | false
        null       | null        | JobSummaryDto.StatusEnum.RECONCILE_FAILED     | false
        1          | null        | JobSummaryDto.StatusEnum.COMPLETED            | false
        null       | 1           | JobSummaryDto.StatusEnum.COMPLETED            | false
        1          | null        | JobSummaryDto.StatusEnum.DISCOVERED           | false
        null       | 1           | JobSummaryDto.StatusEnum.DISCOVERED           | false
        1          | null        | JobSummaryDto.StatusEnum.DISCOVERY_INPROGRESS | false
        null       | 1           | JobSummaryDto.StatusEnum.DISCOVERY_INPROGRESS | false
        1          | null        | JobSummaryDto.StatusEnum.PARTIALLY_RECONCILED | false
        null       | 1           | JobSummaryDto.StatusEnum.PARTIALLY_RECONCILED | false
        1          | null        | JobSummaryDto.StatusEnum.RECONCILE_INPROGRESS | false
        null       | 1           | JobSummaryDto.StatusEnum.RECONCILE_INPROGRESS | false
        1          | null        | JobSummaryDto.StatusEnum.RECONCILE_REQUESTED  | false
        null       | 1           | JobSummaryDto.StatusEnum.RECONCILE_REQUESTED  | false
    }
}
