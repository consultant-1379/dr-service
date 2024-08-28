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
package contracts.api.jobs.createJob.positive

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario for creating a duplicate Job

```
given:
  client posts a duplicate job
when:
  a request is submitted
then:
  the request is accepted
```

""")
    request {
        method POST()
        url "/discovery-and-reconciliation/v1/jobs/${value(consumer(regex(/[0-9]+([0-9]+)*/)))}/duplicate"
        headers {
            accept(applicationJson())
        }
    }
    response {
        status ACCEPTED()
        headers {
            contentType(applicationJson())
        }
        body(id: $(anyNumber()))
        bodyMatchers {
            jsonPath('$.id', byCommand("assertThat(parsedJson.read(\"\$.id\", String.class)).isNotNull()"))
        }
    }
    priority(2)
}

