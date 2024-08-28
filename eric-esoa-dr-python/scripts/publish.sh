#!/bin/bash
#
# /*******************************************************************************
#  * COPYRIGHT Ericsson 2023
#  *
#  *
#  *
#  * The copyright to the computer program(s) herein is the property of
#  *
#  * Ericsson Inc. The programs may be used and/or copied only with written
#  *
#  * permission from Ericsson Inc. or in accordance with the terms and
#  *
#  * conditions stipulated in the agreement/contract under which the
#  *
#  * program(s) have been supplied.
#  ******************************************************************************/
#

# Deploy python package in target directory to artifactory.
# Note: Only released versions will be published.

BASEDIR=${PWD//\//\/\/} # workaround to update absolute path for running in git bash else volume mount will fail
VERSION=$1
BOB_PYTHON_IMAGE="armdocker.rnd.ericsson.se/proj-adp-cicd-drop/bob-python3builder:latest"

if [[ "$VERSION" == *"-SNAPSHOT"* ]]; then
  echo "Skipping publish for snapshot version $VERSION"
  exit 0
fi

python_package=$(ls -t target/*.whl | head -1)
publish_cmd="python3.11 -m pip -q install twine &&\
python3.11 -m twine upload -r local /build/${python_package} --config-file /build/.pypirc"


docker run --rm -v ${BASEDIR}:/build $BOB_PYTHON_IMAGE bash -c "$publish_cmd"