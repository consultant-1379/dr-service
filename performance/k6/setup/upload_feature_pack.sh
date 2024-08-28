#!/bin/bash
DR_URL=$1
FP_FILE=${2:-enm_to_cts_performance.zip}
FP_URL="$DR_URL/discovery-and-reconciliation/v1/feature-packs"

RESPONSE=$(curl -s -F name="fp-perf-`date +'%s'`" -F description="enm to cts feature pack for performance test" -F file=@${FP_FILE} $FP_URL)
echo $RESPONSE
