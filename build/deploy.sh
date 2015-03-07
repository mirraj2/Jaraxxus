#!/bin/sh

echo Building jar.
/usr/local/Cellar/ant/1.9.4/libexec/bin/ant

echo Uploading jar...
scp server.jar root@jaraxxus.com:~/server.jar

echo Restarting server
ssh root@jaraxxus.com 'screen -S jaraxxus -X quit;screen -S jaraxxus -d -m java -jar server.jar'

echo Done.
