import http from 'k6/http'
import {check, fail, sleep, group} from "k6"

const TEST_DATA  =  JSON.parse(open(`${__ENV.TEST_DATA_PATH}`))
const JOBS_URL = `${TEST_DATA['drUrl']}/discovery-and-reconciliation/v1/jobs`
const JOB_POLL_INTERVAL = `${__ENV.JOB_POLL_INTERVAL}`

// By default will execute a single interation with one VU. This can be configured per execution by passing the
// required arguments to the k6 run command.
export const options = {
    thresholds: {
        // Hack to surface these sub-metrics (https://github.com/k6io/docs/issues/205)
        'http_req_duration{DaR:StartDiscovery}': ['max>=0'],
        'http_req_duration{DaR:GetDiscoveryJob}': ['max>=0'],
        'http_req_duration{DaR:StartReconcile}': ['max>=0'],
        'http_req_duration{DaR:GetReconcileJob}': ['max>=0'],
        'http_reqs{DaR:StartDiscovery}': ['count>=0'],
        'http_reqs{DaR:GetDiscoveryJob}': ['count>=0'],
        'http_reqs{DaR:StartReconcile}': ['count>=0'],
        'http_reqs{DaR:GetReconcileJob}': ['count>=0'],
        'group_duration{group:::Discover}': ['max>=0'],
        'group_duration{group:::Reconcile}': ['max>=0'],
    },
};

export default function() {
    let jobId

    group('Discover', function() {
        jobId = startDiscovery()
        pollUntilDiscoveryCompleted(jobId)
    });

    group('Reconcile', function() {
        startReconcile(jobId)
        pollUntilReconcileCompleted(jobId)
    });
}

function startDiscovery() {
    console.log('Starting discovery job: VU=' + __VU + ',Iteration=' + __ITER)
    const data = {
        name: "k6-job",
        featurePackId: TEST_DATA['featurePackId'],
        applicationId: TEST_DATA['applicationId'],
        applicationJobName: TEST_DATA['applicationJobName'],
        inputs: TEST_DATA['jobInputs']
    }
    const response = http.post(JOBS_URL, JSON.stringify(data), {
        headers: { 'Content-Type': 'application/json'},
        tags: { DaR: 'StartDiscovery' }
    })
    const result = check(response, { "status was 202": (r) => r.status == 202 })
    if (!result) {
        fail('Expected discovery to return 202 but returned ' + response.status)
    }
    return response.json().id
}

function pollUntilDiscoveryCompleted(jobId) {
    const url = `${JOBS_URL}/${jobId}`
    const response = http.get(url, { tags: { DaR: 'GetDiscoveryJob' }})
    if (response.status === 200) {
        const status = response.json().status
        console.log('Job ' + jobId + ' status: ' + status)
        if (status === 'DISCOVERY_FAILED') {
            fail('Job failed')
        } else if (status === 'DISCOVERED') {
            return
        }
    }
    sleep(JOB_POLL_INTERVAL)
    pollUntilDiscoveryCompleted(jobId)
}

function startReconcile(jobId) {
    console.log('Starting reconcile: JobId=' + jobId + ',VU=' + __VU + ',Iteration=' + __ITER)
    const url = `${JOBS_URL}/${jobId}/reconciliations`
    const data = { inputs: TEST_DATA['jobInputs']}
    const response = http.post(url, JSON.stringify(data), {
        headers: { 'Content-Type': 'application/json' },
        tags: { DaR: 'StartReconcile' }
    })
    const result = check(response, { "status was 202": (r) => r.status == 202 })
    if (!result) {
        fail('Expected reconcile to return 202 but returned ' + response.status)
    }
}

function pollUntilReconcileCompleted(jobId) {
    const url = `${JOBS_URL}/${jobId}`
    const response = http.get(url, { tags: { DaR: 'GetReconcileJob' }})
    if (response.status === 200) {
        const status = response.json().status
        const objectErrorCount = response.json().reconciledObjectsErrorCount
        console.log('Job ' + jobId + ' status: ' + status)
        if (objectErrorCount > 0) {
            fail('Reconcile failed for one or more discovered objects')
        }
        if (status === 'COMPLETED') {
            return
        }
    }
    sleep(JOB_POLL_INTERVAL)
    pollUntilReconcileCompleted(jobId)
}