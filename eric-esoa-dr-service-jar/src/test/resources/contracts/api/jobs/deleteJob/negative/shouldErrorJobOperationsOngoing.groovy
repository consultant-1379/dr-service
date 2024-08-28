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
package contracts.api.jobs.deleteJob.negative

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents an error scenario for deleting a Job By Id

```
given:
  client requests delete job by id
when:
  a request is submitted
then:
  the request is rejected with 409 conflict
```

""")
    request {
        method DELETE()
        url "/discovery-and-reconciliation/v1/jobs/409"
        headers {
            accept(applicationJson())
        }
    }
    response {
        status CONFLICT()
        headers {
            contentType(applicationJson())
        }
        body (
                """
                       {
                          "errorMessage":"Job with id '409' cannot be deleted as it has status 'DISCOVERY_INPROGRESS'.",
                          "errorCode": "DR-18"
                       }
                """
        )
    }
    priority(1)
}
