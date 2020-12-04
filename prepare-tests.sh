#!/bin/bash
# This script can be used to warmup the environment and execute the tests
# It is used by the docker image at startup

cp pom.xml ./tests/pom.xml

mkdir ./tests/.circleci
cp -R ./.circleci/.circleci.settings.xml ./tests/.circleci/.circleci.settings.xml

mkdir ./tests/graphql-dxm-provider
cp -R ./graphql-dxm-provider/* ./tests/graphql-dxm-provider/

mkdir ./tests/graphql-extension-example
cp -R ./graphql-extension-example/* ./tests/graphql-extension-example/

mkdir ./tests/graphql-test
cp -R ./graphql-test/* ./tests/graphql-test/
