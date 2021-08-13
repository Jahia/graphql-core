#!/usr/bin/env bash
# This script can be used to warmup the environment and execute the tests
# It is used by the docker image at startup

source ./set-env.sh

START_TIME=$SECONDS

# Printing the most important environment variable
# We cannot do a printenv since it would also contain credentials that we don't want to show in logs
# Note: Default credentials for Jahia (such as SUPER_USER_PASSWORD) are not critical and can be shown in logs
echo " == Printing the most important environment variables"
echo " MANIFEST: ${MANIFEST}"
echo " TESTS_IMAGE: ${TESTS_IMAGE}"
echo " JAHIA_IMAGE: ${JAHIA_IMAGE}"
echo " JAHIA_CLUSTER_ENABLED: ${JAHIA_CLUSTER_ENABLED}"
echo " MODULE_ID: ${MODULE_ID}"
echo " JAHIA_URL: ${JAHIA_URL}"
echo " JAHIA_HOST: ${JAHIA_HOST}"
echo " JAHIA_PORT: ${JAHIA_PORT}"
echo " JAHIA_PORT_KARAF: ${JAHIA_PORT_KARAF}"
echo " JAHIA_USERNAME: ${JAHIA_USERNAME}"
echo " JAHIA_PASSWORD: ${JAHIA_PASSWORD}"
echo " JAHIA_USERNAME_TOOLS: ${JAHIA_USERNAME_TOOLS}"
echo " JAHIA_PASSWORD_TOOLS: ${JAHIA_PASSWORD_TOOLS}"
echo " SUPER_USER_PASSWORD: ${SUPER_USER_PASSWORD}"
echo " TIMEZONE: ${TIMEZONE}"
echo " WORKSPACE_EDIT: ${WORKSPACE_EDIT}"

if [[ ${JAHIA_URL} =~ .*/$ ]]; then
  JAHIA_URL=$(echo ${JAHIA_URL} | sed 's/.$//')
fi
echo " == Using JAHIA_URL: ${JAHIA_URL}"
TEST_URL="${JAHIA_URL}/cms"

echo " == Using TEST_URL: ${TEST_URL}"

echo " == Content of the tests folder"
ls -lah

echo " == Waiting for Jahia to startup"
while [[ "$(curl -s -o /dev/null -w ''%{http_code}'' ${JAHIA_URL}/cms/login)" != "200" ]];
  do sleep 5;
done
ELAPSED_TIME=$(($SECONDS - $START_TIME))
echo " == Jahia became alive in ${ELAPSED_TIME} seconds"

echo "$(date +'%d %B %Y - %k:%M') [JAHIA_CLUSTER_ENABLED] == Value: ${JAHIA_CLUSTER_ENABLED} =="
if [[ "${JAHIA_CLUSTER_ENABLED}" == "true" ]]; then
    echo "$(date +'%d %B %Y - %k:%M') [JAHIA_CLUSTER_ENABLED] == Jahia is running in cluster =="
    echo "$(date +'%d %B %Y - %k:%M') [JAHIA_CLUSTER_ENABLED] == Pausing for 60s to give cluster nodes time to start =="
    sleep 60
fi

mkdir -p /tmp/run-artifacts

# Add the credentials to a temporary manifest for downloading files
# Execute jobs listed in the manifest
# If the file doesn't exist, we assume it is a URL and we download it locally
if [[ -e ${MANIFEST} ]]; then
  cp ${MANIFEST} /tmp/run-artifacts
else
  echo "Downloading: ${MANIFEST}"
  curl ${MANIFEST} --output /tmp/run-artifacts/curl-manifest
  MANIFEST="curl-manifest"
fi

echo " == Content of the run-artifacts folder"
ls -lah /tmp/run-artifacts

sed -i -e "s/NEXUS_USERNAME/$(echo ${NEXUS_USERNAME} | sed -e 's/\\/\\\\/g; s/\//\\\//g; s/&/\\\&/g')/g" /tmp/run-artifacts/${MANIFEST}
sed -i -e "s/NEXUS_PASSWORD/$(echo ${NEXUS_PASSWORD} | sed -e 's/\\/\\\\/g; s/\//\\\//g; s/&/\\\&/g')/g" /tmp/run-artifacts/${MANIFEST}
sed -i "" -e "s/JAHIA_VERSION/${JAHIA_VERSION}/g" /tmp/run-artifacts/${MANIFEST}

echo "$(date +'%d %B %Y - %k:%M') == Executing manifest: ${MANIFEST} =="
curl -u root:${SUPER_USER_PASSWORD} -X POST ${JAHIA_URL}/modules/api/provisioning --form script="@/tmp/run-artifacts/${MANIFEST};type=text/yaml"
if [[ $? -eq 1 ]]; then
  echo "PROVISIONING FAILURE - EXITING SCRIPT, NOT RUNNING THE TESTS"
  echo "failure" > /tmp/results/test_failure
  exit 1
fi

