rem This script controls the startup of the container environment

call set-env.bat

echo " == Printing the most important environment variables"
echo " MANIFEST: %MANIFEST%"
echo " TESTS_IMAGE: %TESTS_IMAGE%"
echo " JAHIA_IMAGE: %JAHIA_IMAGE%"
echo " LDAP_TAG: %LDAP_TAG%"

docker-compose pull jahia dockerldap
docker-compose up -d --renew-anon-volumes --remove-orphans --force-recreate jahia dockerldap
