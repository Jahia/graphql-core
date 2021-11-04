#!/bin/bash
# This script can be used to warmup the environment and execute the tests
# It is used by the docker image at startup

source ./set-env.sh

#!/usr/bin/env bash
START_TIME=$SECONDS

echo " == Using MANIFEST: ${MANIFEST}"
echo " == Using JAHIA_URL= ${JAHIA_URL}"
echo " == Using Node version: $(node -v)"
echo " == Using yarn version: $(yarn -v)"

echo " == Waiting for Jahia to startup"
while [[ "$(curl -s -o /dev/null -w ''%{http_code}'' ${JAHIA_URL}/cms/login)" != "200" ]];
  do sleep 5;
done

ELAPSED_TIME=$(($SECONDS - $START_TIME))
echo " == Jahia became alive in ${ELAPSED_TIME} seconds"

mkdir -p ./run-artifacts
mkdir -p ./results

# Add the credentials to a temporary manifest for downloading files
# Execute jobs listed in the manifest
# If the file doesn't exist, we assume it is a URL and we download it locally
if [[ -e ${MANIFEST} ]]; then
  cp ${MANIFEST} ./run-artifacts
else
  echo "Downloading: ${MANIFEST}"
  curl ${MANIFEST} --output ./run-artifacts/curl-manifest
  MANIFEST="curl-manifest"
fi
sed -i -e "s/NEXUS_USERNAME/$(echo ${NEXUS_USERNAME} | sed -e 's/\\/\\\\/g; s/\//\\\//g; s/&/\\\&/g')/g" ./run-artifacts/${MANIFEST}
sed -i -e "s/NEXUS_PASSWORD/$(echo ${NEXUS_PASSWORD} | sed -e 's/\\/\\\\/g; s/\//\\\//g; s/&/\\\&/g')/g" ./run-artifacts/${MANIFEST}
sed -i "" -e "s/JAHIA_VERSION/${JAHIA_VERSION}/g" ./run-artifacts/${MANIFEST}

echo "$(date +'%d %B %Y - %k:%M') == Executing manifest: ${MANIFEST} =="
curl -u root:${SUPER_USER_PASSWORD} -X POST ${JAHIA_URL}/modules/api/provisioning --form script="@./run-artifacts/${MANIFEST};type=text/yaml"
echo
if [[ $? -eq 1 ]]; then
  echo "PROVISIONING FAILURE - EXITING SCRIPT, NOT RUNNING THE TESTS"
  echo "failure" > ./results/test_failure
  exit 1
fi

if [[ -d artifacts/ && $MANIFEST == *"build"* ]]; then
  # If we're building the module (and manifest name contains build), then we'll end up pushing that module individually
  # The artifacts folder is created by the build stage, when running in snapshot the docker container is not going to contain that folder
  cd artifacts/
  echo "$(date +'%d %B %Y - %k:%M') == Content of the artifacts/ folder"
  ls -lah
  echo "$(date +'%d %B %Y - %k:%M') [MODULE_INSTALL] == Will start submitting files"
  for file in $(ls -1 *-SNAPSHOT.jar | sort -n)
  do
    echo "$(date +'%d %B %Y - %k:%M') [MODULE_INSTALL] == Submitting module from: $file =="
    curl -u root:${SUPER_USER_PASSWORD} -X POST ${JAHIA_URL}/modules/api/provisioning --form script='[{"installAndStartBundle":"'"$file"'", "forceUpdate":true}]' --form file=@$file
    echo
    echo "$(date +'%d %B %Y - %k:%M') [MODULE_INSTALL] == Module submitted =="
  done
  cd ..
fi

echo "$(date +'%d %B %Y - %k:%M') == Fetching the list of installed modules =="
./node_modules/jahia-reporter/bin/run utils:modules \
  --moduleId="${MODULE_ID}" \
  --jahiaUrl="${JAHIA_URL}" \
  --jahiaPassword="${SUPER_USER_PASSWORD}" \
  --filepath="results/installed-jahia-modules.json"
echo "$(date +'%d %B %Y - %k:%M') == Modules fetched =="
INSTALLED_MODULE_VERSION=$(cat results/installed-jahia-modules.json | jq '.module.version')
if [[ $INSTALLED_MODULE_VERSION == "UNKNOWN" ]]; then
  echo "$(date +'%d %B %Y - %k:%M') ERROR: Unable to detect module: ${MODULE_ID} on the remote system "
  echo "$(date +'%d %B %Y - %k:%M') ERROR: The Script will exit"
  echo "$(date +'%d %B %Y - %k:%M') ERROR: Tests will NOT run"
  echo "failure" > ./results/test_failure
  exit 1
fi

echo "$(date +'%d %B %Y - %k:%M') == Run tests =="
yarn e2e:ci
if [[ $? -eq 0 ]]; then
  echo "$(date +'%d %B %Y - %k:%M') == Full execution successful =="
  echo "success" > ./results/test_success
  yarn report:merge; yarn report:html
  exit 0
else
  echo "$(date +'%d %B %Y - %k:%M') == One or more failed tests =="
  echo "failure" > ./results/test_failure
  yarn report:merge; yarn report:html
  exit 1
fi
