#!/bin/bash
# This script can be used to warmup the environment and execute the tests
# It is used by the docker image at startup

source ./set-env.sh

yarn e2e:debug
