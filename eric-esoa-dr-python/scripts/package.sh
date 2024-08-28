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

# Create D&R python package in target directory

BASEDIR=${PWD//\//\/\/} # workaround to update absolute path for running in git bash else volume mount will fail
BOB_PYTHON_IMAGE="armdocker.rnd.ericsson.se/proj-adp-cicd-drop/bob-python3builder:latest"
MAVEN_VERSION=$1
RELEASE_VERSION_REGEX='[0-9]+\.[0-9]+\.[0-9]+'

# Maven version is not compatible with python versioning so need to update.
# If SNAPSHOT maven version '1.1.1-1-abc123-SNAPSHOT', then convert to python dev version '1.1.1.dev0'. This results in python package
# 'eric_bos_dr-1.2.4.dev0-py3-none-any.whl.
# If RELEASE maven version '1.1.1-1', then convert to python release version '1.1.1'. This results in python package
# 'eric_bos_dr-1.2.4-py3-none-any.whl'. Leaving '-1' would result in post release package 'eric_bos_dr-1.2.4.post0-py3-none-any.whl'
if [[ $MAVEN_VERSION =~ $RELEASE_VERSION_REGEX ]];then
  PYTHON_PKG_VERSION=${BASH_REMATCH[0]}
  if [[ $MAVEN_VERSION = *SNAPSHOT* ]]; then
    PYTHON_PKG_VERSION=${PYTHON_PKG_VERSION}.dev0
  fi
else
  exit 1
fi

echo "$PYTHON_PKG_VERSION" > src/main/python/version.txt

package_cmd="python3.11 -m pip -q install build &&\
python3.11 -m build --outdir /build/target --wheel /build/src/main/python &&\
chmod -R 777 /build/target &&\
rm -rf /build/src/main/python/build /build/src/main/python/eric_bos_dr.egg-info /build/src/main/python/version.txt"

docker run --rm -v "$BASEDIR:/build" $BOB_PYTHON_IMAGE bash -c "$package_cmd"