if [[ -d artifacts/ ]]; then
  # If we're building the module (and manifest name contains build), then we'll end up pushing that module individually
  # The artifacts folder is created by the build stage, when running in snapshot the docker container is not going to contain that folder
  cd artifacts/
  echo "$(date +'%d %B %Y - %k:%M') == Content of the artifacts/ folder"
  ls -lah
  if [[ -d build-dependencies/ ]]; then
    cd build-dependencies/
    echo "$(date +'%d %B %Y - %k:%M') == Displaying the content of artifacts/build-dependencies prior to files rename to order installation"
    ls -lah
    # Files need to be uploaded in a particular order
    for f in database-connector* ; do mv -- "$f" "01_$f" ; done
    for f in elasticsearch-connector* ; do mv -- "$f" "02_$f" ; done
    echo "$(date +'%d %B %Y - %k:%M') == Files have been renamed"
    ls -lah
    echo "$(date +'%d %B %Y - %k:%M') [MODULE_DEPENDENCY_INSTALL] == Will start submitting files"
    # Installing the files
    for file in $(ls -1 *.jar | sort -n)
    do
      echo "$(date +'%d %B %Y - %k:%M') [MODULE_DEPENDENCY_INSTALL] == Submitting module from: $file =="
      curl -u root:${SUPER_USER_PASSWORD} -X POST ${JAHIA_URL}/modules/api/provisioning --form script='[{"installAndStartBundle":"'"$file"'"}]' --form file=@$file
      echo "$(date +'%d %B %Y - %k:%M') [MODULE_DEPENDENCY_INSTALL] == Module submitted =="
    done
    cd ..
  fi
  echo "$(date +'%d %B %Y - %k:%M') [MODULE_INSTALL] == Will start submitting files"
  for file in $(ls -1 *-SNAPSHOT.jar | sort -n)
  do
    echo "$(date +'%d %B %Y - %k:%M') [MODULE_INSTALL] == Submitting module from: $file =="
    curl -u root:${SUPER_USER_PASSWORD} -X POST ${JAHIA_URL}/modules/api/provisioning --form script='[{"installAndStartBundle":"'"$file"'"}]' --form file=@$file
    echo "$(date +'%d %B %Y - %k:%M') [MODULE_INSTALL] == Module submitted =="
  done
  cd ..
fi

echo "$(date +'%d %B %Y - %k:%M') [ENABLE_SITE] == Start augmented-search-ui and enable on digitall =="
curl -u root:${SUPER_USER_PASSWORD} -X POST ${JAHIA_URL}/modules/api/provisioning --form script='[{"startBundle":"augmented-search-ui"}, {"enable":"augmented-search-ui", "site": "digitall"}]'
echo "$(date +'%d %B %Y - %k:%M') [ENABLE_SITE] == Enable augmented-search-ui on digitall completed=="

cd ./assets
for file in $(ls -1 script-* | sort -n)
do
  echo "$(date +'%d %B %Y - %k:%M') [SCRIPT] == Submitting script: $file =="
  curl -u root:${SUPER_USER_PASSWORD} -X POST ${JAHIA_URL}/modules/api/provisioning --form script='[{"executeScript":"'"$file"'"}]' --form file=@$file
  echo "$(date +'%d %B %Y - %k:%M') [SCRIPT] == Script executed =="
done

echo "$(date +'%d %B %Y - %k:%M') [SITE] == Installing digitall-1 =="
curl -u root:${SUPER_USER_PASSWORD} -X POST ${JAHIA_URL}/modules/api/provisioning --form script='[{"importSite":"'"site-01-digitall-1-jahia8.zip"'"}]' --form file=@site-01-digitall-1-jahia8.zip
curl -u root:${SUPER_USER_PASSWORD} -X POST ${JAHIA_URL}/modules/api/provisioning --form script='[{"import":"'"import-01-digitall-1-roles.zip"'"}]' --form file=@import-01-digitall-1-roles.zip
curl -u root:${SUPER_USER_PASSWORD} -X POST ${JAHIA_URL}/modules/api/provisioning --form script='[{"import":"'"import-01-digitall-1-users.zip"'"}]' --form file=@import-01-digitall-1-users.zip
echo "$(date +'%d %B %Y - %k:%M') [SITE] == Site imported =="

# TO REMOVE (tmp installation of server-availability-manager)
for file in $(ls -1 *.jar | sort -n)
do
  echo "$(date +'%d %B %Y - %k:%M') [SAM-MODULE] == Submitting module: $file =="
  curl -u root:${SUPER_USER_PASSWORD} -X POST ${JAHIA_URL}/modules/api/provisioning --form script='[{"installAndStartBundle":"'"$file"'"}]' --form file=@$file
  echo "$(date +'%d %B %Y - %k:%M') [SAM-MODULE] == module submitted =="
done
cd ..
echo "$(date +'%d %B %Y - %k:%M') == Environment warmup complete =="

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
  echo "failure" > /tmp/results/test_failure
  exit 1
fi

mkdir /tmp/results
mkdir /tmp/results/reports

echo "$(date +'%d %B %Y - %k:%M') == Run tests =="
# The additional settings is useful when you have to get dependencies from internal repositories
mvn -fae -Pmodule-integration-tests -Djahia.test.url=${TEST_URL} clean verify
if [[ $? -eq 0 ]]; then
  echo "$(date +'%d %B %Y - %k:%M') == Full execution successful =="
  echo "success" > /tmp/results/test_success
  cp /tmp/target/surefire-reports/* /tmp/results/reports/
  cp /tmp/target/site/surefire-report.html /tmp/results/reports/
  exit 0
else
  echo "$(date +'%d %B %Y - %k:%M') == One or more failed tests =="
  echo "failure" > /tmp/results/test_failure
  cp /tmp/target/surefire-reports/* /tmp/results/reports/
  cp /tmp/target/site/surefire-report.html /tmp/results/reports/
  exit 1
fi

# After the test ran, we're dropping a marker file to indicate if the test failed or succeeded based on the script test command exit code
