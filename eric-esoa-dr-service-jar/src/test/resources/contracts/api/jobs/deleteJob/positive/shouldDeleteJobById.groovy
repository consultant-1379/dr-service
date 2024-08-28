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
package contracts.api.jobs.deleteJob.positive

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario for deleting a Job By Id

```
given:
  client requests delete job by id
when:
  a request is submitted
then:
  the request is accepted
```

""")
    request {
        method DELETE()
        url "/discovery-and-reconciliation/v1/jobs/${value(consumer(regex(/[0-9]+([0-9]+)*/)))}"
        headers {
            accept(applicationJson())
        }
    }
    response {
        status NO_CONTENT()
    }
    priority(2)
}
