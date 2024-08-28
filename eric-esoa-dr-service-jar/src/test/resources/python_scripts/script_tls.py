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

rest_service_url = os.getenv('REST_SERVICE_URL')
client_cert = os.getenv('CLIENT_CERT')
client_key = os.getenv('CLIENT_KEY')

print(f"{rest_service_url},{client_cert},{client_key}")