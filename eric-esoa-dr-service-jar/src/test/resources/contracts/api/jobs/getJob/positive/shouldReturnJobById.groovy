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
package contracts.api.jobs.getJob.positive

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario for getting a Job By Id

```
given:
  client requests get job by id
when:
  a request is submitted
then:
  the request is accepted
```

""")
    request {
        method GET()
        url "/discovery-and-reconciliation/v1/jobs/${value(consumer(regex(/[0-9]+([0-9]+)*/)))}"
        headers {
            accept(applicationJson())
        }
    }
    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
        body (file("Job.json").toString().replaceAll("ID_TEMP", "${fromRequest().path(3).serverValue}"))
        bodyMatchers {
            jsonPath('$.id', byCommand("assertThat(parsedJson.read(\"\$.id\", String.class)).isNotNull()"))
            jsonPath('$.applicationId', byRegex(nonEmpty()))
            jsonPath('$.applicationName', byRegex(nonEmpty()))
            jsonPath('$.applicationJobName', byRegex(nonEmpty()))
            jsonPath('$.startDate', byRegex(iso8601WithOffset()))
            jsonPath('$.description', byRegex('[\\S\\s]*'))
            jsonPath('$.discoveredObjectsCount', byRegex(number()))
            jsonPath('$.completedDate', byRegex(iso8601WithOffset()))
            jsonPath('$.featurePackId', byRegex(nonEmpty()))
            jsonPath('$.featurePackName', byRegex(nonEmpty()))
            jsonPath('$.name', byRegex(nonEmpty()))
            jsonPath('$.reconciledObjectsCount', byRegex(number()))
            jsonPath('$.reconciledObjectsErrorCount',byRegex(number()))
            jsonPath('$.inputs',byRegex('([\\S\\s]+)?'))
            jsonPath('$.status',byRegex("DISCOVERY_INPROGRESS|DISCOVERY_FAILED|DISCOVERED|RECONCILING|RECONCILED|EXPIRED"))

        }
    }
    priority(2)
}

