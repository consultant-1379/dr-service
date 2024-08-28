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

package com.ericsson.bos.dr.tests.unit.jpa

import com.ericsson.bos.dr.jpa.model.DiscoveredObjectStatusCounts
import com.ericsson.bos.dr.jpa.model.JobStatusEvaluator
import com.ericsson.bos.dr.jpa.model.StatusCount
import spock.lang.Specification

class JobStatusEvaluatorSpec extends Specification {

    def "Job status is evaluated as expected"() {

        setup: "create status counts"
        List<StatusCount> statusCounts = []
        Optional.ofNullable(discovered).ifPresent(count -> statusCounts.add(new StatusCountImpl("DISCOVERED", count)))
        Optional.ofNullable(reconciling).ifPresent(count -> statusCounts.add(new StatusCountImpl("RECONCILING", count)))
        Optional.ofNullable(reconciled).ifPresent(count -> statusCounts.add(new StatusCountImpl("RECONCILED", count)))
        Optional.ofNullable(reconcileFailed).ifPresent(count -> statusCounts.add(new StatusCountImpl("RECONCILE_FAILED", count)))
        Optional.ofNullable(partiallyReconciled).ifPresent(count -> statusCounts.add(new StatusCountImpl("PARTIALLY_RECONCILED", count)))

        when: "evaluate status"
        String status = JobStatusEvaluator.evaluate(new DiscoveredObjectStatusCounts(statusCounts))

        then: "status is as expected"
        status == expectedStatus

        where:
        discovered | reconciling | reconciled | reconcileFailed | partiallyReconciled | expectedStatus
        1          | null        | null       | null            | null                | "DISCOVERED"
        null       | null        | 1          | null            | null                | "COMPLETED"
        null       | null        | null       | null            | 1                   | "PARTIALLY_RECONCILED"
        null       | null        | 1          | 1               | 1                   | "PARTIALLY_RECONCILED"
        null       | null        | null       | 1               | 1                   | "PARTIALLY_RECONCILED"
        null       | null        | 1          | 1               | 1                   | "PARTIALLY_RECONCILED"
        1          | null        | 1          | 1               | 1                   | "PARTIALLY_RECONCILED"
        null       | null        | 1          | 1               | null                | "PARTIALLY_RECONCILED"
        null       | null        | null       | 1               | null                | "RECONCILE_FAILED"
        1          | null        | null       | 1               | null                | "RECONCILE_FAILED"
        1          | 1           | 1          | 1               | 1                   | "RECONCILE_INPROGRESS"
        null       | 1           | 1          | 1               | 1                   | "RECONCILE_INPROGRESS"
        null       | 1           | null       | 1               | 1                   | "RECONCILE_INPROGRESS"
        null       | 1           | null       | null            | 1                   | "RECONCILE_INPROGRESS"
        null       | 1           | null       | null            | null                | "RECONCILE_INPROGRESS"
        1          | null        | 1          | 1               | null                | "PARTIALLY_RECONCILED"
    }

    private static class StatusCountImpl implements StatusCount {

        private final String status;
        private final int count;

        StatusCountImpl(final String status, final int count) {
            this.status = status
            this.count = count
        }

        @Override
        String getStatus() {
            return status
        }

        @Override
        int getCount() {
            return count
        }
    }
}
