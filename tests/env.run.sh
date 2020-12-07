#!/bin/bash
# This script can be used to warmup the environment and execute the tests
# It is used by the docker image at startup

if [[ ! -f .env ]]; then
 cp .env.example .env
fi

#!/usr/bin/env bash
START_TIME=$SECONDS

if [ -z "${JAHIA_CONTEXT}" ];
then
  JAHIA_URL=http://${JAHIA_HOST}:${JAHIA_PORT}
else
  JAHIA_URL=http://${JAHIA_HOST}:${JAHIA_PORT}/${JAHIA_CONTEXT}
fi

echo " == Using MANIFEST: ${MANIFEST}"
echo " == Using JAHIA_URL: ${JAHIA_URL}"

echo " == Waiting for Jahia to startup"
jahia-cli alive --jahiaAdminUrl=${JAHIA_URL}
ELAPSED_TIME=$(($SECONDS - $START_TIME))
echo " == Jahia became alive in ${ELAPSED_TIME} seconds"

# Add the credentials to a temporary manifest for downloading files
mkdir /tmp/run-artifacts
# Execute jobs listed in the manifest
# If the file doesn't exist, we assume it is a URL and we download it locally
if [[ -e ${MANIFEST} ]]; then
  cp ${MANIFEST} /tmp/run-artifacts
else
  echo "Downloading: ${MANIFEST}"
  curl ${MANIFEST} --output /tmp/run-artifacts/curl-manifest
  MANIFEST="curl-manifest"
fi
sed -i -e "s/NEXUS_USERNAME/${NEXUS_USERNAME}/g" /tmp/run-artifacts/${MANIFEST}
sed -i -e "s/NEXUS_PASSWORD/${NEXUS_PASSWORD}/g" /tmp/run-artifacts/${MANIFEST}

echo " == Get the Jahia version =="
JAHIA_FULL_VERSION=$(curl --location --request POST '${JAHIA_URL}/modules/graphql' --header 'Authorization: Basic cm9vdDpyb290' --header 'Content-Type: application/json' --data-raw '{"query":"{ admin { version } }","variables":{}}' | jq '.data.admin.version')
echo " == Using JAHIA_FULL_VERSION: ${JAHIA_FULL_VERSION}" 

# Extract the Jahia version from the full label
JAHIA_VERSION=$(echo ${JAHIA_FULL_VERSION} | sed -r 's/"[a-zA-Z ]* ([0-9\.]*) (\[.*\]) - .*"/\1/g')
echo " == Using JAHIA_VERSION: ${JAHIA_VERSION}" 

sed -i -e "s/JAHIA_VERSION/${JAHIA_VERSION}/g" /tmp/run-artifacts/${MANIFEST}

echo " == Warming up the environement =="
jahia-cli manifest:run --manifest=/tmp/run-artifacts/${MANIFEST} --jahiaAdminUrl=${JAHIA_URL}

echo " == Environment warmup complete =="

mkdir /tmp/results/reports

echo "== Run tests =="
# The additional settings is useful when you have to get dependencies from internal repositories
mvn -s .circleci/.circleci.settings.xml -Pmodule-integration-tests jahia:test surefire-report:report-only
if [[ $? -eq 0 ]]; then
  echo "success" > /tmp/results/test_success
  cp /tmp/target/surefire-reports/* /tmp/results/reports/
  while :; do :; done & kill -STOP $! && wait $!
  exit 0
else
  echo "failure" > /tmp/results/test_failure
  cp /tmp/target/surefire-reports/* /tmp/results/reports/
  while :; do :; done & kill -STOP $! && wait $!
  exit 1
fi

# After the test ran, we're dropping a marker file to indicate if the test failed or succeeded based on the script test command exit code