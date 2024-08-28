#
# COPYRIGHT Ericsson 2023
#
#
#
# The copyright to the computer program(s) herein is the property of
#
# Ericsson Inc. The programs may be used and/or copied only with written
#
# permission from Ericsson Inc. or in accordance with the terms and
#
# conditions stipulated in the agreement/contract under which the
#
# program(s) have been supplied.
#

import os
import requests


def run(subsystem, resource_config, resource_name, payload):
    """Execute run request towards rest-service"""
    rest_service_url = os.getenv('REST_SERVICE_URL')
    client_cert = os.getenv('CLIENT_CERT')
    client_key = os.getenv('CLIENT_KEY')

    rest_service_run_url = f"{rest_service_url}/rest-service/v1/run/{subsystem}/{resource_config}/{resource_name}"
    return requests.post(rest_service_run_url, json=payload, cert=(client_cert, client_key))
