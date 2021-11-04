#!/bin/bash
# This script can be used to warmup the environment and execute the tests
# It is used by the docker image at startup

call set-env.bat

echo " == Using MANIFEST: %MANIFEST%"
echo " == Using JAHIA_URL= %JAHIA_URL%"

yarn e2e:debug
