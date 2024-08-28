#!/bin/bash
NAMESPACE=$1
PODS=$(kubectl -n $NAMESPACE get pod -l app=eric-esoa-dr-service -o custom-columns=":metadata.name" --no-headers)
EXPORT_FILES="ctsExport_TESTS_10.json ctsExport_TESTS_100.json ctsExport_TESTS_1000.json enmExport_TESTS_10.xml enmExport_TESTS_100.xml enmExport_TESTS_1000.xml"

for POD in $PODS;do
  for EXPORT_FILE in $EXPORT_FILES;do
	  echo "Copy $EXPORT_FILE to /tmp on pod $POD"
	  kubectl -n $NAMESPACE cp $EXPORT_FILE $POD:/tmp/$EXPORT_FILE
  done
done
