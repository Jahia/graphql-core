#!/bin/bash
# This script can be used to warmup the environment and execute the tests
# It is used by the docker image at startup

call set-env.bat

echo " == Printing the most important environment variables"
echo " MANIFEST: ${MANIFEST}"
echo " TESTS_IMAGE: ${TESTS_IMAGE}"
echo " JAHIA_IMAGE: ${JAHIA_IMAGE}"
echo " LDAP_TAG: ${LDAP_TAG}"

docker-compose pull jahia dockerldap
docker-compose up -d --renew-anon-volumes --remove-orphans --force-recreate jahia dockerldap

if [[ $1 != "notests" ]]; then
    echo "$(date +'%d %B %Y - %k:%M') [TESTS] == Starting cypress tests =="
    docker-compose up --abort-on-container-exit --renew-anon-volumes cypress
fi
