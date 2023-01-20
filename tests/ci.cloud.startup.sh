#!/usr/bin/env bash

# This script controls the startup of the container environment
# It can be used as an alternative to having docker-compose up started by the CI environment

source ./set-env.sh

echo " == Printing the most important environment variables"
echo " MANIFEST: ${MANIFEST}"
echo " TESTS_IMAGE: ${TESTS_IMAGE}"
echo " JAHIA_URL: ${JAHIA_URL}"
echo " TESTS_PROFILE: ${TESTS_PROFILE}"

if [[ $1 != "notests" ]]; then
    echo "$(date +'%d %B %Y - %k:%M') [TESTS] == Starting cypress tests =="
    docker-compose up --abort-on-container-exit --renew-anon-volumes cypress
fi
