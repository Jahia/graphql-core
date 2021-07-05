#!/bin/bash
# This script can be used to warmup the environment and execute the tests
# It is used by the docker image at startup

if [[ ! -f .env ]]; then
 cp .env.example .env
fi

#!/usr/bin/env bash
START_TIME=$SECONDS

echo " == Using MANIFEST: ${MANIFEST}"

if [[ ${JAHIA_URL} =~ .*/$ ]]; then
  JAHIA_URL=$(echo ${JAHIA_URL} | sed 's/.$//')
fi
echo " == Using JAHIA_URL: ${JAHIA_URL}"
TEST_URL="${JAHIA_URL}/cms"

echo " == Using TEST_URL: ${TEST_URL}"

echo " == Waiting for Jahia to startup"
while [[ "$(curl -s -o /dev/null -w ''%{http_code}'' ${JAHIA_URL}/cms/login)" != "200" ]];
  do sleep 5;
done
ELAPSED_TIME=$(($SECONDS - $START_TIME))
echo " == Jahia became alive in ${ELAPSED_TIME} seconds"

echo " == Get the Jahia version =="
JAHIA_FULL_VERSION=$(curl --location --request POST ${JAHIA_URL}/modules/graphql --header 'Authorization: Basic cm9vdDpyb290' --header "Origin: ${JAHIA_URL}" --header 'Content-Type: application/json' --data-raw '{"query":"{ admin { version } }","variables":{}}' | jq '.data.admin.version')

if [[ ${JAHIA_FULL_VERSION} == null ]]; then
	echo " == Get a more recent version of GraphQL =="
	sed -i -e "s/NEXUS_USERNAME/${NEXUS_USERNAME}/g" warmup-manifest-graphql.yml
	sed -i -e "s/NEXUS_PASSWORD/${NEXUS_PASSWORD}/g" warmup-manifest-graphql.yml
	echo " == Deploy GraphQL module =="
	jahia-cli manifest:run --manifest=warmup-manifest-graphql.yml --jahiaAdminUrl=${JAHIA_URL}
	echo " == Get again the Jahia version =="
	JAHIA_FULL_VERSION=$(curl --location --request POST ${JAHIA_URL}/modules/graphql --header 'Authorization: Basic cm9vdDpyb290' --header "Origin: ${JAHIA_URL}" --header 'Content-Type: application/json' --data-raw '{"query":"{ admin { version } }","variables":{}}' | jq '.data.admin.version')
fi
echo " == Using JAHIA_FULL_VERSION: ${JAHIA_FULL_VERSION}"

# Extract the Jahia version from the full label
# It is needed to get the right jahia-test-module version
JAHIA_VERSION=$(echo ${JAHIA_FULL_VERSION} | sed -r 's/"[a-zA-Z ]*([0-9\.]*) \[*.*\]*[[:space:]]*- .*"/\1/g')
echo " == Using JAHIA_VERSION: ${JAHIA_VERSION}"

# Add the credentials to a temporary manifest for downloading files

# Execute jobs listed in the manifest
# If the file doesn't exist, we assume it is a URL and we download it locally
mkdir /tmp/run-artifacts
if [[ -e ${MANIFEST} ]]; then
  cp ${MANIFEST} /tmp/run-artifacts
else
  echo "Downloading: ${MANIFEST}"
  curl ${MANIFEST} --output /tmp/run-artifacts/curl-manifest
  MANIFEST="curl-manifest"
fi
sed -i -e "s/NEXUS_USERNAME/${NEXUS_USERNAME}/g" /tmp/run-artifacts/${MANIFEST}
sed -i -e "s/NEXUS_PASSWORD/${NEXUS_PASSWORD}/g" /tmp/run-artifacts/${MANIFEST}
sed -i -e "s/JAHIA_VERSION/${JAHIA_VERSION}/g" /tmp/run-artifacts/${MANIFEST}

echo " == Warming up the environement =="
jahia-cli manifest:run --manifest=/tmp/run-artifacts/${MANIFEST} --jahiaAdminUrl=${JAHIA_URL}

echo " == Environment warmup complete =="

mkdir /tmp/results
mkdir /tmp/results/reports

echo "== Run tests =="
# The additional settings is useful when you have to get dependencies from internal repositories
mvn -fae -Pmodule-integration-tests -Djahia.test.url=${TEST_URL} clean verify
if [[ $? -eq 0 ]]; then
  echo "success" > /tmp/results/test_success
  cp /tmp/target/surefire-reports/* /tmp/results/reports/
  cp /tmp/target/site/surefire-report.html /tmp/results/reports/
  exit 0
else
  echo "failure" > /tmp/results/test_failure
  cp /tmp/target/surefire-reports/* /tmp/results/reports/
  cp /tmp/target/site/surefire-report.html /tmp/results/reports/
  exit 1
fi

# After the test ran, we're dropping a marker file to indicate if the test failed or succeeded based on the script test command exit code
