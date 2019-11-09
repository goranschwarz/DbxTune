#! /bin/bash

dbxApp=${1:-"DbxCentral="}


pid=$(ps -ef | egrep "java .*${dbxApp}" | grep -v grep | awk '{print $2}')

if [ -z "${pid}" ]
then
	echo "Sorry: can't find any DbxTune application of type: ${dbxApp}"
	exit 1
fi

if [ "${dbxApp}" == "DbxCentral=" ]
then
	threadName='"CentralPcsWriterHandler"'
else
	threadName='"PersistentCounterHandler"'
fi

printf "\n\n\n"
printf "###########################\n"
printf "Java STACK: for PID=${pid}\n"
date
printf "###########################\n"
ps ${pid}
printf "###########################\n"
#jstack ${pid} | sed -n -e '/MVStore background writer/,/^$/p' -e '/"PersistentCounterHandler"/,/^$/p'
#jstack ${pid} | sed -n -e '/MVStore background writer/,/^$/p' -e '/"CentralPcsWriterHandler"/,/^$/p'
jstack ${pid} | sed -n -e '/MVStore background writer/,/^$/p' -e '/'"${threadName}"'/,/^$/p'
printf "\n\n\n"

