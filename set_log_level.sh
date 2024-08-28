#!/bin/bash
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

LOGGER=$1
LOG_LEVEL=$2
HTTP_SCHEME="http"
PORT=8080
SERVER_CERT_FILE="$TLS_CERT_DIRECTORY/tls.crt"
SERVER_KEY_FILE="$TLS_CERT_DIRECTORY/tls.key"
CURL_TLS_OPTIONS=""
OUTPUT_FILE="/tmp/logger_response.txt"

if [[ $# -lt 2 ]]; then
  printf "Set Application Log Level."
  printf "\n\nUsage: $0 [logger_name] [log_level]"
  printf "\n\nExamples:"
  printf '\n%-40s%s' "Turn on general debug logging" "$0 com.ericsson.bos.dr DEBUG"
  printf '\n%-40s%s' "Log incoming requests and responses" "$0 org.springframework.web.filter.CommonsRequestLoggingFilter DEBUG"
  printf '\n%-40s%s\n' "Log outgoing HTTP requests" "$0 reactor.netty.http.client.HttpClient DEBUG"
  exit 0
fi

if [[ $SECURITY_TLS_ENABLED == 'true' ]]; then
  HTTP_SCHEME="https"
  PORT=8443
  CURL_TLS_OPTIONS="-k --cert $SERVER_CERT_FILE --key $SERVER_KEY_FILE"
fi

response_code=$(curl -s -w "%{http_code}" -o $OUTPUT_FILE $CURL_TLS_OPTIONS ${HTTP_SCHEME}://localhost:$PORT/actuator/loggers/$LOGGER -X POST -H "content-type: application/json" -d "{\"configuredLevel\": \"${LOG_LEVEL}\", \"effectiveLevel\": \"${LOG_LEVEL}\"}")

if [[ $response_code -eq 204 ]];then
  echo "Log level set"
else
  cat $OUTPUT_FILE ; echo
  rm $OUTPUT_FILE 2>/dev/null
fi
