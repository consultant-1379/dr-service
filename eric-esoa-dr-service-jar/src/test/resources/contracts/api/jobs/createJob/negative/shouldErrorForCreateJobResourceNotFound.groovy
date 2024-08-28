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
package contracts.api.jobs.createJob.negative

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents an error scenario for creating a Job

```
given:
  client requests to post a job
when:
  a request is submitted
then:
  the request is rejected with 404 not found
```

""")
    request {
        method POST()
        url "/discovery-and-reconciliation/v1/jobs"
        headers {
            accept(applicationJson())
            contentType(applicationJson())
        }
        body("""
                {
                "name": "jobName",
                "description": "description",
                "featurePackId": "1",
                "applicationId": "404",
                "applicationJobName": "applicationJobName",
                "inputs": {}
                }
            """
        )
        bodyMatchers {
            jsonPath('$.applicationId', byRegex("404"))
            jsonPath('$.applicationJobName', byRegex(nonEmpty()))
            jsonPath('$.description', byRegex('[\\S\\s]*'))
            jsonPath('$.featurePackId', byRegex(nonEmpty()))
            jsonPath('$.name', byRegex(nonEmpty()))
            jsonPath('$.inputs', byRegex(/(.|\s)*/))
        }
    }
    response {
        status NOT_FOUND()
        headers {
            contentType(applicationJson())
        }
        body (
                """
                       {
                          "errorMessage":"Feature pack with id '1' does not exist.",
                          "errorCode":"DR-01"
                       }
                """
        )
    }
    priority(1)
}

