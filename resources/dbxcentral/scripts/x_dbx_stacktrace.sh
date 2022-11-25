#! /bin/bash

echo "Usage: $(basename $0) [sleepTimeInSec]"

sleepTime=${1:-5}
echo "Sleeptime will be: ${sleepTime}"

while true
do
	echo ""
	echo ""
	echo "###############################################################"
	date
	echo "###############################################################"

	pid=$(jps | grep DbxTuneCentral | awk '{print $1}')
	jstack ${pid} | sed -n -e '/"CentralPcsWriterHandler"/,/^$/p' -e '/"MVStore background writer/,/^$/p'

	sleep ${sleepTime}
done

