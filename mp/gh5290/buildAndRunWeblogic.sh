#!/bin/bash
cd ./weblogic

# Attempt Oracle container registry login.
# You need to accept the licence agreement for Weblogic Server at https://container-registry.oracle.com/
docker login container-registry.oracle.com

docker build -t wls-admin .

docker run --rm -d \
  -p 7001:7001 \
  -p 9002:9002 \
  --name wls-admin \
  --hostname wls-admin \
  wls-admin

printf "Waiting for WLS to start ."
while true;
do
  if docker logs wls-admin | grep -q "Server state changed to RUNNING"; then
    break;
  fi
  printf "."
  sleep 5
done
printf " [READY]\n"

echo Deploying example JMS queues
docker exec wls-admin \
/bin/bash \
/u01/oracle/wlserver/common/bin/wlst.sh \
/u01/oracle/setupTestJMSQueue.py;

echo Example JMS queues deployed!
echo Console avaiable at http://localhost:7001/console with admin/Welcome1
echo 'Stop Weblogic server with "docker stop wls-admin"'