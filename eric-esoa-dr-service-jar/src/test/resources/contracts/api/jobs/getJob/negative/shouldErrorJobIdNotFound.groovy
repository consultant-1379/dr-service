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
package contracts.api.jobs.getJob.negative

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents an error scenario for getting a Job By Id

```
given:
  client requests get job by id
when:
  a request is submitted with id that does not exist
then:
  the request is rejected with 404 response
```

""")
    request {
        method GET()
        url "/discovery-and-reconciliation/v1/jobs/404"
        headers {
            accept(applicationJson())
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
                          "errorMessage":"Job with id '404' does not exist.",
                          "errorCode": "DR-17"
                       }
                """
        )
    }
    priority(1)
}

