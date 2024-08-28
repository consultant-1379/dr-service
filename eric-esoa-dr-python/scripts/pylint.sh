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

# Run pylint in src/main/python directory
BASEDIR="${PWD//\//\/\/}"'/src/main/python' # workaround to update absolute path for running in Windows else volume mount will fail
BOB_PYTHON_IMAGE="armdocker.rnd.ericsson.se/proj-adp-cicd-drop/bob-python3builder:latest"

pylint_cmd="pylint --rcfile build/pylintrc build/bos"

docker run --rm -v "$BASEDIR:/build" $BOB_PYTHON_IMAGE bash -c "$pylint_cmd"
