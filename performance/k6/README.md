## Running K6 test

K6 test is executed using 'grafana/k6' docker image. 

There is a single test script 'scripts/discover_and_reconcile.js' 
to perform a discovery and reconciliation job on a target test environment.
The script requires the Feature Pack has been uploaded. The target
environment test data passed as argument to the script identifies the featurePackId, 
applicationId, jobName and jobInputs for test execution. See example 
test data file 'docker_wiremock_data.json'.

By default, the script will execute the job for 1 VUSER and a single iteration.
This can be configured per execution by passing arguments to the k6 run command.

A wrapper script 'run.sh' is provided to run the test. It provides shortcuts for some
k6 supported executions such as ConstantVU and RampingVU executors. Alternatively
the K6 options can be provided directly to the script.

### run.sh examples
#### Run for 10 vusers and 100 iterations
```
run.sh -i 10:100
```
#### Run with 10 vusers for duration of 1 hour
```
run.sh -d 10:1h 
```
#### Ramp vusers
The following command will ramp from 1 to 10 vusers for 3 minutes
and then ramp from 10 to 20 vusers for 1 minutes.
```
run.sh -d "3m:10,1m:20" 
```
#### Pass k6 options directly
The following command will run a fixed number of iterations over a specified period.
```
run.sh -k "--duration 30s --rate 30 --preAllocatedVUs 10" 
```

## K6 Test Summary

The K6 test will output a summary of the http metrics. 

In addition to the built-in metrics, the test script 'scripts/discover_and_reconcile.js'
configures metrics for each of the individual D&R rest calls and the aggregated discovery
and reconcile requests.

The following was output for an executed with 5 vusers and 30 iterations.

The 'Discover' group shows the execution time metrics for the discovery. This is the time for the
discovery to complete with the job in state DISCOVERED.

The 'Reconcile' group then shows the reconciliation execution times.

Above are the individual discover and reconcile request times. The overall
time for the Discovery and Reconciliation is shown in the 'iteration_duration'.

The metrics for the individual REST calls are also output, for example DaR:GetDiscoveryJob 
which corresponds to the 'get job' requests sent to D&R.

```

     checks.........................: 100.00% ✓ 60        ✗ 0
     data_received..................: 78 kB   27 kB/s
     data_sent......................: 37 kB   13 kB/s
     group_duration.................: avg=233.52ms min=111.99ms med=239.12ms max=374.9ms  p(90)=286.73ms p(95)=338.26ms
     ✓ { group:::Discover }.........: avg=231.04ms min=111.99ms med=234.53ms max=336.59ms p(90)=279.39ms p(95)=283.14ms
     ✓ { group:::Reconcile }........: avg=236.01ms min=117.02ms med=242.78ms max=374.9ms  p(90)=322.99ms p(95)=370.26ms
     http_req_blocked...............: avg=37.38µs  min=930ns    med=3.83µs   max=3.44ms   p(90)=7.54µs   p(95)=16.08µs
     http_req_connecting............: avg=22.68µs  min=0s       med=0s       max=2.37ms   p(90)=0s       p(95)=0s
     http_req_duration..............: avg=11.36ms  min=918.06µs med=5.7ms    max=139.58ms p(90)=25.16ms  p(95)=41.16ms
     ✓ { DaR:GetDiscoveryJob }......: avg=5.83ms   min=1.13ms   med=2.98ms   max=32.56ms  p(90)=15.14ms  p(95)=18.5ms
     ✓ { DaR:GetReconcileJob }......: avg=7.21ms   min=918.06µs med=3.91ms   max=35.45ms  p(90)=19.42ms  p(95)=22.65ms
     ✓ { DaR:StartDiscovery }.......: avg=21.17ms  min=4.87ms   med=18.41ms  max=69.74ms  p(90)=47.05ms  p(95)=52.72ms
     ✓ { DaR:StartReconcile }.......: avg=29.37ms  min=3.36ms   med=16.95ms  max=139.58ms p(90)=59.54ms  p(95)=89.13ms
       { expected_response:true }...: avg=11.36ms  min=918.06µs med=5.7ms    max=139.58ms p(90)=25.16ms  p(95)=41.16ms
     http_req_failed................: 0.00%   ✓ 0         ✗ 232
     http_req_receiving.............: avg=1.3ms    min=18.35µs  med=301.2µs  max=22.52ms  p(90)=3.4ms    p(95)=6.06ms
     http_req_sending...............: avg=27.96µs  min=4.96µs   med=17.91µs  max=637.48µs p(90)=42.4µs   p(95)=56.51µs
     http_req_tls_handshaking.......: avg=0s       min=0s       med=0s       max=0s       p(90)=0s       p(95)=0s
     http_req_waiting...............: avg=10.03ms  min=698.19µs med=4.35ms   max=139.51ms p(90)=23.27ms  p(95)=34.21ms
     http_reqs......................: 232     80.726115/s
     ✓ { DaR:GetDiscoveryJob }......: 87      30.272293/s
     ✓ { DaR:GetReconcileJob }......: 85      29.576378/s
     ✓ { DaR:StartDiscovery }.......: 30      10.438722/s
     ✓ { DaR:StartReconcile }.......: 30      10.438722/s
     iteration_duration.............: avg=467.28ms min=288.08ms med=483.09ms max=654.42ms p(90)=549.77ms p(95)=617.27ms
     iterations.....................: 30      10.438722/s
     vus............................: 5       min=5       max=5
     vus_max........................: 5       min=5       max=5


running (00m02.9s), 0/5 VUs, 30 complete and 0 interrupted iterations
default ✓ [======================================] 5 VUs  00m02.9s/10m0s  30/30 shared iters
```

## K6 Reports in Grafana

The K6 test results can be optionally loaded and displayed in Grafana. There
are a number of pre-configured dashboards for K6.

A docker-compose.yml is provided with grafana and influxDB. These services
need to be started prior to running the tests in order to view the results.

The following are steps to start and configured grafana.

1. Start grafana and influxdb services
    ```
    docker-compose -f docker/docker-compose.yml up -d granfa influxdb
    ```
2. Launch grafana
   ```
   http://localost:3000
   ```
3. Add Datasource 
   <br>Provide the following options.
   <br>url=http://influxdb:8086
   <br>database=k6


4. Add Dashboard (Dashboard -> Import)
    <br>Try dashboard id 10660 or 13719 and Load. These are preconfigured k6 dashboards.
    <br>Select the added datasource and then Import

Once running and configured, the K6 test execution results will be displayed after test execution.
   
## Links

1. K6 user guide: https://k6.io/docs/using-k6/
2. K6 javascript api: https://k6.io/docs/javascript-api/
