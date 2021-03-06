#!/bin/bash

VAGRANT_STATUS=`vagrant status`

echo $VAGRANT_STATUS | grep running 2>&1 > /dev/null
if [[ $? -ne 0 ]]; then
  echo $VAGRANT_STATUS | grep saved 2>&1 > /dev/null
  if [[ $? -eq 0 ]]; then
    vagrant resume
  else
    vagrant up --provider=virtualbox
  fi
fi

set -e

cd release && mvn clean install -P release && cd ..


vagrant provision
vagrant ssh -c "cd /home/vagrant && java -Dawt.useSystemAAFontSettings=on -Dswing.aatext=true -Dsun.java2d.xrender=true -Dsun.java2d.dpiaware=false -jar emuStudio.jar"  -- -X

