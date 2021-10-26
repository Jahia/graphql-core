#!/usr/bin/env bash

# This script is executed after the run
# It is mostly useful to export logs files

source ./set-env.sh

docker logs dockerldap > ./artifacts/results/dockerldap.log



