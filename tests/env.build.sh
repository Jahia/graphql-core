#!/bin/bash
# This script can be used to manually build the docker images necessary to run the tests
# It should be executed from the tests folder

# It assumes that you previously built the module you're going to be testing
#   and that the modules artifacts are located one level up

# Copy the root POM
if [[ -e ../pom.xml ]]; then
	cp ../pom.xml ./pom.xml
fi

# Copy the Maven settings
if [ ! -d ./.circleci ]; then
	mkdir ./.circleci
fi
cp -R ../.circleci/.circleci.settings.xml ./.circleci/.circleci.settings.xml

# Copy subprojects
if [ ! -d ./graphql-dxm-provider ]; then
	mkdir ./graphql-dxm-provider
fi
cp -R ../graphql-dxm-provider/* ./graphql-dxm-provider/

if [ ! -d ./graphql-extension-example ]; then
	mkdir ./graphql-extension-example
fi
cp -R ../graphql-extension-example/* ./graphql-extension-example/

if [ ! -d ./graphql-test ]; then
	mkdir ./graphql-test
fi
cp -R ../graphql-test/* ./graphql-test/

# Copy the artifacts previously build locally by the CI tool
if [ ! -d ./artifacts ]; then
  mkdir -p ./artifacts
fi

if [[ -e ../target ]]; then
    cp -R graphql-dxm-provider/target/* ./artifacts/
    cp -R graphql-test/target/* ./artifacts/
    cp ./artifacts/*SNAPSHOT.jar ./artifacts/
fi

docker build -t jahia/graphql-core:latest .